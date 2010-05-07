/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
  *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ejb3.embedded.impl.base;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.bootstrap.api.mc.server.MCBasedServer;
import org.jboss.bootstrap.api.mc.server.MCServer;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.client.spi.main.MainDeployer;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.vfs.spi.client.VFSDeployment;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.ejb3.embedded.api.EJBDeploymentException;
import org.jboss.ejb3.embedded.api.JBossEJBContainer;
import org.jboss.ejb3.embedded.impl.base.scanner.ClassPathEjbJarScanner;
import org.jboss.kernel.Kernel;
import org.jboss.logging.Logger;
import org.jboss.reloaded.shrinkwrap.api.ShrinkWrapDeployer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Base for JBoss {@link EJBContainer}s.  Provides
 * support for deployment operations backed by a supplied {@link MCServer}
 * to be provided by concrete implementations.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public abstract class JBossEJBContainerBase extends EJBContainer implements JBossEJBContainer
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(JBossEJBContainerBase.class);

   /**
    * Name in MC under which the {@link MainDeployer} is bound
    */
   private static final String MC_BIND_NAME_MAIN_DEPLOYER = "MainDeployer";

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Underlying deployer installed into the {@link JBossEJBContainerBase#mcServer}
    */
   private final MainDeployer deployer;

   /**
    * Underlying MC Server
    */
   private final MCBasedServer<?, ?> mcServer;

   /**
    * All deployments currently installed via this container
    */
   private final Set<Deployment> deployments;

   /**
    * @see @see http://community.jboss.org/message/540998
    */
   @Deprecated
   private final ShrinkWrapDeployer shrinkWrapDeployer;

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   protected JBossEJBContainerBase(final Map<?, ?> properties, final String[] modules, final MCBasedServer<?, ?> server)
   {
      // Precondition checks
      if (server == null)
      {
         throw new IllegalArgumentException("MC Server must be specified");
      }

      // Obtain MainDeployer
      final Kernel kernel = server.getKernel();
      final MainDeployer deployer = (MainDeployer) kernel.getController().getInstalledContext(
            MC_BIND_NAME_MAIN_DEPLOYER).getTarget();
      assert deployer != null : "Main Deployer found in the Kernel was null";

      // Obtain ShrinkWrapDeployer
      final ShrinkWrapDeployer shrinkWrapDeployer = (ShrinkWrapDeployer) kernel.getController().getContextByClass(
            ShrinkWrapDeployer.class).getTarget();
      assert shrinkWrapDeployer != null : "ShrinkWrapDeployer found in Kernel was null";

      log.info("Started JBoss Embedded " + EJBContainer.class.getSimpleName());
      log.info("Modules for deployment: " + Arrays.asList(modules));

      // Set
      this.mcServer = server;
      this.deployer = deployer;
      this.deployments = new HashSet<Deployment>();
      this.shrinkWrapDeployer = shrinkWrapDeployer;
   }

   protected JBossEJBContainerBase(final Map<?, ?> properties, final MCServer server)
   {
      this(properties, ClassPathEjbJarScanner.getEjbJars(), server);
   }

   //-------------------------------------------------------------------------------------||
   // Functional Methods -----------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Deploys the specified {@link Deployment}s into the Container
    * 
    * @param deployments One or more {@link Deployment}s to process
    * @throws DeploymentException If an error occured in deployment
    * @throws IllegalArgumentException If at least one {@link Deployment} was not specified
    */
   protected void deploy(final Deployment... deployments) throws EJBDeploymentException, IllegalArgumentException
   {
      // Precondition checks
      if (deployments == null || deployments.length == 0)
      {
         throw new IllegalArgumentException("At least one deployment must be specified");
      }

      // Mark deployments added
      final Set<Deployment> deploymentsAdded = new HashSet<Deployment>();

      // Add all deployments
      for (final Deployment deployment : deployments)
      {
         if (log.isTraceEnabled())
         {
            log.tracef("Adding deployment: ", deployment);
         }
         try
         {
            // Add to the deployer
            deployer.addDeployment(deployment);

            // Mark we've added this
            deploymentsAdded.add(deployment);
         }
         catch (final DeploymentException mainDeploymentException)
         {
            // Remove the pending deployments
            for (final Deployment pending : deploymentsAdded)
            {
               try
               {
                  deployer.removeDeployment(pending);
               }
               catch (final DeploymentException pendingDeploymentRemovalException)
               {
                  log.warn("Could not back out pending deployment due to " + pendingDeploymentRemovalException
                        + " while handing deployment error: " + mainDeploymentException);
               }
            }

            // Translate exception to our API
            throw EJBDeploymentException
                  .newInstance("Could not add deployment: " + deployment, mainDeploymentException);
         }
      }

      // Process and ensure everything's OK
      deployer.process();
      try
      {
         deployer.checkComplete();
      }
      catch (final DeploymentException e)
      {
         throw EJBDeploymentException.newInstance("Processing the pending deployments resulted in error", e);
      }

      // Mark we've got new deployments
      this.deployments.addAll(deploymentsAdded);

   }

   /**
    * Deploys the specified {@link URL}s into the Container
    * 
    * @param urls URLs to deploy; at least one must be specified
    * @throws DeploymentException If an error occurred during deployment
    * @throws IOException
    * @throws IllegalArgumentException
    */
   protected void deploy(final URL... urls) throws EJBDeploymentException, IllegalArgumentException
   {
      // Precondition checks
      if (urls == null || urls.length == 0)
      {
         throw new IllegalArgumentException("At least one URL for deployment must be specified");
      }

      // Hold the deployments
      final Deployment[] deployments = new Deployment[urls.length];

      // For each URL, make a Deployment
      for (int i = 0; i < urls.length; i++)
      {
         final URL url = urls[i];
         final VirtualFile root;
         try
         {
            root = VFS.getChild(url);
         }
         catch (final URISyntaxException urise)
         {
            throw new RuntimeException("Could not create a virtual file to deploy from URL: " + url, urise);
         }
         final VFSDeployment deployment = VFSDeploymentFactory.getInstance().createVFSDeployment(root);
         deployments[i] = deployment;
      }

      // Delegate to real deployment
      this.deploy(deployments);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.ejb3.embedded.api.JBossEJBContainer#deploy(org.jboss.shrinkwrap.api.Archive<?>[])
    */
   @Override
   public void deploy(final Archive<?>... archives) throws EJBDeploymentException, IllegalArgumentException
   {
      // Precondition checks
      if (archives == null)
      {
         throw new IllegalArgumentException("archives must be supplied");
      }

      // Deploy
      try
      {
         shrinkWrapDeployer.deploy(archives);
      }
      catch (final DeploymentException e)
      {
         // Translate
         throw EJBDeploymentException.newInstance("Could not deploy " + Arrays.asList(archives), e);
      }
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.ejb3.embedded.api.JBossEJBContainer#undeploy(org.jboss.shrinkwrap.api.Archive<?>[])
    */
   @Override
   public void undeploy(final Archive<?>... archives) throws EJBDeploymentException, IllegalArgumentException
   {
      // Precondition checks
      if (archives == null)
      {
         throw new IllegalArgumentException("archives must be supplied");
      }

      // Undeploy
      try
      {
         shrinkWrapDeployer.undeploy(archives);
      }
      catch (final DeploymentException e)
      {
         // Translate
         throw EJBDeploymentException.newInstance("Could not undeploy " + Arrays.asList(archives), e);
      }
   }

   /**
    * Exposes the underlying {@link MCServer} to children; take care not to expose this to 
    * end users (clients).
    * 
    * @return
    */
   protected MCBasedServer<?, ?> getMCServer()
   {
      return mcServer;
   }

   /**
    * {@inheritDoc}
    * @see javax.ejb.embeddable.EJBContainer#getContext()
    */
   @Override
   public Context getContext()
   {
      // We could return this assuming the naming system is up, as one idea
      try
      {
         return new InitialContext();
      }
      catch (final NamingException e)
      {
         throw new RuntimeException("Could not create new naming context", e);
      }
   }

}

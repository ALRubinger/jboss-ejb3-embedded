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
package org.jboss.ejb3.embedded.impl.as;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.embeddable.EJBContainer;

import org.jboss.beans.metadata.plugins.builder.BeanMetaDataBuilderFactory;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.bootstrap.api.descriptor.BootstrapDescriptor;
import org.jboss.bootstrap.api.lifecycle.LifecycleState;
import org.jboss.bootstrap.api.mc.server.MCServer;
import org.jboss.bootstrap.api.mc.server.MCServerFactory;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.client.spi.main.MainDeployer;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.reloaded.api.ReloadedDescriptors;
import org.jboss.reloaded.shrinkwrap.api.ShrinkWrapDeployer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.vfs.VFS;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases ensuring that the Embedded {@link EJBContainer}
 * can integrate with an existing {@link MCServer} in "hypervised"
 * mode, as is the case within the JBoss Application Server.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class EmbeddedEJBContainerExistingMCServerIntegrationUnitTest
{
   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(EmbeddedEJBContainerExistingMCServerIntegrationUnitTest.class
         .getName());

   /**
    * Existing MC Server to which the Embedded {@link EJBContainer} will attach
    */
   private static MCServer server;

   /**
    * Filename of the jboss-beans XML which installs the {@link EmbeddedEJBContainerASAdaptor}
    */
   private static final String FILENAME_EMBEDDED_EJB_AS_ADAPTOR_XML = "embedded-ejb-as-adaptor-jboss-beans.xml";

   /**
    * Deployer used by the {@link MCServer}
    */
   private static MainDeployer deployer;

   /**
    * Deployment used in installing the AS / EJBContainer adaptor
    */
   private static Deployment deployment;

   //-------------------------------------------------------------------------------------||
   // Lifecycle --------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Setup JBossXB
    * TODO @see comments below so that this step is not necessary
    */
   @BeforeClass
   public static void setupJBossXb()
   {
      AccessController.doPrivileged(new PrivilegedAction<Void>()
      {
         public Void run()
         {
            // Must use unordered sequence else JBossXB will explode
            //TODO Define a proper vfs.xml which is properly ordered
            System.setProperty(NAME_SYSPROP_JBOSSXB_IGNORE_ORDER, VALUE_SYSPROP_JBOSSXB_IGNORE_ORDER);
            return null;
         }
      });
   }

   /**
    * Name of the system property signaling JBossXB to ignore order
    */
   private static final String NAME_SYSPROP_JBOSSXB_IGNORE_ORDER = "xb.builder.useUnorderedSequence";

   /**
    * Value to set for JBossXB ordering
    */
   private static final String VALUE_SYSPROP_JBOSSXB_IGNORE_ORDER = "true";

   @BeforeClass
   public static void startServer() throws Exception
   {
      // Create a server
      final MCServer mcServer = MCServerFactory.createServer();

      // Add the required bootstrap descriptors
      final List<BootstrapDescriptor> descriptors = mcServer.getConfiguration().getBootstrapDescriptors();
      descriptors.add(ReloadedDescriptors.getClassLoadingDescriptor());
      descriptors.add(ReloadedDescriptors.getVdfDescriptor());

      log.info("Using bootstrap descriptors:" + descriptors);

      // Set
      server = mcServer;

      // Start
      server.start();

      // Install the server into MC again as another alias (AS does not have the type unique, so we ensure
      // that we're correctly doing this by name)
      final BeanMetaDataBuilder bmdb = BeanMetaDataBuilderFactory.createBuilder("JBossServer-alias", server.getClass()
            .getName());
      try
      {
         server.getKernel().getController().install(bmdb.getBeanMetaData());
      }
      catch (final Throwable e1)
      {
         throw new RuntimeException("Could not install JBossServer as alias", e1);
      }

      // Install a mock ShrinkWrapDeployer (we don't need a real one here)
      final BeanMetaDataBuilder builder = BeanMetaDataBuilderFactory.createBuilder(
            MockShrinkWrapDeployer.class.getName()).setName(MockShrinkWrapDeployer.class.getSimpleName());
      try
      {
         server.getKernel().getController().install(builder.getBeanMetaData());
      }
      catch (final Throwable e)
      {
         throw new Exception("Could not install ShrinkWrapDeployer", e);
      }

      // Install the AS / EJB Embedded Adaptor
      final URL codebase = EmbeddedEJBContainerExistingMCServerIntegrationUnitTest.class.getProtectionDomain()
            .getCodeSource().getLocation();
      final URL classes = new URL(codebase, "../classes/");
      final URL adaptorUrl = new URL(classes, FILENAME_EMBEDDED_EJB_AS_ADAPTOR_XML);

      final Deployment deployment = VFSDeploymentFactory.getInstance().createVFSDeployment(VFS.getChild(adaptorUrl));
      EmbeddedEJBContainerExistingMCServerIntegrationUnitTest.deployment = deployment;
      final MainDeployer deployer = (MainDeployer) server.getKernel().getController().getInstalledContext(
            "MainDeployer").getTarget();
      EmbeddedEJBContainerExistingMCServerIntegrationUnitTest.deployer = deployer;

      deployer.deploy(deployment);
      deployer.process();
      deployer.checkComplete();
   }

   /**
    * Stops the server before the test comes down
    * @throws Exception
    */
   @AfterClass
   public static void stopServer() throws Exception
   {
      // If started, stop the server
      if (server != null && server.getState().equals(LifecycleState.STARTED))
      {
         deployer.undeploy(deployment);
         deployer.process();
         deployer.checkComplete();
         server.stop();
      }
   }

   //-------------------------------------------------------------------------------------||
   // Tests ------------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates an {@link JBossASEmbeddedEJBContainer}
    */
   @Test
   public void createEJBContainer() throws Exception
   {
      final EJBContainer container = EJBContainer.createEJBContainer();
      Assert.assertNotNull("Container was null", container);
      Assert.assertTrue("Container is not of expected type", container instanceof JBossASEmbeddedEJBContainer);
   }

   //-------------------------------------------------------------------------------------||
   // Internal Helpers -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Mock NOOP {@link ShrinkWrapDeployer} implementation
    */
   public static final class MockShrinkWrapDeployer implements ShrinkWrapDeployer
   {

      @Override
      public void deploy(final Archive<?>... archives) throws IllegalArgumentException, DeploymentException
      {
         //NO-OP

      }

      @Override
      public void undeploy(final Archive<?>... archives) throws IllegalArgumentException, DeploymentException
      {
         //NO-OP

      }

   }

}

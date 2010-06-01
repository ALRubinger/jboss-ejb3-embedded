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

package org.jboss.ejb3.embedded.impl.shrinkwrap;

import java.util.Arrays;

import javax.naming.Context;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.ejb3.embedded.api.EJBDeploymentException;
import org.jboss.ejb3.embedded.api.JBossEJBContainer;
import org.jboss.ejb3.embedded.api.shrinkwrap.ShrinkWrapEJBContainer;
import org.jboss.ejb3.embedded.spi.JBossEJBContainerProvider;
import org.jboss.kernel.Kernel;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.vdf.api.ShrinkWrapDeployer;

/**
 * Adds ShrinkWrap deployment support (the {@link ShrinkWrapEJBContainer}
 * contract) to a provided {@link JBossEJBContainerProvider} delegate.
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class ShrinkWrapEJBContainerImpl implements ShrinkWrapEJBContainer
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Delegate to which we'll pass {@link JBossEJBContainer} operations
    */
   private final JBossEJBContainer delegate;

   /**
    * Deployer for ShrinkWrap {@link Archive} types
    */
   private final ShrinkWrapDeployer shrinkWrapDeployer;

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates a new {@link ShrinkWrapEJBContainer} using the specified
    * {@link JBossEJBContainerProvider} delegate.
    *  
    * @param delegate
    * @throws IllegalArgumentException
    */
   public ShrinkWrapEJBContainerImpl(final JBossEJBContainerProvider delegate) throws IllegalArgumentException
   {
      // Precondition checks
      if (delegate == null)
      {
         throw new IllegalArgumentException("EJB Container delegate must be specified");
      }

      // Get Kernel
      final Kernel kernel = delegate.getMCServer().getKernel();

      // Obtain ShrinkWrapDeployer
      final ShrinkWrapDeployer shrinkWrapDeployer = (ShrinkWrapDeployer) kernel.getController().getContextByClass(
            ShrinkWrapDeployer.class).getTarget();
      assert shrinkWrapDeployer != null : "ShrinkWrapDeployer found in Kernel was null";

      // Set
      this.delegate = delegate;
      this.shrinkWrapDeployer = shrinkWrapDeployer;

   }

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * {@inheritDoc}
    * @see org.jboss.ejb3.embedded.api.shrinkwrap.ShrinkWrapEJBContainer#deploy(org.jboss.shrinkwrap.api.Archive<?>[])
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
    * @see org.jboss.ejb3.embedded.api.shrinkwrap.ShrinkWrapEJBContainer#undeploy(org.jboss.shrinkwrap.api.Archive<?>[])
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
    * {@inheritDoc}
    * @see org.jboss.ejb3.embedded.api.JBossEJBContainer#close()
    */
   @Override
   public void close()
   {
      delegate.close();

   }

   /**
    * {@inheritDoc}
    * @see org.jboss.ejb3.embedded.api.JBossEJBContainer#getContext()
    */
   @Override
   public Context getContext()
   {
      return delegate.getContext();
   }
}

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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.jboss.bootstrap.api.descriptor.BootstrapDescriptor;
import org.jboss.bootstrap.api.descriptor.UrlBootstrapDescriptor;
import org.jboss.bootstrap.api.lifecycle.LifecycleState;
import org.jboss.bootstrap.api.mc.server.MCServer;
import org.jboss.bootstrap.api.mc.server.MCServerFactory;
import org.jboss.ejb3.embedded.api.EJBDeploymentException;
import org.jboss.ejb3.embedded.api.shrinkwrap.ShrinkWrapEJBContainer;
import org.jboss.ejb3.embedded.impl.base.JBossEJBContainerBase;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.reloaded.api.ReloadedDescriptors;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Ensures that the {@link ShrinkWrapEJBContainerImpl} 
 * implementation is working as contracted by {@link ShrinkWrapEJBContainer}
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class ShrinkWrapEJBContainerImplUnitTest
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * The underlying MC server
    */
   private static final MCServer server = MCServerFactory.createServer();

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * {@link ShrinkWrapEJBContainer} instance under test
    */
   private ShrinkWrapEJBContainer ejbContainer;

   //-------------------------------------------------------------------------------------||
   // Lifecycle --------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Configures and starts the underlying MC server
    */
   @BeforeClass
   public static void startMc() throws Exception
   {

      // Define the descriptors to use to start VDF
      final List<BootstrapDescriptor> descriptors = server.getConfiguration().getBootstrapDescriptors();
      descriptors.add(ReloadedDescriptors.getClassLoadingDescriptor());
      descriptors.add(ReloadedDescriptors.getVdfDescriptor());
      descriptors.add(ReloadedDescriptors.getThreadsDescriptor());
      descriptors.add(new UrlBootstrapDescriptor(Thread.currentThread().getContextClassLoader().getResource(
            "shrinkwrap-deployer-jboss-beans.xml")));

      // Start
      server.start();

   }

   /**
    * Creates the EJB Container under test
    */
   @Before
   public void createEJBContainer()
   {
      ejbContainer = new ShrinkWrapEJBContainerImpl(new TestJBossEJBContainer(new HashMap<Object, Object>(), server,
            new String[]
            {}));
   }

   /**
    * Cleans up and shuts down MC
    * @throws Exception
    */
   @AfterClass
   public static void stopMc() throws Exception
   {
      // Shutdown
      if (server != null && server.getState().equals(LifecycleState.STARTED))
      {
         server.stop();
      }
   }

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Ensures that ShrinkWrap deployments are delegated along as expected
    */
   @Test
   public void shrinkWrapDeployment()
   {
      // Create a test archive which installs a POJO into MC
      final JavaArchive archive = ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(Pojo.class)
            .addResource(
                  new StringAsset(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><deployment xmlns=\"urn:jboss:bean-deployer:2.0\"><bean name=\"Pojo\" class=\""
                              + Pojo.class.getName() + "\" /></deployment>"), "pojo-jboss-beans.xml");

      // Deploy it
      try
      {
         ejbContainer.deploy(archive);
      }
      catch (final EJBDeploymentException e)
      {
         TestCase.fail("Could not deploy " + archive.toString() + ": " + e);
      }

      // Ensure it was deployed
      final KernelController controller = server.getKernel().getController();
      final boolean installed = controller.getContextByClass(Pojo.class) != null;
      Assert.assertTrue("Archive was not deployed", installed);

      // Remove
      try
      {
         ejbContainer.undeploy(archive);
      }
      catch (final EJBDeploymentException e)
      {
         TestCase.fail("Could not undeploy " + archive.toString() + ": " + e);
      }

      // Ensure it was removed
      final boolean removed = controller.getContextByClass(Pojo.class) == null;
      Assert.assertTrue("Archive was not undeployed", removed);

   }

   //-------------------------------------------------------------------------------------||
   // Functional Methods -----------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods ------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Test concrete extension of the 
    */
   private static class TestJBossEJBContainer extends JBossEJBContainerBase
   {

      TestJBossEJBContainer(final Map<?, ?> properties, final MCServer server, final String[] modules)
      {
         super(properties, server, modules);
      }

      /**
       * {@inheritDoc}
       * @see javax.ejb.embeddable.EJBContainer#close()
       */
      @Override
      public void close()
      {
         //NOOP
      }

   }

   /**
    * A simple test Pojo we'll check for installation
    * 
    * 
    * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
    * @version $Revision: $
    */
   public static class Pojo implements Serializable
   {

      /**
       * serialVersionUID
       */
      private static final long serialVersionUID = 1L;

   }
}

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

import java.util.HashMap;
import java.util.Map;

import javax.ejb.embeddable.EJBContainer;
import javax.ejb.spi.EJBContainerProvider;
import javax.naming.Context;

import org.junit.Assert;
import org.junit.Test;

/**
 * Ensures the {@link JBossEJBContainerProviderBase}
 * is working as contracted
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class JBossEJBContainerProviderUnitTest
{

   //-------------------------------------------------------------------------------------||
   // Tests ------------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Ensures that setting the provider property to another 
    * provider than the one in use results in a null
    * {@link EJBContainer}
    */
   @Test
   public void rejectsIncorrectExplicitProviderProperty()
   {
      // Request a container using a dummy provider FQN
      final EJBContainer container = this.createEJBContainerUsingProvider("dummyValue");

      // Ensure we didn't get a container back; we requested a dummy provider value
      Assert.assertNull("Explicit provider property not met by current provider should return a null container",
            container);

   }

   /**
    * Ensures that setting the explicit provider property
    * to the current provider results in creation of an {@link EJBContainer}
    */
   @Test
   public void allowsMatchingExplicitProviderProperty()
   {
      // Request a container using a dummy provider FQN
      final EJBContainer container = this
            .createEJBContainerUsingProvider(TestJBossEJBContainerProvider.class.getName());

      // Ensure we didn't get a container back; we requested a dummy provider value
      Assert.assertNotNull("Explicit provider property set to the current provider should return an EJB Container",
            container);

   }

   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods ------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Uses the test {@link EJBContainerProvider} to create a new EJB Container using the given
    * provider fully-qualified name
    */
   private EJBContainer createEJBContainerUsingProvider(final String providerFqn)
   {
      // Precondition check
      assert providerFqn != null && providerFqn.length() > 0 : "Provider FQN must be specified";

      // Set an explicit provider
      final String providerPropName = EJBContainer.PROVIDER;
      final String providerPropValue = providerFqn;
      final Map<String, String> props = new HashMap<String, String>();
      props.put(providerPropName, providerPropValue);

      // Make the provider
      final EJBContainerProvider provider = new TestJBossEJBContainerProvider();

      // Request a container
      final EJBContainer container = provider.createEJBContainer(props);
      return container;
   }

   /**
    * Concrete {@link EJBContainerProvider} implementation using {@link JBossEJBContainerProviderBase}
    * support.
    */
   private static class TestJBossEJBContainerProvider extends JBossEJBContainerProviderBase
   {

      /**
       * @see org.jboss.ejb3.embedded.impl.base.JBossEJBContainerProviderBase#createEJBContainer(org.jboss.ejb3.embedded.impl.base.JBossEmbeddedContainerStartupParams)
       */
      @Override
      public EJBContainer createEJBContainer(final JBossEmbeddedContainerStartupParams params)
      {
         // Just return some mock
         return new EJBContainer()
         {

            @Override
            public Context getContext()
            {
               return null;
            }

            @Override
            public void close()
            {
            }
         };
      }

   }
}

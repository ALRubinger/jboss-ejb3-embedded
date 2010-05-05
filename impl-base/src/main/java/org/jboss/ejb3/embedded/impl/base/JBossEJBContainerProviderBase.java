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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.embeddable.EJBContainer;
import javax.ejb.spi.EJBContainerProvider;

import org.jboss.logging.Logger;

/**
 * Base support for JBoss {@link EJBContainerProvider}
 * implementations.  Responsible for parsing all properties
 * into an encapsulated {@link JBossEmbeddedContainerStartupParams} object which 
 * will be used to instantiate the real container provider impl.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public abstract class JBossEJBContainerProviderBase implements EJBContainerProvider
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(JBossEJBContainerProviderBase.class);

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * {@inheritDoc}
    * @see javax.ejb.spi.EJBContainerProvider#createEJBContainer(java.util.Map)
    */
   @Override
   public EJBContainer createEJBContainer(Map<?, ?> properties) throws EJBException
   {
      // Normally we'd prohibit null inputs as a precondition check, but the spec doesn't
      // specify that properties are required.  So set new ones.
      if (properties == null)
      {
         properties = Collections.emptyMap();
      }

      /*
       * Ensure we qualify as a valid implementation class
       * 1) Our impl class is equal to the value of "javax.ejb.embeddable.initial"
       * 2) No "javax.ejb.embeddable.provider" property was specified
       * (Note: http://wiki.jcp.org/boards/index.php?t=4468) 
       */
      final String providerProp = EJBContainer.PROVIDER;
      final Object requestedImplClassValue = properties.get(providerProp);
      // If a provider was explicitly-defined
      if (requestedImplClassValue != null)
      {
         // Ensure the right type was provided
         if (!(requestedImplClassValue instanceof String))
         {
            log.warn("Obtained propety \"" + providerProp + "\" should be the String FQN of a provider, instead got: "
                  + requestedImplClassValue + ".  Ignoring.");
         }
         // See if the requested FQN is us
         final String requestedImplClassFqn = (String) requestedImplClassValue;
         if (!this.getClass().getName().equals(requestedImplClassFqn))
         {
            // Requested another provider, return null per spec orders
            log.warn("Got explicit requested provider \"" + requestedImplClassFqn
                  + "\", so returning null from this provider: " + this);
            return null;
         }
      }

      // Create a container
      try
      {
         String modules[] = null;
         if (properties != null)
         {
            Object o = properties.get(EJBContainer.MODULES);
            if (o != null)
            {
               if (o instanceof String)
                  modules = new String[]
                  {(String) o};
               else if (o instanceof Collection<?>)
                  modules = toStringArray(o);
               else
                  throw new EJBException("Illegal type of " + EJBContainer.MODULES + " (" + o.getClass().getName()
                        + ") (EJB 3.1 22.2.2.2)");
            }
         }

         // Create the container from the parameters
         final JBossEmbeddedContainerStartupParams params = new JBossEmbeddedContainerStartupParams(properties, modules);
         return this.createEJBContainer(params);
      }
      catch (final Throwable t)
      {
         if (t instanceof Error)
            throw (Error) t;
         if (t instanceof RuntimeException)
            throw (RuntimeException) t;
         if (t instanceof Exception)
            throw new EJBException((Exception) t);
         throw new RuntimeException(t);
      }
   }

   @SuppressWarnings("unchecked")
   private static final String[] toStringArray(Object o)
   {
      return ((Collection<String>) o).toArray(new String[0]);
   }

   /**
    * 
    * @param params
    * @return
    */
   public abstract EJBContainer createEJBContainer(final JBossEmbeddedContainerStartupParams params);

}

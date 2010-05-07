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

import javax.ejb.embeddable.EJBContainer;

import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.bootstrap.api.mc.server.MCBasedServer;
import org.jboss.bootstrap.api.mc.server.MCServer;

/**
 * Utility class providing static access to the {@link MCServer}
 * and other facilities underpinning the JBoss implementation of 
 * the {@link EJBContainer} used within AS.  Though an antipattern
 * to expose these in a static manner, creation of {@link EJBContainer}s
 * is, by spec, a static operation.  This is therefore the single hook
 * to the underlying wiring.  Integration environments should inject the correct
 * {@link MCServer} instance appropriately.  Creating a new instance exposes
 * the {@link MCServer} statically; once this has been done new requests for 
 * construction of another {@link EmbeddedEJBContainerASAdaptor} will 
 * fail with {@link IllegalStateException}
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public final class EmbeddedEJBContainerASAdaptor
{
   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * {@link MCServer} instance handling all wiring of components which together
    * will compose the JBoss Embedded EJB3 implementation.
    */
   private static volatile MCBasedServer<?, ?> server;

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates a new instance, but more importantly exposes the supplied {@link MCServer}
    * in static fashion.  Once called, subsequent requests for construction will fail with
    * {@link IllegalStateException}.
    * 
    * @param server The MC Server to make available statically.  Must be supplied
    * @throws IllegalArgumentException If the server is not specified
    * @throws IllegalStateException If this has already been called
    */
   public EmbeddedEJBContainerASAdaptor(@Inject final MCBasedServer<?, ?> server) throws IllegalStateException,
         IllegalArgumentException
   {
      // So that we can't reset the MC Server
      synchronized (this)
      {
         // Precondition checks
         if (server == null)
         {
            throw new IllegalStateException("MC Server instance was not specified");
         }

         setMCServer(server);
      }
   }

   //-------------------------------------------------------------------------------------||
   // Utility Methods -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Returns the {@link MCServer} instance underpinning the JBoss {@link EJBContainer}
    * implementation.  May only be called after the server has been set, otherwise an
    * {@link IllegalStateException} will be raised
    * 
    * @throws IllegalStateException If the {@link MCServer} has not yet been set
    */
   static MCBasedServer<?, ?> getMCServer() throws IllegalStateException
   {
      // Precondition checks
      if (server == null)
      {
         throw new IllegalStateException("MC Server has not yet been set");
      }

      // Return
      return server;
   }

   /**
    * Sets the {@link MCServer} instance underpinning the JBoss {@link EJBContainer}
    * implementation.  May only be called once, otherwise a {@link IllegalStateException}
    * will be raised.  The specified kernel instance may not be null.
    * @param server
    * @throws IllegalStateException If the {@link MCServer} has already been set
    * @throws IllegalArgumentException If the specified {@link MCServer} is null
    */
   private static void setMCServer(final MCBasedServer<?, ?> server) throws IllegalStateException,
         IllegalArgumentException
   {
      // Precondition checks
      if (server == null)
      {
         throw new IllegalArgumentException("MC Server instance may not be null");
      }

      if (EmbeddedEJBContainerASAdaptor.server != null)
      {
         throw new IllegalStateException("MC Server has already been set");
      }

      // Set
      EmbeddedEJBContainerASAdaptor.server = server;
   }

}

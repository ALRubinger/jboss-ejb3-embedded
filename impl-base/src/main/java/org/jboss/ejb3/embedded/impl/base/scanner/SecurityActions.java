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

package org.jboss.ejb3.embedded.impl.base.scanner;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Internal security actions not to escape 
 * this package; contains utilities to access
 * privileged actions.
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
class SecurityActions
{

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Blocks instanciation
    */
   private SecurityActions()
   {
      throw new UnsupportedOperationException("No instances permitted");
   }

   //-------------------------------------------------------------------------------------||
   // Functional Methods -----------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Obtains the system property of the specified key
    * 
    * @param key
    */
   static String getSystemProperty(final String key)
   {
      assert key != null && key.length() > 0 : "Key must be specified";
      return AccessController.doPrivileged(new PrivilegedAction<String>()
      {
         @Override
         public String run()
         {
            return System.getProperty(key);
         }
      });
   }

   /**
    * Returns the Thread Context {@link ClassLoader}
    * @return
    */
   static ClassLoader getTccl()
   {
      return AccessController.doPrivileged(GetTcclAction.INSTANCE);
   }

   //-------------------------------------------------------------------------------------||
   // Internal Helper Members ------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * {@link PrivilegedAction} implementation to obtain the TCCL
    */
   private enum GetTcclAction implements PrivilegedAction<ClassLoader> {
      INSTANCE;

      @Override
      public ClassLoader run()
      {
         return Thread.currentThread().getContextClassLoader();
      }
   }

}

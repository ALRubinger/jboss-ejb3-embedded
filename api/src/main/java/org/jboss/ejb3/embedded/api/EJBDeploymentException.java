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

package org.jboss.ejb3.embedded.api;

/**
 * Represents a problem occurred during deployment or
 * undeployment operations to the {@link JBossEJBContainer}
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class EJBDeploymentException extends RuntimeException
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1L;

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates a new instance from the specified cause, providing 
    * exception translation to the EJB layer.
    * 
    * @param message
    * @param cause
    */
   private EJBDeploymentException(final String message, final Throwable cause)
   {
      super(message, cause);
   }

   //-------------------------------------------------------------------------------------||
   // Factory ----------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates a new {@link EJBDeploymentException} instance from the specified
    * message and cause
    * @throws IllegalArgumentException If either the message or cause is not specified
    */
   public static EJBDeploymentException newInstance(final String message, final Throwable cause)
         throws IllegalArgumentException
   {

      // Precondition checks
      if (message == null || message.length() == 0)
      {
         throw new IllegalArgumentException("message must be specified");
      }
      if (cause == null)
      {
         throw new IllegalArgumentException("cause must be specified");
      }
      // Create and return
      return new EJBDeploymentException(message, cause);
   }

}

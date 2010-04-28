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

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;

import org.jboss.shrinkwrap.api.Archive;

/**
 * End-user view of JBoss {@link EJBContainer} implementation.
 * In addition to exposing some support within {@link EJBContainer},
 * this makes available explicit user deployment and undeployment.
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public interface JBossEJBContainer
{
   //-------------------------------------------------------------------------------------||
   // Contracts --------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Retrieve a naming context for looking up references to session beans 
    * executing in the embeddable container.
    * @see {@link EJBContainer#getContext()}
    */
   Context getContext();

   /**
    * Shutdown an embeddable EJBContainer instance. 
    * Embeddable applications should always call close() in order to free up the resources 
    * associated with the embeddable container. 
    * @see {@link EJBContainer#close()}
    */
   void close();

   /**
    * Deploys the specified {@link Archive}s into the container
    * @param archive
    * @throws EJBDeploymentException
    */
   void deploy(Archive<?>... archives) throws EJBDeploymentException;

   /**
    * Undeploys the specified {@link Archive}s from the container
    * @param archive
    * @throws EJBDeploymentException
    */
   void undeploy(Archive<?>... archives) throws EJBDeploymentException;

}

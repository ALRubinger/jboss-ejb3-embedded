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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.embeddable.EJBContainer;

import org.jboss.ejb3.embedded.impl.base.scanner.ClassPathEjbJarScanner;
import org.jboss.logging.Logger;

/**
 * Value object encapsulating the constructor parameters 
 * required to create a new {@link JBossEJBContainerBase}
 * extension instance.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class JBossEmbeddedContainerStartupParams
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(JBossEmbeddedContainerStartupParams.class);

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Spec-defined and JBoss-specific properties used in creating
    * new JBoss {@link EJBContainer} instances.  To be defensively copied
    * and set as immutable during construction.
    */
   private final Map<?, ?> properties;

   /**
    * Value of {@link EJBContainer#MODULES} used to denote the
    * initial deployments to be processed by the Container.  To be defensively
    * copied during construction and copied as returned to the caller.
    */
   private final String[] modules;

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates a new instance using the specified properties and deployment modules
    * 
    * @param properties Properties used in constructing the {@link EJBContainer}.  If null,
    * a new, empty {@link Map} will be allocated
    * @param modules Modules to deploy into the {@link EJBContainer}.  A null argument here
    * signals that the ClassPath should be searched for EJB Modules as defined by {@link EJBContainer#createEJBContainer()}
    * and EJB 3.1 22.2.1.
    */
   public JBossEmbeddedContainerStartupParams(Map<?, ?> properties, String[] modules)
   {
      // Precondition checks and adjustments
      if (properties == null)
      {
         properties = new HashMap<Object, Object>(0);
      }
      if (modules == null)
      {
         if (log.isDebugEnabled())
         {
            log.debug("No modules explicitly passed in; scanning ClassPath for EJBs");
         }
         modules = ClassPathEjbJarScanner.getEjbJars();
      }

      // Defensive copy and set
      final Map<?, ?> copy = new HashMap<Object, Object>(properties);
      this.properties = Collections.unmodifiableMap(copy);

      final String[] copyModules = copy(modules);
      this.modules = copyModules;

   }

   //-------------------------------------------------------------------------------------||
   // Accessors --------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Returns an immutable view of the construction properties
    * used to create the container
    * @return the properties
    */
   public Map<?, ?> getProperties()
   {
      return properties;
   }

   /**
    * Returns a copy of the value of {@link EJBContainer#MODULES} property
    * @return the modules
    */
   public String[] getModules()
   {
      // Return null or a copy
      return modules == null ? null : copy(modules);
   }

   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Copies the specified array into a new object, returning the new instance
    */
   private String[] copy(final String[] original)
   {
      return Arrays.copyOf(original, original.length);
   }

}

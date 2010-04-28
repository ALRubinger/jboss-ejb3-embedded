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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;

import org.jboss.ejb3.embedded.impl.base.scanner.ClassPathEjbJarScanner;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Asset;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests fo ensure that the {@link ClassPathEjbJarScanner} is working
 * as contracted to fulfill the EJB 3.1 Specification
 * 22.2.1
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class ClassPathEjbJarScannerUnitTest
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(ClassPathEjbJarScannerUnitTest.class);

   /**
    * System property key denoting the JVM ClassPath 
    */
   private static final String SYS_PROP_KEY_CLASS_PATH = "java.class.path";

   /**
    * EJB JAR containing @Stateless
    */
   private static final String NAME_JAR_SLSB = "slsb.jar";

   /**
    * EJB JAR containing @Stateful
    */
   private static final String NAME_JAR_SFSB = "sfsb.jar";

   /**
    * EJB JAR containing @Singleton
    */
   private static final String NAME_JAR_SINGLETON = "singleton.jar";

   /**
    * EJB JAR containing @MessageDriven
    */
   private static final String NAME_JAR_MDB = "mdb.jar";

   /**
    * EJB JAR containing a POJO with no descriptor (should not be detected as an EJB Module)
    */
   private static final String NAME_JAR_POJO = "pojo.jar";

   /**
    * EJB JAR containing a POJO with a descriptor
    */
   private static final String NAME_JAR_POJO_WITH_DESCRIPTOR = "descriptor.jar";

   /**
    * The ClassPath entries expected to be reported as modules
    */
   private static final Collection<String> expectedEjbJarClassPathEntries = new ArrayList<String>();

   //-------------------------------------------------------------------------------------||
   // Lifecycle --------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates a series of test EJB JARs and exploded directories to 
    * be scanned as valid EJB modules
    */
   @BeforeClass
   public static void createTestEJBJarsAndDirs() throws Exception
   {

      // Create a bunch of test Archives
      final Collection<JavaArchive> archives = new ArrayList<JavaArchive>();
      archives.add(ShrinkWrap.create(NAME_JAR_SLSB, JavaArchive.class).addClass(Slsb.class));
      archives.add(ShrinkWrap.create(NAME_JAR_SFSB, JavaArchive.class).addClass(Sfsb.class));
      archives.add(ShrinkWrap.create(NAME_JAR_SINGLETON, JavaArchive.class).addClass(Singleton1.class));
      archives.add(ShrinkWrap.create(NAME_JAR_MDB, JavaArchive.class).addClass(Mdb.class));
      archives.add(ShrinkWrap.create(NAME_JAR_POJO, JavaArchive.class).addClass(Pojo.class));
      archives.add(ShrinkWrap.create(NAME_JAR_POJO_WITH_DESCRIPTOR, JavaArchive.class).addClass(Pojo.class)
            .addManifestResource(new Asset()
            {

               @Override
               public InputStream openStream()
               {
                  return new ByteArrayInputStream(new byte[]
                  {});
               }
            }, "ejb-jar.xml"));

      // Flush these out to disk in both JAR and Exploded format
      final URL shrinkwrapOutputUrl = new URL(ClassPathEjbJarScannerUnitTest.class.getProtectionDomain()
            .getCodeSource().getLocation(), "../shrinkwrap");
      final File shrinkwrapOutputDir = new File(shrinkwrapOutputUrl.toURI());
      rm(shrinkwrapOutputDir);
      final File shrinkwrapOutputDirJars = new File(shrinkwrapOutputDir, "jars");
      final File shrinkwrapOutputDirExploded = new File(shrinkwrapOutputDir, "exploded");
      shrinkwrapOutputDirJars.mkdirs();
      shrinkwrapOutputDirExploded.mkdirs();
      final Collection<File> classPathFileEntries = new ArrayList<File>();
      for (final JavaArchive archive : archives)
      {
         // Log
         log.info(archive.toString(true));
         // Export as JAR
         final File jar = new File(shrinkwrapOutputDirJars, archive.getName());
         archive.as(ZipExporter.class).exportZip(jar, true);
         classPathFileEntries.add(jar);
         // Export as exploded directory
         archive.as(ExplodedExporter.class).exportExploded(shrinkwrapOutputDirExploded);
         classPathFileEntries.add(new File(shrinkwrapOutputDirExploded, archive.getName()));
      }

      // Set the ClassPath
      final StringBuilder sb = new StringBuilder();
      for (final File classPathFileEntry : classPathFileEntries)
      {
         sb.append(classPathFileEntry);
         sb.append(File.pathSeparatorChar);
      }
      log.info("Test ClassPath to be scanned: " + sb.toString());
      System.setProperty(SYS_PROP_KEY_CLASS_PATH, sb.toString());

      // Build the expected CP entries
      for (final File classPathFileEntry : classPathFileEntries)
      {
         final String classPathFileEntryName = classPathFileEntry.getAbsolutePath();
         if (!classPathFileEntry.toString().contains(NAME_JAR_POJO))
         {
            expectedEjbJarClassPathEntries.add(classPathFileEntryName);
         }
      }
   }

   //-------------------------------------------------------------------------------------||
   // Tests ------------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Tests that the EJB Modules found on the ClassPath by the {@link ClassPathEjbJarScanner} 
    * are as expected
    */
   @Test
   public void obtainsEjbJarModulesFromClassPath()
   {
      // Get entries identified as modules from the ClassPath
      final List<String> ejbModulesFromClassPath = Arrays.asList(ClassPathEjbJarScanner.getEjbJars());

      // Ensure the size (we should have 10 modules defined)
      Assert.assertEquals("EJB Modules found on ClassPath isn't of expected size", 10, ejbModulesFromClassPath.size());

      // Ensure all expected modules are found
      for (final String expectedToBeFound : expectedEjbJarClassPathEntries)
      {
         Assert.assertTrue("Expected EJB module was not found by ClassPath scanner: " + expectedToBeFound,
               ejbModulesFromClassPath.contains(expectedToBeFound));
      }

   }

   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods ------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Recursively removes the specified directory
    * @param file
    * @throws IOException
    */
   private static void rm(final File file) throws IOException
   {
      if (file.isDirectory())
      {
         for (final File child : file.listFiles())
         {
            rm(child);
         }
      }
      file.delete();
   }

   /*
    * Test EJB Component Classes
    */

   @Stateless
   private static final class Slsb
   {

   }

   @Stateful
   private static final class Sfsb
   {

   }

   @Singleton
   private static final class Singleton1
   {

   }

   @MessageDriven
   private static final class Mdb
   {

   }

   /**
    * Test class with no EJB Component Annotation
    * 
    * 
    * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
    * @version $Revision: $
    */
   private static final class Pojo
   {

   }
}

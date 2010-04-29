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

package org.jboss.ejb3.embedded.impl.base.scanner.filter;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.ejb3.embedded.spi.scanner.filter.ExclusionFilter;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Asset;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.vfs3.ArchiveFileSystem;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests that the {@link BundleSymbolicNameExclusionFilter}
 * is working as expected
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class BundleSymbolicNameExclusionFilterUnitTest
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(BundleSymbolicNameExclusionFilterUnitTest.class);

   /**
    * Service backing the temp file provider
    */
   private static ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

   /**
    * Temp file provider backing mounted Archives
    */
   private static TempFileProvider provider = null;

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Handles to close when done
    */
   private final Set<Closeable> handles = new HashSet<Closeable>();

   //-------------------------------------------------------------------------------------||
   // Lifecycle --------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Sets up the temporary file provider
    */
   @BeforeClass
   public static void createTempFileProvider() throws IOException
   {
      provider = TempFileProvider.create("shrinkwrap-", service);
   }

   /**
    * Shuts down the service
    */
   @AfterClass
   public static void shutdownService()
   {
      service.shutdownNow();
   }

   /**
    * Closes all handles from the mounting process
    * @throws IOException
    */
   @After
   public void closeHandles()
   {
      for (final Closeable handle : handles)
      {
         try
         {
            handle.close();
         }
         catch (final IOException ioe)
         {
            // Ignore
         }
      }
   }

   //-------------------------------------------------------------------------------------||
   // Tests ------------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Ensures that a JAR with a manifest header of 
    * "Bundle-SymbolicName" matching a given pattern is excluded
    */
   @Test
   public void honorsExclusion() throws IOException
   {
      final String bundleValue = "org.jboss.test";
      final boolean excluded = this.filter(bundleValue, bundleValue);
      Assert.assertTrue("Filter should have blocked configured bundle symbolic name header value", excluded);
   }

   /**
    * Ensures that a JAR with a manifest header of 
    * "Bundle-SymbolicName" matching a given pattern is excluded
    */
   @Test
   public void notOverzealous() throws IOException
   {
      final boolean excluded = this.filter("org.jboss.test", " configured value which doesn't match");
      Assert.assertTrue("Filter should not have blocked unconfigured bundle symbolic name header value", !excluded);
   }

   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods ------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Runs a manifest JAR with header "Bundle-SymbolicName"
    * with the specified value through a {@link BundleSymbolicNameExclusionFilter}
    * with the specified configuration 
    * 
    * @param bundleSymbolicName
    * @param filterConfig
    * @throws IOException
    */
   private boolean filter(final String bundleSymbolicName, final String filterConfig) throws IOException
   {
      // Precondition checks
      assert bundleSymbolicName != null : "bundleSymbolicName must be specified";
      assert filterConfig != null : "filterConfig must be specified";

      // Create the archive
      final JavaArchive archive = ShrinkWrap.create("manifest.jar", JavaArchive.class).addManifestResource(new Asset()
      {

         @Override
         public InputStream openStream()
         {
            return new ByteArrayInputStream(("Bundle-SymbolicName=" + bundleSymbolicName).getBytes());
         }
      }, "MANIFEST.MF");
      log.info(archive.toString(true));

      // Mount
      final String archiveName = archive.getName();
      final VirtualFile file = VFS.getChild(archiveName);
      handles.add(VFS.mount(file, new ArchiveFileSystem(archive, provider.createTempDir(archiveName))));

      // Run through the filter
      final ExclusionFilter filter = new BundleSymbolicNameExclusionFilter(filterConfig);
      boolean excluded = filter.exclude(file);
      return excluded;
   }

}

/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue.action;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.VueUtil;
import tufts.vue.Version;
import tufts.vue.VUE;
import tufts.vue.Resource;
import tufts.vue.PropertyEntry;
import tufts.vue.URLResource;
import tufts.vue.Images;
import tufts.vue.IMSCP;
import tufts.vue.LWComponent;
import tufts.vue.LWMap;

import static tufts.vue.Resource.*;

/**
 * Code related to identifying, creating and unpacking VUE archives.
 *
 * @version $Revision: 1.14 $ / $Date: 2010-02-03 19:13:45 $ / $Author: mike $ 
 */
public class Archive
{

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Archive.class);

    private static final String ZIP_IMPORT_LABEL ="Imported";
    
    private static final String MAP_ARCHIVE_KEY = "@(#)TUFTS-VUE-ARCHIVE";    
    private static final String SPEC_KEY = "spec=";
    private static final int SPEC_KEY_LEN = SPEC_KEY.length();

    public static boolean isVueIMSCPArchive(File file) {
        if (!file.getName().toLowerCase().endsWith(".zip"))
            return false;
        
        try {
            ZipFile zipFile = new ZipFile(file);
            return zipFile.getEntry(IMSCP.MAP_FILE) != null && zipFile.getEntry(IMSCP.MANIFEST_FILE) != null;
        } catch (Throwable t) {
            Log.warn(t);
            return false;
        }
    }


    public static boolean isVuePackage(File file) {
        return file.getName().toLowerCase().endsWith(VueUtil.VueArchiveExtension);
    }

    /**
     * @return true if we can create files in the given directory
     * File.canWrite is insufficient to ensure this.   If the filesystem
     * the directory is on is not writeable, we wont know this until
     * we attempt to create a file there, and it fails.
     */
    public static boolean canCreateFiles(File directory) {
        if (directory == null)
            return false;

// Window's Vista as of April 2008 can apparently lie about this.  E.g., it is
// claiming the Desktop folder is not writeable...  -- SMF 2008-04-04
//         if (!directory.canWrite()) {
//             Log.info("Cannot write: " + directory);
//             return false;
//         }

        File tmp = null;
        try {
            tmp = directory.createTempFile(".vueFScheck", "", directory);
        } catch (Throwable t) {
            Log.info("Cannot write to filesystem inside: " + directory + "; " + t);
        }

        if (tmp != null) {
            if (DEBUG.Enabled) Log.debug("Created test file: " + tmp);
            try {
                tmp.delete();
            } catch (Throwable t) {
                Log.error("Couldn't delete tmp file " + tmp, t);
            }
            return true;
        }

        return false;
    }


    /**
     * @param zipFile should be a File pointing to a VUE Package -- a Zip Archive created by VUE
     */

    public static LWMap openVuePackage(final File zipFile)
        throws java.io.IOException,
               java.util.zip.ZipException
    {
        Log.info("Unpacking VUE zip archive: " + zipFile);
        
        final String unpackingDir;

        //File folder = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+"VueMapArchives";

        final File parentDirectory = zipFile.getParentFile();
        if (false && canCreateFiles(parentDirectory)) // for now, always unpack into the temp directory
            unpackingDir = parentDirectory.toString();
        else
            unpackingDir = VUE.getSystemProperty("java.io.tmpdir");
        
        Log.info("Unpacking location: " + unpackingDir);

        final ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));

        final Map<String,String> packagedResources = new HashMap();

        ZipEntry entry;
        ZipEntry mapEntry = null;
        String mapFile = null;
        
        while ( (entry = zin.getNextEntry()) != null ) {

            final String location = unzipEntryToFile(zin, entry, unpackingDir);
            final String comment = Archive.getComment(entry);

            if (comment != null) {
                if (comment.startsWith(MAP_ARCHIVE_KEY)) {
                    mapEntry = entry;
                    mapFile = location;
                    Log.info("Identified map entry: " + comment + " (" + entry.getName() + ")");
                    //Log.info("Found map: " + entry + "; at " + location);
                } else {
                    String spec = comment.substring(comment.indexOf(SPEC_KEY) + SPEC_KEY_LEN);
                    Log.info("             [" + spec + "]"); // todo: revert to debug level eventually
                    if (packagedResources.put(spec, location) != null)
                        Log.warn("repeated resource spec in archive! [" + spec + "]");
                    //Log.debug("       spec= " + spec);
                }
            } else {
                Log.warn("ENTRY WITH NO COMMENT: " + entry);
            }
        }

        zin.close();
           
        // If this package map is being unmarshalled on the same machine it was created
        // on, all the URLResource's will initialize themseleves normally to their
        // original source files, not their package files.  This is a bit inefficient /
        // might confuse things, so we need to be careful.  For now we're just going to
        // completely re-init the Resources from scratch, so they'll init twice.  It
        // would be more ideal to pass in a special UnmarshalListener that would handle
        // setting the PACKAGE_FILE property on the Resources before they init, or at
        // least pass in a context object to the default VueUnmarshalListener, that
        // could be passed to URLResource.XML_completed, which it could check to know if
        // it should wait on attempting to initialize.
        
        final LWMap map =
            ActionUtil.unmarshallMap(new File(mapFile),
                                     new ArchiveMapUnmarshalHandler(zipFile + "(" + mapEntry + ")",
                                                                    zipFile,
                                                                    packagedResources));

        map.setFile(zipFile);
        map.markAsSaved();

        return map;
    }

    private static class ArchiveMapUnmarshalHandler  extends MapUnmarshalHandler
    {
        final Map<String,String> packagedResources;
        final File archiveFile;
        
        ArchiveMapUnmarshalHandler(Object source, File archiveFile, Map<String,String> resourcesFoundInPackage) {
            super(source, Resource.MANAGED_UNMARSHALLING);
            this.packagedResources = resourcesFoundInPackage;
            this.archiveFile = archiveFile;
        }

        /** skip setFile -- we're going to use the package file */
        @Override
        void notifyFile(final LWMap map, final File file)
        {
            super.map = map;
            super.file = file;

            map.setArchiveMap(true);

            // skip setFile: we'll handle it ourselves for the archive
        }

        /** this impl is so we can patch up resources first before completing the restore */
        @Override
        void notifyUnmarshallingCompleted()
        {
            map.runResourceDeserializeInits(map.getAllResources());
            
            patchResourcesForPackage();

            //map.runResourceFinalInits(allResources);
            map.completeXMLRestore(context);
            
            //super.notifyUnmarshallingCompleted();
        }

        private void patchResourcesForPackage() {

            for (Resource r : map.getAllResources()) {
                final String packageCacheFile = packagedResources.get(r.getSpec());
                if (packageCacheFile != null) {
                    //Log.debug("Found packaged resource: " + r + "; " + packageCacheFile);
                    if (DEBUG.Enabled) Log.debug("patching packaged resource: " + packageCacheFile + "; into " + r);
                
                    // This will convert "/" from the zip-entry package name to "\" on Windows
                    // (ZipEntry pathnames always use '/', no matter what the platform).
                    final File localFile = new File(packageCacheFile);
                    final String localPath = localFile.toString();
                    if (DEBUG.RESOURCE && !localPath.equals(packageCacheFile))
                        Log.info("       localized file path: " + localPath);

                    if (r instanceof URLResource) {
                        ((URLResource)r).setPackageFile(localFile, archiveFile);
                    } else {
                        Log.warn("package file fallback for unknown resource type, impl in question: " + Util.tags(r) + "; " + localFile);
                        r.setProperty(PACKAGE_FILE, localFile);
                        r.setCached(true);
                    }
                } else {
                    if (DEBUG.Enabled) Log.debug("No archive entry matching: " + r.getSpec());
                }
            }
        }
    }

    /**
     * @param location -- if null, entry will be unzipped in local (current) working directory,
     * otherwise, entry will be unzipped at the given path location in the file system.
     * @return filename of unzipped file
     */
    public static String unzipEntryToFile(ZipInputStream zin, ZipEntry entry, String location)
        throws IOException
    {
        final String filename;

        if (location == null) {
            filename = entry.getName();
        } else {
            if (location.endsWith(File.separator))
                filename = location + entry.getName();
            else
                filename = location + File.separator + entry.getName();
        }

        if (true||DEBUG.IO) {
            // Note: entry.getSize() is not known until the entry is unpacked
            //final String comment = Archive.getComment(entry);
            String msg = "Unzipping to " + filename + " from entry " + entry;
            //if (comment != null) msg += "\n\t[" + comment + "]";
            Log.info(msg);
            //Log.debug(msg);
        }
        
        final File newFile = createFile(filename);
        //Log.info("UNPACKING " + newFile);
        final FileOutputStream out = new FileOutputStream(newFile);
        byte [] b = new byte[1024];
        int len = 0;
        int wrote = 0;
        while ( (len=zin.read(b))!= -1 ) {
            wrote += len;
            out.write(b,0,len);
        }
        out.close();
        if (DEBUG.IO) {
            Log.debug("    Unzipped " + filename + "; wrote=" + wrote + "; size=" + entry.getSize());
        }

        return filename;
        
    }

    public static File createFile(String name)
        throws IOException
    {
        final File file = new File(name);

        File parent = file;
        while ( (parent = parent.getParentFile()) != null) {
            //Log.debug("Parent: " + parent);
            if (parent.getPath().equals("/")) {
                //Log.debug("skipping " + parent);
                break;
            }
            if (!parent.exists()) {
                Log.debug("Creating: " + parent);
                parent.mkdir();
            }
        }
        file.createNewFile();
        return file;
    }

    private static void unzipIMSCP(ZipInputStream zin, ZipEntry entry)
        throws IOException
    {

        unzipEntryToFile(zin, entry, VueUtil.getDefaultUserFolder().getAbsolutePath());
        
//        String fname = VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+s;
//         if (DEBUG.IO) System.out.println("unzipping " + s + " to " + fname);
//         FileOutputStream out = new FileOutputStream(fname);
//         byte [] b = new byte[512];
//         int len = 0;
//         while ( (len=zin.read(b))!= -1 ) {
//             out.write(b,0,len);
//         }
//         out.close();
    }
    

    
    public static LWMap loadVueIMSCPArchive(File file)
        throws java.io.FileNotFoundException,
               java.util.zip.ZipException,
               java.io.IOException
    {
            Log.info("Unpacking VUE IMSCP zip archive: " + file);
            ZipFile zipFile = new ZipFile(file);
            Vector<Resource> resourceVector = new Vector();
            File resourceFolder = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+IMSCP.RESOURCE_FILES);
            if(resourceFolder.exists() || resourceFolder.mkdir()) {
                ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
                ZipEntry e;
                while ((e=zin.getNextEntry()) != null) {
                    unzipIMSCP(zin, e);
                    //if (DEBUG.IO) System.out.println("ZipEntry: " + e.getName());  
                    if(!e.getName().equalsIgnoreCase(IMSCP.MAP_FILE) && !e.getName().equalsIgnoreCase(IMSCP.MANIFEST_FILE)){
                        // todo: may want to add a Resource.Factory.get(ZipEntry) method
                        Resource resource = Resource.getFactory().get(e.getName());
                        resourceVector.add(resource);
                        //if (DEBUG.IO) System.out.println("Resource: " + resource);
                    }
                }
                zin.close();
            }
           
            File mapFile  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+IMSCP.MAP_FILE);
            LWMap map = ActionUtil.unmarshallMap(mapFile);
            map.setFile(null);
            map.setLabel(ZIP_IMPORT_LABEL);
            for (Resource r : resourceVector) {
                replaceResource(map, r,
                                Resource.getFactory().get(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+r.getSpec()));
                //new URLResource(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+r.getSpec()));
            }

            map.markAsSaved();

            return map;
    }


    public static void replaceResource(LWMap map,Resource r1,Resource r2) {
        for (LWComponent component : map.getAllDescendents()) {
            if(component.hasResource()){
                Resource resource = component.getResource();
                if(resource.getSpec().equals(r1.getSpec()))
                    component.setResource(r2);
            }
        }
    }


    private static final String COMMENT_ENCODING = "UTF-8";
    
    /**
     *
     * There's a java bug (STILL!) as of JAN 2008: comments are encoded in the zip file,
     * but not extractable via any method call in any JDK.  See:
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6646605
     *
     * This method encapsulates workaround comment setting code.  We add comments anyway
     * for easy debug (e.g., unzip -l), and then encode them again as "extra" zip entry
     * bytes, which we can extract later as the comment.
     *
     * Note also that for special characters to make it through this process across
     * multiple platforms, the same, platform-neutral encoding must be used
     * both when setting and getting.
     *
     */
    private static void setComment(ZipEntry entry, String comment) {

        // Using the default ZipEntry.setComment here is currently
        // only useful in that it provides for visual inspection of
        // archive entry comments, using, for example the "unzip"
        // command line tool.

        entry.setComment(comment);
        
        try {
            //entry.setComment(new String(comment.getBytes(), COMMENT_ENCODING));
            //entry.setComment(new String(comment.getBytes(COMMENT_ENCODING), COMMENT_ENCODING));
            entry.setExtra(comment.getBytes(COMMENT_ENCODING));
        } catch (Throwable t) {
            Log.warn("Couldn't " + COMMENT_ENCODING + " encode 'extra' bytes into ZipEntry comment; " + entry + "; [" + comment + "]", t);
            entry.setExtra(comment.getBytes());
        }
    }

    /**
     * Extract comments from the given ZipEntry.
     * @See setComment
     */

    public static String getComment(ZipEntry entry) {

        // See setComment for why we do this this way:

        byte[] extra = entry.getExtra();
        String comment = null;
        if (extra != null && extra.length > 0) {
            if (DEBUG.IO && DEBUG.META) Log.debug("getComment found " + extra.length + " extra bytes");
            try {
                comment = new String(extra, COMMENT_ENCODING);
            } catch (Throwable t) {
                Log.warn("Couldn't " + COMMENT_ENCODING + " decode 'extra' bytes from ZipEntry comment; " + entry, t);
                comment = new String(extra);
            }
            //comment = "extra(" + new String(extra) + ")";
        }

        return comment;
    }


    private static int UniqueNameFailsafeCount = 1;
    
    /**
     * Generate a package file name from the given URLResource.  We could just as easily
     * generate random names, but we base it on the URL for easy debugging and
     * and exploring of the package in Finder/Explorer (e.g., we also try to make
     * sure the documents have appropriate extensions so the OS shell applications
     * can generate appropriate icons, etc).
     *
     * @param existingNames -- if provided, will put the result of generated names
     * in this set, and will be used to ensure that no repeated names are generated
     * on future calls.
     */
    private static String generatePackageFileName(Resource r, Set<String> existingNames) {

        String packageName = null;

        try {
            packageName = generateInformativePackageFileName(r);
        } catch (Throwable t) {
            Log.warn("Failed to create informative package name for " + r, t);
        }

        if (packageName != null && packageName.length() > 250) {
            // 255 is the max length on modern Mac's an PC's
            // We truncate to 250 in case we need a few extra chars for establishing uniqueness.
            Log.info("Truncating long name: " + packageName);
            packageName = packageName.substring(0, 250);
        }

        if (packageName == null) {
            packageName = String.format("vuedata%03d", UniqueNameFailsafeCount++); // note: count is static: not thread-safe
        } else if (existingNames != null) {
            if (existingNames.contains(packageName)) {
                //if (DEBUG.Enabled) Log.debug("Existing names already contains [" + packageName + "]; " + existingNames);
                Log.info("repeated name [" + packageName + "]");
                int cnt = 1;
                String uniqueName = packageName;
                int lastDot = packageName.lastIndexOf('.');
                if (lastDot > 0) {
                    // if there's an extension for the filename, introduce the unique-ifying
                    // index before it, so the filename will still have a recognizable extension
                    // to OS shell applications.
                    final String preDot = packageName.substring(0, lastDot);
                    final String postDot = packageName.substring(lastDot);
                    do {
                        uniqueName = String.format("%s.%03d%s", preDot, cnt++, postDot);
                    } while (existingNames.contains(uniqueName));
                } else {
                    do {
                        uniqueName = String.format("%s.%03d", packageName, cnt++);
                    } while (existingNames.contains(uniqueName));
                }
                packageName = uniqueName;
                Log.info("uniqified package name: " + packageName);
            }
            existingNames.add(packageName);
        }

        return packageName;
        
    }
    
    private static String generateInformativePackageFileName(Resource r)
        throws java.io.UnsupportedEncodingException
    {
        if (DEBUG.IO) Log.debug("Generating package file name from " + r + "; " + r.getProperties());

        try {
            if (r.hasProperty(PACKAGE_FILE)) {
                File pf = (File) r.getPropertyValue(PACKAGE_FILE);
                String name = pf.getName();
                // this prevents cascading encodings of special chars (e.g., $20 becomces $2420 each
                // time the package is saved, growing longer each time)
                if (DEBUG.IO) Log.debug("Using pre-existing package file name: " + name);
                return name;
            }
        } catch (Throwable t) {
            Log.warn(t);
        }
        
        final Object imageSource = r.getImageSource(); 
        //final URL url = r.getImageSource(); // better as URI?

        if (imageSource == null) 
            return r.getSpec(); // failsafe
        
        String packageName;
        if (imageSource instanceof File){
            packageName = ((File)imageSource).getName();
        } else if (imageSource instanceof URL) {

            final URL url = (URL) imageSource;

            packageName = url.toString(); // this could be very messy with queries...

            if (packageName.startsWith("http://")) {
                // strip off the most common case -- this not informative (can be assumed),
                // and makes package file names easier to read
                packageName = packageName.substring(7);
            }

            // packageName = url.getHost() + url.getFile();
                
            // If the resource is image content, and the generated name doesn't
            // look like something that has an extension that most OS shell
            // applications would recognize as an image (e.g., Finder, Explorer),
            // add an extension so that when looking at unpacked archives directories,
            // image icons can easily be seen.
            
            if (r.isImage() && r.hasProperty(IMAGE_FORMAT) && !Resource.looksLikeImageFile(packageName))
                packageName += "." + r.getProperty(IMAGE_FORMAT).toLowerCase();
        } else {
            throw new IllegalArgumentException("image source is neither URL or File: " + Util.tags(imageSource));
        }

//         String packageName;
//         if ("file".equals(url.getProtocol())) {
//             File file = new File(url.getFile());
//             packageName = file.getName();
//         } else {

//             packageName = url.toString(); // this could be very messy with queries...

//             if (packageName.startsWith("http://")) {
//                 // strip off the most common case -- this not informative (can be assumed),
//                 // and makes package file names easier to read
//                 packageName = packageName.substring(7);
//             }

//             // packageName = url.getHost() + url.getFile();
                
//             // If the resource is image content, and the generated name doesn't
//             // look like something that has an extension that most OS shell
//             // applications would recognize as an image (e.g., Finder, Explorer),
//             // add an extension so that when looking at unpacked archives directories,
//             // image icons can easily be seen.
            
//             if (r.isImage() && r.hasProperty(IMAGE_FORMAT) && !Resource.looksLikeImageFile(packageName))
//                 packageName += "." + r.getProperty(IMAGE_FORMAT).toLowerCase();
//         }
        
        if (DEBUG.IO) Log.debug("     decoding " + packageName);
        // Decode (to prevent any redundant encoding), then re-encode
        packageName = java.net.URLDecoder.decode(packageName, "UTF-8");
        if (DEBUG.IO) Log.debug("   decoded to " + packageName);
        packageName = java.net.URLEncoder.encode(packageName, "UTF-8");
        if (DEBUG.IO) Log.debug("re-encoded to " + packageName);
        // now "lock-in" the encoding: as this is now a fixed file-name, we don't ever want it to be
        // accidentally decoded, which might create something that looks like a path when we don't want it to.
        packageName = packageName.replace('%', '$');
        if (DEBUG.IO) Log.debug(" locked in at " + packageName);

//         if (URLResource.ALLOW_URI_WHITESPACE) {

//             // TODO: may be able to just decode these '+' encodings back to the actual
//             // space character, tho would need to do lots of testing of the entire
//             // workflow code path on multiple platforms. This would be especially nice
//             // at least for document names (e.g., non-images), as they'll often have
//             // spaces, and '$20' in the middle of the document name is pretty ugly to look
//             // at if they open the document (e.g., PDF, Word, Excel etc).

//             // 2008-03-31 Not currently working, at least on the mac: finding the local files eventually fails

//             packageName = packageName.replace('+', ' ');
            
//         } else {

            // So Mac openURL doesn't decode these space indicators later when opening:
            
            packageName = packageName.replaceAll("\\+", "\\$20");
            
            // Replacing '+' with '-' is a friendler whitespace replacement (more
            // readable), tho it's "destructive" in that the original URL could no
            // longer be reliably reverse engineered from the filename.  We don't
            // actually depend on being able to do that, but it's handy for
            // debugging, and could be useful if we ever have to deal with any kind of
            // recovery from data corruption.
            
            //packageName = packageName.replace('+', '-');
//         }
        
        
        return packageName;
    }

    private static class Item {
        final ZipEntry entry;
        final Resource resource;
        final File dataFile;

        Item(ZipEntry e, Resource r, File f) {
            entry = e; resource = r; dataFile = f;
        }

        public String toString() {
            return "Item[" + entry.toString() + "; " + resource + "; " + dataFile + "]";
        }
    }
                    
    /**

     * Write the map to the given file as a Zip archive, along with all unique resources
     * for which data can be found locally (local user files, or local image cache).
     * Entries for Resource data in the zip archive are annotated with their original
     * Resource spec, so they can be identified on unpacking, and associated with their
     * original aResources.

     */
    
    public static void writeArchive(LWMap map, File archive)
        throws java.io.IOException
    {
        Log.info("Writing archive package " + archive);

//         final String label = map.getLabel();
//         final String mapName;
//         if (label.endsWith(".vue"))
//             mapName = label.substring(0, label.length() - 4);
//         else
//             mapName = label;

        final String label = archive.getName();
        final String mapName;
        if (label.endsWith(VueUtil.VueArchiveExtension))
            mapName = label.substring(0, label.length() - 4);
        else
            mapName = label;
        

        final String dirName = mapName + ".vdr";

        //-----------------------------------------------------------------------------
        // Find source data-files for all unique resources
        //-----------------------------------------------------------------------------
        
        final Collection<Resource> uniqueResources = map.getAllUniqueResources();
        final Collection<PropertyEntry> manifest = new ArrayList();
        final List<Item> items = new ArrayList();
        final Set<String> uniqueEntryNames = new HashSet();

        Archive.UniqueNameFailsafeCount = 1;  // note: static variable -- not threadsafe
        
        for (Resource r : uniqueResources) {

            try {

                final File sourceFile = r.getActiveDataFile();
                final String description = "" + (DEBUG.Enabled ? r : r.getSpec());

                // todo: if source file is a .vue file, could load the map (faster if is
                // already open and we find it), then archive THAT out as a tmp .vpk file,
                // and add that to to archive uncompressed, converting the .vue resource
                // to a .vpk resource.

                if (sourceFile == null) {
                    Log.info("skipped: " + description);
                    continue;
                } else if (!sourceFile.exists()) {
                    Log.warn("Missing local file: " + sourceFile + "; for " + r);
                    continue;
                }
            
                final String packageEntryName = generatePackageFileName(r, uniqueEntryNames);

                final ZipEntry entry = new ZipEntry(dirName + "/" + packageEntryName);
                Archive.setComment(entry, "\t" + SPEC_KEY + r.getSpec());

                final Item item = new Item(entry, r, sourceFile);
                
                //Log.info("created: " + entry + "; " + description);

                items.add(item);
                manifest.add(new PropertyEntry(r.getSpec(), packageEntryName));
                
                if (DEBUG.Enabled) Log.info("created: " + item);

            } catch (Throwable t) {
                Log.error("writeArchive: failed to handle " + Util.tags(r), t);
            }
                
        }


        //-----------------------------------------------------------------------------
        // Write the map to the archive
        //-----------------------------------------------------------------------------

        final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archive)));
        final ZipEntry mapEntry = new ZipEntry(dirName + "/" + mapName + "$map.vue");
        final String comment = MAP_ARCHIVE_KEY + "; VERSION: 2;"
            + " Saved " + new Date() + " by " + VUE.getName() + " built " + Version.AllInfo + "; items=" + items.size() + ";"
            + ">" // /usr/bin/what terminatior
            //+ "\n\tmap-name(" + mapName + ")"
            //+ "\n\tunique-resources(" + resources.size() + ")"
            ;
        Archive.setComment(mapEntry, comment);
        zos.putNextEntry(mapEntry);

        final Writer mapOut = new OutputStreamWriter(zos);

        // TODO: need to handle a map marshalling failure, in which case we want to
        // abandon writing the entire archive -- this will require a big reorg here --
        // we need to write the entire archive to a tmp archive first, and only create
        // the new file if everything succeeds.

        try {
            map.setArchiveManifest(manifest);
            ActionUtil.marshallMapToWriter(map, mapOut);
        } catch (Throwable t) {
            Log.error(t);
            throw new RuntimeException(t);
        } finally {
            // TODO: do NOT reset this if this map is already a packaged map...
            map.setArchiveManifest(null);
        }

        //-----------------------------------------------------------------------------
        // Write the resources to the archive
        //-----------------------------------------------------------------------------
        
        for (Item item : items) {

            if (DEBUG.Enabled)
                Log.debug("writing: " + item);
            else
                Log.info("writing: " + item.entry);
            
            try {
                zos.putNextEntry(item.entry);
                copyBytesToZip(item.dataFile, zos);
            } catch (Throwable t) {
                Log.error("Failed to archive item: " + item, t);
            }
        }
        
        zos.closeEntry();
        zos.close();

        Log.info("Wrote " + archive);

    }

//     /**
//      * @deprecated - doesn't need to be this complicated, and makes ensuring uniquely named
//      * archive entries surprisingly difficult -- SMF 2008-04-01
//      *
//      * Create a ZIP archive that contains the given map, as well as all resources who's
//      * data is currently available.  This means currently only data in local user files,
//      * or in the image cache can be archived. Non-image remote data (e.g., documents:
//      * Word, Excel, PDF, etc) cannot currently be archived.  The resources in the map
//      * will be annotated with package cache information before the map is written out.
//      *
//      * STEPS:
//      *
//      *  1 - All resources in the map are cloned, and all LWComponents on the map are
//      *  temporarily assigned these cloned resources, so we may make special
//      *  modifications to them for the archived map (e.g., the resources are tagged with
//      *  the name of their archive file).
//      *
//      *  2 - The map, with it's temporary set of modified resources, is marshalled
//      *  directly to the zip archive.  It's always the first item in the archive, tho
//      *  this is not currently a requrement.  However, it is essential that it be tagged
//      *  in the archive with the MapArchiveKey (via a zip entry comment), so it
//      *  can be identified during extraction.
//      *
//      *  3 - The original resources are restored to the map.  We're done with the clones.
//      *
//      *  4 - Of the resource data available, the unique set of them is identified,
//      *  and they are written to the zip archive.
//      *
//      * This code is not thread-safe.  The map should not be modified during
//      * this codepath.  (E.g., if ever used as part of an auto-save feature,
//      * it would not be safe to let it run in a background thread).
//      */
    
//     private static void writeAnnotatedArchive(LWMap map, File archive)
//         throws java.io.IOException
//     {
//         Log.info("Writing map-annotated archive package " + archive);

//         String mapName = map.getLabel();
//         if (mapName.endsWith(".vue"))
//             mapName = mapName.substring(0, mapName.length() - 4);

//         final String dirName = mapName + ".vdr";
        
//         final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archive)));
//         final ZipEntry mapEntry = new ZipEntry(dirName + "/" + mapName + "$map.vue");
//         final String comment = MAP_ARCHIVE_KEY + "; VERSION: 1;"
//             + " Saved " + new Date() + " by " + VUE.getName() + " built " + Version.AllInfo
//             //+ "\n\tmap-name(" + mapName + ")"
//             //+ "\n\tunique-resources(" + resources.size() + ")"
//             ;
//         Archive.setComment(mapEntry, comment);
//         zos.putNextEntry(mapEntry);

//         final Map<LWComponent,Resource> savedResources = new IdentityHashMap();
//         final Map<Resource,Resource> clonedResources = new IdentityHashMap();
//         final Map<Resource,File> onDiskFiles = new IdentityHashMap();

//         UniqueNameFailsafeCount = 1;  // note: static variable -- not threadsafe

//         for (LWComponent c : map.getAllDescendents(LWComponent.ChildKind.ANY)) {

//             final Resource resource = c.getResource();

//             if (resource == null)
//                 continue;
            
//             if (resource instanceof URLResource == false) {
//                 Log.error("UNHANDLED NON-URLResource: " + Util.tags(resource));
//                 continue;
//             }
           
//             final URLResource r = (URLResource) resource;
            
//             File sourceFile = r.getActiveDataFile();
//             boolean wasLocal = r.isLocalFile();

//             //if (DEBUG.Enabled) Log.debug(r + "; sourceDataFile=" + sourceFile);
                
//             if (sourceFile != null && sourceFile.exists()) {
//                 savedResources.put(c, r);
//                 final URLResource cloned = (URLResource) r.clone();
//                 onDiskFiles.put(cloned, sourceFile);

//                 final String packageName = generatePackageFileName(r, null);
                    
//                 cloned.setProperty(PACKAGE_KEY_DEPRECATED, packageName);
//                 if (wasLocal) {
//                     //Log.info("STORING LOCAL PROPERTY: " + r.getSpec());
//                     cloned.setHiddenProperty("Package.orig", r.getSpec());
//                     //Log.info("STORED LOCAL PROPERTY: " + cloned.getProperty("@package.orig"));
//                 }
//                 clonedResources.put(r, cloned);
//                 c.takeResource(cloned);
//                 Log.debug("Clone: " + cloned);
//             } else {
//                 if (sourceFile == null)
//                     Log.info("No cache file for: " + r);
//                 else
//                     Log.info("Missing local file: " + sourceFile);
//             }
//         }
                    

//         //-----------------------------------------------------------------------------
//         // Archive up the map with it's re-written resources
//         //-----------------------------------------------------------------------------

//         final Writer mapOut = new OutputStreamWriter(zos);

//         try {
//             ActionUtil.marshallMapToWriter(map, mapOut);
//         } catch (Throwable t) {
//             Log.error(t);
//             throw new RuntimeException(t);
//         }

//         //-----------------------------------------------------------------------------
//         // Restore original resources to the map:
//         //-----------------------------------------------------------------------------
        
//         for (Map.Entry<LWComponent,Resource> e : savedResources.entrySet())
//             e.getKey().takeResource(e.getValue());
        
//         //-----------------------------------------------------------------------------
//         // Write out all UNIQUE resources -- we only want to write the data once
//         // no matter how many times the resource is on the map (and we don't currently
//         // support single unique instance ensuring resource factories).
//         //-----------------------------------------------------------------------------

//         final Collection<Resource> uniqueResources = map.getAllUniqueResources();
        
//         //ActionUtil.marshallMap(File.createTempFile("vv-" + file.getName(), ".vue"), map);

//         // TODO: this might be much simpler if we just processed the same list of
//         // of resources, with precomputed files, and just kept a separate map
//         // of zip entries and skipped any at the last moment if they'd already been added.

// //         final Set<String> uniqueEntryNames = new HashSet();

        
//         for (Resource r : uniqueResources) {

//             final Resource cloned = clonedResources.get(r);
//             final File sourceFile = onDiskFiles.get(cloned);
//             final String packageFileName = (cloned == null ? "[missing clone!]" : cloned.getProperty(Resource.PACKAGE_KEY_DEPRECATED));

//             ZipEntry entry = null;

//             if (sourceFile != null) {
//                 entry = new ZipEntry(dirName + "/" + packageFileName);
//             }
            
//             // TODO: to get the re-written resources to unpack, weather we specially
//             // encode the SPEC, or add another special property for the local cache file
//             // access (prob better), the Images.java code will need to keep the resource
//             // around more, so we can decide to go to package cache or original source.
//             // which could be handled also maybe via UrlAuth, tho really, we should just
//             // be converting the Resource to provide the data fetch, tho whoa, THAT is a
//             // problem if not all unique resources, because we still need to check the
//             // cache for remote URL's...  Okay, this really isn't that big of a deal.

//             final String debug = "" + (DEBUG.Enabled ? r : r.getSpec());

//             if (entry == null) {
//                 Log.info("skipped: " + debug);
//             } else {
//                 Log.info("writing: " + entry + "; " + debug);

//                 Archive.setComment(entry, "\t" + SPEC_KEY + r.getSpec());
                
//                 try {
//                     zos.putNextEntry(entry);
//                     copyBytesToZip(sourceFile, zos);
//                 } catch (Throwable t) {
//                     Log.error("Failed to archive entry: " + entry + "; for " + r, t);
//                 }
//             }
                
//         }
        
//         zos.closeEntry();
//         zos.close();

//         Log.info("Wrote " + archive);



//     }

    private static void copyBytesToZip(File file, ZipOutputStream zos)
        throws java.io.IOException
    {
        final BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
        byte[] buf = new byte[2048];
        int len;
        int total = 0;
        while ((len = fis.read(buf)) > 0) {
            if (DEBUG.IO && DEBUG.META) System.err.print(".");
            zos.write(buf, 0, len);
            total += len;
        }
        //Log.debug("wrote " + total + " bytes for " + file);
        fis.close();
    }
    

    
    

}
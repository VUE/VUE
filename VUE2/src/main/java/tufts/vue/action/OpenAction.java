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

/*
 * OpenAction.java
 *
 * Created on April 2, 2003, 12:40 PM
 */

package tufts.vue.action;

/**
 *
 * @author  akumar03
 */
import java.io.*;
import java.util.zip.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.*;
import tufts.Util;

public class OpenAction extends VueAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(OpenAction.class);
    
    //public static final String ZIP_IMPORT_LABEL ="Imported";
    public OpenAction(String label) {
        super(label, null, ":general/Open");
    }
    
    public OpenAction() {
        this("Open");
    }
    
    
    // workaround for rapid-succession Ctrl-O's which pop multiple open dialogs
    private static final Object LOCK = new Object();
    private static boolean openUnderway = false;
    public void actionPerformed(ActionEvent e) {
        synchronized (LOCK) {
            if (openUnderway)
                return;
            openUnderway = true;
        }
        try {
            File[] file = ActionUtil.openMultipleFiles("Open Map", VueFileFilter.VUE_DESCRIPTION);
            
            if (file == null)
            	return;
            
            for (int i=0;i<file.length;i++)
            	displayMap(file[i]);            
            Log.info(e.getActionCommand() + ": completed.");
        } finally {
            openUnderway = false;
            
            
        }
    }
    
    public static void reloadMap(LWMap map)
    {
    	displayMap(map.getFile());
    }
    
    public static void displayMap(File file) {
        VUE.displayMap(file);
        /*
        if (file != null) {
        	RecentlyOpenedFilesManager rofm = RecentlyOpenedFilesManager.getInstance();
            rofm.updateRecentlyOpenedFiles(file.getAbsolutePath());
            VUE.activateWaitCursor();
            try {
                LWMap loadedMap = loadMap(file.getAbsolutePath());
                VUE.displayMap(loadedMap);
            } finally {
                VUE.clearWaitCursor();
                VUE.getPathwayPanel().updateEnabledStates();
                
            }            
        }
        */
    }

//     public static boolean isVueIMSCPArchive(File file) {
//         if (!file.getName().toLowerCase().endsWith(".zip"))
//             return false;
        
//         try {
//             ZipFile zipFile = new ZipFile(file);
//             return zipFile.getEntry(IMSCP.MAP_FILE) != null && zipFile.getEntry(IMSCP.MANIFEST_FILE) != null;
//         } catch (Throwable t) {
//             Log.warn(t);
//             return false;
//         }
//     }


//     public static boolean isVueArchive(File file) {
//         return file.getName().toLowerCase().endsWith(VueUtil.VueArchiveExtension);
//     }
    
    
    public static LWMap loadMap(java.net.URL url) {
        try {
            if (DEBUG.CASTOR) Log.debug("Unmarshalling from " + url);
            LWMap map = ActionUtil.unmarshallMap(url);
            return map;
        } catch (Exception e) {
            Log.error("loadMap " + tufts.Util.tags(url), e);
            VueUtil.alert(null, VueResources.getString("openaction.mapopen.error"),VueResources.getString("openaction.mapopen.title"));
            //tufts.Util.printStackTrace(e);
            return null;
        }
    }
    
    // todo: have only one root loadMap that hanldes files & urls -- actually, make it ALL url's
    // TODO: this should be re-named openFile or openVueContent (as it handles all sorts of "vue" files)
    // (and also, again, merge this with ActionUtil unmarshall code?)
    public static LWMap loadMap(String filename) {

        LWMap map = null;
        
        try {
            map = doLoadMap(filename);
        } catch (FileNotFoundException e) {
            // maybe move all exception code here, taking the file-not-found handling
            Log.error("loadMap " + Util.tags(filename), e);
            VueUtil.alert(null, "\"" + filename + "\"" +VueResources.getString("openaction.mapnotfound.error"), VueResources.getString("openaction.mapnotfound.title"));
            map = LWMap.create(filename);
        } catch (Throwable t) {
            // out of the Open File dialog box.
            Log.error("loadMap " + Util.tags(filename), t);
            if (t.getCause() != null)
                t = t.getCause();
            final String message;
            final String exception;
            if (t instanceof EmptyFileException) {
                message = "There is no data in file \"%s\"\n\n%s";
                exception = t.getMessage();
                // special case: create a new map with the empty file name
                map = LWMap.create(filename); 
            } else {
                // if we still want the "version" part of this warning, it needs to be
                // delivered from below when we actually detect a VUE data-versioning problem:
                // this message is inappropriate for generic exceptions
                //message = "\"%s\" cannot be opened in this version of VUE.\n\nProblem:\n%s";
                message = "\"%s\" cannot be opened in VUE.\n\nProblem:\n%s";
                exception = t.toString();
            }
            
            VueUtil.alert(String.format(message, filename, Util.formatLines(exception, 80)),
                          VueResources.getString("openaction.openmapproblem.title"));
            //tufts.Util.printStackTrace(t);
        }
        return map;
    }

    private static LWMap doLoadMap(String filename)
        throws java.io.FileNotFoundException,
               java.util.zip.ZipException,
               java.io.IOException
    {
        if (DEBUG.CASTOR || DEBUG.IO) Log.debug("doLoadMap: name=" + filename);
        File file = new File(filename);

        //int dotIndex = file.getName().lastIndexOf('.');
        //String extension = "";
        //if (dotIndex >= 0 && file.getName().length() > 1)
        //    extension = file.getName().substring(dotIndex + 1).toLowerCase();
        //System.out.println("Extension = "+extension);

        // TODO: the current method of saving VUE zip archives doesn't preserve the
        // original resource reference.  We could easily do this by changing this to
        // a system where the original resources are left alone, (and the archiving
        // process can speedily pull the images from the disk image cache), and
        // here, when restoring, simply pre-load the disk cache with images included
        // in the archive.  Unless we want to provide other functionaly, such as the
        // ability for the user to get at the content directly in a special folder,
        // look at/edit it, etc.

        // Also problem: opening a VUE .zip archive, then trying to save it normally
        // won't produce something restorable.   Don't know if saving it as
        // a zip archive again will work or not.

        // TODO PREF: may want option for user to by default use the cached on-disk
        // image version of something when double clicking, v.s., going back out
        // online for the original resource.  (Would be nice also to do this by
        // default if not online!)  E.g., opening an image from the cache on MacOSX
        // would immediately open in, usually in Preview, instead of reloading it in
        // Safari.

        LWMap map = null;
        if (Archive.isVuePackage(file)) {

            map = Archive.openVuePackage(file);
            
        } else if (Archive.isVueIMSCPArchive(file)) {

            map = Archive.loadVueIMSCPArchive(file);
            
        } else {
            
            map = ActionUtil.unmarshallMap(file);
        }

        return map;
        
    }

//     private static LWMap unpackVueArchive(File file)
//         throws java.io.IOException,
//                java.util.zip.ZipException
//     {
//         Log.info("Unpacking VUE zip archive: " + file);
//         final ZipFile zipFile = new ZipFile(file);
        
//         //Vector<Resource> resourceVector = new Vector();
//         //File resourceFolder = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+IMSCP.RESOURCE_FILES);
//         //if(resourceFolder.exists() || resourceFolder.mkdir()) {
        
//         final ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
//         ZipEntry entry;
//         ZipEntry mapEntry = null;
//         String mapFile = null;

//         final String unpackingDir = VUE.getSystemProperty("java.io.tmpdir"); // or, could use same dir a current package file is at
        
        
//         while ( (entry = zin.getNextEntry()) != null ) {

//             String location = unzip(zin, entry, unpackingDir);

//             final String comment = SaveAction.getComment(entry);

//             if (comment != null && comment.startsWith(SaveAction.MapArchiveKey)) {
//                 mapEntry = entry;
//                 mapFile = location;
//                 Log.debug("Found map in archive: " + entry + "; at " + location);
                
//             }

            
//             //unzip(zin, entry, null);
            
// //             //if (DEBUG.IO) System.out.println("ZipEntry: " + e.getName());  
// //             if(!e.getName().equalsIgnoreCase(IMSCP.MAP_FILE) && !e.getName().equalsIgnoreCase(IMSCP.MANIFEST_FILE)){
// //                 // todo: may want to add a Resource.Factory.get(ZipEntry) method
// //                 Resource resource = Resource.getFactory().get(e.getName());
// //                 resourceVector.add(resource);
// //                 //if (DEBUG.IO) System.out.println("Resource: " + resource);
// //             }
//         }
//         zin.close();
           
// //         File mapFile  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+IMSCP.MAP_FILE);
// //         LWMap map = ActionUtil.unmarshallMap(mapFile);
// //         map.setFile(null);
// //         map.setLabel(ZIP_IMPORT_LABEL);
// //         for (Resource r : resourceVector) {
// //             replaceResource(map, r,
// //                             Resource.getFactory().get(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+r.getSpec()));
// //             //new URLResource(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+r.getSpec()));
// //         }

// //         map.markAsSaved();

//         return ActionUtil.unmarshallMap(new File(mapFile));

//         //return null;
//     }

//     /**
//      * @param location -- if null, entry will be unzipped in local (current) working directory,
//      * otherwise, entry will be unzipped at the given path location in the file system.
//      * @return filename of unzipped file
//      */
//     public static String unzip(ZipInputStream zin, ZipEntry entry, String location)
//         throws IOException
//     {
//         final String filename;

//         if (location == null)
//             filename = entry.getName();
//         else
//             filename = location + File.separator + entry.getName();

//         if (DEBUG.IO) {
//             // Note: entry.getSize() is not known until the entry is unpacked
//             final String comment = SaveAction.getComment(entry);
//             String msg = "Unzipping to " + filename + " from entry " + entry;
//             if (comment != null)
//                 msg += "\n\t[" + comment + "]";
//             Log.debug(msg);
//         }
        
//         final File newFile = createFile(filename);
//         Log.info("Unpacking " + newFile);
//         final FileOutputStream out = new FileOutputStream(newFile);
//         byte [] b = new byte[1024];
//         int len = 0;
//         int wrote = 0;
//         while ( (len=zin.read(b))!= -1 ) {
//             wrote += len;
//             out.write(b,0,len);
//         }
//         out.close();
//         if (DEBUG.IO) {
//             Log.debug("    Unzipped " + filename + "; wrote=" + wrote + "; size=" + entry.getSize());
//         }

//         return filename;
        
//     }

//     public static File createFile(String name)
//         throws IOException
//     {
//         final File file = new File(name);

//         File parent = file;
//         while ( (parent = parent.getParentFile()) != null) {
//             //Log.debug("Parent: " + parent);
//             if (parent.getPath().equals("/")) {
//                 //Log.debug("skipping " + parent);
//                 break;
//             }
//             if (!parent.exists()) {
//                 Log.debug("Creating: " + parent);
//                 parent.mkdir();
//             }
//         }
//         file.createNewFile();
//         return file;
//     }

//     private static void unzipIMSCP(ZipInputStream zin, ZipEntry entry)
//         throws IOException
//     {

//         unzip(zin, entry, VueUtil.getDefaultUserFolder().getAbsolutePath());
        
// //        String fname = VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+s;
// //         if (DEBUG.IO) System.out.println("unzipping " + s + " to " + fname);
// //         FileOutputStream out = new FileOutputStream(fname);
// //         byte [] b = new byte[512];
// //         int len = 0;
// //         while ( (len=zin.read(b))!= -1 ) {
// //             out.write(b,0,len);
// //         }
// //         out.close();
//     }
//     private static LWMap loadVueIMSCPArchive(File file)
//         throws java.io.FileNotFoundException,
//                java.util.zip.ZipException,
//                java.io.IOException
//     {
//             Log.info("Unpacking VUE IMSCP zip archive: " + file);
//             ZipFile zipFile = new ZipFile(file);
//             Vector<Resource> resourceVector = new Vector();
//             File resourceFolder = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+IMSCP.RESOURCE_FILES);
//             if(resourceFolder.exists() || resourceFolder.mkdir()) {
//                 ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
//                 ZipEntry e;
//                 while ((e=zin.getNextEntry()) != null) {
//                     unzipIMSCP(zin, e);
//                     //if (DEBUG.IO) System.out.println("ZipEntry: " + e.getName());  
//                     if(!e.getName().equalsIgnoreCase(IMSCP.MAP_FILE) && !e.getName().equalsIgnoreCase(IMSCP.MANIFEST_FILE)){
//                         // todo: may want to add a Resource.Factory.get(ZipEntry) method
//                         Resource resource = Resource.getFactory().get(e.getName());
//                         resourceVector.add(resource);
//                         //if (DEBUG.IO) System.out.println("Resource: " + resource);
//                     }
//                 }
//                 zin.close();
//             }
           
//             File mapFile  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+IMSCP.MAP_FILE);
//             LWMap map = ActionUtil.unmarshallMap(mapFile);
//             map.setFile(null);
//             map.setLabel(ZIP_IMPORT_LABEL);
//             for (Resource r : resourceVector) {
//                 replaceResource(map, r,
//                                 Resource.getFactory().get(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+r.getSpec()));
//                 //new URLResource(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+r.getSpec()));
//             }

//             map.markAsSaved();

//             return map;
//     }
//     public static void replaceResource(LWMap map,Resource r1,Resource r2) {
//         Iterator i = map.getAllDescendentsIterator();
//         while(i.hasNext()) {
//             LWComponent component = (LWComponent) i.next();
//             if(component.hasResource()){
//                 Resource resource = component.getResource();
//                 if(resource.getSpec().equals(r1.getSpec()))
//                     component.setResource(r2);
//             }
//         }
//     }
    
    /** test harness for opening a whole bunch of map files just to make sure we can parse and create an LWMap model from them */
    public static void main(String args[]) throws Exception {

        //VUE.parseArgs(args);
        VUE.debugInit(false);
        VUE.init(args);
        //VUE.initApplication();
        //new TextBox(null, "DEBUG-TEXTBOX");
        SaveAction.PACKAGE_DEBUG = true;
        DEBUG.IO = true;

        LWMap map = null;
        for (String arg : args) {
            map = null;
            if (arg.charAt(0) == '-')
                continue;
            System.err.println("Attempting to read map from " + arg);

            Throwable tx = null;
            try {
                if (arg.indexOf(':') >= 0)
                    map = OpenAction.loadMap(new java.net.URL(arg));
                else
                    map = OpenAction.doLoadMap(arg);
            } catch (OutOfMemoryError e) {
                System.err.println("@@@OUT OF MEMORY " + e);
                System.exit(-1);
            } catch (Throwable t) {
                t.printStackTrace();
                tx = t;
            }

            final Object result;

            if (map == null && tx != null) {
                if (tx.getCause() != null)
                    result = tx.getCause();
                else
                    result = tx;
            } else
                result = map;

            // If exception has multi-line content, and we're grepping output for '@@@',
            // we're ensure to include this token after the exception is printed, so
            // we can still see the file that failed.
            //Log.error(String.format("@@@Free: %4.1fm; Loaded: %-60s from @@@ %s\n",
            System.err.format("@@@Free: %5.1fm; Loaded: %-60s from @@@ [%s]\n",
                              (float) (Runtime.getRuntime().freeMemory() / (float) (1024*1024)),
                              result, arg);
            //System.err.println("@@@ARG[" + arg + "]");

            //System.out.println("@@@Loaded map: " + map + " from " + arg);
        }
        System.out.println("@@@Done.");

//         if (map != null) {
//             //SaveAction.writeArchive(map, new File("test.var"));
//             //createVUEArchive(map, new File("test.var"));
//         }

        

//         String file = args.length == 0 ? "test.xml" : args[0];
//         System.err.println("Attempting to read map from " + file);
//         DEBUG.Enabled = true;
//         LWMap map;
//         if (file.indexOf(':') >= 0)
//             map = OpenAction.loadMap(new java.net.URL(file));
//         else
//             map = OpenAction.loadMap(file);
//         System.out.println("Loaded map: " + map);
        
    }


    
    
}

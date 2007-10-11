/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
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

public class OpenAction extends VueAction
{
    public static final String ZIP_IMPORT_LABEL ="Imported";
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
            System.out.println("Action["+e.getActionCommand()+"] completed.");
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
    
    
    // todo: have only one root loadMap that hanldes files & urls -- actually, make it ALL url's
    public static LWMap loadMap(String filename) {
        try {
            return doLoadMap(filename);
        } catch (FileNotFoundException e) {
            // maybe move all exception code here, taking the file-not-found handling
            System.err.println("OpenAction.loadMap[" + filename + "]: " + e);
            VueUtil.alert(null, "\"" + filename + "\": file not found.", "Map Not Found");
        } catch (Throwable t) {
            // out of the Open File dialog box.
            System.err.println("OpenAction.loadMap[" + filename + "]: " + t);
            VueUtil.alert(null, "\"" + filename + "\" cannot be opened in this version of VUE.", "Map Open Error");
            t.printStackTrace();
        }
        return null;
    }

    private static LWMap doLoadMap(String filename)
        throws java.io.FileNotFoundException,
               java.util.zip.ZipException,
               java.io.IOException
    {
        if (DEBUG.CASTOR || DEBUG.IO) System.err.println("\nloadMap " + filename);
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

        if (isVueIMSCPArchive(file)) {
            Log.info("Unpacking VUE IMSCP zip archive: " + file);
            ZipFile zipFile = new ZipFile(file);
            Vector<Resource> resourceVector = new Vector();
            File resourceFolder = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+IMSCP.RESOURCE_FILES);
            if(resourceFolder.exists() || resourceFolder.mkdir()) {
                ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
                ZipEntry e;
                while ((e=zin.getNextEntry()) != null) {
                    unzip(zin, e.getName());
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
        } else {
            LWMap map = ActionUtil.unmarshallMap(file);
            return map;
        }
    }
    
    public static LWMap loadMap(java.net.URL url) {
        try {
            if (DEBUG.CASTOR) System.err.println("\nUnmarshalling from " + url);
            LWMap map = ActionUtil.unmarshallMap(url);
            return map;
        }
        catch (Exception e) {
            VueUtil.alert(null, "The following map can't be opened in current version of VUE.","Map Open Error");
            System.err.println("OpenAction.loadMap[" + url + "]: " + e);
            e.printStackTrace();
            return null;
        }
    }
    
    /** test harness for opening a whole bunch of map files just to make sure we can parse and create an LWMap model from them */
    public static void main(String args[]) throws Exception {

        VUE.parseArgs(args);

        LWMap map = null;
        for (String arg : args) {
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

            if (map == null) {
                if (tx.getCause() != null)
                    result = tx.getCause();
                else
                    result = tx;
            } else
                result = map;

            // If exception has multi-line content, and we're grepping output for '@@@',
            // we're ensure to include this token after the exception is printed, so
            // we can still see the file that failed.
            System.err.format("@@@Free: %4.1fm; Loaded: %-60s from @@@ %s\n",
                              (float) (Runtime.getRuntime().freeMemory() / (float) (1024*1024)),
                              result, arg);
            //System.err.println("@@@ARG[" + arg + "]");

            //System.out.println("@@@Loaded map: " + map + " from " + arg);
        }
        System.out.println("@@@Done.");

        if (map != null) {
            createVUEArchive(map, new File("test.var"));
        }

        

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

    // Could just unpack all resources into the cache, tho if it matches an existing
    // resource there, it will overwrite the old version, which isn't ideal, tho
    // allowing the old version + the new version violates the idea of atomic resources.
    // I guess we really need versionable resources (and ultimately track expiration).
    // For now, we can probably live with the overwrite, tho ideally we'd unpack the vue
    // archive with it's own cache directory (both for the automatic, or manual unzip
    // case) and the stored resource would have a hardcoded reference to it's special
    // cached version.
    

    private static void createVUEArchive(LWMap map, File zipFile)
        throws java.util.zip.ZipException,
               java.io.IOException
    {
        final String mapName = map.getLabel();
        final File mapFile = map.getFile();
        
        Log.debug("create archive of " + map + " to " + zipFile);
        zipFile.createNewFile();
        //ZipFile archive = new ZipFile(file);

        File tmpMapFile = File.createTempFile("vuetmp", ".vue");
        ActionUtil.marshallMap(tmpMapFile, map);

        Log.debug("created " + tmpMapFile);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        ZipEntry mapEntry = new ZipEntry(map.getLabel());
        //ZipEntry mapEntry = new ZipEntry("foo");
        mapEntry.setComment("[" + mapFile + "]");
        //mapEntry.setMethod(ZipEntry.DEFLATED);
        zos.putNextEntry(mapEntry);

        Log.debug("writing map to zip");
        
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(tmpMapFile));
        byte[] buf = new byte[1024];
        int len;
        int total = 0;
        while ((len = fis.read(buf)) > 0) {
            System.err.print(".");
            zos.write(buf, 0, len);
            total += len;
        }
        Log.debug("wrote " + total + " bytes");
        fis.close();
        zos.closeEntry();
        zos.close();
        
        Log.debug("done");
        
        
    }
    
    public static void unzip(ZipInputStream zin, String s) throws IOException {
        String fname = VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+s;
        if (DEBUG.IO) System.out.println("unzipping " + s + " to " + fname);
        FileOutputStream out = new FileOutputStream(fname);
        byte [] b = new byte[512];
        int len = 0;
        while ( (len=zin.read(b))!= -1 ) {
            out.write(b,0,len);
        }
        out.close();
    }
    
    public static void replaceResource(LWMap map,Resource r1,Resource r2) {
        Iterator i = map.getAllDescendentsIterator();
        while(i.hasNext()) {
            LWComponent component = (LWComponent) i.next();
            if(component.hasResource()){
                Resource resource = component.getResource();
                if(resource.getSpec().equals(r1.getSpec()))
                    component.setResource(r2);
            }
        }
    }
    private static boolean debug = true;
    
    /*
    private static Unmarshaller unmarshaller = null;
    private Unmarshaller getUnmarshaller()
    {
        if (unmarshaller == null) {
            unmarshaller = new Unmarshaller();
            Mapping mapping = new Mapping();
            try {
                if (debug) System.err.println("Loading " + XML_MAPPING + "...");
                mapping.loadMapping(XML_MAPPING);
                if (debug) System.err.println(" Loaded " + XML_MAPPING + ".");
                unmarshaller.setMapping(mapping);
                if (debug) System.err.println("The loaded mapping has been set on the unmarshaller.");
            } catch (Exception e) {
                System.err.println("OpenAction.getUnmarshaller: " + e);
            }
        }
        return unmarshaller;
    }
     */
    
}

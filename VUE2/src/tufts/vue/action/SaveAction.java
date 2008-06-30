/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
 * SaveAction.java
 *
 * Created on March 31, 2003, 1:33 PM
 */

package tufts.vue.action;


import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.zip.*;

import tufts.vue.*;
import tufts.Util;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueFrame;

import static tufts.vue.Resource.*;
    
/**
 * Save the currently active map.
 *
 * @author akumar03
 * @author Scott Fraize
 */
public class SaveAction extends VueAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SaveAction.class);

    private boolean saveAs = true;
    private boolean export = true;
    
    public SaveAction(String label, boolean saveType, boolean export){
        super(label, null, ":general/Save");
        setSaveAs(saveType);
        this.export = export;
    }
    
    public SaveAction(String label, boolean saveType)
    {
    	this(label,saveType,false);
    }
    public SaveAction(String label) {
        this(label, true,false);
    }
    
    public SaveAction() {
        this("Save", false,false);
    }
    
    public boolean isSaveAs() {
        return this.saveAs;
    }
    
    public void setSaveAs(boolean saveAs){
        this.saveAs = saveAs;
    }      
    
    /*    
    public void setFileName(String fileName) {
        file = new File(fileName);
    }
    
    public String getFileName() {
        return file.getAbsolutePath();
    }
    */
    
    private boolean inSave = false;
    public void actionPerformed(ActionEvent e)
    {
        if (inSave) // otherwise rapid Ctrl-S's will trigger multiple dialog boxes
            return;            	
        
        try {
            inSave = true;
            Log.info("Action["+e.getActionCommand()+"] invoked...");
            if (saveMap(tufts.vue.VUE.getActiveMap(), isSaveAs(),export))
                Log.info("Action["+e.getActionCommand()+"] completed.");
            else
                Log.info("Action["+e.getActionCommand()+"] aborted.");
        } finally {
            inSave = false;
        }
    }

    /**
     * @return true if success, false if not
     */
      
    public static boolean saveMap(LWMap map, boolean saveAs, boolean export)
    {
        Log.info("saveMap: " + map);        
        
        GUI.activateWaitCursor();         
       
        
        if (map == null)
            return false;
        
        File file = map.getFile();
        int response = -1;
        if (map.getSaveFileModelVersion() == 0) {

        	final Object[] defaultOrderButtons = { "Save a copy","Save"};
            response = JOptionPane.showOptionDialog
            (VUE.getDialogParent(),
        
       		"Saving "+ map.getLabel()+ " in this version of VUE will prevent older versions of VUE" +       				
              "\nfrom displaying it properly.  You may wish to save this map under a new name.",         
             "Version Notice: " + map.getLabel(),
             JOptionPane.YES_NO_OPTION,
             JOptionPane.PLAIN_MESSAGE,
             null,
             defaultOrderButtons,             
             "Save a copy"
             );
        }
        
        if (response == 0) {
            saveAs=true;
        } if ((saveAs || file == null) && !export) {
            //file = ActionUtil.selectFile("Save Map", "vue");
            file = ActionUtil.selectFile("Save Map", null);
        } else if (export) {
            file = ActionUtil.selectFile("Export Map", "export");
        }
            
        if (file == null) {
            //GUI.clearWaitCursor();
            try {
                return false;
            } finally {
                GUI.clearWaitCursor();
            }
        }

        try {
        	         	
            final String name = file.getName().toLowerCase();

            Log.info("saveMap: name[" + name + "]");
            
            if (name.endsWith(".rli.xml")) {
                new IMSResourceList().convert(map,file);
            }
            else if (name.endsWith(".xml") || name.endsWith(".vue")) {
                ActionUtil.marshallMap(file, map);
            }
            else if (name.endsWith(".jpeg") || name.endsWith(".jpg"))
                ImageConversion.createActiveMapJpeg(file,VueResources.getDouble("imageExportFactor"));
            else if (name.endsWith(".png"))
                ImageConversion.createActiveMapPng(file,VueResources.getDouble("imageExportFactor"));
            else if (name.endsWith(".svg"))
                new SVGConversion().createSVG(file);
            
            else if (name.endsWith(".pdf"))
            {
            	PresentationNotes.createMapAsPDF(file);
                //new PDFTransform().convert(file);
            }
            else if (name.endsWith(".zip"))
            {   Vector resourceVector = new Vector();
            	Iterator i = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
            	while(i.hasNext()) {	
            		LWComponent component = (LWComponent) i.next();
            		System.out.println("Component:"+component+" has resource:"+component.hasResource());
            		if(component.hasResource() && (component.getResource() instanceof URLResource)){
                    
            			URLResource resource = (URLResource) component.getResource();                    
                
            			//   	if(resource.getType() == Resource.URL) {
            			try {
                        // File file = new File(new URL(resource.getSpec()).getFile());
                        if(resource.isLocalFile()) {
                        	String spec = resource.getSpec();                        	                        
                        	System.out.println(resource.getSpec());
                            Vector row = new Vector();
                            row.add(new Boolean(true));
                            row.add(resource);
                            row.add(new Long(file.length()));
                            row.add("Ready");
                            resourceVector.add(row);
                        }
            			}catch (Exception ex) {
            				System.out.println("Publisher.setLocalResourceVector: Resource "+resource.getSpec()+ ex);
            				ex.printStackTrace();
            			}                    
            		}                
            	}
            	File savedCMap =PublishUtil.createZip(map, resourceVector);
            	 InputStream istream = new BufferedInputStream(new FileInputStream(savedCMap));
                OutputStream ostream = new BufferedOutputStream(new FileOutputStream(file));
                int fileLength = (int)savedCMap.length();
                byte bytes[] = new  byte[fileLength];
                try
                {
                	while (istream.read(bytes,0,fileLength) != -1)
                		ostream.write(bytes,0,fileLength);
                }
                catch(Exception e)
                {
                	e.printStackTrace();
                }
                finally
                {
                	istream.close();
                	ostream.close();
                }
            }
            //else if (name.endsWith(".html"))
              //  new HTMLConversion().convert(file);
            
            //else if (name.endsWith(".imap"))
            else if (name.endsWith(".html")) {
                new ImageMap().createImageMap(file);
            }
//             else if (name.endsWith(".htm")) {
//                 writeHTMLOutline(map, file);
//             }
            else if(name.endsWith(".rdf"))
            {
               edu.tufts.vue.rdf.RDFIndex index = new edu.tufts.vue.rdf.RDFIndex();
               
               String selectionType = VueResources.getString("rdf.export.selection");
               
               if(selectionType.equals("ALL"))
               {
                 Iterator<LWMap> maps = VUE.getLeftTabbedPane().getAllMaps();
                 while(maps.hasNext())
                 {
                     index.index(maps.next());
                 }
               }
               else if(selectionType.equals("ACTIVE"))
               {
                 index.index(VUE.getActiveMap());  
               }    
               else
               {    
                 index.index(VUE.getActiveMap());
               }  
               FileWriter writer = new FileWriter(file);
               index.write(writer);
               writer.close();
            }
            else if (name.endsWith(VueUtil.VueArchiveExtension))
            {
                Archive.writeArchive(map, file);
                
            } else {
                Log.warn("Unknown save type for filename extension: " + name);
                return false;
            }

			// don't know this as not all the above stuff is passing
            // exceptions on to us!
            Log.debug("Save completed for " + file);

            VueFrame frame = (VueFrame)VUE.getMainWindow();
            String title = VUE.getName() + ": " + name;                      
            frame.setTitle(title);
            
            if (name.endsWith(".vue"))
            {
             RecentlyOpenedFilesManager rofm = RecentlyOpenedFilesManager.getInstance();
             rofm.updateRecentlyOpenedFiles(file.getAbsolutePath());
            }
            return true;

        } catch (Throwable t) {
            Log.error("Exception attempting to save file " + file + ": " + t);
            Throwable e = t;
            if (t.getCause() != null)
                e = t.getCause();
            if (e instanceof java.io.FileNotFoundException) {
                Log.error("Save Failed: " + e);
            } else {
                Log.error("Save failed for \"" + file + "\"; " + e);
                tufts.Util.printStackTrace(e);
            }
            if (e != t)
                Log.error("Exception attempting to save file " + file + ": " + e);
        } finally {
            GUI.clearWaitCursor();
        }

        return false;
    }


//     public static final String MapArchiveKey = "@(#)TUFTS-VUE-ARCHIVE";

//     private static void setComment(ZipEntry entry, String comment) {

//         // Java bug, (STILL!) as of JAN 2008, comments are encoded in the zip file,
//         // but not extractable via any method call in any JDK.
//         // See: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6646605

//         // So we add comments anyway for easy debug, and then
//         // encode them again as extra bytes so we can extract them.
        
//         entry.setComment(comment);
//         entry.setExtra(comment.getBytes());
        
//     }

//     public static String getComment(ZipEntry entry) {

//         // See setComment for why we do this this way:

//         byte[] extra = entry.getExtra();
//         String comment = null;
//         if (extra != null && extra.length > 0) {
//             if (DEBUG.IO) Log.debug("getComment found " + extra.length + " extra bytes");
//             comment = new String(extra);
//             //comment = "extra(" + new String(extra) + ")";
//         }

//         return comment;
//     }


//     /**
//      * Generate a package file name from the given URL.  We could just as easily
//      * generate random names, but we base it on the URL for easy debugging and
//      * and exploring of the package in Finder/Explorer (e.g., we also try to make
//      * sure the documents have appropriate extensions so the OS shell applications
//      * can generate appropriate icons, etc.
//      */
    
//     // TODO: collisions still theoretically possible: keep a map of names and ensure
//     // unique-ness (could use PropertyMap.addProperty which automatically does this).
//     // Also, we're not using any of the other URL info beyond host/path, such as port,
//     // user, etc.  We don't need to, but would be nice for debugging.  Also, at some
//     // point we may need to deal with OS limitations on maximum file name lengths.

//     private static int BackupCount = 1;
    
//     private static String generatePackageFileName(URLResource r) {

//         String packageName = null;

//         try {
//             packageName = generateInformativePackageFileName(r);
//         } catch (Throwable t) {
//             Log.warn("Failed to create informative package name for " + r, t);
//         }

//         if (packageName == null)
//             packageName = String.format("vuedata%03d", BackupCount++); // note: BackupCount is static: not thread-safe

//         if (packageName.length() > 250) {
//             // 255 is the max length on modern Mac's an PC's
//             // We truncate to 250 in case we need a few extra chars for establishing uniqueness.
//             Log.info("Truncating long name: " + packageName);
//             packageName = packageName.substring(0, 250);
//         }

//         return packageName;
        
//     }
    
//     private static String generateInformativePackageFileName(URLResource r)
//         throws java.io.UnsupportedEncodingException
//     {
//         final URL url = r.getImageSource(); // better as URI?

//         if (url == null) {
//             // failsafe:
//             return r.getSpec();
//         }
        
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
            
//             //if (r.isImage() && r.hasProperty(IMAGE_FORMAT) && !Resource.looksLikeImageFile(url.getFile()))
//             if (r.isImage() && r.hasProperty(IMAGE_FORMAT) && !Resource.looksLikeImageFile(packageName))
//                 packageName += "." + r.getProperty(IMAGE_FORMAT).toLowerCase();
//         }

//         // Decode (to prevent any redundant encoding), then re-encode
//         packageName = java.net.URLDecoder.decode(packageName, "UTF-8");
//         packageName = java.net.URLEncoder.encode(packageName, "UTF-8");
//         // now "lock-in" the encoding: as this is now a fixed file-name, we don't ever want it to be
//         // accidentally decoded, which might create something that looks like a path when we don't want it to.
//         packageName = packageName.replace('%', '$');

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

//             // So Mac openURL doesn't decode these space indicators later when opening:
            
//             packageName = packageName.replaceAll("\\+", "\\$20");
            
//             // Replacing '+' with '-' is a friendler whitespace replacement (more
//             // readable), tho it's "destructive" in that the original URL could no
//             // longer be reliably reverse engineered from the filename.  We don't
//             // actually depending on being able to do that, but it's handy for
//             // debugging, and could be useful if we ever have to deal with any kind of
//             // recovery from data corruption.
            
//             //packageName = packageName.replace('+', '-');
//         }
        
        
//         return packageName;
//     }
                    
     
//     //private static final String OriginalSpecKey = "@ORIGINAL_SPEC";


//     /**
//      *
//      * Create a ZIP archive that contains the given map, as well as all resources who's
//      * data is currently available.  This means currently only data in local user files,
//      * or in the image cache can be archived. Non-image remote data (e.g., documents:
//      * Word, Excel, PDF, etc) cannot currently be archived.
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
    
//     public static void writeArchive(LWMap map, File archive)
//         throws java.io.IOException
//     {
//         Log.info("Writing archive package " + archive);

//         String mapName = map.getLabel();
//         if (mapName.endsWith(".vue"))
//             mapName = mapName.substring(0, mapName.length() - 4);

//         final String dirName = mapName + ".vdr";
        
//         final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archive)));
//         final ZipEntry mapEntry = new ZipEntry(dirName + "/" + mapName + "-map.vue");
//         final String comment = MapArchiveKey + "; VERSION: 1;"
//             + " Saved " + new Date() + " by " + VUE.getName() + " built " + Version.AllInfo
//             //+ "\n\tmap-name(" + mapName + ")"
//             //+ "\n\tunique-resources(" + resources.size() + ")"
//             ;
//         setComment(mapEntry, comment);
//         zos.putNextEntry(mapEntry);

//         final Map<LWComponent,Resource> savedResources = new IdentityHashMap();
//         final Map<Resource,Resource> clonedResources = new IdentityHashMap();
//         final Map<Resource,File> onDiskFiles = new IdentityHashMap();

//         BackupCount = 1;  // note: static variable -- not threadsafe

//         for (LWComponent c : map.getAllDescendents(LWComponent.ChildKind.ANY)) {

//             final Resource resource = c.getResource();

//             if (resource == null)
//                 continue;
            
//             if (resource instanceof URLResource == false) {
//                 Log.error("UNHANDLED NON-URLResource: " + Util.tags(resource));
//                 continue;
//             }
           
//             final URLResource r = (URLResource) resource;
            
//             File sourceFile = null;
//             boolean wasLocal = false;

//             if (r.hasProperty(PACKAGE_FILE)) {
//                 // we're saving something that came from an existing package
//                 sourceFile = new File(r.getProperty(PACKAGE_FILE));
//             } else if (r.isLocalFile()) {
//                 //Log.info("LOCAL FILE: " + r);
//                 sourceFile = new File(r.getSpec());
//                 wasLocal = true;
//             } else if (r.isImage()) {
//                 sourceFile = Images.findCacheFile(r);
//             }

//             //if (DEBUG.Enabled) Log.debug(r + "; sourceDataFile=" + sourceFile);
                
//             if (sourceFile != null && sourceFile.exists()) {
//                 savedResources.put(c, r);
//                 final URLResource cloned = (URLResource) r.clone();
//                 onDiskFiles.put(cloned, sourceFile);

//                 final String packageName = generatePackageFileName(r);
                    
//                 cloned.setProperty(PACKAGE_KEY, packageName);
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

// //         } else if (existingNames.contains(packageName)) {
// //             if (DEBUG.Enabled) Log.debug("Existing names already contains ["  +packageName + "]; " + existingNames);
// //             int cnt = 1;
// //             String uniqueName = packageName;
// //             while (existingNames.contains(uniqueName))
// //                 uniqueName = String.format("%s.%03d", packageName, cnt++);
// //             packageName = uniqueName;
// //             Log.info("Uniqified package name: " + packageName);
// //         }
// //        existingNames.add(packageName);
        
//         for (Resource r : uniqueResources) {

//             final Resource cloned = clonedResources.get(r);
//             final File sourceFile = onDiskFiles.get(cloned);
//             final String packageFileName = (cloned == null ? "[missing clone!]" : cloned.getProperty(Resource.PACKAGE_KEY));

//             ZipEntry entry = null;

//             if (sourceFile != null) {
//                 //final String fileName = java.net.URLEncoder.encode(file.getName(), "UTF-8");
// //                 try {
// //                     URI uri = new URI(file.getName());
// //                 } catch (Throwable t) {
// //                     Log.error(file.getName() + "; " + t.getMessage());
// //                 }
// //                 final String fileName = file.getName();
// //                 entry = new ZipEntry(dirName + "/" + fileName);
//                 entry = new ZipEntry(dirName + "/" + packageFileName);
//             }
            
// //             if (r.isLocalFile()) {

// //                 // TODO: handle duplicate basenames
                
// //                 file = new File(r.getSpec());
// //                 if (!file.exists()) {
// //                     Log.error("Missing local file: " + r);
// //                     continue;
// //                 }

// //                 if (cloned != null) {
// //                     Log.debug("Archiving local file: " + r);
// //                     Log.debug("       Archive clone: " + cloned);
// //                     //ZipEntry entry = new ZipEntry(dirName + "/" + file.getName());
// //                     entry = new ZipEntry(dirName + "/" + cloned.getSpec().substring(2));
// //                 } else {
// //                     Log.error("No archive map cloned resource found for: " + r);
// //                 }
// // //          } else if (r.getCacheFile() != null) {
// // //              file = r.getCacheFile();
// // //              Log.debug("Archiving cache file: " + r + "; " + file);
// // //              ZipEntry entry = new ZipEntry(dirName + "/" + file.getName());                
// //             } else if (r.isImage()) {
// //                 file = Images.getCacheFile(r);
// //                 if (file != null) {
// //                     Log.debug("Archiving cache file: " + r + "; " + file);
// //                     // COMPUTE FILE ONCE ABOVE SO ONLY NEED THIS CODE ONCE
// //                     // (and will need to anyway so the local file reference is right)
// //                     String name = file.getName();
// //                     if (name.startsWith("http%3A%2F%2F"))
// //                         name = name.substring(13);
// //                     entry = new ZipEntry(dirName + "/" + name);
// //                 }
// //             }

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
//                 Log.info(" skipping: " + debug);
//             } else {
//                 Log.info("archiving: " + entry + "; " + debug);

//                 // technically, putting the package file name in the comment is now be redunant:
//                 // it's now always same as the zip archive entry name
//                 //entry.setComment("\tname=" + packageFileName + "\n\tspec=" + r.getSpec());
//                 entry.setComment("\tspec=" + r.getSpec());
                
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

//     private static void copyBytesToZip(File file, ZipOutputStream zos)
//         throws java.io.IOException
//     {
//         final BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
//         byte[] buf = new byte[1024];
//         int len;
//         int total = 0;
//         while ((len = fis.read(buf)) > 0) {
//             //System.err.print(".");
//             zos.write(buf, 0, len);
//             total += len;
//         }
//         //Log.debug("wrote " + total + " bytes for " + file);
//         fis.close();
//     }
        


    private static void writeHTMLOutline(LWMap map, File file)
        throws IOException
    {
        PrintWriter out;
        if (file == null) {
            System.err.println("Writing to stdout");
            out = new PrintWriter(System.out, true);
        } else {
            System.out.println("Writing: " + file);
            out = new PrintWriter(file);
        }
            
        writeHTMLOutline(map, out);
        out.flush();
        out.close();
    }
    
    private static void writeHTMLOutline(LWMap map, PrintWriter out)
    {
        String title = map.getLabel();
        if (title.endsWith(".vue") || title.endsWith(".htm"))
            title = title.substring(0, title.length()-4);

        out.println("<html>");
        out.println("<head>");
        out.println("<title>" + title + "</title>");
        out.println("<!-- generated by " + tufts.vue.Version.WhatString + " -->");
        out.println("<!-- generated on " + new java.util.Date() + " from " + map.getFile() + " -->");
        out.println("</head>");
        out.println("<body>");
        //out.println("<h1>" + title + "</h1>");

        writeUnorderedNode(map, out);
        
        out.println("</body>");
        out.println("</html>");
    }

    // TODO: make successfully recursive to depth, with deeper ordered
    // lists, tracking level so we can use <h2> or whatever at top level,
    // shrinking as we go down.

    private static void writeUnorderedNode(LWComponent node, PrintWriter out)
    {
        final java.util.List<LWComponent> topChildren = node.getChildList();
        final LWComponent[] ordered = topChildren.toArray(new LWComponent[topChildren.size()]);
        java.util.Arrays.sort(ordered, LWComponent.GridSorter);

        for (LWComponent c : ordered) {
            if (c.hasLabel()) {
                out.print("<h2>");
                writeLabel(c, out);
                out.print("</h2>");
            }
            writeNode(c, out);
        }
    }
    
    private static void writeNode(LWComponent c, PrintWriter out) {
        if (c instanceof LWGroup || c instanceof LWSlide)
            writeUnorderedNode(c, out);
        else if (c instanceof LWLink)
            ;
        else
            writeOrderedChildren(c, out);
    }
    
    private static void writeOrderedChildren(LWComponent node, PrintWriter out)
    {
        
        if (node.hasChildren()) {
            out.println("<ul>");
            for (LWComponent c : node.getChildList()) {
                out.print("<li>");
                writeLabel(c, out);
                if (c.hasChildren())
                    writeNode(c, out);
                //writeNode(node, out);
                out.println();
            }
            out.println("</ul>");
        }
        
    }

    private static void writeLabel(LWComponent node, PrintWriter out)
    {

        if (node.hasResource())
            out.println("<a href=\"" + node.getResource() + "\">");

        java.awt.Font f = node.getFont();
        if (f.isBold()) out.print("<b>");
        if (f.isItalic()) out.print("<i>");

        boolean didFontColor = false;
        if (node.getTextColor() != null && !java.awt.Color.black.equals(node.getTextColor())) {
            out.print("<font color=\"" + node.getXMLtextColor() + "\">");
            didFontColor = true;
        }
        
        out.print(node.getLabel());
        if (f.isBold()) out.print("</b>");
        if (f.isItalic()) out.print("</i>");

        if (didFontColor)
            out.print("</font>");

        if (node.hasResource())
            out.print("</a>");
        

    }
    
    public static boolean saveMap(LWMap map) {
        return saveMap(map, false,false);
    }
    
    public static boolean PACKAGE_DEBUG = false;

    public static void main(String args[])
        throws IOException
    {
        //VUE.parseArgs(args);
        DEBUG.Enabled=true;
        DEBUG.IMAGE=true;
        PACKAGE_DEBUG = true;
        VUE.debugInit(false);
        Images.loadDiskCache();

        final LWMap map = ActionUtil.unmarshallMap(new File(args[0]));
        Log.debug("MAIN: Unmarshalled map: " + map);

        Archive.writeArchive(map, new File(map.getLabel().substring(0, map.getLabel().length() - 4) + VueUtil.VueArchiveExtension));
        //writeArchive(map, new File("test" + VueUtil.VueArchiveExtension));

        
//         if (args.length > 1)
//             writeHTMLOutline(map, new File(args[1]));
//         else
//             writeHTMLOutline(map, (File) null);

        
//         LWMap map = new LWMap("test");
//         map.setFile(new File("test.xml"));
//         map.addNode(new LWNode("Test Node"));
//         System.err.println("Attempting to save test map " + map);
//         DEBUG.Enabled = DEBUG.INIT = true;
//         new SaveAction().saveMap(map, false);
    }

    
    /*
    public void actionPerformed_writes_over_other_saved_maps(ActionEvent e)
    {
        System.out.println("Action["+e.getActionCommand()+"] invoked...");
         
        boolean saveCondition = true;
        
        if (isSaveAs() || file == null)
        {
          file = ActionUtil.selectFile("Save Map", "xml");
          
          if (file == null)
              saveCondition = false;
        }
        
        if (saveCondition == true)
        {
            if (file.getName().endsWith(".xml")) {
                LWMap map = tufts.vue.VUE.getActiveMap();
                map.setLabel(file.getName());
                ActionUtil.marshallMap(file, map);
            }
          else if (file.getName().endsWith(".jpeg"))
            new ImageConversion().createJpeg(file);
          
          else if (file.getName().endsWith(".svg"))
            new SVGConversion().createSVG(file);
          
          else if (file.getName().endsWith(".pdf"))
            new PDFTransform().convert(file);
          
          else if (file.getName().endsWith(".html"))
            new HTMLConversion().convert(file);
          
          System.out.println("Saved " + getFileName());
        }
            
        System.out.println("Action["+e.getActionCommand()+"] completed.");
    }
    */

    
}







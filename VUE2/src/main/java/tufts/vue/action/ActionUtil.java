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

import tufts.Util;
import tufts.vue.VueUtil;
import tufts.vue.VUE;
import tufts.vue.UrlAuthentication;
import tufts.vue.LWMap;
import tufts.vue.VueFileFilter;
import tufts.vue.VueResources;
import tufts.vue.XMLUnmarshalListener;
import tufts.vue.DEBUG;
import tufts.vue.gui.VueFileChooser;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.MarshalListener;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.UnmarshalListener;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.tufts.vue.preferences.PreferencesManager;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import java.net.URL;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.*;

/**
 * A class which defines utility methods for any of the action class.
 * Most of this code is for save/restore persistence thru castor XML.
 *
 * @version $Revision: 1.142 $ / $Date: 2010-02-03 19:13:45 $ / $Author: mike $
 * @author  Daisuke Fujiwara
 * @author  Scott Fraize
 */

public class ActionUtil
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ActionUtil.class);
    
    private final static String XML_MAPPING_CURRENT_VERSION_ID = VueResources.getString("mapping.lw.current_version");
    private final static URL XML_MAPPING_DEFAULT =      VueResources.getURL("mapping.lw.version_" + XML_MAPPING_CURRENT_VERSION_ID);
    private final static URL XML_MAPPING_UNVERSIONED =  VueResources.getURL("mapping.lw.version_none");
    private final static URL XML_MAPPING_OLD_RESOURCES =VueResources.getURL("mapping.lw.version_resource_fix");

    private final static String VUE_COMMENT_START = "<!-- Do Not Remove:";
    private final static String OUTPUT_ENCODING = "US-ASCII";
    private final static String DEFAULT_WINDOWS_ENCODING = "windows-1252"; // (a.k.a Cp1252) for reading pre ASCII enforced save files from Windows
    private final static String DEFAULT_MAC_ENCODING = "UTF-8"; // "MacRoman" not supported on Windows platform


    /**
     * Our default is to always read with an input encoding of UTF-8, even if the XML
     * was written with a US-ASCII encoding.  This is because pure ascii will translate
     * fine through UTF-8, but in case it winds up being that the XML was written out my
     * the marshaller with a UTF-8 encoding, we're covered.  (tho maybe with extremely
     * save files with platform specific encodings, (e.g, MacRoman or
     * windows-1255/Cp1255) we'll lose a special char here or there, such as left-quote
     * and right-quote).
     */
    private final static String DEFAULT_INPUT_ENCODING = "UTF-8"; // safest default input encoding
    
    // Note: the encoding format of the incoming file will normally either be UTF-8 for
    // older VUE save files, or US-ASCII for newer files.  In any case, the encoding is
    // indicated in the <?xml> tag at the top of the file, and castor handles adjusting
    // for it.  If we want to write UTF-8 files, we have to be sure the stream that's
    // created to write the file is created with the same encoding, or we sometimes get
    // problems, depending on the platform.  We always READ (unmarshall) via a UTF-8
    // stream, no matter what, as US-ASCII will pass through a UTF-8 stream untouched,
    // and it will handle UTF-8 if that turns out to be the encoding.
    
    public ActionUtil() {}
    
    /**A static method which displays a file chooser for the user to choose which file to save into.
       It returns the selected file or null if the process didn't complete*/
    protected static VueFileChooser saveChooser = null;        
    public static File selectFile(String title, final String fileType)
    {
        File picked = null;
       
    	
        	saveChooser =  VueFileChooser.getVueFileChooser();
    		
    	
    	saveChooser.setDialogTitle(title);
    	saveChooser.setAcceptAllFileFilterUsed(false);    
        //chooser.set
    	
        if (fileType != null && !fileType.equals("export"))
        	saveChooser.setFileFilter(new VueFileFilter(fileType)); 
        else if (fileType != null && fileType.equals("export"))
        { 
        	saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.JPEG_DESCRIPTION));
        	saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.PNG_DESCRIPTION));
            saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.SVG_DESCRIPTION));        	
        	saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.IMS_DESCRIPTION));
        	saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.IMAGEMAP_DESCRIPTION));
          //  chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.ZIP_DESCRIPTION));
        }
        else
        {
            VueFileFilter defaultFilter = new VueFileFilter(VueFileFilter.VUE_DESCRIPTION);
            
            saveChooser.addChoosableFileFilter(defaultFilter);  
            saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.VPK_DESCRIPTION));
        //SIMILE    chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.SIMILE_DESCRIPTION));
            saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.IMAGEMAP_DESCRIPTION));
            saveChooser.addChoosableFileFilter(new VueFileFilter("PDF"));
            
            saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.JPEG_DESCRIPTION));
            saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.PNG_DESCRIPTION));
            saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.SVG_DESCRIPTION));
            //chooser.addChoosableFileFilter(new VueFileFilter("html"));
            
            saveChooser.addChoosableFileFilter(new VueFileFilter("RDF"));
            saveChooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.IMS_DESCRIPTION));
         //   chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.ZIP_DESCRIPTION));
            
            //chooser.addChoosableFileFilter(new VueFileFilter("HTML Outline", "htm"));
            
            saveChooser.setFileFilter(defaultFilter); 
        }         
        saveChooser.addPropertyChangeListener(new PropertyChangeListener()
        {
			public void propertyChange(PropertyChangeEvent arg0) {				
				if (arg0.getPropertyName() == VueFileChooser.FILE_FILTER_CHANGED_PROPERTY)
				{
					adjustExtension();
				}
			}
        });
        adjustExtension();
        int option = saveChooser.showSaveDialog(VUE.getDialogParentAsFrame());//, VueResources.getString("dialog.save.title"));
        
        if (option == VueFileChooser.APPROVE_OPTION) 
        {
            picked = saveChooser.getSelectedFile();
            
            String fileName = picked.getAbsolutePath();

            /**
             * 2009-10-16 There was a bug in here that sliced up the filename, I think removing this block
             * of code should fix the issue. If you had a name like Object.Generic-Tufts, VUE would get
             * all confused and put files in the wrong directory or make a file called Object the filename
             * slicing seemed unnecessary. -MK
             * 
             */
            String extension = ((VueFileFilter)saveChooser.getFileFilter()).getExtensions()[0];  
            //if it isn't a file name with the right extension 
            if (!picked.getName().endsWith("." + extension)) {
                fileName += "." + extension;
                picked = new File(fileName);
            }
            
            if (picked.exists()) {            	
                int n = VueUtil.confirm(null, VueResources.getString("replaceFile.text") + " \'" + picked.getName() + "\'", 
                        VueResources.getString("replaceFile.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                  
                if (n == JOptionPane.NO_OPTION){
                	picked = null;                	
                	saveChooser.showDialog(VUE.getDialogParentAsFrame(), VueResources.getString("dialog.save.title"));                	
                }
            } 
            
            if (picked != null)
                VueUtil.setCurrentDirectoryPath(picked.getParent());
        }
        
        return picked;
    }
    
    private final static void adjustExtension()
    {
        String name = null;

    	try 
        {
		  name = saveChooser.getSelectedFile().getName();
        } 
        catch (Throwable t) {
          //Util.printStackTrace(t);
        }
		String baseName = null;
		String extension = ((VueFileFilter)saveChooser.getFileFilter()).getExtensions()[0];  
						
		if (name == null || (name !=null && name.length() <1))
			baseName = VUE.getActiveMap().getLabel();
		else
	       baseName = name;

		if (name == null || (name !=null && name.length() <1))
		{
			if (baseName.lastIndexOf(".") > 0)
				baseName = VUE.getActiveMap().getLabel().substring(0, baseName.lastIndexOf("."));
		}
		else
		{
			if (baseName.lastIndexOf(".") > 0)
				baseName = baseName.substring(0, baseName.lastIndexOf("."));
			
		}
		String curDir = saveChooser.getCurrentDirectory().getAbsolutePath();
		File f = new File(curDir+File.separator + baseName + "." + extension);
   		if (f.exists())
   			baseName = baseName  + "-copy"+"."+extension;			    		
   		else
   			baseName = baseName + "." + extension;
		      			        	
        	ActionUtil.saveChooser.setSelectedFile(new File(baseName));			        	
       
    }
   
    /**A static method which displays a file chooser for the user to choose which file to open.
       It returns the selected file or null if the process didn't complete
    TODO BUG: do not allow more than one dialog open at a time -- two "Ctrl-O" in quick succession
    will open two open file dialogs. */
    
    public static File openFile(String title, String extension)
    {
        File file = null;              
        
        VueFileChooser chooser = VueFileChooser.getVueFileChooser();
        
    //chooser.setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        int option = chooser.showOpenDialog(VUE.getDialogParent());
        
        if (option == VueFileChooser.APPROVE_OPTION) {
            final File chooserFile = chooser.getSelectedFile();
            if (chooserFile == null)
                return null;
            final String fileName;
            final String chosenPath = chooserFile.getAbsolutePath();
            
            // if they type a file name w/out an extension
            if (chooserFile.getName().lastIndexOf('.') < 0)
                fileName = chosenPath + "." + extension;
            else
                fileName = chosenPath;

            file = new File(fileName);

            if (file.exists()) {
                VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
            } else {
                File dir = new File(chosenPath);
                if (dir.exists() && dir.isDirectory()) {
                    //System.out.println("chdir " + chosenPath);
                    VueUtil.setCurrentDirectoryPath(chosenPath);
                } else {
                    Log.debug("File '" + chosenPath + "' " + file + " can't  be found.");
                    tufts.vue.VueUtil.alert(chooser,  VueResources.getString("actionutil.filenotfound.error")+" "+ file, VueResources.getString("actionutil.filenotfound.error"));
                }
                file = null;
            }
        }
        return file;
    }
    
    /**A static method which displays a file chooser for the user to choose which file to open.
    It returns the selected file or null if the process didn't complete
 	TODO BUG: do not allow more than one dialog open at a time -- two "Ctrl-O" in quick succession
 	will open two open file dialogs. */
 
    public static File[] openMultipleFiles(String title, String extension)
    {
    	File file = null;
     
    	VueFileChooser chooser = VueFileChooser.getVueFileChooser();        
    	
    	
    	chooser.setDialogTitle(title);
    	chooser.setMultiSelectionEnabled(true);
    	chooser.setFileFilter(new VueFileFilter(extension));
     
    	int option = chooser.showOpenDialog(VUE.getDialogParent());
     
    	if (option == VueFileChooser.APPROVE_OPTION) {
         final File[] chooserFile = chooser.getSelectedFiles();
         if (chooserFile == null)
             return null;
         final String fileName;
         
         if (chooserFile.length == 1)
         {
        	 //this scenario can only happen if there's only 1 file in the array...
         
        	 final String chosenPath = chooserFile[0].getAbsolutePath();
         
        	 // if they type a file name w/out an extension
        	 if (chooserFile[0].getName().lastIndexOf('.') < 0)
        		 fileName = chosenPath + "." + extension;
        	 else
        		 fileName = chosenPath;

        	 chooserFile[0] = new File(fileName);

        	 if (chooserFile[0].exists()) {
        		 VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
        	 } else {
        		 File dir = new File(chosenPath);
        		 if (dir.exists() && dir.isDirectory()) {
                 //System.out.println("chdir " + chosenPath);
        			 VueUtil.setCurrentDirectoryPath(chosenPath);
        		 } else {
        			 Log.debug("File '" + chosenPath + "' " + file + " can't  be found.");
        			 tufts.vue.VueUtil.alert(chooser, VueResources.getString("actionutil.filenotfound.error")+" "+ file, VueResources.getString("actionutil.filenotfound.error"));
        		 }
        		 chooserFile[0] = null;
        	 }
         	}
         return chooserFile;
    	}
    	return null;
    }

    
    /**
     * Return the current mapping used for saving new VUE data.
     */
    public static Mapping getDefaultMapping()
    {
        Object result = _loadMapping(XML_MAPPING_DEFAULT);
        if (result instanceof Exception) {
            VueUtil.alert(null, VueResources.getString("dialog.mappingfile.message")
                                          + "\n"+ VueResources.getString("dialog.mappingurl.message") + XML_MAPPING_DEFAULT
                                          + "\n" + result,
                                          VueResources.getString("dialog.mappingfile.title"), JOptionPane.ERROR_MESSAGE);
        }
        return (Mapping) result;
    }

    public static Unmarshaller getDefaultUnmarshaller()
        throws org.exolab.castor.mapping.MappingException
    {
        return getDefaultUnmarshaller(null, "(unknown source)");
    }
    public static Unmarshaller getDefaultUnmarshaller(String sourceName)
        throws org.exolab.castor.mapping.MappingException
    {
        return getDefaultUnmarshaller(null, sourceName);
    }


	    
    
    /**
     * Return the default unmarshaller for VUE data, which includes an installed
     * unmarshall listener, which is required for the proper restoration of VUE objects.
     */
    public static Unmarshaller getDefaultUnmarshaller(Mapping mapping, String sourceName)
        throws org.exolab.castor.mapping.MappingException
    {
        if (mapping == null)
            mapping = getDefaultMapping();
        
        // todo: can cache this with it's mapping set (tho need to cache by mapping,
        // as we still have different mapping files for old versions of the VUE save file)
        Unmarshaller unmarshaller = new Unmarshaller();
        
        unmarshaller.setIgnoreExtraAttributes(true);
        unmarshaller.setIgnoreExtraElements(true);
        unmarshaller.setValidation(false);
        unmarshaller.setObjectFactory(new XMLObjectFactory(sourceName));
        //unmarshaller.setWhitespacePreserve(true); // doesn't affect elements!  (e.g. <notes> foo bar </notes>)
        // HOWEVER: castor 0.9.7 now automatically encodes/decodes white space for attributes...
        unmarshaller.setLogWriter(new PrintWriter(System.err)); // todo: deprecated; now uses commons-logging

        if (DEBUG.XML) unmarshaller.setDebug(true);
        
        unmarshaller.setUnmarshalListener(new MapUnmarshalHandler(sourceName, "DEFAULT("+sourceName + ")"));
        unmarshaller.setMapping(mapping);

        if (DEBUG.CASTOR || DEBUG.XML || DEBUG.IO)
            Log.debug("got default unmarshaller for mapping " + mapping + " source " + sourceName);

        return unmarshaller;
    }
    

    private static Mapping getMapping(URL mappingSource) {
        if (DEBUG.IO) Log.debug("Fetching mapping: " + mappingSource);
        Object result = _loadMapping(mappingSource);
        if (result instanceof Mapping)
            return (Mapping) result;
        else
            return null;
    }
    
    private static HashMap LoadedMappings = new HashMap();
    /** return's a Mapping if successful, or an Exception if not.
     * Results are cached (if load was successful) for future calls.*/
    private static Object _loadMapping(URL mappingSource)
    //throws java.io.IOException //, org.exolab.castor.mapping.MappingException
    {
        if (!DEBUG.CASTOR && LoadedMappings.containsKey(mappingSource))
            return (Mapping) LoadedMappings.get(mappingSource);
        Mapping mapping = new Mapping();
        if (DEBUG.IO || DEBUG.INIT) Log.debug("Loading mapping " + mappingSource + "...");
        try {
            mapping.loadMapping(mappingSource);
        } catch (Exception e) { // MappingException or IOException
            e.printStackTrace();
            System.err.println("Failed to load mapping " + mappingSource);
            return e;
        }
        if (DEBUG.IO || DEBUG.INIT) Log.debug("Loaded mapping " + mappingSource);
        LoadedMappings.put(mappingSource, mapping);
        return mapping;
    }
    
    public static class VueMarshalListener implements MarshalListener {
        public boolean preMarshal(Object o) {
            //if (true||DEBUG.XML) Log.debug("VML  pre: " + Util.tags(o));
            //if (o instanceof tufts.vue.Resource)
            try {
                // TODO: create a ConditionalMarshalling interface for embedding this logic
                // in the client classes so it's not kept here.

                // Note that ALL objects pass through here.
                
                String key = null;
                if (o.getClass() == tufts.vue.PropertyEntry.class) // is final class
                    key = ((tufts.vue.PropertyEntry)o).getEntryKey();
                
                if (key != null && tufts.vue.Resource.isRuntimePropertyKey(key)) {
                    if (DEBUG.XML) Log.debug(" no-marshal " + Util.tags(o));
                    return false;
                } else {
                    if (DEBUG.XML) Log.debug("marshalling " + Util.tags(o));
                    return true;
                }
            } catch (Throwable t) {
                Util.printStackTrace(t, "Marshalling condition failure on " + o);
            }
            return true;
        }
        public void postMarshal(Object o) {
            //if (true||DEBUG.XML) Log.debug("VML post: " + Util.tags(o));
        }
    }
    
    /*
     * This method checks whether a file can be safely saved and opened by castor
     * @param map the map whose save compatiblity needs to be tested
     * @returrn true if compatible to castor save, false otherwise 
     */
    public static boolean isCastorCompatible(LWMap map) {
        try {
            File tempFile  = File.createTempFile("vueTest","vue");
            tempFile.deleteOnExit();
            marshallMap(tempFile,map);
            unmarshallMap(tempFile);
            return true;
        } catch(Throwable t) {
              tufts.vue.VueUtil.alert(VueResources.getString("actionutil.filesave.error") + t, VueResources.getString("actionutil.filesave.title"));
            Log.error("Testing save: "+map+";"+t);
            Util.printStackTrace(t);
        }
        return false;
    }

    private static class WrappedMarshallException extends RuntimeException {
        WrappedMarshallException(Throwable cause) {
            super(cause);
        }
    }

    /**A static method which creates an appropriate marshaller and marshal the active map*/
    public static void marshallMap(File file)
    {
        marshallMap(file, tufts.vue.VUE.getActiveMap());
    }

    /**
     * Marshall the given map to XML and write it out to the given file.
     */
    public static void marshallMap(File targetFile, LWMap map) {
        File tmpFile = null;

        try {
            tmpFile  = File.createTempFile(targetFile.getName() + "$new",
                                           ".vue",
                                           targetFile.getParentFile());
            if (DEBUG.IO) Log.debug("created new tmp file " + tmpFile);
            doMarshallMap(targetFile, tmpFile, map);
        } catch (Throwable t) {
            if (t instanceof WrappedMarshallException)
                t = t.getCause();
            Log.error("marshalling: " + map + "; to " + tmpFile + "; destination: " + targetFile, t);
            // until everyone has chance to update their code
            // to handle the exceptions, wrap this in a runtime exception.
            throw new RuntimeException(t);
        }
        //if (DEBUG.EnableD) Log.debug("

        if (targetFile.exists()) {
            File backup = null;
            try {
                final String backupName = String.format(".~%s", targetFile.getName());
                //if (DEBUG.IO) Log.debug(String.format("creating backup named [%s]", backupName));
                backup = new File(targetFile.getParent(), backupName);
                if (backup.delete())  // Required on Win32 or rename will fail
                    Log.info("  deleted prior backup: " + backup);
                Log.info("renaming old to backup: " + backup);
                if (!targetFile.renameTo(backup))
                    Log.warn("failed to make backup of " + targetFile);
            } catch (Throwable t) {
                Log.warn("backup failed: " + backup, t);
            }
        }
        
        Log.info("renaming new to target: " + targetFile);
//         if (targetFile.delete()) // Required on Win32 or rename will fail
//             Log.warn("deleted prior save file: " + targetFile);
        if (!tmpFile.renameTo(targetFile)) {
            Log.error("Failed to rename temp file " + tmpFile + "; to target file: " + targetFile);
            //Object obj[] = {tmpFile,targetFile};
           // VueResources.getFormatMessage(obj, pattern)
            VueUtil.alert(String.format(Locale.getDefault(),VueResources.getString("actionutil.rename.error"),
                                        tmpFile, targetFile),
                                        VueResources.getString("actionutil.rename.title"));
            
        }
    }
    
    private static void doMarshallMap(final File targetFile, final File tmpFile, final LWMap map)
        throws java.io.IOException,
               org.exolab.castor.xml.MarshalException,
               org.exolab.castor.xml.ValidationException,
               org.exolab.castor.mapping.MappingException
    {
        final String path = tmpFile.getAbsolutePath().replaceAll("%20"," ");
        final FileOutputStream fos = new FileOutputStream(path);
        final OutputStreamWriter writer;
        FileDescriptor FD = null;
        
        try {
            FD = fos.getFD();
            // getting the FileDescriptor is not required -- we just use it
            // for a final call to sync after we save to increase the likelyhood
            // of our save file actually making it to disk.
        } catch (Throwable t) {
            Log.warn("No FileDescriptor for " + path + "; failsafe sync will be skipped: " + t);
        }
        
        if (OUTPUT_ENCODING.equals("UTF-8") || OUTPUT_ENCODING.equals("UTF8")) {
            writer = new OutputStreamWriter(fos, OUTPUT_ENCODING);
        } else {
            
            // For the actual file writer we can use the default encoding because we're
            // marshalling specifically in US-ASCII.  E.g., because we direct castor to
            // fully encode any special characters via setEncoding("US-ASCII"), we'll
            // only have ASCII chars to write anyway, and any default encoding will
            // handle that...
            
            writer = new OutputStreamWriter(fos);
            
            // below creates duplicate FOS, but may be better for debug?  (MarshallException's can find file?)
            //if (FD == null)
            //    writer = new FileWriter(path);
            //else
            //    writer = new FileWriter(FD);
            
        }

        if (DEBUG.IO) {
            try {
                Log.debug(String.format("%s; %s; encoding: \"%s\", which will represent \"%s\" XML content",
                                        tmpFile,
                                        writer,
                                        writer.getEncoding(),
                                        OUTPUT_ENCODING));
            } catch (Throwable t) {
                Log.warn(t);
            }
        }

        //=======================================================
        // Marshall the map to the tmp file:
        // ---------------------------------

        marshallMapToWriter(writer, map, targetFile, tmpFile);
        
        //=======================================================

        // Run a filesystem sync if we can just to be sure: Especially helpful on some
        // linux file systems, such as Ext3, which may not normally touch the disk for
        // another 5 seconds, or XFS/Ext4, which may take their own sweet time.
        // For more see:
        // https://bugs.launchpad.net/ubuntu/+source/linux/+bug/317781/comments/54
        
        if (DEBUG.IO) Log.debug("flushing " + writer);
        writer.flush();
        
        if (FD != null) {
            try {
                if (DEBUG.IO) Log.debug("syncing " + FD + "; for " + tmpFile);
                // just as backup -- must however be done before writer.close()
                FD.sync();
                Log.info(" sync'd " + FD + "; for " + tmpFile);
            } catch (Throwable t) {
                Log.warn("after save to " + targetFile + "; sync failed: " + t);
            }
        }

        if (DEBUG.IO) Log.debug("closing " + writer);
        writer.close();
        if (DEBUG.IO) Log.debug(" closed " + writer);
        
    }

    /**
     * Marshall the given map to the given Writer without touching the map in any
     * way.
     */
    static void marshallMapToWriter(final LWMap map, final Writer writer)
        throws java.io.IOException,
               org.exolab.castor.xml.MarshalException,
               org.exolab.castor.xml.ValidationException,
               org.exolab.castor.mapping.MappingException
    {
        marshallMapToWriter(writer, map, null, null);
    }
    
    /**
     * @param file - if null, map state is untouched, otherwise, map state is updated
     */
    private static void marshallMapToWriter(final Writer writer,
                                            final LWMap map,
                                            final File targetFile,
                                            final File tmpFile)
        throws java.io.IOException,
               org.exolab.castor.xml.MarshalException,
               org.exolab.castor.xml.ValidationException,
               org.exolab.castor.mapping.MappingException
    {
        map.makeReadyForSaving(targetFile);
        
        Log.info("marshalling " + map + " to: " + tmpFile);
        
        Marshaller marshaller = null;
        String name = "";
        if (targetFile != null)
            name = targetFile.getName();
        else
            name = map.getLabel();
        if (name == null)
            name = "";

        final java.util.Date date = new java.util.Date();
        final String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);

        String headerText = VueResources.getString("vue.version") + " concept-map (" + name + ") " + today;
        
        headerText = org.apache.commons.lang.StringEscapeUtils.escapeXml(headerText);
        
        writer.write("<!-- Tufts VUE " + headerText + " -->\n");
        
        writer.write("<!-- Tufts VUE: http://vue.tufts.edu/ -->\n");
        
        writer.write(VUE_COMMENT_START
                     + " VUE mapping "
                     + "@version(" + XML_MAPPING_CURRENT_VERSION_ID + ")"
                     + " " + XML_MAPPING_DEFAULT
                     + " -->\n");
        writer.write(VUE_COMMENT_START
                     + " Saved date " + date
                     + " by " + VUE.getSystemProperty("user.name")
                     + " on platform " + VUE.getSystemProperty("os.name")
                     + " " + VUE.getSystemProperty("os.version")
                     + " in JVM " + VUE.getSystemProperty("java.runtime.version")
                     + " -->\n");
        writer.write(VUE_COMMENT_START
                     + " Saving version " + tufts.vue.Version.WhatString
                     + " -->\n");
        if (DEBUG.CASTOR || DEBUG.IO) Log.debug("Wrote VUE header to " + writer);
        marshaller = new Marshaller(writer);
        //marshaller.setDebug(DEBUG.CASTOR);
        marshaller.setEncoding(OUTPUT_ENCODING);
        // marshaller.setEncoding("UTF-8");
          // marshal as document (default): make sure we add at top: <?xml version="1.0" encoding="<encoding>"?>
        marshaller.setMarshalAsDocument(true);
        marshaller.setNoNamespaceSchemaLocation("none");
        marshaller.setMarshalListener(new VueMarshalListener());
        // setting to "none" gets rid of all the spurious tags like these:
        // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"

        //marshaller.setDoctype("foo", "bar"); // not in 0.9.4.3, must wait till we can run 0.9.5.3+

        /*
          marshaller.setMarshalListener(new MarshalListener() {
          public boolean preMarshal(Object o) {
          System.out.println(" preMarshal " + o.getClass().getName() + " " + o);
          return true;
          }
          public void postMarshal(Object o) {
          System.out.println("postMarshal " + o.getClass().getName() + " " + o);
          }
          });
        */

        //marshaller.setRootElement("FOOBIE"); // overrides name of root element
            
        marshaller.setMapping(getDefaultMapping());

        //----------------------------------------------------------------------------------------
        // 
        // 2007-10-01 SMF -- turning off validation during marshalling now required
        // w/castor-1.1.2.1-xml.jar, otherwise, for some unknown reason, LWLink's
        // with any connected endpoints cause validation exceptions when attempting to
        // save.  E.g, from a map with one node and one link connected to it:
        //
        // ValidationException: The following exception occured while validating field: childList of class:
        // tufts.vue.LWMap: The object associated with IDREF "LWNode[2         "New Node"  +415,+24 69x22]" of type
        // class tufts.vue.LWNode has no ID!;
        // - location of error: XPATH: /LW-MAP
        // The object associated with IDREF "LWNode[2         "New Node"  +415,+24 69x22]" of type class tufts.vue.LWNode has no ID!
        //
        // Even tho the node's getID() is correctly returning "2"
        //
        marshaller.setValidation(false); 
        //----------------------------------------------------------------------------------------
            
        marshaller.setLogWriter(new PrintWriter(System.err));  // todo: deprecated; now uses commons-logging

        // Make modifications to the map at the last minute, so any prior exceptions leave the map untouched.

        final int oldModelVersion = map.getModelVersion();
        final File oldSaveFile = map.getFile();
        if (targetFile != null) {
            map.setModelVersion(LWMap.getCurrentModelVersion());
            // note that if this file is different from it's last save file, this
            // operation may cause any/all of the resources in the map to be
            // updated before returning.
            map.setFile(targetFile);
        }
       // map.addNode(new tufts.vue.LWNode("Hello "+((char)15)+((char)23)));
        
        //if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Marshalling " + map + " ...");
        Log.debug("marshalling " + map + " ...");
        writer.flush();
        //map.addNode(new tufts.vue.LWNode("Hello World:"+((char)11)));
        try {

            //-----------------------------------------------------------------------------
            //-----------------------------------------------------------------------------
            // try the test map first
            // TODO: DOES NOT ACTUALLY DO A TEST WRITE FIRST
            // It will still completely blow away the user's map if there is any kind of error.
            // Was this ever tested?
            //-----------------------------------------------------------------------------
            //-----------------------------------------------------------------------------

            marshaller.marshal(map);
            writer.flush();
            if (DEBUG.Enabled) Log.debug("marshalled " + map + " to " + writer + "; file=" + tmpFile);
        } catch (Throwable t) {
            Log.error(tmpFile + "; " + map, t);

            //-----------------------------------------------------------------------------
            // This was a poor choice of message.  This describes just one of many,
            // many errors that may occur, and can be entirely misleading.
            // VueUtil.alert("The map contains characters that are not supported. Reverting to earlier saved version",
            //-----------------------------------------------------------------------------

            VueUtil.alert(VueResources.getString("actionutil.filecorrupted.error")+"\n\n"
                          + VueResources.getString("actionutil.filecorrupted.description")+"\n" + Util.formatLines(t.toString(), 80),
                          VueResources.getString("actionutil.rename.title"));
            
            try {
                if (targetFile != null) {
                    // revert map model version & save file
                    map.setModelVersion(oldModelVersion);
                    map.setFile(oldSaveFile);
                }
            } catch (Throwable tx) {
                VueUtil.alert(Util.formatLines(tx.toString(), 80), VueResources.getString("actionutil.internalsave.title"));    
                Util.printStackTrace(tx);
            } finally {
                throw new WrappedMarshallException(t);
            }
        }
            
        if (tmpFile != null) {
            try {
                // should never fail, but if it does, the save itself has still worked
                map.markAsSaved();
            } catch (Throwable t) {
                Log.error(t);
            }
            try {
                Log.info("wrote " + map + " to " + tmpFile);
            } catch (Throwable t) {
                Log.error("debug", t);
            }
        }
    }

    public static LWMap unmarshallMap(File file)
        throws IOException
    {
        return unmarshallMap(file, null);
    }
    
    /** Unmarshall a LWMap from the given file (XML map data) */
    public static LWMap unmarshallMap(File file, MapUnmarshalHandler handler)
        throws IOException
    {
        if (file.isDirectory())
            throw new MapException("is a directory, not a map file: " + file);
        if (!file.exists())
            throw new FileNotFoundException("does not exist");
        if (file.length() == 0)
            throw new EmptyFileException();
         
        return unmarshallMap(file.toURL(), handler);
    }

    private static class MapReader {
        final BufferedReader reader;
        final File file;

        MapReader(BufferedReader r, File f) {
            reader = r;
            file = f;
        }
    }

    private static MapReader getMapReaderForURL(URL url, String charsetEncoding, boolean allowRedirects)
        throws java.io.IOException
    {

        // Not sure if it's still worth paying attention to the encoding, but we do use
        // any special encoding (if found) from the map file just in case.  All
        // current-day VUE maps should be in pure US-ASCII, so we could theoretically
        // ignore the encoding, but this may be needed for some very old maps.

        File file = tufts.vue.Resource.getLocalFileIfPresent(url);

        if (file != null && file.isDirectory())
            throw new MapException("cannot open " + file + ": is directory");
        
        Reader reader = null;
        
        try {
                
            if (file != null) {
            
                if (charsetEncoding != null)
                    reader = new InputStreamReader(new FileInputStream(file), charsetEncoding);
                else
                    reader = new FileReader(file); // could default to UTF-8
                
            } else {

                // No local file was found: it must have been a remote URL
            
                if (allowRedirects) {
                    final URL redirectURL = tufts.vue.UrlAuthentication.getRedirectedUrl(url, 10); // number of redirects to follow
                    url = redirectURL;
                     file = new File(url.getFile());
                    // AK: Thanks for leaving the comments. 
                    // we need to open the redirect url
                    // SMF 2008-04-08: Anoop's semantics as of 2008-03-12: we do NOT
                    // open the redirect url for reading, we open the original.  Not
                    // sure if this was intended.
                    
                    // This would allow opening the redirect:
                    // url = redirectURL;
                }
                
                if (charsetEncoding != null)
                    reader = new InputStreamReader(UrlAuthentication.getAuthenticatedStream(url), charsetEncoding);
                else
                    reader = new InputStreamReader(UrlAuthentication.getAuthenticatedStream(url)); // could default to UTF-8

 
            }
        
        } catch (Throwable t) {
            Log.error("Could not get reader for: " + file + "; source url=" + url, t);
        }
            
        if (reader == null) {
            Log.error("No reader found for " + Util.tags(url));
            throw new MapException("no reader found for: " + url);
        }

        Log.debug("got reader for " + Util.tags(url) + "; encoding=" + charsetEncoding + ": " + reader);

        return new MapReader(new BufferedReader(reader), file);


// Anoop code as of 2008-03-12:
//         final InputStream urlStream;
//          final File file; 
            
//         if ("file".equals(url.getProtocol())){
//             //FIX to deal with # problems in the filename
// //         System.out.println("URL: "+url);
//              file = new File(url.toString().substring(5));
//              urlStream =  new BufferedInputStream(new FileInputStream(file)); // remove file:/ from the begining of the file
             
//           //  urlStream = url.openStream();
//         } else {
//               redirectedUrl = tufts.vue.UrlAuthentication.getRedirectedUrl(url,10); // number of redirects to follow
//               file = new File(redirectedUrl.getFile());
//               urlStream = tufts.vue.UrlAuthentication.getAuthenticatedStream(url);
//             // urlStream =  url.openStream();
           
//         }
//         final BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream, charsetEncoding));


        
//         if ("file".equals(url.getProtocol())) {
           
//             File file = new File(url.getPath());
//              if(url.toString().contains("#"))  {
//                  file = new File(url.getPath()+"#"+url.getRef()); // special case for dealing with # in filename
//             }

//             if (file.isDirectory())
//                 throw new MapException("is directory");
            
//             reader = new BufferedReader(new FileReader(file));
//         } else {
//             reader = new BufferedReader(new InputStreamReader(tufts.vue.UrlAuthentication.getAuthenticatedStream(url)));
//             //reader = new BufferedReader(new InputStreamReader(url.openStream()));
//         }
        
    }

    /**
     * Input encoding shouldn't matter for the bootstrap reading of the first few lines
     * of the file (as they should be all US_ASCII), tho using DEFAULT_INPUT_ENCODING,
     * instead of null (which will get us the local platform default), would probably
     * make the most sense.  We're leaving this as the default platform encoding (null) for now
     * only because it's been this way for a while...  SMF 2008-04-08
     */
    private static final String BOOTSTRAP_ENCODING = null;
        
    public static LWMap unmarshallMap(java.net.URL url)
        throws IOException
    {
        return unmarshallMap(url, null);
    }
    
    public static LWMap unmarshallMap(java.net.URL url, MapUnmarshalHandler handler)
        throws IOException
    {
        // We scan for lines at top of file that are comments.  If there are NO comment lines, the
        // file is of one of our original save formats that is not versioned, and that may need
        // special processing for the Resource class to Resource interface change over.  If there
        // are comments, the version instance of the string "@version(##)" will set the version ID
        // to ##, and we'll use the mapping appropriate for that version of the save file.

        if (DEBUG.CASTOR || DEBUG.IO) {
            Log.debug("unmarshallMap: " + Util.tags(url));
            //Util.printStackTrace("UM " + url);
        }

        final BufferedReader reader = getMapReaderForURL(url, BOOTSTRAP_ENCODING, false).reader;
        
        String firstNonCommentLine;
        String versionID = null;
        boolean savedOnWindowsPlatform = false;
        boolean savedOnMacPlatform = false;
        String guessedEncoding = null;
        Mapping mapping = null;
    
        // We need to skip past the comments to position the reader at the <?xml line for
        // unmarshalling to start.  Also, we look at these comments to determine version of the
        // mapping to use, as well as if it's a pre VUE 1.5 (August 2006) save file, in which case
        // we must guess an encoding, and re-open the file using an InputStreamReader with the
        // proper encoding.

        String savingVersion = "unknown VUE version";

        for (;;) {
            reader.mark(2048); // a single comment line can't be longer than this...
            String line = reader.readLine();
            if (line == null) {
                Log.error("Unexpected end-of-stream in [" + url + "]");
                throw new java.io.IOException("end of stream in " + url);
            }
            if (DEBUG.CASTOR || DEBUG.IO) Log.debug("Scanning[" + line + "]");
            if (line.startsWith("<!--") == false) {
                // we should have juadst hit thie "<?xml ..." line -- done with comments
                firstNonCommentLine = line;
                break;
            }

            if (line.startsWith(VUE_COMMENT_START + " Saved")) {

                // The "saved on platform" comments were never expected to be used functionally
                // (only for debug), so determining if the save file was written on a Windows box
                // it's not 100% reliable: e.g., if a user somehow had the name "platform Windows",
                // we would mistake this "saved by" for a "saved on", but we're just going to take
                // this risk -- this is just a workaround backward compat hack because castor
                // wasn't naming the real encoding in it's XML output (turns out it was always
                // puting UTF-8), even if it was using the default Windows encoding of
                // Cp1252/windows-1252.
                
                //if (DEBUG.IO) System.out.println("scanning for Windows platform...");
                if (line.indexOf("platform Windows") > 0) {
                    if (DEBUG.IO) Log.debug(url + " was saved in the Windows environment");
                    savedOnWindowsPlatform = true;
                } else if (line.indexOf("platform Mac") > 0) {
                    if (DEBUG.IO) Log.debug(url + " was saved in the Mac environment");
                    savedOnMacPlatform = true;
                }
            } else if (line.startsWith(VUE_COMMENT_START + " Saving version")) {
                if (DEBUG.IO) Log.debug("Found saving version line: " + line);
                final int savingVersionIndex = line.indexOf("VUE");
                if (savingVersionIndex > 0) {
                    savingVersion = line.substring(line.indexOf("VUE"), line.length());
                    if (savingVersion.indexOf("-->") > 10)
                        savingVersion = savingVersion.substring(0, savingVersion.indexOf("-->"));
                    savingVersion = savingVersion.trim();
                } else {
                    Log.warn(url + ": unknown saving version XML comment [" + line + "]");
                }
                if (DEBUG.IO) Log.debug("Saving version: [" + savingVersion + "]");
            }
                
            
            
            // Scan the comment line for a version tag to base our mapping on:
            int idx;
            if ((idx = line.indexOf("@version(")) >= 0) {
                String s = line.substring(idx);
                //System.out.println("Found version start:" + s);
                int x = s.indexOf(')');
                if (x > 0) {
                    versionID = s.substring(9,x);
                    if (DEBUG.CASTOR || DEBUG.IO) Log.debug(url + "; Found mapping version ID[" + versionID + "]");
                    if (versionID.equals(XML_MAPPING_CURRENT_VERSION_ID)) {
                        mapping = getDefaultMapping();
                    } else {
                        URL mappingURL = VueResources.getURL("mapping.lw.version_" + versionID);
                        if (mappingURL == null) {
                            Log.error("Failed to find mapping for version tag [" + versionID + "], attempting default.");
                            mapping = getDefaultMapping();
                        } else {
                            mapping = getMapping(mappingURL);
                        }
                    }
                }
            }
        }
        reader.close();

        if (firstNonCommentLine.startsWith("<?xml")) {
            // Check to see if we need to use a non-default input encoding.
            // NOTE: We make sure we only attempt guessedEncoding if the given encoding is
            // the default input encoding: otherwise assume we're here recursively,
            // after already guessing at an encoding (otherwise, we'll loop, and blow stack)
            if (DEBUG.IO) Log.debug("XML head [" + firstNonCommentLine + "]");
            
            if (firstNonCommentLine.indexOf("encoding=\"UTF-8\"") > 0) {

                boolean localEncoding = false;
                
                // If encoding is UTF-8, this a 2nd generation save file (mapping is
                // versioned, but not all US-ASCII encoding): the actual encoding is
                // unknown: make a guess as how to best handle it.  This is our rule: if
                // we're on the SAME platform as the save file, assuming the local
                // encoding (assume it's the same user, on the same machine, and the
                // current default system encoding is the same one that was active when
                // the file was originally saved).  If we're on a different platform,
                // assume a default encoding for that platform.
                
                if (Util.isWindowsPlatform()) {
                    if (savedOnWindowsPlatform) {
                        localEncoding = true;
                    } else if (savedOnMacPlatform)
                        guessedEncoding = DEFAULT_MAC_ENCODING;
                    else
                        guessedEncoding = DEFAULT_WINDOWS_ENCODING;
                } else if (Util.isMacPlatform()) {
                    if (savedOnMacPlatform)
                        localEncoding = true;
                    else if (savedOnWindowsPlatform)
                        guessedEncoding = DEFAULT_WINDOWS_ENCODING;
                    else
                        guessedEncoding = DEFAULT_MAC_ENCODING;
                }
                
                if (localEncoding)
                    guessedEncoding = Util.getDefaultPlatformEncoding();
                    
                Log.info(url + "; assuming "
                             + (localEncoding ? "LOCAL " : "PLATFORM DEFAULT ")
                             + "\'" + guessedEncoding + "\' charset encoding");

                // Note: doing this is a real tradeoff amongst bugs: any old save file
                // that had fancy unicode characters in UTF, such as something in a
                // japanese charset, will be screwed by this, so we optimizing for what
                // we think is the most likely case.  if this becomes a real problem, we
                // could introduce a special convert dialog.  Also, pre US-ASCII save
                // files could have different strings in them saved in many DIFFERENT
                // charsets (e.g., japanese, UTF, etc), and it's complete luck as to
                // when those charsets would each be properly handled.
                
            }
        } else {
            Log.warn("Missing XML header in [" + firstNonCommentLine + "]");
        }

        boolean oldFormat = false;

        if (versionID == null && mapping == null) {
            oldFormat = true;
            Log.info(url + "; save file is of old pre-versioned type.");
            mapping = getMapping(XML_MAPPING_UNVERSIONED);
        }
        
        if (mapping == null)
            mapping = getDefaultMapping();        

        final String encoding = guessedEncoding == null ? DEFAULT_INPUT_ENCODING : guessedEncoding;

        return unmarshallMap(url, mapping, encoding, oldFormat, savingVersion, handler);
    }


    private static LWMap unmarshallMap(final java.net.URL url,
                                       Mapping mapping,
                                       String charsetEncoding,
                                       boolean allowOldFormat,
                                       String savingVersion,
                                       MapUnmarshalHandler mapHandler)
      //throws IOException, org.exolab.castor.mapping.MappingException, org.exolab.castor.xml.ValidationException
        throws IOException
    {
        LWMap map = null;

        Log.info("unmarshalling: " + url + "; charset=" + charsetEncoding);
        
        // TODO: now that we support opening maps via HTTP URL's, it's a bit obscene to
        // open the URL twice just to support old maps where we might need to detect the
        // encoding, then re-open the file with the proper encoding.  We could
        // presumably have the MapReader keep an underlying buffered raw InputStream,
        // then create Reader's on top of that with different encodings to support this
        // more smoothly.  SMF 2008-04-08
        final MapReader mapReader = getMapReaderForURL(url, charsetEncoding, true);
        
        final BufferedReader reader = mapReader.reader;

        // Skip over comments to get to start of XML

        for (;;) {
            reader.mark(2048); // a single comment line can't be longer than this...
            String line = reader.readLine();
            if (line == null) {
                Log.error("Unexpected end-of-stream in [" + url + "]");
                throw new java.io.IOException("end of stream in " + url);
            }
            if (line.startsWith("<!--") == false) {
                // we should have just hit thie "<?xml ..." line -- done with comments
                break;
            }
            if (DEBUG.META && (DEBUG.CASTOR || DEBUG.IO)) Log.debug("Skipping[" + line + "]");
        }

        // Reset the reader to the start of the last line read, which should be the <?xml line,
        // which is what castor needs to see at start (it can't handle ignoring comments...)
        reader.reset();

        final String sourceName = url.toString();

        try {
            Unmarshaller unmarshaller = getDefaultUnmarshaller(mapping, sourceName);

            if (mapHandler == null)
                mapHandler = new MapUnmarshalHandler(url, tufts.vue.Resource.MANAGED_UNMARSHALLING); // managed is the default
                    
            if (DEBUG.Enabled) Log.debug("unmarshal handler: " + mapHandler);
            unmarshaller.setUnmarshalListener(mapHandler);

            // unmarshall the map:
            
            try {
                map = (LWMap) unmarshaller.unmarshal(new InputSource(reader));
                //} catch (org.exolab.castor.xml.MarshalException me) {
            } catch (org.exolab.castor.xml.MarshalException me) {
                //if (allowOldFormat && me.getMessage().endsWith("tufts.vue.Resource")) {
                //if (allowOldFormat && me.getMessage().indexOf("Unable to instantiate tufts.vue.Resource") >= 0) {
                // 2007-10-01 SMF: rev forward the special exception to check for once again in new castor version: castor-1.1.2.1-xml.jar
                // TODO: 2009-03-25: upgraded to Castor release 1.3: the below message check may no longer work...
                if (allowOldFormat && me.getMessage() != null && me.getMessage().indexOf("tufts.vue.Resource can no longer be constructed") >= 0) {
                    Log.warn("ActionUtil.unmarshallMap: " + me);
                    Log.warn("Attempting specialized MapResource mapping for old format.");
                    // NOTE: delicate recursion here: won't loop as long as we pass in a non-null mapping.
                    return unmarshallMap(url, getMapping(XML_MAPPING_OLD_RESOURCES), charsetEncoding, false, savingVersion, mapHandler);
                } else
                    throw me;
            }
            
            reader.close();

            Log.info("unmarshalled: " + map);

            // The below three notify calls must be called in exact sequence (file, then version, then completed)

            mapHandler.notifyFile(map, mapReader.file);

            mapHandler.notifyVersionOfVueThatSavedMap(savingVersion);

            mapHandler.notifyUnmarshallingCompleted();
            
            Log.debug("completed: " + map);
        }
        catch (Exception e) {
            tufts.Util.printStackTrace(e, "Exception restoring map from [" + url + "]: " + e.getClass().getName());
            Log.info("map-as-is: " + Util.tags(map)); // presumably null
            map = null;
            throw new Error("Exception restoring map from [" + url + "]", e);
        }

        
        return map;
    }

    /**
     * This class is only there to provide something for the old mapping description for LWMergeMap to refer
     * to. XMLObjectFactory will turn it into an LWMap.  For the re-mapping to work, it must be a subclass of LWMap.
     * Castor should never need to actuall instance or load this class.
     */ public static final class OLD_MERGE_MAP_STUB extends tufts.vue.LWMap {static{if(true)throw new Error("should-not-classload");}}

}


/**
 * This class serves as both an UnmarshalListener impl for VUE LWMap's and their components,
 * as well as a handles what to do to / with a map once it's been unmarshalled to get
 * it into it's final, completed state (e.g., in some cases, transformations may need to be performed).
 */

class MapUnmarshalHandler implements UnmarshalListener {

    public static final Object CONTEXT_NONE = "NONE";

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MapUnmarshalHandler.class);

    final Object source;
    final Object context;

    protected LWMap map;
    protected File file;
        
    MapUnmarshalHandler(Object source, Object context) {
        this.source = source;
        if (context == null)
            this.context = CONTEXT_NONE;
        else
            this.context = context;
    }

    /** This must be called first */
    void notifyFile(final LWMap map, final File file)
    {
        this.map = map;
        this.file = file;

        map.setFile(file); // VUE-713: do this always

        // Note: LWMap.setFile sets the map label
        
    }

    /** This must be called in sequence after notifyFile */
    void notifyVersionOfVueThatSavedMap(String savingVersion)
    {
        final String fileName = file.getName();

        if (map.getModelVersion() > LWMap.getCurrentModelVersion()) {
            VueUtil.alert(String.format(Locale.getDefault(),
                                        VueResources.local("actionutil.notifyversion.message")+ "\n\n" +
                                        VueResources.local("actionutil.notifyversion.datamodel")+" \n",
                                        file, map.getModelVersion(), LWMap.getCurrentModelVersion())
                          + "\n"+VueResources.local("actionutil.notifyversion.savedversion")   +"\n" + savingVersion
                          + "\n"+VueResources.local("actionutil.notifyversion.currentversion") +"\n"
                          + "        " + VueResources.local("actionutil.notifyversion.vuebuilt") + " " + tufts.vue.Version.AllInfo
                          + " (public v" + VueResources.local("vue.version") + ")"
                          + "\n"
                          + "\n"+VueResources.local("actionutil.notifyversion.displaymap")
                          + "\n"+VueResources.local("actionutil.notifyversion.corruptmap")
                          ,
                          String.format(Locale.getDefault(),VueResources.local("actionutil.notifyversion.versionwarning"), fileName));

            map.setLabel(fileName + " (as available)");
            // Skip setting the file: this will force save-as if they try to save.
        } else {

            // VUE-713: don't do this conditionallly
            //                 // This setFile also sets the label name, so it appears as a modification in the map.
            //                 // So be sure to do completeXMLRestore last, as it will reset the modification count.
            //                 if (map.getModelVersion() < 1) {
            //                     map.setLabel(file.getName());
            //                     // force save as for old maps as they will no longer work in old stable versions of VUE (1.5 & prior)
            //                     // if they're saved in this new version of VUE.
            //                 } else {
            //                     map.setFile(file);
            //                 }
                
            if (DEBUG.DATA && DEBUG.META) map.setLabel("|" + map.getModelVersion() + "| " + map.getLabel());
        }

        Log.debug("label-set: " + map);
        
    }

    /** This must be called last */
    void notifyUnmarshallingCompleted() {
        // note that map.setFile should normally have been completed before this is called
        map.completeXMLRestore(context);
    }
    
            
    /** @see org.exolab.castor.xml.UnmarshalListener */
    public void initialized(Object o) {
        if (DEBUG.XML && DEBUG.META) Log.debug(" initialized: " + Util.tags(o));
        if (o instanceof XMLUnmarshalListener) {
            try {
                ((XMLUnmarshalListener)o).XML_initialized(context);
            } catch (Throwable t) {
                Log.error(this, t);
            }
        }
    }
    
    /** @see org.exolab.castor.xml.UnmarshalListener */
    public void attributesProcessed(Object o) {
        if (DEBUG.XML && DEBUG.META) Log.debug("  attributes: " + Util.tags(o));
    }
    
    /** @see org.exolab.castor.xml.UnmarshalListener */
    public void unmarshalled(Object o) {
        if (DEBUG.XML && DEBUG.META) Log.debug("unmarshalled: " + Util.tags(o));
            
        if (o instanceof XMLUnmarshalListener) {
            try {
                ((XMLUnmarshalListener)o).XML_completed(context);
            } catch (Throwable t) {
                Log.error(this, t);
            }
        }
    }

    /** @see org.exolab.castor.xml.UnmarshalListener */
    public void fieldAdded(String name, Object parent, Object child) {
        if (DEBUG.XML) {

            //final String field = "child[" + Util.TERM_YELLOW + name + Util.TERM_CLEAR + "] ";
            //final String field = Util.TERM_YELLOW + name + Util.TERM_CLEAR + ": ";
            final String field = Util.tag(parent) + "/" + Util.TERM_YELLOW + name + Util.TERM_CLEAR + " = ";
            
            if (DEBUG.META) {
                Log.debug("  fieldAdded: parent: " + Util.tags(parent) + " " + field + Util.tags(child) + "\n");
                //System.out.println("VUL fieldAdded: parent: " + parent.getClass().getName() + "\t" + tos(parent) + "\n"
                //+ "             new child: " +  child.getClass().getName() + " \"" + name + "\" " + tos(child) + "\n");
            } else {
                if (child instanceof String)
                    Log.debug(field + Util.TERM_RED + '"' + child + '"' + Util.TERM_CLEAR);
                else
                    Log.debug(field + Util.TERM_PURPLE + Util.tags(child) + Util.TERM_CLEAR);
                //Log.debug(field + Util.tags(child));
            }
        }
        if (parent instanceof XMLUnmarshalListener) {
            try {
                ((XMLUnmarshalListener)parent).XML_fieldAdded(context, name, child);
            } catch (Throwable t) {
                Log.error(this, t);
            }
        }
        if (child instanceof XMLUnmarshalListener) {
            try {
                ((XMLUnmarshalListener)child).XML_addNotify(context, name, parent);
            } catch (Throwable t) {
                Log.error(this, t);
            }
        }
    }

    public String toString() {
        return getClass().getName() + "[" + context + "; " + source + "]";
    }

//         // exception trapping toString in case the object isn't initialized enough
//         // for it's toString to work...
//         private String tos(Object o) {
//             if (o == null)
//                 return "<null-object>";
            
//             String s = o.getClass().getName() + " ";
//             //String s = null;
//             String txt = null;
//             try {
//                 txt = o.toString();
//                 if (
//             } catch (Throwable t) {
//                 txt = t.toString();
//                 // "[" + t.toString() + "]";
//             }
//             return s;
//         }
    
}

final class XMLObjectFactory extends org.exolab.castor.util.DefaultObjectFactory {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(XMLObjectFactory.class);

    final Object source;

    XMLObjectFactory(Object source) {
        this.source = source;
        if (DEBUG.Enabled) Log.debug("new " + Util.tags(this) + "; " + source); 
    }
		
    @Override
    public Object createInstance(Class type, Object[] args) throws IllegalAccessException, InstantiationException {
        //System.err.println("VOF0 ASKED FOR " + type + " args=" + args);
        Log.info("*** ASKED FOR " + type + " args=" + args); // apparently never called
        return this.createInstance(type, null, null);
    }

    @Override
    public Object createInstance(Class type) throws IllegalAccessException, InstantiationException {
        //System.err.println("VOF1 ASKED FOR " + type);
        Log.info("*** ASKED FOR " + type); // apparently never called
        return this.createInstance(type, null, null);
    }


    @Override
    public Object createInstance(final Class _type, final Class[] argTypes, final Object[] args)
        throws IllegalAccessException, InstantiationException
    {
            
        final Class type;
        if (_type == ActionUtil.OLD_MERGE_MAP_STUB.class) {
            type = tufts.vue.LWMap.class;
        }
        // else if (_type == tufts.vue.MapResource.class || _type == tufts.vue.CabinetResource.class)
        //     type = tufts.vue.URLResource.class;
        else
            type = _type;
        
        if (_type != type) Log.info("request for instance of " + _type.getName() + " yields: " + type);

        //System.err.println("VOF ASKED FOR " + type + " argTypes=" + argTypes);
        //Object o = super.createInstance(type);
        final Object o = type.newInstance();
        if (DEBUG.Enabled) {
            if ((DEBUG.IO && DEBUG.META) ||
                DEBUG.XML || DEBUG.CASTOR || (DEBUG.RESOURCE && DEBUG.META && o instanceof tufts.vue.Resource)) {
                
                // Do not use Util.tags(o) or allow toString to be called on object -- unmarshalling can fail
                // if there are side-effects (!!!) due to calling it -- this happened at one point with an
                // instance of FavoritesDataSource.  Util.tag(o) is fine tho.
                
                Log.debug("+= " + Util.tag(o));
            }
        }
        return o;
    }
}

class MapException extends IOException {
    public MapException(String s) {
        super(s);
    }
}
    
    
class EmptyFileException extends IOException {
    EmptyFileException() {
        super("Empty file (zero length); no VUE data here");
    }
}



class PaddedCellRenderer extends DefaultListCellRenderer
{	  
	   public Component  getListCellRendererComponent(JList list,
	         Object value, // value to display
	         int index,    // cell index
	         boolean iss,  // is selected
	         boolean chf)  // cell has focus?
	   {
		   super.getListCellRendererComponent(list, 
                   value, 
                   index, 
                   iss, 
                   chf);
		   
		    setText(((VueFileFilter)value).getDescription());
		   this.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));
		   
		  return this;
	   }			        	
}
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

package tufts.vue.action;

import tufts.vue.VueUtil;
import tufts.vue.VUE;
import tufts.vue.LWMap;
import tufts.vue.VueFileFilter;
import tufts.vue.VueResources;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.MarshalListener;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.UnmarshalListener;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.util.Logger;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import java.net.URL;
import java.net.URI;
import java.util.HashMap;
import java.io.*;

/**
 * A class which defines utility methods for any of the action class.
 * Most of this code is for save/restore persistance thru castor XML.
 *
 * @author  Daisuke Fujiwara
 * @author  Scott Fraize
 * 
 */
public class ActionUtil {
    private final static String XML_MAPPING_CURRENT_VERSION_ID = VueResources.getString("mapping.lw.current_version");
    private final static URL XML_MAPPING_DEFAULT =      VueResources.getURL("mapping.lw.version_" + XML_MAPPING_CURRENT_VERSION_ID);
    private final static URL XML_MAPPING_UNVERSIONED =  VueResources.getURL("mapping.lw.version_none");
    private final static URL XML_MAPPING_OLD_RESOURCES =VueResources.getURL("mapping.lw.version_resource_fix");
    
    public ActionUtil() {}
    
    /**A static method which displays a file chooser for the user to choose which file to save into.
       It returns the selected file or null if the process didn't complete*/
    public static File selectFile(String title, String fileType)
    {
        File picked = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        
        chooser.setAcceptAllFileFilterUsed(false);
        
        if (fileType != null)
         chooser.setFileFilter(new VueFileFilter(fileType)); 
        
        else
        {
            VueFileFilter defaultFilter = new VueFileFilter("vue");
            
            chooser.addChoosableFileFilter(defaultFilter);  
            chooser.addChoosableFileFilter(new VueFileFilter("jpeg"));  
            chooser.addChoosableFileFilter(new VueFileFilter("svg"));
            chooser.addChoosableFileFilter(new VueFileFilter("pdf"));
            chooser.addChoosableFileFilter(new VueFileFilter("html"));
            chooser.addChoosableFileFilter(new VueFileFilter("imap"));
            
            chooser.setFileFilter(defaultFilter); 
        }
            
        if(VueUtil.isCurrentDirectoryPathSet()) 
          chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        
        int option = chooser.showDialog(tufts.vue.VUE.frame, "Save");
        
        if (option == JFileChooser.APPROVE_OPTION) 
        {
            picked = chooser.getSelectedFile();
            
            String fileName = picked.getAbsolutePath();
            String extension = chooser.getFileFilter().getDescription();
                
            //if it isn't a file name with the right extention 
            if (!fileName.endsWith("." + extension)) {
                fileName += "." + extension;
                picked = new File(fileName);
            }
            
            if (picked.exists()) {
                int n = JOptionPane.showConfirmDialog(null, "Would you Like to Replace the File \'" + picked.getName() + "\'", 
                        "Replacing File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                  
                if (n == JOptionPane.NO_OPTION)
                    picked = null;
            } 
            
            if (picked != null)
                VueUtil.setCurrentDirectoryPath(picked.getParent());
        }
        
        return picked;
    }
    
    /**A static method which displays a file chooser for the user to choose which file to open.
       It returns the selected file or null if the process didn't complete
    TODO BUG: do not allow more than one dialog open at a time -- two "Ctrl-O" in quick succession
    will open two open file dialogs. */
    public static File openFile(String title, String extension)
    {
        File file = null;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new VueFileFilter(extension));
        
        if (VueUtil.isCurrentDirectoryPathSet()) 
            chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        
        int option = chooser.showDialog(tufts.vue.VUE.frame, "Open");
        
        if (option == JFileChooser.APPROVE_OPTION) 
        {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            
            // if they type a file name w/out an extension
            if (fileName.indexOf('.') < 0)
                fileName += "." + extension;
            
            //if the file with the given name exists
            if ((file = new File(fileName)).exists())
            {
                VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
            }
            
            else
            {
                System.err.println("File name '"+fileName+"' " + file + " can't  be found.");
                tufts.vue.VueUtil.alert(chooser,file + " can't  be found.", "File Not Found");
                file = null;
            }
        }
        
        return file;
    }
    
    /**
     * Return the current mapping used for saving new VUE data.
     */
    public static Mapping getDefaultMapping()
    {
        Object result = _loadMapping(XML_MAPPING_DEFAULT);
        if (result instanceof Exception) {
            JOptionPane.showMessageDialog(null, "Mapping file error: will be unable to load or save maps!"
                                          + "\nMapping url: " + XML_MAPPING_DEFAULT
                                          + "\n" + result,
                                          "XML Mapping File Exception", JOptionPane.ERROR_MESSAGE);
        }
        return (Mapping) result;
    }

    private static Mapping getMapping(URL mappingSource) {
        Object result = _loadMapping(mappingSource);
        if (result instanceof Mapping)
            return (Mapping) result;
        else
            return null;
    }
    
    private static HashMap LoadedMappings = new HashMap();
    /** return's a Mapping if succesful, or an Exception if not.
     * Results are cahced (if load was successful) for future calls.*/
    private static Object _loadMapping(URL mappingSource)
    //throws java.io.IOException //, org.exolab.castor.mapping.MappingException
    {
        if (LoadedMappings.containsKey(mappingSource))
            return (Mapping) LoadedMappings.get(mappingSource);
        Mapping mapping = new Mapping();
        System.out.println("Loading mapping " + mappingSource + "...");
        try {
            mapping.loadMapping(mappingSource);
        } catch (Exception e) { // MappingException or IOException
            e.printStackTrace();
            System.err.println("Failed to load mapping " + mappingSource);
            return e;
        }
        System.out.println("*Loaded mapping " + mappingSource);
        LoadedMappings.put(mappingSource, mapping);
        return mapping;
    }
    
    /**A static method which creates an appropriate marshaller and marshal the active map*/
    public static void marshallMap(File file)
    {
        marshallMap(file, tufts.vue.VUE.getActiveMap());
    }

    
    /**
     * Marshall the given map to XML and write it out to the given file.
     */
    public static void marshallMap(File file, LWMap map)
    /*throws java.io.IOException,
               org.exolab.castor.mapping.MappingException,
               org.exolab.castor.xml.MarshalException,
               org.exolab.castor.xml.ValidationException*/
    {
        Marshaller marshaller = null;
            
        try {  
            FileWriter writer = new FileWriter(file.getAbsolutePath().replaceAll("%20"," "));
            writer.write("<!-- Do Not Remove:"
                         + " VUE mapping "
                         + "@version(" + XML_MAPPING_CURRENT_VERSION_ID + ")"
                         + " " + XML_MAPPING_DEFAULT
                         + " -->\n");
            writer.write("<!-- Do Not Remove:"
                         + " Saved " + new java.util.Date()
                         + " by " + System.getProperty("user.name")
                         + " on platform " + System.getProperty("os.name")
                         + " " + System.getProperty("os.version")
                         + " -->\n");
            //System.out.println("Wrote " + VersionString);
            marshaller = new Marshaller(writer);
            marshaller.setDebug(true);

            // on by default -- adds at top: <?xml version="1.0" encoding="UTF-8"?>
            //marshaller.setMarshalAsDocument(false);

            marshaller.setNoNamespaceSchemaLocation("none");
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
            
            System.out.println("Marshalling " + map + " ...");
            marshaller.marshal(map);
            System.out.println("Completed marshalling " + map);
            
            writer.flush();
            writer.close();

            map.setFile(file);
            map.markAsSaved();

            System.out.println("Wrote " + file);

        } catch (Exception e) {
            System.err.println("ActionUtil.marshallMap: " + e);
            e.printStackTrace();
            // until everyone has chance to update their code
            // to handle the exceptions, wrap this in a runtime exception.
            throw new RuntimeException(e);
        }

    }

    private static class VueUnmarshalListener implements UnmarshalListener {
        public void attributesProcessed(Object o) {
            System.out.println("\tattributes processed " + o.getClass().getName() + " " + o);
        }
        public void fieldAdded(String name, Object parent, Object child) {
            System.out.println("fieldAdded: parent=" + parent
                               + " child=" + child.getClass().getName() + " " + child
                               );
        }
        public void initialized(Object o) {
            System.out.println("initialized " + o.getClass().getName() + " " + o);
        }
        public void unmarshalled(Object o) {
            System.out.println("unmarshalled " + o.getClass().getName() + " " + o);
        }
    }

    /** Unmarshall a LWMap from the given file (XML map data) */
    public static LWMap unmarshallMap(File file)
        throws java.io.IOException
    {
        return unmarshallMap(file.toURI().toURL());
    }

    /** Unmarshall a LWMap from the given URL (XML map data) */
    public static LWMap unmarshallMap(java.net.URL url)
        throws java.io.IOException
    {
        //return unmarshallMap(url, getDefaultMapping());
        return unmarshallMap(url, null);
    }

    /** Unmarshall a LWMap from the given URL using the given mapping */
    private static LWMap unmarshallMap(java.net.URL url, Mapping mapping)
        throws java.io.IOException
    {
        LWMap map = null;

        // Scan lines at top of file that are comments.  If there
        // are NO comment lines, file is of one of our original save
        // formats that is not versioned, and that may need special
        // processing for the Resource class to Resource interface
        // change over.  If there are comments, the version instance
        // of the string "@version(##)" will set the version ID to ##,
        // and we'll use the mapping appropriate for that version
        // of the save file.

        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        int commentCount = 0;
        boolean oldFormat = false;
        String versionID = null;
        for (;;) {
            reader.mark(2048);
            line = reader.readLine();
            if (line == null) {
                System.err.println("Unexpected end-of-stream in [" + url + "]");
                throw new java.io.IOException("end of stream in " + url);
            }
            System.out.println("Top of file[" + line + "]");
            if (!line.startsWith("<!--"))
                break;
            commentCount++;
            // if we're being given an override mapping (or we already found one),
            // don't bother to search for one based on the version.
            if (mapping != null)
                continue;
            // Scan the comment line for a version tag to base our mapping on:
            int i = line.indexOf("@version(");
            if (i >= 0) {
                String s = line.substring(i);
                //System.out.println("Found version start:" + s);
                int x = s.indexOf(')');
                if (x > 0) {
                    versionID = s.substring(9,x);
                    System.out.println("Found version ID[" + versionID + "]");
                    if (versionID.equals(XML_MAPPING_CURRENT_VERSION_ID)) {
                        mapping = getDefaultMapping();
                    } else {
                        URL mappingURL = VueResources.getURL("mapping.lw.version_" + versionID);
                        if (mappingURL == null) {
                            System.err.println("Failed to find mapping for version tag [" + versionID + "], attempting default.");
                            mapping = getDefaultMapping();
                        } else {
                            mapping = getMapping(mappingURL);
                        }
                    }
                }
            }
        } 
        reader.reset();
        if (versionID == null) {
            oldFormat = true;
            System.out.println("Save file is of old pre-versioned type.");
            if (mapping == null)
                mapping = getMapping(XML_MAPPING_UNVERSIONED);
        }
            
        try {
            Unmarshaller unmarshaller = new Unmarshaller(); // todo: can cache this with it's mapping set
            unmarshaller.setDebug(true);
            unmarshaller.setLogWriter(new Logger(System.out));
            unmarshaller.setIgnoreExtraElements(true);
            //unmarshaller.setWhitespacePreserve(true); // not in our version yet
            if (false) unmarshaller.setUnmarshalListener(new VueUnmarshalListener());
            unmarshaller.setMapping(mapping);

            // unmarshall the map:
            
            try {
                map = (LWMap) unmarshaller.unmarshal(new InputSource(reader));
            } catch (org.exolab.castor.xml.MarshalException me) {
                if (oldFormat && me.getMessage().endsWith("tufts.vue.Resource")) {
                    System.err.println("ActionUtil.unmarshallMap: " + me);
                    System.err.println("Attempting specialized MapResource mapping for old format.");
                    // NOTE: delicate recusion here: won't loop as long as we pass in a non-null mapping.
                    return unmarshallMap(url, getMapping(XML_MAPPING_OLD_RESOURCES));
                    /*
                    unmarshaller.setMapping(loadMapping(XML_MAPPING_OLD));
                    reader.reset(); // todo: need to make sure buffer is big enough for rollback!
                    // okay, stream has been closed: would it make any difference if didn't use the url.openStream? (File instead)
                    map = (LWMap) unmarshaller.unmarshal(new InputSource(reader));
                    */
                } else
                    throw me;
            }
            
            map.setFile(new File(url.getFile()));// appears as a modification, so be sure to do completeXMLRestore last.
            map.completeXMLRestore();
            reader.close();
        }
        catch (Exception e) 
        {
            System.err.println("ActionUtil.unmarshallMap:");
            System.err.println("\texception: " + e.getClass());
            System.err.println("\tcause: " + e.getCause());
            System.err.println("\t" + e);
            //System.err.println("\tmessage=" + e.getLocalizedMessage());
            //System.err.println("\tXML_MAPPING WAS " + XML_MAPPING);
            e.printStackTrace();
            map = null;
        }
        
        return map;
    }
}

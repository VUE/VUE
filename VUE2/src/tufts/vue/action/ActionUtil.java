/*
 * Class.java
 *
 * Created on June 13, 2003, 4:50 PM
 */

package tufts.vue.action;

import javax.swing.*;
import java.io.*;
import tufts.vue.VueUtil;
import tufts.vue.VUE;
import tufts.vue.LWMap;
import tufts.vue.VueFileFilter;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;

import org.xml.sax.InputSource;
/**
 *
 * @author  Daisuke Fujiwara
 */

/** A class which defines utility methods for any of the action class
 */
public class ActionUtil {
    final static java.net.URL XML_MAPPING = tufts.vue.VueResources.getURL("mapping.lw");
    //private static final String XML_MAPPING = VUE.CASTOR_XML_MAPPING;
    
    /** Creates a new instance of Class */
    public ActionUtil() {
    }
    
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
            VueFileFilter defaultFilter = new VueFileFilter("xml");
            
            chooser.addChoosableFileFilter(defaultFilter);  
            chooser.addChoosableFileFilter(new VueFileFilter("jpeg"));  
            chooser.addChoosableFileFilter(new VueFileFilter("svg"));
            chooser.addChoosableFileFilter(new VueFileFilter("pdf"));
            chooser.addChoosableFileFilter(new VueFileFilter("html"));
            
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
                file = null;
            }
        }
        
        return file;
    }
    
    /**A static method which creates an appropriate marshaller and marshal the active map*/
    public static void marshallMap(File file)
    {
        marshallMap(file, tufts.vue.VUE.getActiveMap());
    }
    
    /**A static method which creates an appropriate marshaller and marshal the given map*/
    public static void marshallMap(File file, LWMap map)
    /*
      // really need to put these in and let everyone handle the exceptions --
      // otherwise we can't know how it may have failed!
        throws java.io.IOException,
               org.exolab.castor.mapping.MappingException,
               org.exolab.castor.xml.MarshalException,
               org.exolab.castor.xml.ValidationException
    */
    {
        Marshaller marshaller = null;
        Mapping mapping = new Mapping();
            
        try {  
            FileWriter writer = new FileWriter(file);
            
            marshaller = new Marshaller(writer);
            mapping.loadMapping(XML_MAPPING);
            marshaller.setMapping(mapping);
            
            System.out.println("start of marshall");
            marshaller.marshal(map);
            System.out.println("end of marshall");
            
            writer.flush();
            writer.close();

            map.setFile(file);
            map.markAsSaved();

            System.out.println("Wrote " + file);

        } catch (Exception e) {
            System.err.println("ActionUtil.marshallMap: " + e);
            // until everyone has chance to update their code
            // to handle the exceptions, wrap this in a runtime exception.
            throw new RuntimeException(e);
        }

    }
    
    /**A static method which creates an appropriate unmarshaller and unmarshal the given concept map*/
    public static LWMap unmarshallMap(File file)
    {
        Unmarshaller unmarshaller = null;
        LWMap map = null;
        
        //if (this.unmarshaller == null) {   
        Mapping mapping = new Mapping();
            
        try 
        {
            unmarshaller = new Unmarshaller();
            mapping.loadMapping(XML_MAPPING);    
            unmarshaller.setMapping(mapping);  
            
            FileReader reader = new FileReader(file);
            
            map = (LWMap) unmarshaller.unmarshal(new InputSource(reader));
            map.setFile(file); // appears as a modification, so be sure to do completeXMLRestore last.
            map.completeXMLRestore();
            
            reader.close();
        } 
        
        catch (MappingException me)
        {
            me.printStackTrace(System.err);
            JOptionPane.showMessageDialog(null, "Error in mapping file, closing the application", 
              "LW_Mapping Exception", JOptionPane.PLAIN_MESSAGE);
                
            // besides being very unfriendly, this could really hamper
            // debbugging!
            //System.exit(0);
        }
        
        catch (Exception e) 
        {
            System.err.println("XML_MAPPING ="+XML_MAPPING.getFile());
            System.err.println("ActionUtil.unmarshallMap: " + e);
            e.printStackTrace();
            map = null;
        }
        //}
        
        return map;
    }
}

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
    private static final String XML_MAPPING = VUE.CASTOR_XML_MAPPING;
    
    /** Creates a new instance of Class */
    public ActionUtil() {
    }
    
    /**A static method which displays a file chooser for the user to choose which file to save into.
       It returns the selected file or null if the process didn't complete*/
    public static File selectFile(String title, String fileType)
    {
        File file = null;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        
        chooser.setAcceptAllFileFilterUsed(false);
        
        if (fileType != null)
         chooser.setFileFilter(new VueFileFilter(fileType)); 
        
        else
        {
            //chooser.addChoosableFileFilter(new VueFileFilter("xml"));  
            chooser.setFileFilter(new VueFileFilter("xml")); 
            chooser.addChoosableFileFilter(new VueFileFilter("jpeg"));  
            chooser.addChoosableFileFilter(new VueFileFilter("svg"));
            chooser.addChoosableFileFilter(new VueFileFilter("pdf"));
            chooser.addChoosableFileFilter(new VueFileFilter("html"));
        }
            
        if(VueUtil.isCurrentDirectoryPathSet()) 
          chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        
        int option = chooser.showDialog(tufts.vue.VUE.frame, "Save");
        
        if (option == JFileChooser.APPROVE_OPTION) 
        {
            boolean proceed = true;
            
            if (chooser.getSelectedFile().exists())
              {
                int n = JOptionPane.showConfirmDialog(null, "Would you Like to Replace the File", 
                        "Replacing File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                  
                if (n == JOptionPane.NO_OPTION)
                  proceed = false;
                
              } 
        
            if (proceed == true)
              {
                String fileName = chooser.getSelectedFile().getAbsolutePath();
                  
                String extension = chooser.getFileFilter().getDescription();
                
                //if it isn't a file name with the right extention 
                if (!fileName.endsWith("." + extension))
                fileName += "." + extension;
                
                file = new File(fileName); 
                  
                // if they choose nothing, fileName will be null -- detect & abort
                VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
              }
        }
        
        return file;
    }
    
    /**A static method which displays a file chooser for the user to choose which file to open.
       It returns the selected file or null if the process didn't complete */
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
            
            //if it isn't a file name with the right extention 
            if (!fileName.endsWith("." + extension))
              fileName += "." + extension;
            
            //if the file with the given name exists
            if ((file = new File(fileName)).exists())
            {
                VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
            }
            
            else
            {
                //what to do here?
                System.err.println("the file doesn't exist");
                file = null;
            }
        }
        
        return file;
    }
    
    public static void marshallMap(File file)
    {
        marshallMap(file, tufts.vue.VUE.getActiveMap());
    }
    
    /**A static method which creates an appropriate marshaller and marshal the active map*/
    public static void marshallMap(File file, LWMap map)
    {
        Marshaller marshaller = null;
        
        //if (this.marshaller == null) {
        Mapping mapping = new Mapping();
            
        try 
        {  
            FileWriter writer = new FileWriter(file);
            
            marshaller = new Marshaller(writer);
            mapping.loadMapping(XML_MAPPING);
            marshaller.setMapping(mapping);
            
            System.out.println("start of marshall");
            marshaller.marshal(map);
            System.out.println("end of marshall");
            
            writer.flush();
            writer.close();
            
        } 
        catch (Exception e) 
        {
            System.err.println("ActionUtil.marshallMap: " + e);
        }
        //}
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
            System.err.println("ActionUtil.unmarshallMap: " + e);
            e.printStackTrace();
            map = null;
        }
        //}
        
        return map;
    }
}

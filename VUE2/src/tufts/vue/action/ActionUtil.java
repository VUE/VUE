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

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;

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
       It returns the selected file or null if the process didn't */
    public static File selectFile(String extension)
    {
        File file = null;
    
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecting a file");
        //chooser.setFileFilter(new VueFileFilter());
            
        if(VueUtil.isCurrentDirectoryPathSet()) 
          chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        
        int option = chooser.showDialog(tufts.vue.VUE.frame, "Save");
        
        if (option == JFileChooser.APPROVE_OPTION) 
        {
            if (chooser.getSelectedFile().exists())
              {
                int n = JOptionPane.showConfirmDialog(null, "Would you Like to Replace the File", 
                        "Replacing File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                  
                if (n == JOptionPane.YES_OPTION)
                {
                  String fileName = chooser.getSelectedFile().getAbsolutePath();
                  
                  //if it isn't a file name with the right extention 
                  if (!fileName.endsWith("." + extension))
                     fileName += extension;
                
                  file = new File(fileName); 
                  
                  // if they choose nothing, fileName will be null -- detect & abort
                  VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
                }
                
              } 
        }
        
        return file;
    }
    
    /**A static method which creates an appropriate marshaller and marshal the active map*/
    public static void marshallFile(String fileName)
    {
        Marshaller marshaller = null;
        
        //if (this.marshaller == null) {
        Mapping mapping = new Mapping();
            
        try 
        {
            FileWriter writer = new FileWriter(fileName);
            
            marshaller = new Marshaller(writer);
            mapping.loadMapping(XML_MAPPING);
            marshaller.setMapping(mapping);
            
            marshaller.marshal(tufts.vue.VUE.getActiveMap());
            
            writer.flush();
            writer.close();
        } 
        catch (Exception e) 
        {
            System.err.println("ActionUtil.getMarshaller: " + e);
        }
        //}
    }
    
    /**possible to change? */
    public static Unmarshaller getUnmarshaller()
    {
        Unmarshaller unmarshaller = null;
        
        //if (this.unmarshaller == null) {   
        Mapping mapping = new Mapping();
            
        try 
        {
            unmarshaller = new Unmarshaller();
            mapping.loadMapping(XML_MAPPING);    
            unmarshaller.setMapping(mapping);  
        } 
        catch (Exception e) 
        {
            System.err.println("ActionUtil.getUnmarshaller: " + e);
        }
        //}
        return unmarshaller;
    }
}

/*
 * SaveAction.java
 *
 * Created on March 31, 2003, 1:33 PM
 */

package tufts.vue.action;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;

import tufts.vue.*;
/**
 *
 * @author  akumar03
 *
 */
public class SaveAction extends AbstractAction
{
    final String XML_MAPPING = VUE.CASTOR_XML_MAPPING;
    private static  String fileName = "";
    private Marshaller marshaller = null;
    private boolean saveAs = true;
   
    
    
    public SaveAction() {
    }
    
    public SaveAction(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public SaveAction(String label, boolean saveType){
        super(label);
        setSaveAs(saveType);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public boolean isSaveAs() {
        return this.saveAs;
    }
    
    public void setSaveAs(boolean saveAs){
        this.saveAs = saveAs;
    }      
    
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileName() {
        return this.fileName;
    }
    
    
    public void actionPerformed(ActionEvent e)
    {
        System.out.println("Action["+e.getActionCommand()+"] invoked...");
        try {
            fileName ="";
            if (isSaveAs()){
                selectFile();
            }
            //FileWriter writer = new FileWriter(getFileName(), false);
            marshaller = getMarshaller();
            marshaller.marshal(tufts.vue.VUE.getActiveMap());
            //writer.close();
            System.out.println("Saved " + getFileName());
        }catch(Exception ex) {
            System.out.println("problem with marshalling process: "+ex);
        }
        
        System.out.println("Action["+e.getActionCommand()+"] completed.");
    }
    
    
    
    private void selectFile()
    {
        try {  
          
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Map");
        chooser.setFileFilter(new VueFileFilter());
        if(VueUtil.isCurrentDirectoryPathSet()) 
            chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        int option = chooser.showDialog(tufts.vue.VUE.frame, "Save");
        if (option == JFileChooser.APPROVE_OPTION
                && chooser.getSelectedFile()!=null) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
            if(!fileName.endsWith(".xml")) fileName += ".xml";
            // if they choose nothing, fileName will be null -- detect & abort
            VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
        }
       }catch(Exception ex) {System.out.println(ex);}   
    }
 
    private Marshaller getMarshaller()
    {
        //this.marshaller = null; // SF temporary debug -- always reload     
        //fileWriter needs to be reloaded to save to a given file a second time
        //if (this.marshaller == null) {
            Mapping mapping = new Mapping();
            try {                
                this.marshaller = new Marshaller(new FileWriter(fileName));
                System.out.println("Marshaller loading mapping: " + XML_MAPPING);
                mapping.loadMapping(XML_MAPPING);
                marshaller.setMapping(mapping);
                System.out.println("Marshaller mapping has been set to: " + XML_MAPPING);
            } catch (Exception e) {
                System.err.println("SaveAction.getMarshaller: " + e);
            }
        //}
        return this.marshaller;
    }
}







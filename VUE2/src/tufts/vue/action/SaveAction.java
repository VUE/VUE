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
 * modified by John Briedis 5/29/03
 *
 */
public class SaveAction extends AbstractAction
{
    //final String XML_MAPPING = "concept_map.xml";
    final String XML_MAPPING = "vue2d_map.xml";
    private static  String fileName = "test.xml";
    private Marshaller marshaller = null;
    private boolean saveAs = true;
    
    /** Creates a new instance of SaveAction */
    
    public SaveAction() {
    }
    
    public SaveAction(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public SaveAction(String label, boolean saveType){
        super(label);
        setSaveAs(saveType);
        //this.saveType = saveType;   
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
            if (isSaveAs()){
                selectFile();
            }    
            marshaller = getMarshaller();
            marshaller.marshal(tufts.vue.VUE.getActiveMap());
            System.out.println("Saved " + getFileName());
        }catch(Exception ex) {
            System.out.println(ex);
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
        if (option == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
            // if they choose nothing, fileName will be null -- detect & abort
            VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
        }
       }catch(Exception ex) {System.out.println(ex);}   
    }
 
    private Marshaller getMarshaller()
    {
        //this.marshaller = null; // SF temporary debug -- always reload
        
        if (this.marshaller == null) {
            Mapping mapping = new Mapping();
            try {
                this.marshaller = new Marshaller(new FileWriter(getFileName()));
                System.out.println("Marshaller loading mapping: " + XML_MAPPING);
                mapping.loadMapping(XML_MAPPING);
                marshaller.setMapping(mapping);
                System.out.println("Marshaller mapping has been set to: " + XML_MAPPING);
            } catch (Exception e) {
                System.err.println("SaveAction.getMarshaller: " + e);
            }
        }
        return this.marshaller;
    }
}







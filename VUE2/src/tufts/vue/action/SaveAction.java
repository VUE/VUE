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
 */
public class SaveAction extends AbstractAction
{
    final String XML_MAPPING = "concept_map.xml";
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
    
    public SaveAction(String label, boolean saveAs){
        super(label);
        setSaveAs(saveAs);
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
    
    
    public void actionPerformed(ActionEvent e) {
      try {  
        if(isSaveAs())  selectFile();
        marshaller = getMarshaller();
        marshaller.marshal(tufts.vue.VUE.getActiveMap());
        
      }catch(Exception ex) {System.out.println(ex);}
          System.out.println("Action["+e.getActionCommand()+"] performed!");
    }
    
    private void selectFile() {
       try {  
          
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new VueFileFilter());
        if(VueUtil.isCurrentDirectoryPathSet()) 
            chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        int option = chooser.showSaveDialog(tufts.vue.VUE.frame);
   
        if(option == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
            VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
        }
      }catch(Exception ex) {System.out.println(ex);}   
    }
 
    private Marshaller getMarshaller()
    {
        if (this.marshaller == null) {
            Mapping mapping = new Mapping();
            try {
                this.marshaller = new Marshaller(new FileWriter(fileName));
                mapping.loadMapping(XML_MAPPING);
                marshaller.setMapping(mapping);
            } catch (Exception e) {
                System.err.println("OpenAction.getMarshaller: " + e);
            }
        }
        return this.marshaller;
    }
}

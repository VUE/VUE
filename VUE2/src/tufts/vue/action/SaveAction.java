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
    
    /** Creates a new instance of SaveAction */
    
    public SaveAction() {
    }
    
    public SaveAction(String label) {
        super(label);
    }
   
    public void actionPerformed(ActionEvent e) {
      try {  
        Mapping mapping;
        Marshaller marshaller;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new VueFileFilter());
        if(VueUtil.isCurrentDirectoryPathSet()) 
            chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        int option = chooser.showSaveDialog(tufts.vue.VUE.frame);
        String fileName = "test.xml";
        if(option == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
            VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
        }
        marshaller = new Marshaller(new FileWriter(fileName));
        mapping =  new Mapping();
        mapping.loadMapping(XML_MAPPING);
        marshaller.setMapping(mapping);
        marshaller.marshal(tufts.vue.VUE.getActiveMap());
        
      }catch(Exception ex) {System.out.println(ex);}
          System.out.println("Action["+e.getActionCommand()+"] performed!");
    }
    
}

/*
 * OpenAction.java
 *
 * Created on April 2, 2003, 12:40 PM
 */

package tufts.vue.action;

/**
 *
 * @author  akumar03
 */
import javax.swing.*;
import java.awt.event.*;
import java.io.*;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;

import org.xml.sax.InputSource;

import tufts.vue.*;
public class OpenAction extends AbstractAction {
    
    /** Creates a new instance of OpenAction */
    public OpenAction() {
    }
    public OpenAction(String label) {
        super(label);
    }
    
     public void actionPerformed(ActionEvent e) {
      try {  
        Mapping mapping;
        Unmarshaller unmarshaller;
        unmarshaller = new Unmarshaller();
        mapping =  new Mapping();
        mapping.loadMapping( "mapping.xml" );
        unmarshaller.setMapping(mapping);
        JFileChooser chooser = new JFileChooser();
        if(VueUtil.isCurrentDirectoryPathSet()) 
            chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        int option = chooser.showOpenDialog(tufts.vue.VUE.frame);
        String fileName = "test.xml";
        if(option == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
            VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
            VUE.setMap((ConceptMap)unmarshaller.unmarshal(new InputSource(new FileReader(fileName))));
            
        }
        
        
      }catch(Exception ex) {System.out.println(ex);}
          System.out.println("Action["+e.getActionCommand()+"] performed!");
    }
    
}

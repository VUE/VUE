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
import java.io.*;
import java.awt.event.*;
import javax.swing.*;

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

    public void actionPerformed(ActionEvent e)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new VueFileFilter());
        if (VueUtil.isCurrentDirectoryPathSet()) 
            chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        int option = chooser.showOpenDialog(tufts.vue.VUE.frame);
        String fileName = "test.xml";
        if (option == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
            VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
            ConceptMap loadedMap = loadMap(fileName);
            VUE.displayMap(loadedMap);
        }
        System.out.println("Action["+e.getActionCommand()+"] performed!");
    }

    private ConceptMap loadMap(String filename)
    {
        try {
            return (ConceptMap) getUnmarshaller()
                .unmarshal(new InputSource(new FileReader(filename)));
        } catch (Exception e) {
            System.err.println("OpenAction.loadMap[" + filename + "]: " + e);
            return null;
        }
    }
    

    private Unmarshaller unmarshaller = null;
    private Unmarshaller getUnmarshaller()
    {
        if (this.unmarshaller == null) {
            this.unmarshaller = new Unmarshaller();
            Mapping mapping = new Mapping();
            try {
                mapping.loadMapping("mapping.xml");
                unmarshaller.setMapping(mapping);
            } catch (Exception e) {
                System.err.println("OpenAction.getUnmarshaller: " + e);
            }
        }
        return this.unmarshaller;
    }

    class VueFileFilter extends javax.swing.filechooser.FileFilter
    {
        private final String[] extensions = {".vue", ".xml"};
        public boolean accept(File f)
        {
            String lname = f.getName().toLowerCase();
            for (int i = 0; i < extensions.length; i++)
                if (lname.endsWith(extensions[i]))
                    return true;
            return false;
        }
        public String getDescription()
        {
            return "VUE Files";
        }
    }

    
}

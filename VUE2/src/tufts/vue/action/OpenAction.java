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

public class OpenAction extends AbstractAction
{
    final String XML_MAPPING = "concept_map.xml";
    
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
            VUE.activateWaitCursor();
            try {
                ConceptMap loadedMap = loadMap(fileName);
                VUE.displayMap(loadedMap);
            } finally {
                VUE.clearWaitCursor();
            }
        }
        System.out.println("Action["+e.getActionCommand()+"] performed!");
    }

    private ConceptMap loadMap(String filename)
    {
        try {
            Unmarshaller unmarshaller = getUnmarshaller();
            if (debug) System.err.println("Unmarshalling from " + filename);
            ConceptMap map = (ConceptMap) unmarshaller
                .unmarshal(new InputSource(new FileReader(filename)));
            if (debug) System.err.println("Resloving links in " + map);
            map.resolvePersistedLinks();
            return map;
        } catch (Exception e) {
            System.err.println("OpenAction.loadMap[" + filename + "]: " + e);
            e.printStackTrace();
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
                if (debug) System.err.println("Loading " + XML_MAPPING + "...");
                mapping.loadMapping(XML_MAPPING);
                if (debug) System.err.println("Loaded " + XML_MAPPING);
                unmarshaller.setMapping(mapping);
                if (debug) System.err.println("Unmarshaller mapping has been set.");
            } catch (Exception e) {
                System.err.println("OpenAction.getUnmarshaller: " + e);
            }
        }
        return this.unmarshaller;
    }

    public static void main(String args[])
    {
        System.err.println("Attempting to read map from " + args[0]);
        debug = true;
        new OpenAction().loadMap(args[0]);
    }

    private static boolean debug = false;

}

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
import tufts.vue.*;

public class OpenAction extends AbstractAction
{
    /** Creates a new instance of OpenAction */
    public OpenAction() {
    }
    
    public OpenAction(String label) {
        super(label);
        //System.out.println("in openAction constructor...");
        putValue(Action.SHORT_DESCRIPTION,label);
    }

    public void actionPerformed(ActionEvent e)
    {
        /*
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Map");
        chooser.setFileFilter(new VueFileFilter());
        if (VueUtil.isCurrentDirectoryPathSet()) 
            chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        int option = chooser.showDialog(tufts.vue.VUE.frame, "Open");
        String fileName = "test.xml";
        if (option == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
            VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getP //if it isn't a file name with the right extention 
                if (!fileName.endsWith("." + extension))
                fileName += "." + extension;arent());
         */
           
        File file = ActionUtil.openFile("Open Map", "vue");
        displayMap(file);
        
        
        
        System.out.println("Action["+e.getActionCommand()+"] completed.");
    }

    
    public static void displayMap(File file) {
        //if (file != null && file.getName().endsWith(".xml"))
        if (file != null)
        {
            VUE.activateWaitCursor();
            try 
            {
                //LWMap loadedMap = loadMap(fileName);
                LWMap loadedMap = loadMap(file.getAbsolutePath());
                VUE.displayMap(loadedMap);
            } 
            finally 
            {
                VUE.clearWaitCursor();
            }
        }
        
        
    }
    public static LWMap loadMap(String filename)
    {
        try 
        {
            
            //Unmarshaller unmarshaller = ActionUtil.getUnmarshaller();
            if (debug) System.err.println("Unmarshalling from " + filename);
            
            //LWMap map = (LWMap) unmarshaller.unmarshal(new InputSource(reader));
            //map.completeXMLRestore();
            //return map;
            File file = new File(filename);
            LWMap map = ActionUtil.unmarshallMap(file);
            return map;
        } 
        catch (Exception e) 
        {
            System.err.println("OpenAction.loadMap[" + filename + "]: " + e);
            e.printStackTrace();
            return null;
        }
    }
    
    
    /*
    private static Unmarshaller unmarshaller = null;
    private Unmarshaller getUnmarshaller()
    {
        if (unmarshaller == null) {
            unmarshaller = new Unmarshaller();
            Mapping mapping = new Mapping();
            try {
                if (debug) System.err.println("Loading " + XML_MAPPING + "...");
                mapping.loadMapping(XML_MAPPING);
                if (debug) System.err.println(" Loaded " + XML_MAPPING + ".");
                unmarshaller.setMapping(mapping);
                if (debug) System.err.println("The loaded mapping has been set on the unmarshaller.");
            } catch (Exception e) {
                System.err.println("OpenAction.getUnmarshaller: " + e);
            }
        }
        return unmarshaller;
    }

    public static void main(String args[])
    {
        System.err.println("Attempting to read map from " + args[0]);
        debug = true;
        new OpenAction().loadMap(args[0]);
    }

     */
    private static boolean debug = true;
    
}

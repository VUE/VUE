/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

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


    // workaround for rapid-succession Ctrl-O's which pop multiple open dialogs
    private static final Object LOCK = new Object();
    private static boolean openUnderway = false; 
    public void actionPerformed(ActionEvent e)
    {
        synchronized (LOCK) {
            if (openUnderway)
                return;
            openUnderway = true;
        }
        try {
            File file = ActionUtil.openFile("Open Map", "vue");
            displayMap(file);
            System.out.println("Action["+e.getActionCommand()+"] completed.");
        } finally {
            openUnderway = false;
        }
    }

    
    public static void displayMap(File file) {
        if (file != null) {
            VUE.activateWaitCursor();
            try {
                LWMap loadedMap = loadMap(file.getAbsolutePath());
                VUE.displayMap(loadedMap);
            } finally {
                VUE.clearWaitCursor();
            }
        }
    }
    
    // todo: have only one root loadMap that hanldes files & urls -- actually, make it ALL url's
    public static LWMap loadMap(String filename)
    {
        try 
        {
            if (debug) System.err.println("\nUnmarshalling from " + filename);
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
    
    public static LWMap loadMap(java.net.URL url)
    {
        try {       
            if (debug) System.err.println("\nUnmarshalling from " + url);
            LWMap map = ActionUtil.unmarshallMap(url);
            return map;
        } 
        catch (Exception e) 
        {
            System.err.println("OpenAction.loadMap[" + url + "]: " + e);
            e.printStackTrace();
            return null;
        }
    }
    
    public static void main(String args[])
        throws Exception
    {
        String file = args.length == 0 ? "test.xml" : args[0];
        System.err.println("Attempting to read map from " + file);
        debug = true;
        DEBUG.Enabled = true;
        //DEBUG.INIT = true;
        LWMap map;
        if (file.indexOf(':') >= 0)
            map = new OpenAction().loadMap(new java.net.URL(file));
        else
            map = new OpenAction().loadMap(file);
        System.out.println("Loaded map: " + map);
    }

    private static boolean debug = true;
    
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
    */

}

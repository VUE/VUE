/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.*;

import tufts.vue.*;
import tufts.vue.gui.GetUrlDialog;

public class OpenURLAction extends VueAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(OpenAction.class);
    
    //public static final String ZIP_IMPORT_LABEL ="Imported";
    public OpenURLAction(String label) {
        super(label, null, ":general/OpenURL");
    }
    
    public OpenURLAction() {
        this("OpenURL");
    }
    
    
    // workaround for rapid-succession Ctrl-O's which pop multiple open dialogs
    private static final Object LOCK = new Object();
    private static boolean openUnderway = false;
    public void actionPerformed(ActionEvent e) {
        synchronized (LOCK) {
            if (openUnderway)
                return;
            openUnderway = true;
        }
        try {
        /*	final Object[] defaultButtons = { "OK","Cancel"};
        	JOptionPane optionPane= new JOptionPane("Enter the Map URL to open: ",JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION,null,defaultButtons,"OK");
            javax.swing.JDialog dialog = optionPane.createDialog((Component)VUE.getApplicationFrame(), "Open URL");
            dialog.setModal(true);
            
            optionPane.setWantsInput(true);
            optionPane.enableInputMethods(true);

            dialog.setSize(new Dimension(350,125));
            dialog.setVisible(true);
            
       
          */
        	GetUrlDialog gud = new GetUrlDialog();
        	URL url = null;
        	
        	String option = (String)GetUrlDialog.getURL();
            
            if (option == null || option.length() <= 0)
                return;
            
            try {
                url = new URL(option);
            } catch (MalformedURLException ex) {
                JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
                        "Malformed URL, resource could not be added.", 
                        "Malformed URL", 
                        JOptionPane.ERROR_MESSAGE);
                return;
			}
         
            displayMap(url);            
            
            Log.info(e.getActionCommand() + ": completed.");
        } finally {
            openUnderway = false;
            
            
        }
    }
    
    public static void reloadMap(LWMap map)
    {
    	displayMap(map.getFile());
    }
    
    public static void displayMap(File file)
    {
    	VUE.displayMap(file);
    }
    public static void displayMap(URL url) {
        VUE.displayMap(url);
      
    }

    
    
}

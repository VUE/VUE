
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

package edu.tufts.vue.ontology.action;

import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import tufts.vue.Actions;
import edu.tufts.vue.ontology.ui.*;

/*
 * OntologyControlsOpenAction.java
 *
 * Created on April 9, 2007, 2:26 PM
 *
 * @author dhelle01
 */
public class OntologyControlsOpenAction extends tufts.vue.VueAction {
    
    private tufts.vue.gui.DockWindow ontologyDock;
    private edu.tufts.vue.ontology.ui.OntologyBrowser browser;
    
    public OntologyControlsOpenAction(String label) 
    {
        super(label);
        final KeyStroke acceleratorKey = KeyStroke.getKeyStroke(KeyEvent.VK_8, Actions.COMMAND);
    	putValue(Action.ACCELERATOR_KEY, acceleratorKey);    	
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        browser = OntologyBrowser.getBrowser();
        if(browser.isInitialized())
        {
          ontologyDock = OntologyBrowser.getBrowser().getDockWindow();
        }
        else
        {
            //ontologyDock = tufts.vue.gui.GUI.createDockWindow("Ontologies");
            tufts.vue.gui.DockWindow searchDock = null;
        //browser = new edu.tufts.vue.ontology.ui.OntologyBrowser(true, ontologyDock, searchDock);
           // browser = OntologyBrowser.createBrowser(false,ontologyDock,searchDock);
            browser.initializeBrowser(false,searchDock);
        //browser = new edu.tufts.vue.ontology.ui.OntologyBrowser(false, ontologyDock, searchDock);
            ontologyDock = OntologyBrowser.getBrowser().getDockWindow();
            ontologyDock.setBounds(100,100,300, (int) (tufts.vue.gui.GUI.GScreenHeight * 0.75));
        }

        ontologyDock.setVisible(true);
    }
    
}


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
        final KeyStroke acceleratorKey = KeyStroke.getKeyStroke(KeyEvent.VK_4, Actions.COMMAND);
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
            ontologyDock.setBounds(0,tufts.vue.gui.DockWindow.ToolbarHeight+100,
                                   300, (int) (tufts.vue.gui.GUI.GScreenHeight * 0.75));
        }

        ontologyDock.setVisible(true);
    }
    
}

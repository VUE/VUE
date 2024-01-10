
/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

package tufts.vue.action;

import edu.tufts.vue.ontology.ui.TypeList;

import tufts.vue.*;
import tufts.vue.gui.DockWindow;

/*
 * FedoraOntologyOpenAction.java
 *
 * Created on March 12, 2007, 12:39 PM
 *
 * @author dhelle01
 */
public class FedoraOntologyOpenAction extends VueAction {
    
    /** Creates a new instance of FedoraOntologyOpenAction */
    public FedoraOntologyOpenAction(String label) {
        super(label);
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        TypeList typeList = new edu.tufts.vue.ontology.ui.TypeList();
        //DockWindow typeWindow = tufts.vue.gui.GUI.createDockWindow("Fedora Ontology: " + (TypeList.count++),edu.tufts.vue.ontology.ui.TypeList.createTestPanel(typeList));
        //typeWindow.setLocation(200,100);
        //typeWindow.pack();
        //typeWindow.setVisible(true);
    }
}

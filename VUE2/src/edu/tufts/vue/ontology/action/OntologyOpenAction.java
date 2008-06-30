
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

import edu.tufts.vue.ontology.ui.*;

/*
 * OntologyOpenAction.java
 *
 * Created on April 11, 2007, 2:35 PM
 *
 * @author dhelle01
 */
public class OntologyOpenAction extends tufts.vue.VueAction {
    
    private OntologyBrowser browser;
    
    public OntologyOpenAction(String label,OntologyBrowser browser) 
    {
        super(label);
        this.browser = browser;
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        edu.tufts.vue.ontology.ui.OntologyChooser2 chooser = new OntologyChooser2(tufts.vue.VUE.getDialogParentAsFrame(),
                                                                                "Add an Ontology",
                                                                                browser);
    }
}

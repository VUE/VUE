
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

import java.awt.event.ActionEvent;
import java.net.URL;

import edu.tufts.vue.ontology.ui.OntologyBrowser;
import edu.tufts.vue.ontology.ui.OntologyChooser2;
import edu.tufts.vue.ontology.ui.TypeList;

import tufts.vue.VueAction;
import tufts.vue.VueResources;


/*
 * AddFedoraOntology.java
 *
 * Created on November 5, 2007, 10:02 AM
 *
 * @author dhelle01
 */
public class AddFedoraOntology extends VueAction{
    
    private OntologyBrowser browser;
    
    public AddFedoraOntology(OntologyBrowser browser) 
    {
        setActionName(VueResources.getString("ontology.addfedora"));
        this.browser = browser;
    }
            
    public void actionPerformed(ActionEvent e) 
    {
                TypeList list = new TypeList();
                URL ontURL = VueResources.getURL("fedora.ontology.rdf");
                URL cssURL = VueResources.getURL("fedora.ontology.css");
                tufts.vue.gui.Widget w = browser.addTypeList(list,edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ontURL.getFile()),ontURL);
                //list.loadOntology(ontURL,cssURL,OntologyChooser2.getOntType(ontURL),browser,w);
                list.loadOntology(ontURL,cssURL,edu.tufts.vue.ontology.OntologyType.RDFS_TYPE,browser,w);

    }
    
}


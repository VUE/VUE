
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

package edu.tufts.vue.ontology.action;

import java.awt.event.ActionEvent;
import java.net.URL;

import edu.tufts.vue.ontology.ui.OntologyBrowser;
import edu.tufts.vue.ontology.ui.OntologyChooser2;
import edu.tufts.vue.ontology.ui.TypeList;

import tufts.vue.VueAction;
import tufts.vue.VueResources;


/*
 * AddFedoraOntologies.java
 *
 * Created on October 18, 2007, 8:04 AM
 *
 * @author dhelle01
 */
public class AddFedoraOntologies extends VueAction{
    
    private OntologyBrowser browser;
    
    public AddFedoraOntologies(OntologyBrowser browser) 
    {
        setActionName("Add Fedora Ontologies");
        this.browser = browser;
    }
            
    public void actionPerformed(ActionEvent e) 
    {
                TypeList list = new TypeList();
                URL ontURL = VueResources.getURL("fedora.ontology.rdf");
                URL cssURL = VueResources.getURL("fedora.ontology.css");
                //tufts.vue.gui.Widget w = addTypeList(list,"Fedora Relationships",ontURL);
                tufts.vue.gui.Widget w = browser.addTypeList(list,edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ontURL.getFile()),ontURL);
                list.loadOntology(ontURL,cssURL,OntologyChooser2.getOntType(ontURL),browser,w);
                TypeList list2 = new TypeList();
                ontURL = VueResources.getURL("fedora.support.ontology.rdf");
                cssURL = VueResources.getURL("fedora.support.ontology.css");
                //tufts.vue.gui.Widget w2 = addTypeList(list2,"Fedora node",ontURL);
                tufts.vue.gui.Widget w2 = browser.addTypeList(list2,edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ontURL.getFile()),ontURL);
                list2.loadOntology(ontURL,cssURL,OntologyChooser2.getOntType(ontURL),browser,w2);
    }
    
}

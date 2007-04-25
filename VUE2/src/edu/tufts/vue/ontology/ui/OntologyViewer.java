
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

package edu.tufts.vue.ontology.ui;

import java.awt.*;
import javax.swing.*;


/*
 * OntologyViewer.java
 *
 * Created on April 5, 2007, 2:10 PM
 *
 * @author dhelle01
 */
public class OntologyViewer extends javax.swing.JPanel {
    
    private static OntologyBrowser ontologyBrowser;
    private static OntologyList ontologyList;
    
    public OntologyViewer(OntologyBrowser browser) 
    {
        setLayout(new java.awt.BorderLayout());
        ontologyBrowser = browser;
        ontologyList = new OntologyList(this);
        add(ontologyList);
        
        /*Action[] actions = {
            new edu.tufts.vue.ontology.action.RDFSOntologyOpenAction("RDFS"),
            new edu.tufts.vue.ontology.action.OwlOntologyOpenAction("OWL")
        };
        tufts.vue.gui.Widget.setMenuActions(browser,actions);*/
    }
    
    public OntologyList getList()
    {
        return ontologyList;
    }
    
    public OntologyBrowser getBrowser()
    {
        return ontologyBrowser;
    }
    
}

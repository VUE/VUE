
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
import tufts.vue.*;
import tufts.vue.gui.*;

/*
 * OntologyBrowser.java
 *
 * Created on April 5, 2007, 2:15 PM
 *
 * @author dhelle01
 */
public class OntologyBrowser extends JPanel {
    
    public static final Object POPULATE_TYPES = java.awt.BorderLayout.CENTER;
    
    final JPanel ontologiesPanel;
    final Widget typesPane = new Widget("types");
    
    final DockWindow dockWindow;
    final DockWindow ontologyDock;
    final DockWindow typeDock;
    
    private OntologyViewer ontologyViewer;
    
    // corresponds (roughly) to searchPane from DRBrowser (the original inspiration for OntologyBrowser
    // implementation architecture)
    final JComponent populatePane = new Widget("Populate Types") {
            private Component editor, result;
            {
                setOpaque(false);
            }
    };
    
    private WidgetStack resultsStack = new WidgetStack("types stack");
    
    public void addTypeList(edu.tufts.vue.ontology.ui.TypeList list,String name)
    {
        Widget w = new Widget(name);
        w.add(list);
        resultsStack.addPane(w);
        revalidate();
    }
    
    
    public OntologyBrowser(boolean delayedLoading, DockWindow ontologyDock,DockWindow typeDock) 
    {
        //super(new BorderLayout());
        //super(new java.awt.GridLayout(0,1));
        setName("Ontologies");
        
        this.dockWindow = ontologyDock;
        this.ontologyDock = ontologyDock;
        this.typeDock = typeDock;
        this.ontologiesPanel = this;
        
        if(delayedLoading)
        {
            //TBD see DRBrowser for likely path that will be taken when loading ontologies at startup
            // e.g. fedora ontology
        }
        else
        {
            loadOntologyViewer();
        }
        
        populatePane.add(resultsStack);
        ((Widget)populatePane).setTitleHidden(true);
        
        buildSingleDockWindow();
          
        tufts.vue.VueAction addRDFSToBrowser = new tufts.vue.VueAction()
        {
              {
                  setActionName("Add RDFS Ontology");
              }
              
              public void actionPerformed(java.awt.event.ActionEvent e)
              {
                 // actually this shows up in the title of the Ontology Browser Dock Window..
                 //setName("Browser rdfs");
                 edu.tufts.vue.ontology.action.RDFSOntologyOpenAction rdfsooa = new edu.tufts.vue.ontology.action.RDFSOntologyOpenAction("Browser - RDFS");
                 rdfsooa.setViewer(ontologyViewer);
                 rdfsooa.actionPerformed(e);
                 rdfsooa.setViewer(null);
              }
        };
          
        tufts.vue.VueAction addOWLToBrowser = new tufts.vue.VueAction()
        {
              {
                  setActionName("Add OWL Ontology");
              }
              
              public void actionPerformed(java.awt.event.ActionEvent e)
              {
                 // shows up in Ontology Browser Dock Window title
                 //setName("Browser owl");
                 edu.tufts.vue.ontology.action.OwlOntologyOpenAction owlsooa = new edu.tufts.vue.ontology.action.OwlOntologyOpenAction("OWL - RDFS");
                 owlsooa.setViewer(ontologyViewer);
                 owlsooa.actionPerformed(e);
                 owlsooa.setViewer(null);
              }
        };
          
        Action[] actions = {
            new edu.tufts.vue.ontology.action.OntologyOpenAction("Add an Ontology",this),
            //new edu.tufts.vue.ontology.action.RDFSOntologyOpenAction("RDFS"),
            //new edu.tufts.vue.ontology.action.OwlOntologyOpenAction("OWL"),
            addRDFSToBrowser,
            addOWLToBrowser
        };
        tufts.vue.gui.Widget.setMenuActions(this,actions);
          
    }
    
    public void loadOntologyViewer()
    {
        //OntologyViewer ontologyViewer = new OntologyViewer(this);
        ontologyViewer = new OntologyViewer(this);
        ontologyViewer.setName("Ontology Viewer");
        ontologiesPanel.add(ontologyViewer);
        revalidate();
    }
    
    public OntologyViewer getViewer()
    {
        return ontologyViewer;
    }
    
    public void buildSingleDockWindow()
    {
        // may not need these next two lines, these are adapted from DRBrowser
        //typesPane.setTitleHidden(true);
        //typesPane.setHidden(true);
        
        WidgetStack stack = new WidgetStack(getName());

        Widget.setWantsScroller(stack, true);

        stack.addPane(ontologiesPanel, 0f);
        stack.addPane(populatePane,0f);
       // stack.addPane(typesPane, 0f);
        
        this.dockWindow.setContent(stack);
    }
    
    public JComponent getPopulatePane()
    {
        return populatePane;
    }
    
}

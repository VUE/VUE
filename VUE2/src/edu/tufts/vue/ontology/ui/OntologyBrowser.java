
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

import edu.tufts.vue.ontology.OntManager;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import tufts.vue.*;
import tufts.vue.gui.*;
import java.net.*;
/*
 * OntologyBrowser.java
 *
 * Created on April 5, 2007, 2:15 PM
 *
 * @author dhelle01
 */
public class OntologyBrowser extends JPanel {
    
    
    JPanel ontologiesPanel;
    
    private HashMap<OntologyBrowserKey,Widget> widgetMap = new HashMap<OntologyBrowserKey,Widget>();
    
    final static DockWindow ontologyDock = tufts.vue.gui.GUI.createDockWindow("Ontologies");;
    DockWindow typeDock;
    private static boolean initialized = false;
    
    private ArrayList<OntologySelectionListener> ontologySelectionListenerList = new ArrayList<OntologySelectionListener>();
    
    private OntologyViewer ontologyViewer;
    
    private static OntologyBrowser singleton;
    
    final JComponent populatePane = new Widget("Populate Types") {
        private Component editor, result;
        {
            setOpaque(false);
        }
    };
    
    private WidgetStack resultsStack = new WidgetStack("types stack");
    
    private static TypeList selectedOntology = null;
    
    public DockWindow getDockWindow() {
        return ontologyDock;
    }
    
    public static TypeList getSelectedList() {
        return selectedOntology;
    }
    
    public Widget addTypeList(final edu.tufts.vue.ontology.ui.TypeList list,String name,URL url) {
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                selectedOntology = list;
                fireOntologySelectionChanged(list);
            }
        });
        
        String loadingString = "Loading " + name;
        
        Widget w = null;
        
        OntologyBrowserKey key = new OntologyBrowserKey(name,url);
        Widget old = widgetMap.get(key);
        
        
        
        if(old!=null)
        {
           old.setHidden(true);
           widgetMap.remove(old);
        }
        
        w = new Widget(loadingString);
        w.add(list);
        widgetMap.put(new OntologyBrowserKey(name,url),w);
        resultsStack.addPane(w);
        
        list.revalidate();
        w.revalidate();
        resultsStack.revalidate();
        revalidate();
        return w;
    }
    
    public static OntologyBrowser getBrowser() {
        if (singleton == null)
            singleton = new OntologyBrowser();
        
        return singleton;
    }
    
    
    private OntologyBrowser() {
        ontologiesPanel = this;
        typeDock = null;
        ontologyDock.setResizeEnabled(false);
    }
    
    public void initializeBrowser(boolean delayedLoading, DockWindow typeDock) {
        
        setLayout(new javax.swing.BoxLayout(this,javax.swing.BoxLayout.Y_AXIS));
        setName("Ontologies");
        
        this.typeDock = typeDock;
        this.ontologiesPanel = this;
        edu.tufts.vue.ontology.OntManager.getOntManager().load();
        
        for( edu.tufts.vue.ontology.Ontology o: edu.tufts.vue.ontology.OntManager.getOntManager().getOntList()) {
            TypeList list = new TypeList();
           
            tufts.vue.gui.Widget w = null;  
            try {
               w = addTypeList(list, o.getLabel(),new URL(o.getBase()));
               list.loadOntology(new URL(o.getBase()),o.getStyle(),OntologyChooser.getOntType(new URL(o.getBase())),this,w);
            } catch(Exception ex) {
                System.out.println("OntologyBrowser.initializeBrowser: "+ex);
            }
            
        }
         
        if(delayedLoading) {
            //TBD see DRBrowser for likely path that will be taken when loading ontologies at startup
            // e.g. fedora ontology
        } else {
            loadOntologyViewer();
        }
        
        populatePane.add(resultsStack);
        ((Widget)populatePane).setTitleHidden(true);
        
        buildSingleDockWindow();
                
        tufts.vue.VueAction applyStyle = new tufts.vue.VueAction() {
            {
                setActionName("Import Style Sheet");
            }
            
            public void actionPerformed(java.awt.event.ActionEvent e) {
                javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                chooser.showOpenDialog(OntologyBrowser.this);
                if(chooser.getSelectedFile()!=null) {
                    java.net.URL cssURL = null;
                    try {
                        cssURL = chooser.getSelectedFile().toURL();
                    } catch(java.net.MalformedURLException mue) {
                        System.out.println("trouble opening css file: " + mue);
                    }
                    int selectedOntology = getViewer().getList().getSelectedIndex();
                    ((edu.tufts.vue.ontology.Ontology)(getViewer().getList().getModel().getElementAt(selectedOntology))).applyStyle(cssURL);
                   
                    //should be able to do better than this -- validate(), repaint() don't seem to work..
                    resultsStack.updateUI();
                    getViewer().getList().updateUI();

                }
            }
            
        };
        
        
       tufts.vue.VueAction removeOntology = new tufts.vue.VueAction() {
            {
                setActionName("Remove Selected Ontology");
            }
            
            public void actionPerformed(java.awt.event.ActionEvent e) {
                
                try
                {
                  
                    
                  edu.tufts.vue.ontology.Ontology ont = (edu.tufts.vue.ontology.Ontology)getBrowser().getViewer().getList().getSelectedValue();
                  URL ontURL = new java.net.URL(ont.getBase());
                  
                  OntManager.getOntManager().removeOntology(ontURL);
                  edu.tufts.vue.ontology.OntManager.getOntManager().save();
                                  
                  Widget w = widgetMap.get(new OntologyBrowserKey(edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ont.getBase()),ontURL));
                  resultsStack.setHidden(w,true);
                  resultsStack.remove(w);
                  widgetMap.remove(w);
           
                  if (widgetMap.size() <= 1)
                  {
                	  VueToolbarController.getController().hideOntologicalTools();
                  }
                  
                  //resultsStack.updateUI();
                }
                catch(java.net.MalformedURLException mue)
                {
                  System.out.println("OntologyBrowser: remove ontology url exception" + mue);
                }
                
                getViewer().getList().updateUI();
                //repaint();
                revalidate();
            }
            
        };

        tufts.vue.VueAction addFedoraOntologies = new tufts.vue.VueAction() {
            {
                setActionName("Add Fedora Ontologies");
            }
            
            public void actionPerformed(java.awt.event.ActionEvent e) {
                TypeList list = new TypeList();
                URL ontURL = VueResources.getURL("fedora.ontology.rdf");
                URL cssURL = VueResources.getURL("fedora.ontology.css");
                tufts.vue.gui.Widget w = addTypeList(list,"Fedora Relationships",ontURL);
                list.loadOntology(ontURL,cssURL,OntologyChooser2.getOntType(ontURL),OntologyBrowser.this,w);
                ontURL = VueResources.getURL("fedora.support.ontology.rdf");
                cssURL = VueResources.getURL("fedora.support.ontology.css");
                tufts.vue.gui.Widget w2 = addTypeList(list,"Fedora node",ontURL);
                list.loadOntology(ontURL,cssURL,OntologyChooser2.getOntType(ontURL),OntologyBrowser.this,w2);
            }
            
        };
        
        Action[] actions = {
            new edu.tufts.vue.ontology.action.OntologyOpenAction("Add an Ontology",this),
            applyStyle,
            removeOntology,
            addFedoraOntologies
            // ,about this ontology

        };
        tufts.vue.gui.Widget.setMenuActions(this,actions);
        
        //singleton = this;
        initialized = true;
        
        
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public void loadOntologyViewer() {
        //OntologyViewer ontologyViewer = new OntologyViewer(this);
        ontologyViewer = new OntologyViewer(this);
        ontologyViewer.setName("Ontology Viewer");
        ontologiesPanel.add(ontologyViewer);
        revalidate();
    }
    
    public OntologyViewer getViewer() {
        return ontologyViewer;
    }
    
    public static Object getSelectedOntology() {
        Object value =  getBrowser().getViewer().getList().getSelectedValue();
        return value + ":" + value.getClass();
    }
    
    public void buildSingleDockWindow() {
        
        WidgetStack stack = new WidgetStack(getName());
        
        Widget.setWantsScroller(stack, true);
        
        stack.addPane(ontologiesPanel, 0f);
        stack.addPane(populatePane,0f);
        
        ontologyDock.setContent(stack);
    }
    
    /*public JComponent getPopulatePane()
    {
        return populatePane;
    }*/
    
    public void addOntologySelectionListener(OntologySelectionListener osl) {
        ontologySelectionListenerList.add(osl);
    }
    
    public void removeOntologySelectionListener(OntologySelectionListener osl) {
        ontologySelectionListenerList.remove(osl);
    }
    
    private void fireOntologySelectionChanged(TypeList selection) {
        Iterator<OntologySelectionListener> i = ontologySelectionListenerList.iterator();
        while(i.hasNext()) {
            OntologySelectionListener osl = i.next();
            osl.ontologySelected(new OntologySelectionEvent(selection));
        }
    }
    
    // note: plan is that eventually user will be able to change display name
    public class OntologyBrowserKey
    {
        private String displayName;
        private URL identifyingURL;
        
        public OntologyBrowserKey(String displayName,URL identifyingURL)
        {
            this.displayName = displayName;
            this.identifyingURL = identifyingURL;
        }
        
        // note: *must* be overidden to use as key for map - see above
        public int hashCode()
        {
            return displayName.hashCode() + identifyingURL.toString().hashCode();
        }
        
        public boolean equals(Object o)
        {
            if(!(o instanceof OntologyBrowserKey))
                return false;
            else
            {
              OntologyBrowserKey key = (OntologyBrowserKey)o;
              return (displayName.equals(key.displayName) && identifyingURL.toString().equals(
                        key.identifyingURL.toString()));
            }
        }
    }
    
}


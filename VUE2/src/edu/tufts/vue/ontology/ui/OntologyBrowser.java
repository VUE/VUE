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

package edu.tufts.vue.ontology.ui;

import edu.tufts.vue.ontology.OntManager;
import edu.tufts.vue.ontology.action.OntologyOpenAction;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import tufts.Util;
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
	public static final long	serialVersionUID = 1;
    private final static boolean DEBUG_LOCAL = false;

    JPanel ontologiesPanel = new Widget(VueResources.getString("dockWindow.ontologies.title"));

    private HashMap<OntologyBrowserKey,Widget> widgetMap = new HashMap<OntologyBrowserKey,Widget>();

    DockWindow typeDock;
    private static boolean initialized = false;

    private ArrayList<OntologySelectionListener> ontologySelectionListenerList = new ArrayList<OntologySelectionListener>();

    private OntologyViewer ontologyViewer;

    private static OntologyBrowser singleton;

    final JComponent populatePane = new Widget("Populate Types") {
    	public static final long	serialVersionUID = 1;
        private Component	editor,
        					result;

        {
            setOpaque(false);
        }
    };

    private WidgetStack resultsStack = new WidgetStack("types stack");

    private static TypeList selectedOntology = null;

    public DockWindow getDockWindow() {
        return VUE.getContentDock();
    }

    public static TypeList getSelectedList() {
        return selectedOntology;
    }

    public static void setSelectedList(TypeList list)
    {
        if (list != selectedOntology)
        {    
          selectedOntology = list;
          getBrowser().fireOntologySelectionChanged(list);
        }
    }

    public static int getSelectedIndex(TypeList tlist)
    {
        Component[] comps = getBrowser().getComponents();

        for (int i = 0; i < comps.length; i++)
        {
            if (tlist == comps[i])
                return i;
        }

        return -1;
    }

    public Widget addTypeList(final edu.tufts.vue.ontology.ui.TypeList list,String name,URL url) {
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
           //     TypeList oldSelection = selectedOntology;
           //     selectedOntology = list;
           //     if (oldSelection != null)
           //     {
           //       oldSelection.repaint();
           //     }
                fireOntologySelectionChanged(list);
            }
        });

        String loadingString = "Loading " + name;

        Widget w = null;

        OntologyBrowserKey key = new OntologyBrowserKey(name, url);
        Widget old = widgetMap.get(key);

        if (old != null)
        {
           old.setHidden(true);
           widgetMap.remove(old);
        }

        w = new Widget(loadingString);
        w.add(list);
        widgetMap.put(new OntologyBrowserKey(name, url), w);
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
        typeDock = null;
    }

    private OntologyOpenAction ontologyOpenAction = new edu.tufts.vue.ontology.action.OntologyOpenAction(VueResources.getString("ontology.openaction"), this);

    protected tufts.vue.VueAction removeOntology = new tufts.vue.VueAction() {
    	public static final long	serialVersionUID = 1;

    	{
            setActionName(VueResources.getString("ontology.removeselected"));
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            try {
              edu.tufts.vue.ontology.Ontology ont = (edu.tufts.vue.ontology.Ontology)getBrowser().getViewer().getList().getSelectedValue();
              URL ontURL = new java.net.URL(ont.getBase());

              OntManager.getOntManager().removeOntology(ontURL);
              edu.tufts.vue.ontology.OntManager.getOntManager().save();

              Widget w = widgetMap.get(new OntologyBrowserKey(edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ont.getBase()), ontURL));

              if (DEBUG_LOCAL)
              {    
                System.out.println("TypeList remove w from key: " + w);
              }

              resultsStack.setHidden(w, true);
              resultsStack.remove(w);
              widgetMap.remove(w);

              if (widgetMap.size() <= 1)
              {
            	  VueToolbarController.getController().hideOntologicalTools();
              }

              //resultsStack.updateUI();
            }
            catch (java.net.MalformedURLException mue)
            {
              System.out.println("OntologyBrowser: remove ontology url exception" + mue);
            }

            getViewer().getList().updateUI();
            //repaint();
            revalidate();
        }
    };

    protected tufts.vue.VueAction applyStyle = new tufts.vue.VueAction() {
    	public static final long	serialVersionUID = 1;

    	{
            setActionName(VueResources.getString("menu.addstylesheet"));
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            VueFileChooser chooser = VueFileChooser.getVueFileChooser();

            chooser.showOpenDialog(OntologyBrowser.this);

            if (chooser.getSelectedFile() != null) {
                java.net.URL cssURL = null;

                try {
                    cssURL = chooser.getSelectedFile().toURL();
                } catch (java.net.MalformedURLException mue) {
                    System.out.println("trouble opening css file: " + mue);
                }

                int selectedOntology = getViewer().getList().getSelectedIndex();
                edu.tufts.vue.ontology.Ontology ont = ((edu.tufts.vue.ontology.Ontology)
                         (getViewer().getList().getModel().getElementAt(selectedOntology)));

                ont.applyStyle(cssURL);
 
                // need to update typelist!!
                try {        
                  URL url = new URL(ont.getBase());
                  TypeList tlist = (TypeList)widgetMap.get(new OntologyBrowserKey(
                                        edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ont.getBase()),url)).getComponent(0);
                  tlist.setCSSURL(cssURL.toString());
                  tlist.getOntology().applyStyle(cssURL);
                  tlist.styleApplied();
                } catch (Exception urle) {
                    System.out.println("Typelist -- error refreshing type list" + urle);
                }
 
                //should get rid of message next to ontology name
                //should be able to do better than this -- validate(), repaint() don't seem to work..
                resultsStack.updateUI();
                getViewer().getList().updateUI();

                edu.tufts.vue.ontology.OntManager.getOntManager().save();
            }
        }
    };

    public Widget getWidgetForOntology(edu.tufts.vue.ontology.Ontology o)
    {
        URL ontURL = null;

		try {
			ontURL = new java.net.URL(o.getBase());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}

    	return widgetMap.get(new OntologyBrowserKey(edu.tufts.vue.ontology.Ontology.getLabelFromUrl(o.getBase()),ontURL));
    }

    public void initializeBrowser(boolean delayedLoading, DockWindow typeDock) {
    	if (initialized)
    		return;

        setLayout(new javax.swing.BoxLayout(this,javax.swing.BoxLayout.Y_AXIS));
        setName(VueResources.getString("dockWindow.ontologies.title"));

        this.typeDock = typeDock;

        edu.tufts.vue.ontology.OntManager.getOntManager().load();

        for (edu.tufts.vue.ontology.Ontology o: edu.tufts.vue.ontology.OntManager.getOntManager().getOntList()) {
            TypeList list = new TypeList();

            tufts.vue.gui.Widget w = null;

            try {
               w = addTypeList(list, o.getLabel(),new URL(o.getBase()));
               list.loadOntology(new URL(o.getBase()),o.getStyle(),OntologyChooser.getOntType(new URL(o.getBase())),this,w);
            } catch (Exception ex) {
                System.out.println("OntologyBrowser.initializeBrowser: "+ex);
            }
        }

        if (delayedLoading) {
            //TBD see DRBrowser for likely path that will be taken when loading ontologies at startup
            // e.g. fedora ontology
        } else {
            loadOntologyViewer();
        }

        populatePane.add(resultsStack);
        ((Widget)populatePane).setTitleHidden(true);

        buildSingleDockWindow();

/*		tufts.vue.VueAction addFedoraOntologies = new tufts.vue.VueAction() {
            {
                setActionName("Add Fedora Ontologies");
            }
            
            public void actionPerformed(java.awt.event.ActionEvent e) {
                TypeList list = new TypeList();
                URL ontURL = VueResources.getURL("fedora.ontology.rdf");
                URL cssURL = VueResources.getURL("fedora.ontology.css");
                //tufts.vue.gui.Widget w = addTypeList(list,"Fedora Relationships",ontURL);
                tufts.vue.gui.Widget w = addTypeList(list,edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ontURL.getFile()),ontURL);
                list.loadOntology(ontURL,cssURL,OntologyChooser2.getOntType(ontURL),OntologyBrowser.this,w);
                TypeList list2 = new TypeList();
                ontURL = VueResources.getURL("fedora.support.ontology.rdf");
                cssURL = VueResources.getURL("fedora.support.ontology.css");
                //tufts.vue.gui.Widget w2 = addTypeList(list2,"Fedora node",ontURL);
                tufts.vue.gui.Widget w2 = addTypeList(list2,edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ontURL.getFile()),ontURL);
                list2.loadOntology(ontURL,cssURL,OntologyChooser2.getOntType(ontURL),OntologyBrowser.this,w2);
            }
            
        };  */

        addOntologySelectionListener(getViewer().getList());

        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public void loadOntologyViewer() {
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

        if (value == null)
            return null;

        return value + ":" + value.getClass();
    }

    public void buildSingleDockWindow() {
        WidgetStack stack = new WidgetStack(getName());
		Action[] actions = {
		        ontologyOpenAction,
		        applyStyle,
		        removeOntology,
		        new edu.tufts.vue.ontology.action.AddFedoraOntology(this)
		        // ,about this ontology
		    };

        Widget.setWantsScroller(stack, false);
        Widget.setWantsScrollerAlways(stack, false);

        stack.addPane(ontologiesPanel, 0f);
        stack.addPane(populatePane, 0f);
		Widget.setHelpAction(ontologiesPanel, VueResources.getString("dockWindow.Ontologies.helpText"));
        Widget.setMiscAction(ontologiesPanel, new MiscWidgetAction(), "dockWindow.addButton");
        Widget.setMenuActions(ontologiesPanel, actions);

        add(stack);

//      if (Util.isMacPlatform()) {
//          GUI.setAlwaysOnTop(ontologyDock.window(),true);  
//      }
    }

    class MiscWidgetAction extends MouseAdapter
    {
    	public void mouseClicked(MouseEvent e)
    	{    		    	
    				ontologyOpenAction.actionPerformed(null);		    		    		 
    	}
    }
    
    public void addOntologySelectionListener(OntologySelectionListener osl) {
        ontologySelectionListenerList.add(osl);
    }

    public void removeOntologySelectionListener(OntologySelectionListener osl) {
        ontologySelectionListenerList.remove(osl);
    }

    private void fireOntologySelectionChanged(TypeList selection) {
        Iterator<OntologySelectionListener> i = ontologySelectionListenerList.iterator();
        while (i.hasNext()) {
            OntologySelectionListener osl = i.next();
            osl.ontologySelected(new OntologySelectionEvent(selection));
        }
    }

    // note: plan is that eventually user will be able to change display name
    public class OntologyBrowserKey
    {
        private String displayName;
        private URL identifyingURL;

        public OntologyBrowserKey(String displayName, URL identifyingURL)
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
            if (!(o instanceof OntologyBrowserKey))
                return false;
            else {
              OntologyBrowserKey key = (OntologyBrowserKey)o;
              return (displayName.equals(key.displayName) && identifyingURL.toString().equals(
                        key.identifyingURL.toString()));
            }
        }
    }
}

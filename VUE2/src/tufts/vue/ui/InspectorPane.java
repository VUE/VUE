/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

package tufts.vue.ui;

import tufts.Util;
import tufts.vue.*;
import tufts.vue.gui.*;
import tufts.vue.NotePanel;
import tufts.vue.filter.NodeFilterEditor;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;

import edu.tufts.vue.metadata.ui.MetadataEditor;
import edu.tufts.vue.metadata.ui.OntologicalMembershipPane;
import edu.tufts.vue.fsm.event.SearchEvent;
import edu.tufts.vue.fsm.event.SearchListener;

/**
 * Display information about the selected Resource, or LWComponent and it's Resource.
 *
 * @version $Revision: 1.73 $ / $Date: 2008-05-21 03:13:18 $ / $Author: sfraize $
 */

public class InspectorPane extends WidgetStack
    implements VueConstants, ResourceSelection.Listener, LWSelection.Listener, SearchListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(InspectorPane.class);
    
    public static final int META_VERSION = VueResources.getInt("metadata.version");
    /** meta-data */ public static final int OLD = 0;
    /** meta-data */ public static final int NEW = 1;
    
    private final Image NoImage = VueResources.getImage("NoImage");

    private final boolean isMacAqua = GUI.isMacAqua();

    //-------------------------------------------------------
    // Node panes
    //-------------------------------------------------------

    private final LabelPane mSummaryPane = new LabelPane(); // old style; new SummaryPane()
    private final NotePanel mNotes = new NotePanel();
    private final NotePanel mPathwayNotes = new NotePanel(false);
    private final UserMetaData mKeywords = new UserMetaData();
    private final OntologicalMembershipPane ontologicalMetadata = new OntologicalMembershipPane();
    
    //-------------------------------------------------------
    // Resource panes
    //-------------------------------------------------------
    private final MetaDataPane mResourceMetaData = new MetaDataPane(false);
    private final ResourcePreview mPreview = new ResourcePreview();
    
    private Resource mResource; // the current resource
    private WidgetStack stack = null;

    private static class Pane {

        final static Collection<Pane> AllPanes = new ArrayList();

        final String name;
        final JComponent widget;
        final float size;
        final int bits;

        Pane(String s, JComponent c, float sz, int b) {
            name = s;
            widget = c;
            size = sz;
            bits = b;
            AllPanes.add(this);
        }
    }

    private static final int INFO = 1;
    private static final int NOTES = 2;
    private static final int KEYWORD = 4;
    private static final int RESOURCE = 8;

    public InspectorPane()
    {
        super("Info");

      //mSummaryPane = new SummaryPane();
        //mSummaryPane = new LabelPane();
      //mResourceMetaData = new PropertiesEditor(false);
        //mResourceMetaData = new MetaDataPane(false);
        mResourceMetaData.setName("contentInfo");
        //mPreview = new ResourcePreview();

        stack = this;
        //stack = new WidgetStack("Info");
        //stack = new WidgetStack(getName());
        
        Widget.setWantsScroller(stack, true);
        Widget.setWantsScrollerAlways(stack, true);

        final float EXACT_SIZE = 0f;
        final float EXPANDER = 1f;

        new Pane("Label",                  mSummaryPane,           EXACT_SIZE,  INFO+NOTES+KEYWORD);
        new Pane("Content Preview",        mPreview,               EXACT_SIZE,  RESOURCE);
        new Pane("Content Info",           mResourceMetaData,      EXACT_SIZE,  RESOURCE);
        new Pane("Notes",                  mNotes,                 EXPANDER,    INFO+NOTES);
        new Pane("Pathway Notes",          mPathwayNotes,          EXPANDER,    INFO+NOTES);
        new Pane("Keywords",               mKeywords,          EXACT_SIZE,  KEYWORD);
        new Pane("Ontological Membership", ontologicalMetadata,    EXACT_SIZE,  0);

        for (Pane p : Pane.AllPanes)
            stack.addPane(p.name, p.widget, p.size);

//       //stack.addPane("Information",            mSummaryPane,           EXACT_SIZE);
//         stack.addPane("Label",                  mSummaryPane,           EXACT_SIZE);
//         stack.addPane("Content Preview",        mPreview,               EXACT_SIZE);
//         stack.addPane("Content Info",           mResourceMetaData,      EXACT_SIZE);
//         stack.addPane("Notes",                  mNotes,                 AUTO_EXPANDER);
//         stack.addPane("Pathway Notes",          mPathwayNotes,          AUTO_EXPANDER);
//         stack.addPane("Keywords",               mKeywords,          EXACT_SIZE);
//         stack.addPane("Ontological Membership", ontologicalMetadata,    EXACT_SIZE);

       // add(stack, BorderLayout.CENTER);

        VUE.getSelection().addListener(this);
        VUE.addActiveListener(LWComponent.class, this);
        VUE.addActiveListener(LWPathway.Entry.class, this);
        VUE.getResourceSelection().addListener(this);
        
        Widget.setHelpAction(mSummaryPane,VueResources.getString("dockWindow.Info.summaryPane.helpText"));;
        Widget.setHelpAction(mPreview,VueResources.getString("dockWindow.Info.previewPane.helpText"));;
        Widget.setHelpAction(mResourceMetaData,VueResources.getString("dockWindow.Info.resourcePane.helpText"));;
        Widget.setHelpAction(mNotes,VueResources.getString("dockWindow.Info.notesPane.helpText"));;
        Widget.setHelpAction(mKeywords,VueResources.getString("dockWindow.Info.userPane.helpText"));;
        Widget.setHelpAction(ontologicalMetadata,VueResources.getString("dockWindow.Info.ontologicalMetadata.helpText"));


        //Set the default state of the inspector pane to completely empty as nothign 
        //is selected and its misleading to have the widgets on there.
        hideAll();
        
//         loadResource(null);
//         //this.setEnabled(false);
//         showNodePanes(false);
//         Widget.setHidden(mKeywords,true);
//         Widget.setHidden(ontologicalMetadata,true);
//         Widget.setExpanded(mKeywords, false);
//         Widget.setExpanded(ontologicalMetadata, false);
//         //Widget.setExpanded(mResourceMetaData, false);
//         //Widget.setExpanded(mNodeTree, false);
//         showResourcePanes(false);

        setMinimumSize(new Dimension(300,500)); // if WidgetStack overrides getMinimumSize w/out checking for a set, this won't work.
    }

    public WidgetStack getWidgetStack() {
    	return stack;
    }

    public void resourceSelectionChanged(ResourceSelection.Event e)
    {    	
        if (e.selected == null)
            return;
        if (DEBUG.RESOURCE) out("resource selected: " + e.selected);
        showNodePanes(false);
        showResourcePanes(true);
        loadResource(e.selected);
        setVisible(true);
    }

    public void activeChanged(final tufts.vue.ActiveEvent e, final LWPathway.Entry entry)
    {
//         if (entry.pathway.isShowingSlides())
//             return;

        mPathwayNotes.attach(entry.getSlide());
    }
    
    
    public void activeChanged(final tufts.vue.ActiveEvent e, final LWComponent c)
    {
        //showNodePanes(true);
        if (c == null) {
            
            hideAll();
            //loadResource(null);
            
//             //Widget.setHidden(ontologicalMetadata, true);
//             loadResource(null);
//             //this.setEnabled(false);
//            // this.getParent().setEnabled(false);
//             showNodePanes(false);
//             showResourcePanes(false);
//             stack.putClientProperty("TITLE-INFO", null);
            
        } else {
             	
//             if (c instanceof LWSlide || c.hasAncestorOfType(LWSlide.class)) {
//                 Widget.setHidden(mKeywords, true);
//                 Widget.setHidden(ontologicalMetadata, true);
//             } else
//                 Widget.setHidden(ontologicalMetadata, false);

            showNodePanes(true);
             	
            if (c.hasResource()) {
                loadResource(c.getResource());
                showResourcePanes(true);
            } else {
                showResourcePanes(false);
            }
            mSummaryPane.load(c);
            mKeywords.load(c);

            setVisible(true);
            
            
            //mNodeTree.load(c);

            //setTypeName(mNotePanel, c, "Notes");
        }
    }

//     public void selectionChanged(LWSelection selection) {
         
//         // also might need criteria for slides (remove slides from multiples
//         // and don't active if only slide is selected.. SLIDE_STYLE in component?'
//         // just use LWSlide -- though can this lead to phantom selections? have
//         // to make sure SearchAction also doesn't include slides in result sets'
         
//         // todo: make the single selection work as before with showNodePanes
//         // i.e. as Active drives it.
         
// //         if (selection.contents().size() == 1) {
// //             if(selection.contents().get(0) instanceof LWSlide ||
// //                selection.contents().get(0).hasAncestorOfType(LWSlide.class))
// //             {
// //                 Widget.setHidden(mKeywords,true);
// //                 Widget.setHidden(ontologicalMetadata, true);
// //                 return;
// //             }
// //             else {
// //                 Widget.setHidden(ontologicalMetadata,false);
// //             }
// //         }
// //         else if(selection.contents().size() > 0 || selection.isEmpty()) {
// //             Widget.setHidden(ontologicalMetadata,true); 
// //         }
          
         
//         if (selection.size() > 1) {                     
//             mKeywords.load(selection.get(0));
//             Widget.setHidden(mKeywords, false);
//             showResourcePanes(false);
//         } else {
//             Widget.setHidden(mKeywords, true);  
//         }
//     }

     public void selectionChanged(LWSelection selection) {

           // also might need criteria for slides (remove slides from multiples
           // and don't active if only slide is selected.. SLIDE_STYLE in component?'
           // just use LWSlide -- though can this lead to phantom selections? have
           // to make sure SearchAction also doesn't include slides in result sets'
         
           // todo: make the single selection work as before with showNodePanes
           // i.e. as Active drives it.
         
           if(selection.contents().size() == 1)
               if(selection.contents().get(0) instanceof LWSlide ||
                   selection.contents().get(0).hasAncestorOfType(LWSlide.class))
               {
                   Widget.setHidden(mKeywords,true);
                   Widget.setHidden(ontologicalMetadata, true);
                   return;
               }
               else
               {
                   Widget.setHidden(ontologicalMetadata,false);
               }
           else if(selection.contents().size() > 0 || selection.isEmpty())
           {
              Widget.setHidden(ontologicalMetadata,true); 
           }
          
         
           if(!selection.contents().isEmpty())
           {
                    
             mKeywords.load(selection.contents().get(0));
             Widget.setHidden(mKeywords, false);
             showResourcePanes(false);
           }
           else
           {
             Widget.setHidden(mKeywords, true);  
           }
     }
    

    private void setTypeName(JComponent component, LWComponent c, String suffix)
    {
        final String type = c.getComponentTypeLabel();
        String title;
        if (DEBUG.Enabled) {
            String name = c.getDisplayLabel();
            
            if (name == null)
                title = type;
            else
                title = type + " (" + name + ")";
            stack.putClientProperty("TITLE-ITEM", title);
        }// else
        //stack.putClientProperty("TITLE-INFO", null);

        if (suffix != null)
            title = type + " " + suffix;
        else
            title = type;
        component.setName(title);
    }

    private void loadResource(final Resource r) {
        
        if (DEBUG.RESOURCE) out("loadResource: " + r);
        
        if (r == null)
            return;

        mResource = r;
        mResourceMetaData.loadResource(r);
        mPreview.loadResource(r);
        
        /*
        long size = r.getSize();
        String ss = "";
        if (size >= 0)
            ss = VueUtil.abbrevBytes(size);
        mSizeField.setText(ss);
        */
    }

    private void hideAll()
    {
        setVisible(false);
        
        for (Pane p : Pane.AllPanes)
            Widget.setHidden(p.widget, true);

        stack.putClientProperty("TITLE-ITEM", null);
    }
        
    private void expandCollapsePanes(int type) {

        for (Pane p : Pane.AllPanes) {

            if (Widget.isHidden(p.widget))
                continue;
            
            if ((p.bits & type) != 0) {
                if (!Widget.isExpanded(p.widget))
                    Widget.setExpanded(p.widget, true);                    
            } else {
                if (Widget.isExpanded(p.widget))
                    Widget.setExpanded(p.widget, false);                    
            }
        }
    }
    
    private void showNodePanes(boolean visible) {
        Widget.setHidden(mSummaryPane, !visible);        
        Widget.setHidden(mNotes, !visible);

        Widget.setHidden(mKeywords, !visible);
        Widget.setHidden(ontologicalMetadata, !visible);
        
        
        //user meta data now hides and shows in LWSelection listener:
        //Widget.setHidden(mKeywords, !visible);
        //Widget.setHidden(mNodeTree, !visible);
        //Widget.setHidden(ontologicalMetadata, !visible);
        
    }
    private void showResourcePanes(boolean visible) {
        Widget.setHidden(mResourceMetaData, !visible);
        Widget.setHidden(mPreview, !visible);
    }
    
    public void showKeywordView()
    {
        expandCollapsePanes(KEYWORD);
        
//     	if (!Widget.isHidden(mSummaryPane) && !Widget.isExpanded(mSummaryPane))
//     		Widget.setExpanded(mSummaryPane, true);
//     	if (!Widget.isHidden(mKeywords) && !Widget.isExpanded(mKeywords))
//     		Widget.setExpanded(mKeywords, true);
//     	if (!Widget.isHidden(mNotes) && Widget.isExpanded(mNotes))
//     		Widget.setExpanded(mNotes, false);    	    	
//     	if (!Widget.isHidden(mResourceMetaData) && Widget.isExpanded(mResourceMetaData))
//     		Widget.setExpanded(mResourceMetaData, false);    	
    }
    
    public void showNotesView()
    {
        expandCollapsePanes(NOTES);
        
//     	if (!Widget.isHidden(mSummaryPane) && !Widget.isExpanded(mSummaryPane))    		
//     		Widget.setExpanded(mSummaryPane, true);
//     	if (!Widget.isHidden(mNotes) && !Widget.isExpanded(mNotes))
//     		Widget.setExpanded(mNotes, true);
//     	if (!Widget.isHidden(mKeywords) && Widget.isExpanded(mKeywords))
//     		Widget.setExpanded(mKeywords, false);    	
//     	if (!Widget.isHidden(mResourceMetaData) && Widget.isExpanded(mResourceMetaData))
//     		Widget.setExpanded(mResourceMetaData, false);    
    	
    	SwingUtilities.invokeLater(new Runnable() { 
            public void run() { 
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                mNotes.getTextPane().requestFocusInWindow();
            } 
    	} );

    }
    
    public void showInfoView()
    {
        expandCollapsePanes(INFO);
        
//     	if (!Widget.isHidden(mSummaryPane) && !Widget.isExpanded(mSummaryPane))
//     		Widget.setExpanded(mSummaryPane, true);
//     	if (!Widget.isHidden(mNotes) && !Widget.isExpanded(mNotes))
//     		Widget.setExpanded(mNotes, true);
//     	if (!Widget.isHidden(mResourceMetaData) && !Widget.isExpanded(mResourceMetaData))
//     		Widget.setExpanded(mResourceMetaData, true);
//     	if (!Widget.isHidden(mPreview) && !Widget.isExpanded(mPreview))
//     		Widget.setExpanded(mPreview, true);    	    	
    }


    private class ResourcePreview extends tufts.vue.ui.PreviewPane
    {
        ResourcePreview() {
            setName("contentPreview");
        }
        void loadResource(Resource r) {
            super.loadResource(r);
            String title = r.getTitle();
            if (title == null)
                title = r.getProperty("title");
            // TODO: resource property lookups should be case insensitive
            if (title == null)
                title = r.getProperty("Title");
            if (title == null)
                title = "Content Preview";
            Widget.setTitle(this, title);
            setToolTipText(title);
        }
        
        public Dimension getMinimumSize() {
            return new Dimension(200,128);    	
        }
        
        public Dimension getPreferredSize() {
            return new Dimension(200,128);
        }
    }
    private class InlineTitleResourcePreview extends tufts.vue.ui.PreviewPane
    {
        private final JLabel mTitleField;
        //private final JTextPane mTitleField;
        //private final JTextArea mTitleField;
        //private final PreviewPane mPreviewPane = new PreviewPane();
        
        InlineTitleResourcePreview() {
            //super(new BorderLayout());

            // JTextArea -- no good (no HTML)
            //mTitleField = new JTextArea();
            //mTitleField.setEditable(false);

            // JTextPane -- no good, too fuckin hairy and slow (who needs an HTML editor?)
            //mTitleField = new JTextPane();
            //StyledDocument doc = new javax.swing.text.html.HTMLDocument();
            //mTitleField.setStyledDocument(doc);
            //mTitleField.setEditable(false);

            mTitleField = new JLabel("", JLabel.CENTER);

            //-------------------------------------------------------

            GUI.apply(GUI.TitleFace, mTitleField);
            mTitleField.setAlignmentX(0.5f);
            //mTitleField.setBorder(new LineBorder(Color.red));
                
            //mTitleField.setOpaque(false);
            //mTitleField.setBorder(new EmptyBorder(0,2,5,2));
            //mTitleField.setSize(200,50);
            //mTitleField.setPreferredSize(new Dimension(200,30));
            //mTitleField.setMaximumSize(new Dimension(Short.MAX_VALUE,Short.MAX_VALUE));
            //mTitleField.setMinimumSize(new Dimension(100, 30));


            //add(mPreviewPane, BorderLayout.CENTER);
            add(mTitleField, BorderLayout.SOUTH);
        }

        void loadResource(Resource r) {
            super.loadResource(r);
            String title = r.getTitle();

            //mPreviewPane.setVisible(false);
            
            if (title == null || title.length() < 1) {
                mTitleField.setVisible(false);
                return;
            }
            
            // Always use HTML, which creates auto line-wrapping for JLabels
            title = "<HTML><center>" + title;

            
            mTitleField.setVisible(true);

            if (true) {
                mTitleField.setText(title);
            } else { 
                //remove(mTitleField);
                out("OLD            size=" + mTitleField.getSize());
                out("OLD   preferredSize=" + mTitleField.getPreferredSize());
                mTitleField.setText(title);
                out("NOLAY          size=" + mTitleField.getSize());
                out("NOLAY preferredSize=" + mTitleField.getPreferredSize());
                //mTitleField.setSize(298, mTitleField.getHeight());
                //mTitleField.setSize(298, mTitleField.getHeight());
                //mTitleField.setPreferredSize(new Dimension(298, mTitleField.getHeight()));
                //mTitleField.setSize(mTitleField.getPreferredSize());
                //mTitleField.setSize(mTitleField.getPreferredSize());
                out("SETSZ          size=" + mTitleField.getSize());
                out("SETSZ preferredSize=" + mTitleField.getPreferredSize());
                //out("SETSZ preferredSize=" + mTitleField.getPreferredSize());
                mTitleField.setPreferredSize(null);
                //add(mTitleField, BorderLayout.SOUTH);
                //mTitleField.revalidate();
                //repaint();
                //mTitleField.setVisible(true);
            }

            //mPreviewPane.loadResource(mResource);
            //mPreviewPane.setVisible(true);
            //VUE.invokeAfterAWT(this);
        }

        /*
        public void run() {
            //mTitleField.revalidate();
            //mTitleField.setVisible(true);
            VUE.invokeAfterAWT(new Runnable() { public void run() {
                mPreviewPane.loadResource(mResource);
            }});
        }
        */
    }


    /*
    public static class NodeTree extends JPanel
    {
        private final OutlineViewTree tree;
        
        public NodeTree()
        {
            super(new BorderLayout());
            setName("Nested Nodes");
            tree = new OutlineViewTree();
            
            JScrollPane mTreeScrollPane = new JScrollPane(tree);
            mTreeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(mTreeScrollPane);
        }

        public void load(LWComponent c)
        {
            // if the tree is not intiliazed, hidden, or doesn't contain the given node,
            // then it switches the model of the tree using the given node
            
            if (!tree.isInitialized() || !isVisible() || !tree.contains(c)) {
                //panelLabel.setText("Node: " + pNode.getLabel());
                if (c instanceof LWContainer)
                    tree.switchContainer((LWContainer)c);
                else if (c instanceof LWLink)
                    tree.switchContainer(null);
            }
        }
    }
    */
    
    public void ontologicalMetadataUpdated()
    {
        ontologicalMetadata.refresh();
    }
    
    public static class UserMetaData extends JPanel
    {
        private NodeFilterEditor userMetaDataEditor = null;
        private MetadataEditor userMetadataEditor = null;
        
        public UserMetaData()
        {
            super(new BorderLayout());
          
            if(META_VERSION == NEW)
                setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            //setBorder( BorderFactory.createEmptyBorder(10,10,10,6));

            // todo in VUE to create map before adding panels or have a model that
            // has selection loaded when map is added.
            // userMetaDataEditor = new NodeFilterEditor(mNode.getNodeFilter(),true);
            // add(userMetaDataEditor);
        }
        
        // might someday be useful for Metadata version NEW
        // but only in that case (see ontologicalMetadataUpdated() above)
        public void refresh()
        {
            if(userMetadataEditor != null)
            {
              userMetadataEditor.listChanged();
            }
        }

//         @Override
//         public Dimension getMinimumSize() {
//             //return new Dimension(200,150);
//             Dimension s = getPreferredSize();
//             s.height += 9;
//             return s;
//         }
        
     
        void load(LWComponent c) {
            //setTypeName(this, c, "Keywords");
            if(META_VERSION == OLD)
            {
              if (DEBUG.SELECTION) System.out.println("NodeFilterPanel.updatePanel: " + c);
              if (userMetaDataEditor != null) {
                  //System.out.println("USER META SET: " + c.getNodeFilter());
                  userMetaDataEditor.setNodeFilter(c.getNodeFilter());
              } else {
                  if (VUE.getActiveMap() != null && c.getNodeFilter() != null) {
                      // NodeFilter bombs entirely if no active map, so don't let
                      // it mess us up if there isn't one.
                      userMetaDataEditor = new NodeFilterEditor(c.getNodeFilter(), true);
                      add(userMetaDataEditor, BorderLayout.CENTER);
                      //System.out.println("USER META DATA ADDED: " + userMetaDataEditor);
                  }
              }
            }
            else
            {
                if(userMetadataEditor == null)
                {
                  setOpaque(false);
                  userMetadataEditor = new MetadataEditor(c,true,true);

                  LWSelection selection = VUE.getSelection();
                  if(selection.contents().size() > 1)
                  {
                      userMetadataEditor.selectionChanged(selection);
                  }
                  
                  add(userMetadataEditor,BorderLayout.CENTER);
                }
            }
        }
    }

    private JLabel makeLabel(String s) {
        JLabel label = new JLabel(s);
        GUI.apply(GUI.LabelFace, label);
        //label.setBorder(new EmptyBorder(0,0,0, GUI.LabelGapRight));
        return label;
    }

    // summary fields
    /*
    private final Object[] labelTextPairs = {
        "-Title",   mTitleField,
        "-Where",   mWhereField,
        "-Size",    mSizeField,
    };
    */

    public class LabelPane extends tufts.Util.JPanelAA
    {
        final VueTextPane labelValue = new VueTextPane();
        
        LabelPane() {
            super(new BorderLayout());

            final int si = 5; // size inner

            if (Util.isMacPlatform()) {
                final int so = 7; // size outer
                // be sure to fetch and include the existing border, in case it's a special mac hilighting border
                //labelValue.setBorder(new CompoundBorder(new EmptyBorder(so,so,so,so),
                labelValue.setBorder(new CompoundBorder(new MatteBorder(so,so,so,so,SystemColor.control),
                                                        new CompoundBorder(labelValue.getBorder(),
                                                                           new EmptyBorder(si,si,si,si)
                                                                           )));
            } else {
                final int so = 5; // size outer
                setBorder(new EmptyBorder(so,so,so,so));
                labelValue.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED),
                                                        //new BevelBorder(BevelBorder.LOWERED),
                                                        new EmptyBorder(si,si,si,si)));
            }
            setName("nodeLabel");
            add(labelValue);
        }

        void load(LWComponent c) {
            setTypeName(this, c, "Label");
            if (c instanceof LWText)
            	labelValue.setEditable(false);
            else
            	labelValue.setEditable(true);
            labelValue.attachProperty(c, LWKey.Label);
        }
    }
    
    public class SummaryPane extends tufts.Util.JPanelAA
        implements Runnable
    {
        final VueTextPane labelValue = new VueTextPane();
        final JScrollBar labelScrollBar;
        //final VueTextField contentValue = new VueTextField();
        
        SummaryPane()
        {
            super(new GridBagLayout());
            //setBorder(new EmptyBorder(4, GUI.WidgetInsets.left, 4, 0));
            setBorder(GUI.WidgetInsetBorder);
            setName("nodeSummary");
            
            //If you're trying to debug lists, you'll want to see the HTML code somewhere,
            //and here is as good as any place right now.  It may be a TODO to put this label
            //on a tabbed pane with an editable version of the HTML code so you can tweak it
            //in case of an error.
          //  if (!DEBUG.LISTS)
          //  	labelValue.setContentType("text/html");
            //contentValue.setEditable(false);
            
            labelValue.setBorder(null);
            JScrollPane labelScroller = new JScrollPane(labelValue,
                                                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                                                        );
            int lineHeight = labelValue.getFontMetrics(labelValue.getFont()).getHeight(); 
            labelScroller.setMinimumSize(new Dimension(70, (lineHeight*3)+5));
            labelScrollBar = labelScroller.getVerticalScrollBar();
                
            addLabelTextPairs(new Object[] {
                    "Label", labelScroller,
                    //"Content", contentValue,
                },
                this);
            
            //setPreferredSize(new Dimension(Short.MAX_VALUE,100));
            //setMinimumSize(new Dimension(200,90));
            //setMinimumSize(new Dimension(200,63));
            //setMaximumSize(new Dimension(Short.MAX_VALUE,63));

            // Workaround: if we don't do this, sometimes the top-pixel of our Widget
            // title gets clipped when the InspectorPane content grows large enough
            // to activate the scroll-bar
            setMinimumSize(new Dimension(200,80));
            setPreferredSize(new Dimension(200,80));
            
        }

//         public Dimension getMinimumSize() {
//             return new Dimension(200,80);    	
//         }
//         public Dimension getPreferredSize() {
//             return new Dimension(200,80);
//         }
        
        public void run() {
            labelScrollBar.setValue(0);
            labelScrollBar.setValueIsAdjusting(false);
        }


        void load(LWComponent c) {
            setTypeName(this, c, "Information");
            if (c instanceof LWText)
            	labelValue.setEditable(false);
            else
            	labelValue.setEditable(true);
            labelScrollBar.setValueIsAdjusting(true);
            labelValue.attachProperty(c, LWKey.Label);
            /*
            if (c.hasResource()) {
                contentValue.setText(c.getResource().toString());
            } else {
                contentValue.setText("");
            }
            */
            
            GUI.invokeAfterAWT(this);
            
            //out("ROWS " + labelValue.getRows() + " border=" + labelValue.getBorder());
        }
        
    }
    
    
        

    /**
     *
     * This works somewhat analogous to a JTable, except that the renderer's are persistent.
     * We fill a GridBagLayout with all the labels and value fields we might ever need, set their
     * layout constraints just right, then set the text values as properties come in, and setting
     * all the unused label's and fields invisible.  There is a maximum number of rows that can
     * be displayed (initally 20), but this number is doubled when exceeded.
     *
     */
    //----------------------------------------------------------------------------------------
    // Utility methods
    //----------------------------------------------------------------------------------------
    
    private void addLabelTextPairs(Object[] labelTextPairs, Container gridBag) {
        JLabel[] labels = new JLabel[labelTextPairs.length / 2];
        JComponent[] values = new JComponent[labels.length];
        for (int i = 0, x = 0; x < labels.length; i += 2, x++) {
            //out("ALTP[" + x + "] label=" + labelTextPairs[i] + " value=" + GUI.name(labelTextPairs[i+1]));
            String labelText = (String) labelTextPairs[i];
            labels[x] = new JLabel(labelText + ":");
            values[x] = (JComponent) labelTextPairs[i+1];
        }
        addLabelTextRows(0, labels, values, gridBag, GUI.LabelFace, GUI.ValueFace);
    }

    private final int topPad = 2;
    private final int botPad = 2;
    private final Insets labelInsets = new Insets(topPad, 0, botPad, GUI.LabelGapRight);
    private final Insets fieldInsets = new Insets(topPad, 0, botPad, GUI.FieldGapRight);
    
    /** labels & values must be of same length */
    private void addLabelTextRows(int starty,
                                  JLabel[] labels,
                                  JComponent[] values,
                                  Container gridBag,
                                  Font labelFace,
                                  Font fieldFace)
    {
        // Note that the resulting alignment ends up being somehow FONT dependent!
        // E.g., works great with Lucida Grand (MacOSX), but with system default,
        // if the field value is a wrapping JTextPane (thus gets taller as window
        // gets narrower), the first line of text rises slightly and is no longer
        // in line with it's label.
        
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        c.weighty = 0;
        c.gridheight = 1;

        
        for (int i = 0; i < labels.length; i++) {

            //out("ALTR[" + i + "] label=" + GUI.name(labels[i]) + " value=" + GUI.name(values[i]));
            
            boolean centerLabelVertically = false;
            final JLabel label = labels[i];
            final JComponent field = values[i];
            
            if (labelFace != null)
                GUI.apply(labelFace, label);

            if (field instanceof JTextComponent) {
                if (field instanceof JTextField)
                    centerLabelVertically = true;
//                 JTextComponent textField = (JTextComponent) field;
//                 editable = textField.isEditable();
//                 if (field instanceof JTextArea) {
//                     JTextArea textArea = (JTextArea) field;
//                     c.gridheight = textArea.getRows();
//                     } else if (field instanceof JTextField)
            } else {
                if (fieldFace != null)
                    GUI.apply(fieldFace, field);
            }
            
            //-------------------------------------------------------
            // Add the field label
            //-------------------------------------------------------
            
            c.gridx = 0;
            c.gridy = starty++;
            c.insets = labelInsets;
            c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last in row
            c.fill = GridBagConstraints.NONE; // the label never grows
            if (centerLabelVertically)
                c.anchor = GridBagConstraints.EAST;
            else
                c.anchor = GridBagConstraints.NORTHEAST;
            c.weightx = 0.0;                  // do not expand
            gridBag.add(label, c);

            //-------------------------------------------------------
            // Add the field value
            //-------------------------------------------------------
            
            c.gridx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;     // last in row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            //c.anchor = GridBagConstraints.NORTH;
            c.insets = fieldInsets;
            c.weightx = 1.0; // field value expands horizontally to use all space
            gridBag.add(field, c);

        }

        // add a default vertical expander to take up extra space
        // (so the above stack isn't vertically centered if it
        // doesn't fill the space).

        c.weighty = 1;
        c.weightx = 1;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JComponent defaultExpander = new JPanel();
        defaultExpander.setPreferredSize(new Dimension(Short.MAX_VALUE, 1));
        if (DEBUG.BOXES) {
            defaultExpander.setOpaque(true);
            defaultExpander.setBackground(Color.red);
        } else
            defaultExpander.setOpaque(false);
        gridBag.add(defaultExpander, c);
    }
    
    private void loadText(JTextComponent c, String text) {
        String hasText = c.getText();
        // This prevents flashing where fields of
        // length greater the the visible area do
        // a flash-scroll when setting the text, even
        // if it's the same as what's there.
        if (hasText != text && !hasText.equals(text))
            c.setText(text);
    }
    private void loadText(JLabel c, String text) {
        String hasText = c.getText();
        // This prevents flashing where fields of
        // length greater the the visible area do
        // a flash-scroll when setting the text, even
        // if it's the same as what's there.
        if (hasText != text && !hasText.equals(text))
            c.setText(text);
    }
    
    private void out(Object o) {
        Log.debug(o==null?"null":o.toString());
    }
    
    public static void displayTestPane(String rsrc)
    {
//         //MapResource r = new MapResource("file:///System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Home");
//         if (rsrc == null)
//             rsrc = "file:///VUE/src/tufts/vue/images/splash_graphic_1.0.gif";

//         InspectorPane p = new InspectorPane();
//         LWComponent node = new LWNode("Test Node");
//         node.setNotes("I am a note.");
//         System.out.println("Loading resource[" + rsrc + "]");
//         Resource r = Resource.getFactory().get(rsrc);
//         System.out.println("Got resource " + r);
//         r.setTitle("A Very Long Long Resource Title Ya Say");
//         node.setResource(r);
//         for (int i = 1; i < 6; i++)
//             r.setProperty("field_" + i, "value_" + i);

//         DockWindow w = null;
//         if (false) {
//             //ToolWindow w = VUE.createToolWindow("LWCInfoPanel", p);
//             javax.swing.JScrollPane sp = new javax.swing.JScrollPane(p,
//                                              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
//                                              //JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
//                                              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
//                                              );
//             w = GUI.createDockWindow("Inspector", sp);
//         }
//         else
//         {
//             w = GUI.createDockWindow(p);
//             //w = GUI.createDockWindow("Resource Inspector", p.mSummaryPane);
//             //tufts.Util.displayComponent(p);
//         }

//         if (w != null) {
//             w.setUpperRightCorner(GUI.GScreenWidth, GUI.GInsets.top);
//             w.setVisible(true);
//         }
        
//         VUE.getSelection().setTo(node); // setLWComponent does diddly -- need this

    }

    
    public static void main(String args[]) {

//         VUE.init(args);

//         // Must have at least ONE active frame for our focus manager to work
//         //new Frame("An Active Frame").setVisible(true);

//         String rsrc = null;
//         if (args.length > 0 && args[0].charAt(0) != '-')
//             rsrc = args[0];

//         Resource r = Resource.getFactory().get("file:///VUE/src/tufts/vue/images/splash_graphic_1.0.gif");

//         if (true) {
//             displayTestPane(rsrc);
//         } else {
//             InspectorPane ip = new InspectorPane();
//             VUE.getResourceSelection().setTo(r, "main::test");
//             Widget.setExpanded(ip.mResourceMetaData, true);
//             GUI.createDockWindow("Test Properties", ip).setVisible(true);
//         }

        
    }

    public void searchPerformed(SearchEvent evt) {
        if ((VUE.getSelection().size() > 0) && (VUE.getResourceSelection().get() == null))
            return;

        showNodePanes(false);
        showResourcePanes(false);
        LWSelection selection = VUE.getSelection();
			
        LWComponent c = selection.first();
        
        if (c != null) {
            if (c.hasResource()) {
                loadResource(c.getResource());
                showNodePanes(true);
                showResourcePanes(true);                
            }
            else
                showNodePanes(true);
            
        }
    }
    
}

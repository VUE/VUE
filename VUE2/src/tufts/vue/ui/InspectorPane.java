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
 * @version $Revision: 1.92 $ / $Date: 2008-05-28 07:51:54 $ / $Author: sfraize $
 */

public class InspectorPane extends WidgetStack
    implements VueConstants, ResourceSelection.Listener, LWSelection.Listener, SearchListener, Runnable
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

    private final LabelPane mLabelPane = new LabelPane(); // old style; new SummaryPane()
    private final NotePanel mNotes = new NotePanel();
    private final NotePanel mPathwayNotes = new NotePanel(false);
    private final UserMetaData mKeywords = new UserMetaData();
    private final OntologicalMembershipPane ontologicalMetadata = new OntologicalMembershipPane();
    
    //-------------------------------------------------------
    // Resource panes
    //-------------------------------------------------------
    private final MetaDataPane mResourceMetaData = new MetaDataPane(false);
    private final Preview mPreview = new Preview();
    
    private final WidgetStack stack;
    
    private Resource mResource; // the current resource

    private JScrollPane mScrollPane;
    private boolean inScroll;
    
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

        stack = this;
        
        Widget.setWantsScroller(stack, true);
        Widget.setWantsScrollerAlways(stack, true);

        final float EXACT_SIZE = 0f;
        final float EXPANDER = 1f;

        new Pane("Label",                  mLabelPane,          EXACT_SIZE,  INFO+NOTES+KEYWORD);
        new Pane("Content Preview",        mPreview,            EXACT_SIZE,  RESOURCE);
        new Pane("Content Info",           mResourceMetaData,   EXACT_SIZE,  RESOURCE);
        new Pane("Notes",                  mNotes,              EXPANDER,    INFO+NOTES);
        new Pane("Pathway Notes",          mPathwayNotes,       EXPANDER,    INFO+NOTES);
        new Pane("Keywords",               mKeywords,           EXACT_SIZE,  KEYWORD);
        new Pane("Ontological Membership", ontologicalMetadata, EXACT_SIZE,  0);

        for (Pane p : Pane.AllPanes)
            stack.addPane(p.name, p.widget, p.size);

        VUE.getSelection().addListener(this);
      //VUE.addActiveListener(LWComponent.class, this);
        VUE.addActiveListener(LWPathway.Entry.class, this);
        VUE.getResourceSelection().addListener(this);
        
        Widget.setHelpAction(mLabelPane,VueResources.getString("dockWindow.Info.summaryPane.helpText"));;
        Widget.setHelpAction(mPreview,VueResources.getString("dockWindow.Info.previewPane.helpText"));;
        Widget.setHelpAction(mResourceMetaData,VueResources.getString("dockWindow.Info.resourcePane.helpText"));;
        Widget.setHelpAction(mNotes,VueResources.getString("dockWindow.Info.notesPane.helpText"));;
        Widget.setHelpAction(mKeywords,VueResources.getString("dockWindow.Info.userPane.helpText"));;
        Widget.setHelpAction(ontologicalMetadata,VueResources.getString("dockWindow.Info.ontologicalMetadata.helpText"));


        //Set the default state of the inspector pane to completely empty as nothign 
        //is selected and its misleading to have the widgets on there.
        hideAll();
        
        // These two are present, but un-expanded by default:
        Widget.setExpanded(ontologicalMetadata, false);
        Widget.setExpanded(mKeywords, false);

        setMinimumSize(new Dimension(300,500)); // if WidgetStack overrides getMinimumSize w/out checking for a set, this won't work.
    }

    public WidgetStack getWidgetStack() {
    	return stack;
    }

    @Override
    public void addNotify() {
        super.addNotify();

        if (mScrollPane == null) {
            mScrollPane = (JScrollPane) javax.swing.SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
            if (DEBUG.Enabled) Log.debug("got scroller " + GUI.name(mScrollPane));
        }

        inScroll = (mScrollPane != null);
    }
    
    private void displayHold() {
        if (inScroll) {
            mScrollPane.getVerticalScrollBar().setValueIsAdjusting(true);
        }
    }

    private void displayRelease() {
        if (inScroll) {
            //out("DO-SCROLL");
            GUI.invokeAfterAWT(this);
        }
    }
    
    public void run() {
        
        // Always put the scroll-bar back at the top, as it defaults to moving to the
        // bottom.  E.g., when selecting through search results that all have tons of
        // meta-data, we're left looking at just the meta-data, not the preview.
        // Ideally, we could check the scroller position first to see if we were
        // already at the top at the start and only do this if that was the case.
        
        //out("SCROLL-TO-TOP");
        mScrollPane.getVerticalScrollBar().setValue(0);
        mScrollPane.getVerticalScrollBar().setValueIsAdjusting(false);

        //VUE.getInfoDock().scrollToTop();

        // No idea why we might need to do this twice right now...
        
        GUI.invokeAfterAWT(new Runnable() { 
                public void run() { 
                    mScrollPane.getVerticalScrollBar().setValue(0);
                    mScrollPane.getVerticalScrollBar().setValueIsAdjusting(false);
                    //VUE.getInfoDock().scrollToTop();
                } 
            });
        
           
    }
    

    public void resourceSelectionChanged(ResourceSelection.Event e)
    {    	
        if (e.selected == null)
            return;
        displayHold();
        if (DEBUG.RESOURCE) out("resource selected: " + e.selected);
        showNodePanes(false);
        showResourcePanes(true);
        loadResource(e.selected);
        setVisible(true);
        stack.setTitleItem("Content");
        displayRelease();
    }

    private LWComponent activeEntrySelectionSync;
    private LWPathway.Entry loadedEntry; // not needed at the moment

    public void activeChanged(final tufts.vue.ActiveEvent _e, final LWPathway.Entry entry)
    {
        displayHold();
        
        final int index = (entry == null ? -9 : entry.index() + 1);

        loadedEntry = entry;

      //final boolean slidesShowing = LWPathway.isShowingSlideIcons();
        final boolean slidesShowing = entry.pathway.isShowingSlides();
        
        if (index < 1 || slidesShowing) {
            
            mPathwayNotes.setHidden(true);
            mPathwayNotes.detach();
            activeEntrySelectionSync = null;

        } else {

            // We always get activeChanged events for the LWPathway.Entry BEFORE the
            // resulting selection change on the map, so we can reliably set
            // activeEntrySelectioSync here, and check for it later when are notified
            // that the selection has changed.

            if (false && slidesShowing) {
                // This adds the reverse case: display node notes when a slide is selected:
                activeEntrySelectionSync = entry.getSlide();
                mPathwayNotes.attach(entry.node);
                mPathwayNotes.setTitle("Node Notes");
            }
            else {
                activeEntrySelectionSync = entry.node;
                mPathwayNotes.attach(entry.getSlide());
                mPathwayNotes.setTitle("Slide Notes (" + entry.pathway.getLabel() + ": #" + index + ")");
            }
            
            mPathwayNotes.setHidden(false);
        }

        displayRelease(); // can optimize out: should be able to rely on follow-on selectionChanged...
        
    }
    
    public void selectionChanged(final LWSelection s)
    {
        displayHold();
        
        if (s.size() == 0) {
            
            hideAll();
            
        } else if (s.size() == 1) {

            loadSingleSelection(s.first());
            setVisible(true);
            
        } else {
            
            loadMultiSelection(s);
            setVisible(true);
            
        }

        displayRelease();
    }
    
    private void loadSingleSelection(LWComponent c)
    {
        showNodePanes(true);

        if (c instanceof LWSlide || c.hasAncestorOfType(LWSlide.class)) {
            mKeywords.setHidden(true);
            Widget.setHidden(ontologicalMetadata, true);
        }

        if (activeEntrySelectionSync != c) {
            mPathwayNotes.setHidden(true);
            loadedEntry = null;
        } 
        activeEntrySelectionSync = null;

        loadData(c);
             	
        if (c.hasResource()) {
            loadResource(c.getResource());
            showResourcePanes(true);
        } else {
            showResourcePanes(false);
        }
        
    }

    private void loadMultiSelection(final LWSelection s)
    {
        loadedEntry = null;
        hideAllPanes();
        mKeywords.loadKeywords(null);
      //Widget.setExpanded(mKeywords, true);
        Widget.setHidden(mKeywords, false);
    }
    

    private void loadData(LWComponent c) {

        LWComponent slideTitle = null;

        if (c instanceof LWSlide && c.isPathwayOwned() && c.hasChildren()) {
            search:
            try { 
                final LWComponent titleStyle = ((LWSlide)c).getEntry().pathway.getMasterSlide().getTitleStyle();
                    
                if (titleStyle == null)
                    break search;
                
                // check top-level children first:
                for (LWComponent sc : c.getChildren()) {
                    if (sc.getStyle() == titleStyle) {
                        slideTitle = sc;
                        break search;
                    }
                }
                
                // check everything else second
                // We wind up rechecking the slide's immediate children, but this is just a fallback condition.
                for (LWComponent sc : c.getAllDescendents()) {
                    if (sc.getStyle() == titleStyle) {
                        slideTitle = sc;
                        break search;
                    }
                }
            } catch (Throwable t) {
                Log.error("load " + c, t);
            }
        }

        if (slideTitle == null)
            mLabelPane.load(c);
        else
            mLabelPane.load(slideTitle, c);
        mKeywords.loadKeywords(c);
        
        if (DEBUG.Enabled)
            setTitleItem(c.getUniqueComponentTypeLabel());
        else
            setTitleItem(c.getComponentTypeLabel());
    }


    private void setTypeName(JComponent component, LWComponent c, String suffix)
    {
        final String type = c.getComponentTypeLabel();
        
        String title;
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
    }

    private void hideAllPanes()
    {
        for (Pane p : Pane.AllPanes)
            Widget.setHidden(p.widget, true);
    }

    private void hideAll()
    {
        setVisible(false);
        
        hideAllPanes();

        setTitleItem(null);
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
        Widget.setHidden(mLabelPane, !visible);        
        Widget.setHidden(mNotes, !visible);

        Widget.setHidden(mKeywords, !visible);
        Widget.setHidden(ontologicalMetadata, !visible);
        
    }
    private void showResourcePanes(boolean visible) {
        Widget.setHidden(mResourceMetaData, !visible);
        Widget.setHidden(mPreview, !visible);
    }
    
    public void showKeywordView()
    {
        expandCollapsePanes(KEYWORD);
    }
    
    public void showNotesView()
    {
        expandCollapsePanes(NOTES);
    	
    	SwingUtilities.invokeLater(new Runnable() { 
            public void run() { 
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                mNotes.getTextPane().requestFocusInWindow();
            } 
    	} );

    }
    
    public void showInfoView() {
        expandCollapsePanes(INFO);
    }


    private class Preview extends tufts.vue.ui.PreviewPane
    {
        Preview() {
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
    
    public class UserMetaData extends Widget
    {
        //private NodeFilterEditor userMetaDataEditor = null;
        private MetadataEditor userMetadataEditor = null;
        
        public UserMetaData()
        {
            super("Keywords");
            setLayout(new BorderLayout());
          
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
            Log.debug("REFRESH " + this);
            if (userMetadataEditor != null)
                userMetadataEditor.listChanged();
        }

        void loadKeywords(LWComponent c) {
            if (userMetadataEditor == null) {
                setOpaque(false);
                userMetadataEditor = new MetadataEditor(c,true,true);
                
                LWSelection selection = VUE.getSelection();
                if(selection.contents().size() > 1) {
                    userMetadataEditor.selectionChanged(selection);
                }
                add(userMetadataEditor,BorderLayout.CENTER);
            }
            if (c != null && c.hasMetaData())
                mKeywords.setTitle("Keywords (" + c.getMetadataList().getCategoryListSize() + ")");
            else
                mKeywords.setTitle("Keywords");
            
        }

//         @Override
//         public Dimension getMinimumSize() {
//             //return new Dimension(200,150);
//             Dimension s = getPreferredSize();
//             s.height += 9;
//             return s;
//         }
        
        
//         void load(LWComponent c) {
//             //setTypeName(this, c, "Keywords");
//             if(META_VERSION == OLD)
//             {
//               if (DEBUG.SELECTION) System.out.println("NodeFilterPanel.updatePanel: " + c);
//               if (userMetaDataEditor != null) {
//                   //System.out.println("USER META SET: " + c.getNodeFilter());
//                   userMetaDataEditor.setNodeFilter(c.getNodeFilter());
//               } else {
//                   if (VUE.getActiveMap() != null && c.getNodeFilter() != null) {
//                       // NodeFilter bombs entirely if no active map, so don't let
//                       // it mess us up if there isn't one.
//                       userMetaDataEditor = new NodeFilterEditor(c.getNodeFilter(), true);
//                       add(userMetaDataEditor, BorderLayout.CENTER);
//                       //System.out.println("USER META DATA ADDED: " + userMetaDataEditor);
//                   }
//               }
//             }
//             else
//             {
//                 if(userMetadataEditor == null)
//                 {
//                   setOpaque(false);
//                   userMetadataEditor = new MetadataEditor(c,true,true);

//                   LWSelection selection = VUE.getSelection();
//                   if(selection.contents().size() > 1)
//                   {
//                       userMetadataEditor.selectionChanged(selection);
//                   }
                  
//                   add(userMetadataEditor,BorderLayout.CENTER);
//                 }
//             }
//         }
        
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
            load(c, c);
        }
        
        void load(LWComponent c, LWComponent editType) {
            setTypeName(this, editType, "Label");
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
//             //w = GUI.createDockWindow("Resource Inspector", p.mLabelPane);
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

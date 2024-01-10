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

package tufts.vue.ui;

import tufts.Util;
import tufts.vue.*;
import tufts.vue.gui.*;
import tufts.vue.NotePanel;
//import tufts.vue.filter.NodeFilterEditor;
import tufts.vue.ActiveEvent;

import java.util.*;
import java.io.*;
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

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Display information about the selected Resource, or LWComponent and it's Resource.
 *
 * @version $Revision: 1.131 $ / $Date: 2010-02-03 19:16:31 $ / $Author: mike $
 */

public class InspectorPane extends WidgetStack
    implements VueConstants, LWSelection.Listener, SearchListener, Runnable
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(InspectorPane.class);
    
//     public static final int META_VERSION = VueResources.getInt("metadata.version");
//     /** meta-data */ public static final int OLD = 0;
//     /** meta-data */ public static final int NEW = 1;
    
    private final Image NoImage = VueResources.getImage("NoImage");

    private final boolean isMacAqua = GUI.isMacAqua();

    //private static final boolean EASY_READING_DESCRIPTION = VUE.VUE3;

    //-------------------------------------------------------
    // Node panes
    //-------------------------------------------------------

    private final LabelPane mLabelPane = new LabelPane(); // old style; new SummaryPane()
    private final NotePanel mNotes = new NotePanel("LWNotes");
    private final NotePanel mPathwayNotes = new NotePanel("PathwayNotes", false);
    private final UserMetaData mKeywords = new UserMetaData();
    private final OntologicalMembershipPane ontologicalMetadata = new OntologicalMembershipPane();
    
    //-------------------------------------------------------
    // Resource panes
    //-------------------------------------------------------
    private final MetaDataPane mResourceMetaData = new MetaDataPane("Resource Properties", false);
    private final MetaDataPane mDataSetData = new MetaDataPane("Data Set Fields", false);
    private final Widget mDescription = new Widget("contentInfo"); // for GUI.init property applicaton (use same as meta-data pane)
    private final Preview mPreview = new Preview();
    
    private final JLabel mSelectionInfo = new JLabel("", JLabel.CENTER);
    
    private final WidgetStack stack;
    
    private Resource mResource; // the current resource

    private JScrollPane mScrollPane;
    private boolean inScroll;
    
    private static class Pane {

        static Collection<Pane> AllPanes = new ArrayList<>();

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
    private static final int DATA = 16;
  
    public InspectorPane()
    {
        super("Info");

        stack = this;
        
        Widget.setWantsScroller(stack, true);
        Widget.setWantsScrollerAlways(stack, true);

        final float EXACT_SIZE = 0f;
        final float EXPANDER = 1f;

        mDescription.setBorder(GUI.WidgetInsetBorder);
        mDescription.setOpaque(true);
        mDescription.setBackground(Color.white);

        mSelectionInfo.setFont(tufts.vue.gui.GUI.StatusFace);
        //mSelectionInfo.setFont(tufts.vue.gui.GUI.StatusFaceSmall);
        //mSelectionInfo.setFont(VueConstants.SmallFont);
        mSelectionInfo.setForeground(Color.darkGray);
        mSelectionInfo.setBorder(GUI.WidgetInsetBorder);

        new Pane("_multi-selection-info",  mSelectionInfo,      EXACT_SIZE,    0);
        new Pane(VueResources.getString("inspectorpane.label"),          mLabelPane,          EXACT_SIZE,  INFO+NOTES+KEYWORD);
        new Pane(VueResources.getString("inspectorpane.contentpreviews"),mPreview,EXACT_SIZE,  RESOURCE);
        new Pane(VueResources.getString("inspectorpane.datasetfields"),mDataSetData,EXACT_SIZE,  INFO+NOTES+KEYWORD+DATA);
        new Pane(VueResources.getString("inspectorpane.contentinfo"),mResourceMetaData,   EXACT_SIZE,  RESOURCE);
        new Pane(VueResources.getString("inspectorpane.contentsummary"),mDescription,        0.5f,        RESOURCE+DATA);
        new Pane(VueResources.getString("nodeNotesTabName"),mNotes,EXACT_SIZE,    INFO+NOTES);
        new Pane(VueResources.getString("inspectorpane.pathwaynotes"),mPathwayNotes,EXPANDER,INFO+NOTES);
        new Pane(VueResources.getString("jlabel.keyword"),mKeywords,EXACT_SIZE,  KEYWORD);
        new Pane(VueResources.getString("combobox.mergepropertychoices.ontologicalmembership"), ontologicalMetadata, EXACT_SIZE,  0);

        for (Pane p : Pane.AllPanes)
            stack.addPane(p.name, p.widget, p.size);

        VUE.getSelection().addListener(this);
        VUE.addActiveListener(LWComponent.class, this);
        VUE.addActiveListener(Resource.class, this);
        VUE.addActiveListener(LWPathway.Entry.class, this);
        VUE.addActiveListener(MetaMap.class, this);
        //VUE.getResourceSelection().addListener(this);
        
        Widget.setHelpAction(mLabelPane,VueResources.getString("dockWindow.Info.summaryPane.helpText"));
        Widget.setHelpAction(mPreview,VueResources.getString("dockWindow.Info.previewPane.helpText"));
        Widget.setHelpAction(mResourceMetaData,VueResources.getString("dockWindow.Info.resourcePane.helpText"));
        Widget.setHelpAction(mNotes,VueResources.getString("dockWindow.Info.notesPane.helpText"));
        Widget.setHelpAction(mKeywords,VueResources.getString("dockWindow.Info.userPane.helpText"));
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
    
    /** interface Runnable */
    public void run() {
        
        // TODO: move this code up to a generic Widget capability,
        // merging it with similar code in MetaDataPane Widget.
        
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
    
    public void activeChanged(final tufts.vue.ActiveEvent e, final Resource resource)
    {    	
        if (resource == null)
            return;

        if (e.source instanceof ActiveEvent && ((ActiveEvent)e.source).type == LWComponent.class) {
            // if this resource change was due to the component change, ignore: is being
            // handled all at once via our selectionChanged listener
            return;
        }

        if (notShowing(e)) return;
        
        displayHold();
        //if (DEBUG.RESOURCE) out("resource selected: " + e.selected);
//         showNodePanes(false);
//         showResourcePanes(true);
        displayPanes(RESOURCE);
        loadResource(resource, null);
        setVisible(true);
        stack.setTitleItem("Content");
        displayRelease();
    }

    // These events generally coming from the DataTree for data-set browsing (there's
    // no LWComponent to browse against)
    public void activeChanged(final tufts.vue.ActiveEvent e, final MetaMap dataMap)
    {    	
        if (dataMap == null)
            return;

        if (notShowing(e)) return;
        
        // todo: below essentially repeats Resource active changed
        
        displayHold();
        displayPanes(DATA);
        loadContentSummary(null, dataMap);
        mDataSetData.loadTable(dataMap);
        setVisible(true);
        stack.setTitleItem("Data");
        displayRelease();
    }
    

    private LWComponent activeEntrySelectionSync;
    private LWPathway.Entry loadedEntry; // not needed at the moment

    public void activeChanged(final tufts.vue.ActiveEvent _e, final LWPathway.Entry entry)
    {
        if (notShowing(_e)) return;
        
        displayHold();
        
        final int index = (entry == null ? -9 : entry.index() + 1);

        loadedEntry = entry;

//         //final boolean slidesShowing = LWPathway.isShowingSlideIcons();
//         final boolean slidesShowing = entry.pathway.isShowingSlides();
        
        if (index < 1 || entry.pathway.isShowingSlides()) {
            
            Widget.hide(mPathwayNotes);
            mPathwayNotes.detach();
            activeEntrySelectionSync = null;

        } else {

            // We always get activeChanged events for the LWPathway.Entry BEFORE the
            // resulting selection change on the map, so we can reliably set
            // activeEntrySelectioSync here, and check for it later when are notified
            // that the selection has changed.

//             if (slidesShowing) {
//                 // This adds the reverse case: display node notes when a slide is selected:
//                 activeEntrySelectionSync = entry.getSlide();
//                 mPathwayNotes.attach(entry.node);
//                 mPathwayNotes.setTitle("Node Notes");
//          } else {
                activeEntrySelectionSync = entry.node;
                mPathwayNotes.attach(entry.getSlide());
                mPathwayNotes.setTitle("Pathway Notes (" + entry.pathway.getLabel() + ": #" + index + ")");
//          }

            Widget.show(mPathwayNotes);
        }

        displayRelease(); // can optimize out: should be able to rely on follow-on selectionChanged...
        
    }

    private boolean selectionChanged;
    
    public void activeChanged(final tufts.vue.ActiveEvent e, final LWComponent c) {

//         if (VUE.getSelection().size() == 0 && c != null) {
//             // to pick up active layer sets
//             loadSingleSelection(c);
//             setVisible(true);
//         }
            

//         selectionChanged = false;

//         GUI.invokeAfterAWT(new Runnable() {
//                 public void run() {
//                     if (!selectionChanged) {
//                         Log.debug("ACTIVE COMPONENT CHANGE W/OUT SELECTION CHANGE: " + c);
//                         loadSingleSelection(c);
//                     }
//                 }
//             });
        
    }

    private boolean notShowing(Object event) {

        // hack for now:
        return VUE.inPresentMode();
        
// not good enough: may need to check our parent window, not the widget stack, which may
// not be visible yet, even if the parent window is 
//         if (isShowing()) {
//             if (DEBUG.Enabled) Log.debug("SHOWING");
//             return false;
//         } else {
//             if (DEBUG.Enabled) Log.debug("NOT SHOWING");
//             return true;
//         }
    }
    
    public void selectionChanged(final LWSelection s)
    {
        selectionChanged = true;

        if (notShowing(s)) return;

        displayHold();
        
        if (s.size() == 0) {
            
            hideAll();
            mSelectionInfo.setText(VueResources.getString("inspectionpane.nothingselected.tooltip"));
            Widget.show(mSelectionInfo);
            setVisible(true);
            
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
        Widget.hide(mSelectionInfo);
        
        //displayPanes(NODE);
        showNodePanes(true);

        if (c instanceof LWSlide || c.hasAncestorOfType(LWSlide.class)) {
            Widget.hide(mKeywords);
            Widget.hide(ontologicalMetadata);
        }

        if (activeEntrySelectionSync != c) {
            Widget.hide(mPathwayNotes);
            loadedEntry = null;
        } 
        activeEntrySelectionSync = null;

        loadAllNodePanes(c);
             	
//         if (c.hasResource()) {
//             //loadResource(c.getResource());
//             loadResource(c.getResource(), c);
//             showResourcePanes(true);
//         } else {
//             showResourcePanes(false);
//         }
        
    }

    private static final String ItemsSelectedFmt = VueResources.getString("infowindow.itemselected");

    private void loadMultiSelection(final LWSelection s)
    {
        loadedEntry = null;
        hideAllPanes();
        mKeywords.loadKeywords(null);

        setTitleItem(String.format(ItemsSelectedFmt, s.size()));

        final String desc = getSelectionDescription(s);

        if (DEBUG.Enabled) Log.debug("selection-description: " + Util.tags(desc));

        // why after AWT?
        //GUI.invokeAfterAWT(new Runnable() { public void run() {
            mSelectionInfo.setText(desc);
            //}});
 
        mLabelPane.loadLabel(s);
        Widget.show(mLabelPane); // connect up to schematic-field style node?
        Widget.show(mSelectionInfo);
      //Widget.setExpanded(mKeywords, true);
        Widget.show(mKeywords);

    }


    private static final String SelectionStatsFmt = VueResources.local("mapinspectorpanel.objectStats.format");
    private static final String WordNodes = VueResources.local("mapinspectorpanel.objectStats.nodes");
    private static final String WordLinks = VueResources.local("mapinspectorpanel.objectStats.links");
    private static final String WordGroups = VueResources.local("mapinspectorpanel.objectStats.groups");
    private static final String WordImages = "Images:";

    private static final String DescriptionFormat = "<html>%s&nbsp;%d&nbsp;&nbsp;&nbsp; %s&nbsp;%d&nbsp;&nbsp;&nbsp; %s&nbsp;%d</html>";

    private static final String SP = " ";
    private static final String SP3 = "&nbsp;&nbsp;&nbsp; ";
    private static final String GAP = " &nbsp; ";

    /** A convenience class whose toString() returns "" the first time, and real content after that.
     * Useful for generating lines of output with separators that aren't of course wanted the first time.
     */private static final class EmptyOnce {
        final String s; boolean did;
        EmptyOnce(String in) { s = in; }
        public String toString() { if (did) return s; else { did = true; return ""; } }
    }

    //private static final EmptyOnce Gap = new EmptyOnce("<br>");
    private static final EmptyOnce Gap = new EmptyOnce(GAP);
    private static final String Post = "";
    //private static final String Gap = "<ul>";
    //private static final String Post = "</ul>";
        
    protected static String getSelectionDescription(final LWSelection s)
    {
        final StringBuilder b = new StringBuilder(40);

        // todo: could actually call getTypes() on LWSelection to count types of each kind 

        final int nodeCount = s.count(LWNode.class);
        final int linkCount = s.count(LWLink.class);
        final int groupCount = s.count(LWGroup.class);
        final int imageCount = s.count(LWImage.class);

        Gap.did = false;

        // Recall that <font color=green>, etc, actually works as well.  Try below if want to play w/justified labels/values.
        // b.append("<html><b><br><table border=0 cellpadding=1><tr align=right><td>foo</td><td>barbella</td><tr><td>bizwing</td><td>box</td></table>");

        b.append("<html><b>");
        
        if (nodeCount > 0)  { b.append(Gap).append(WordNodes) .append(SP).append(nodeCount).append(Post);  }
        if (linkCount > 0)  { b.append(Gap).append(WordLinks) .append(SP).append(linkCount).append(Post);  }
        if (imageCount > 0) { b.append(Gap).append(WordImages).append(SP).append(imageCount).append(Post); }
        if (groupCount > 0) { b.append(Gap).append(WordGroups).append(SP).append(groupCount).append(Post); }
        
        if (s.getDescription() != null) {
            b.append("<br>");
            b.append(s.getDescription());
        }
        b.append("</html>");

        return b.toString();
    }
    
    // Selection already keeps counts:
    // return String.format(Locale.getDefault(), SelectionStatsFmt,
    //                      WordNodes, s.count(LWNode.class),
    //                      WordLinks, s.count(LWLink.class),
    //                      WordGroups, s.count(LWGroup.class));
    // protected String countObjects(LWSelection sel) {
    //     int	nodeCount = 0,
    //         linkCount = 0,
    //     	    groupCount = 0;
    //     for (LWComponent comp : sel) {
    //     		if (comp instanceof LWNode) {
    //     			nodeCount++;
    //     		} else if (comp instanceof LWLink) {
    //     			linkCount++;
    //     		} else if (comp instanceof LWGroup) {
    //     			groupCount++;
    //     		}
    //     }
    //     return String.format(Locale.getDefault(), VueResources.getString("mapinspectorpanel.objectStats.format"),
    //     		VueResources.getString("mapinspectorpanel.objectStats.nodes"), nodeCount,
    //     		VueResources.getString("mapinspectorpanel.objectStats.links"), linkCount,
    //     		VueResources.getString("mapinspectorpanel.objectStats.groups"), groupCount);
    // }
    
    private void loadAllNodePanes(LWComponent c) {

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
            mLabelPane.loadLabel(c);
        else
            mLabelPane.loadLabel(slideTitle, c);


        if (c.getDataTable() == null) {
            Widget.hide(mDataSetData);
        } else {
            mDataSetData.loadTable(c.getDataTable());
            Widget.show(mDataSetData);
        }
        
        if (c.hasResource()) {
            //loadResource(c.getResource());
            loadResource(c.getResource(), c);
            showResourcePanes(true);
        } else {
            showResourcePanes(false);
        }
        
        mKeywords.loadKeywords(c);
        
        if (DEBUG.Enabled)
            setTitleItem(c.getUniqueComponentTypeLabel());
        else
            setTitleItem(c.getComponentTypeLabel());
    }


    private static void setTypeName(JComponent component, LWComponent c, String suffix)
    {
        final String type;

        if (c == null)
            type = null;
        else if (DEBUG.Enabled)
            type = c.getUniqueComponentTypeLabel();
        else
            type = c.getComponentTypeLabel();
        
        String title;
        if (suffix != null) {
            if (type == null)
                title = suffix;
            else
                title = type + " " + suffix;
        } else
            title = type;
        
        component.setName(title);
    }

//     //-----------------------------------------------------------------------------
//     // experimental code to export to PropertyMap:
//     private static final class Key<T> {
//         final String name;
//         //Key(T t) { type = t; }
//         Key(String s) { name = s; }
//         //Key() { name = T.class; } // can't query type for class
//         @Override
//         public String toString() {
//             return name;
//         }

//         public T cast(Object o) { // would this be needed / handy?  (would it even work to throw a cast-class, or is this all type-erased?)
//             return (T) o;
//         }
//     }
    
//     //private static final Key<JComponent> DESCRIPTION_VIEWER = new Key(JComponent.class);
//     private static final Key<JComponent> DESCRIPTION_VIEWER = new Key("description_viewer");
//     private static final Key<Integer> DESCRIPTION_VIEWER_N = new Key("test_int");

//     private static <T> T get(Key<T> key) { return (T) null; }

//     // or could infer a new key from arguments? (of course, would need to cache the keys tho)
//     //private static <T> void put(Class<T> type, String name, T value) {
//     private static <T> T put(String name, T value) {
//         Key<T> key = new Key(name);

//         return get(key);
//     }

//     static {
// //         int x = put("foo", 1);
// //         long z = put("foo", 1);
// //         char c = put("foo", 'x');
// //         JLabel l = put("bar", new JLabel("baz"));
// //         //JLabel m = put("bar", new JPanel()); // error, as appropriate
        
// //         JComponent foo = get(DESCRIPTION_VIEWER);
// //         int i = get(DESCRIPTION_VIEWER_N);
//     }    
//     //-----------------------------------------------------------------------------

    private void loadResource(final Resource r, LWComponent node) {
        
        if (DEBUG.RESOURCE) out("loadResource: " + r);
        
        if (r == null)
            return;

        mResource = r;
        //mResourceMetaData.loadResource(r);
        mResourceMetaData.loadTable(r.getProperties());
        mPreview.loadResource(r);

        Widget.hide(mSelectionInfo);
        loadContentSummary(r, node == null ? null : node.getRawData());

    }

    private static final String DESCRIPTION_VIEWER_KEY = Resource.HIDDEN_RUNTIME_PREFIX + "ipCache";

    private boolean loadContentSummary(Resource r, MetaMap data) {
        JComponent descriptionView = (r == null ? null : (JComponent) r.getPropertyValue(DESCRIPTION_VIEWER_KEY));
            
        boolean gotView = (descriptionView != null);

        if (descriptionView == null) {

            try {
                descriptionView = buildSummary(r, data);
                if (descriptionView != null)
                    gotView = true;
            } catch (Throwable t) {
                Log.error("loadResource " + r, t);
                // html will enable text-wrap
                descriptionView = new JLabel("<html>"+ r + "<p>" + t.toString()); 
            }
                
            // todo: the below should ideally be auto-cleared when any change to the
            // resource is made and/or any change to it's component properties.
            // (would need to separate "real" meta-data properties from runtime
            // client properties such as this one in that case).  It would be nice
            // if resource properties could individually be specified as having
            // these special attributes themselves.  (e.g., "soft" or
            // "auto-clear-on-change")

            if (gotView && r != null)
                r.setProperty(DESCRIPTION_VIEWER_KEY, new java.lang.ref.SoftReference(descriptionView));
            // r.putSoft
            // r.data.putSoft
        }

        mDescription.removeAll();
        if (gotView) {
            mDescription.add(descriptionView);
            mDescription.repaint();
            return true;
        } else {
            mDescription.setHidden(true);
            return false;
        }

        
    }

    private static final HyperlinkListener DefaultHyperlinkListener =
        new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                //Log.debug(e);
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    java.net.URL url = null;
                    try {
                        url = e.getURL();
                        if (url == null) {
                            Log.warn("no URL in " + e);
                        } else {
                            tufts.vue.VueUtil.openURL(url.toString());
                        }
                    } catch (Throwable t) {
                        Log.warn("hyperlinkUpdate; url=" + url + "; " + e, t);
                    }
                }
            }
        };

//     private static String findProperty(Resource r, LWComponent node, String... keys) {
        
//         for (String k : keys) {
//             String value = null;
//             if (node != null) {
//                 value = node.getDataValue(k);
//                 if (value != null)
//                     return value;
//             }
//             if (r != null) {
//                 value = r.getProperty(k);
//                 if (value != null)
//                     return value;
//             }
//         }
        
//         return null;
//     }
    private static String findProperty(Resource r, MetaMap data, String... keys) {
        
        for (String k : keys) {
            String value = null;
            if (data != null) {
                value = data.getString(k);
                if (value != null)
                    return value;
                if (Character.isUpperCase(k.charAt(0)))
                    value = data.getString(k.toLowerCase());
                if (value != null)
                    return value;
                
            }
            if (r != null) {
                value = r.getProperty(k);
                if (value != null)
                    return value;
                if (Character.isUpperCase(k.charAt(0)))
                    value = r.getProperty(k.toLowerCase());
                if (value != null)
                    return value;
                
            }
        }
        
        return null;
    }
    

    private static JComponent buildSummary(final Resource r, final MetaMap data)
    {
        final JTextPane htmlText = new JTextPane();
        htmlText.setEditable(false);
        htmlText.setContentType("text/html");
        htmlText.setName("description:" + r);
        htmlText.addHyperlinkListener(DefaultHyperlinkListener);

        //final MetaMap data = (node == null ? null : node.getRawData());

        final String desc = findProperty(r, data, "descriptionSummary", "Description", "Summary");
            
        final String summary;
        
        if (desc != null || r == null) {
            
            summary = buildSummaryWithDescription(r, data, desc);
            if (summary == null || summary.length() == 0)
                return null;
                //htmlText.setText("No Description");// todo: should just hide panel
            else
                htmlText.setText(summary);

            if (DEBUG.DATA && data != null) data.put("$reformatted", "[" + summary + "]");
            
        } else { //if (r != null) {
            
            //------------------------------------------------------------------
            // No description was found: build a summary from just the Resource
            //------------------------------------------------------------------
            
            summary = buildSummaryFromResource(r);

            htmlText.setText(summary);

            if (DEBUG.DATA) r.setProperty("~reformatted", summary);
            
        }
        //else return null;

        // This must be done last.  Why this doesn't work up front I don't know: there
        // must be some other way...
        
        GUI.setDocumentFont(htmlText, GUI.ContentFace);

        return htmlText;
       
 //         if (loader != null) {
//             // this not actually helping, other than we get to see "Loading..." before it hangs.
//             final Thread _loader = loader;
//             GUI.invokeAfterAWT(new Runnable() { public void run() {
//                 _loader.start();
//             }});
//         }
                    
            
    }

    private static String buildSummaryFromResource(final Resource r)
    {
        final StringBuilder b = new StringBuilder(128);
        final String title = r.getTitle();

        if (title != null) {
            b.append("<b>");
            // note that the title should already have been HTML UNescaped when set:
            // now we're escaping it again in case it's natural state actually
            // contains anything that looks like an HTML tag.  (technically, we
            // should really only need to escape '<', '>' and '&' at this point)
            b.append(StringEscapeUtils.escapeHtml(title));
            b.append("</b>");
            b.append("<p>");
        }
                
        String spec = r.getSpec();

        //-----------------------------------------------------------------------------

        // We escape HTML here because at least on Mac OS X, valid HTML tags could
        // appear in a file name.  Note that doing this also effects behaviour of
        // our line-breaking tweaks below.  We also want to do this before before we
        // insert any special unicode characters.  E.g., otherwise, inserting
        // zero-width unicode space character \u200B would result in
        // inserting HTML entity &#8203;, which does still appear to work as a break,
        // but is waste to encode.  
            
        // In case of muti-encoded URL's (contains embedded URL's / queries),
        // decode multiple times for a more readable display.
        //spec = Util.decodeURL(spec);

        // todo: start by looking for any top-level query: if none found, decodeURL once, then start
        // over looking for a top level query. Then process each key/value, indenting if embedded queries found,
        // and always applying an extry decodeURL to each value in "&key=value"
        // also: don't make entire thing a multi-line href -- all the whitespace will be accidentally
        // clickable to launch, and also we'll want to inspect each value for an http: value
        // (or any key name ending in "url") and make THAT a link, so it can be explored separately.
        spec = Util.decodeURL(spec);
        spec = StringEscapeUtils.escapeHtml(spec);
        //-----------------------------------------------------------------------------
            
        // will allow text pane to line-break the url (plus makes easier to read queries)
            
        // \u200B is Unicode zero-width space: JTextPane will break on these. We add
        // as desired to break URL's / file paths as painlessly as possible.  Note
        // that apparently including any special unicode character, or perhaps and
        // break-related unicode character into the JTextPane, appears to
        // automatically turn on breaking at basic punctuation: e.g. '.', '-' and '?'.
        // But it does NOT slash or ampersand which we really need, nor '=', or underscore.
            
        // BTW, \uFEFF is the Unicode zero-width NO-BREAK space, which JTextPane,
        // treats properly as non-breaking.

        //-----------------------------------------------------------------------------
        // break after these (the special char should end the previous line)
        //-----------------------------------------------------------------------------
            
        spec = spec.replaceAll("/", "/\u200B");
        spec = spec.replaceAll("=", "=\u200B");
            
        //-----------------------------------------------------------------------------
        // break before these (the special char should start the new line):
        //-----------------------------------------------------------------------------

        // note that breaking AFTER '&' will probably not work, as now we're
        // inserting breaks into a string that has been HTML escaped, so
        // at this point, we're actually inserting a break before any HTML entity,
        // (e.g., &amp; &quot', etc)  not just '&'
        if (false) {
            spec = spec.replaceAll("&", "\u200B&");
        } else {
            //spec = spec.replaceAll("&amp;", "<br>&amp;");
            //spec = spec.replaceAll("&amp;", "<li>");
            spec = spec.replaceAll("&amp;", "<br>& ");
            spec = spec.replaceFirst("\\?", "<br>?");
            spec = spec.replaceFirst("\\*\\*", "<br>**"); // yahoo image URL's use this
        }
            
        spec = spec.replaceAll("_", "\u200B_");
        spec = spec.replaceAll("@", "\u200B@");
        // spec = spec.replaceAll(":", "\u200B:"); // todo: for generic descriptions which might contain big URN's (e.g., tufts DL)
            
        spec = spec.replaceAll("\\+", "\u200B+");
        spec = spec.replaceAll("\\?", "\u200B?");

        // todo: ideally, also put break before lower-case to upper-case char transitions
        //-----------------------------------------------------------------------------
            
        b.append("<font size=-1>");
        b.append("<a href=\"");

        try {
            if (r.isLocalFile()) {
                if (r.getSpec().startsWith("file:"))
                    b.append(r.getSpec());
                else
                    b.append("file://" + r.getSpec());
            } else
                b.append(r.getSpec());
            //b.append(r.getActiveDataFile().toURL());
        } catch (Throwable t) {
            Log.warn(r, t);
            b.append(r.getSpec());
        }
            
        b.append("\">");
        b.append(spec);
        b.append("</a>");
        
        return b.toString();
    }


    private static String cleanForTextPaneAndTrim(String desc) {
        
        // text pane doesn't handle (e.g. <br/> is very common)
        desc = desc.replaceAll("/>", ">");
                
        // remote some initial space + breaks
        desc = desc.replaceAll("^\\s*<br>\\s*", "");
        desc = desc.replaceAll("^\\s*<br>\\s*", "");
        desc = desc.replaceAll("^\\s*<br>\\s*", "");

        // remote some trailing space + breaks
        desc = desc.replaceAll("\\s*<br>\\s*$", "");
        desc = desc.replaceAll("\\s*<br>\\s*$", "");
        desc = desc.replaceAll("\\s*<br>\\s*$", "");

        return desc;
    }


    private static String buildSummaryWithDescription(final Resource r,
                                                    final MetaMap data,
                                                    String desc)
    {

        final StringBuilder buf = new StringBuilder(128 + (desc == null ? 0 : desc.length()));

        if (desc != null)
            desc = cleanForTextPaneAndTrim(desc);

        String title = findProperty(r, data, "Title");
        if (title == null && r != null)
            title = r.getTitle();

        if (title == null && desc == null)
            return null;

        if (desc == null || !desc.contains("<style")) {
            // only add a title if no style sheet present ("complex content" e.g., jackrabbit jira)

            // final String thumb = findProperty(r, data, "@Thumb", "thumbnailURL");
            // if (thumb != null) {
            //     // Note we're dealing with ANCIENT java text-pane HTML...
            //     //buf.append("<img valign=middle src=");
            //     buf.append("<img align=middle src=");
            //     buf.append(thumb);
            //     buf.append(">");
            //     //buf.append("<br>");
            // }

            if (title != null) {
                buf.append("<b><font size=+1>"); // note: h1/h2 are useless here.  font+1 a bit more than we want tho...
                buf.append(title);
                buf.append("</font></b>");
            }
            
            final String published = findProperty(r, data, "Published", "pubDate", "dc:date", "Date", "Created", "dateUpdated");
                
            if (published != null) {
                buf.append("<br>\n");
                //buf.append("<font size=-1 color=808080>");
                buf.append("<font color=B0B0B0><b>");
                buf.append(published);
                String author = findProperty(r, data, "Author", "dc:creator", "Creator", "Publisher", "Reporter", "Name");
                if (author != null) {
                    buf.append(" - ");
                    buf.append(author);
                }
                buf.append("</b></font>");
                //buf.append("Published " + r.getProperty("Published"));
            }

            //             if (r.hasProperty("Published")) {
            //                 buf.append("<p>");
            //                 //buf.append("<font size=-1 color=808080>");
            //                 buf.append("<font color=B0B0B0><b>");
            //                 buf.append(r.getProperty("Published"));
            //                 //buf.append("Published " + r.getProperty("Published"));
            //             }

                
            if (desc != null) {

                // first, handle white-space around breaks so as not to overbreak later handling newlines
                desc = desc.replaceAll("<br>\\s*<br>\\s*", "<p>");
                
                // now handle common paragraph breaks
                desc = desc.replaceAll("\n\n", "<p>");
                
                if (!desc.startsWith("<p>"))
                    buf.append("<p>\n");
            }
                
        }


//         final boolean willDoNetworkIO;

//         if (DEBUG.Enabled)
//             willDoNetworkIO = desc.indexOf("<img") >= 0;
//         else
//             willDoNetworkIO = false;
                
        boolean hasDesc = false;

        if (desc != null && desc.length() > 0 && !desc.equals(title)) {
            // some OSID's create with spec same as title (!?) e.g., NCBI
            // (URL property is set, but the spec is not)

            if (DEBUG.IO) {
                // some images don't seem to appear:
                desc = desc.replaceAll("<img", "<i>[IO:IMAGE]</i><img");
                // actually, those appear to be embedded invisible refs
                // so content providers know when their content is displayed
            } else {
                // display the image tag and let it display for debugging:
                //desc = desc.replaceAll("<img ", "");
            }

            // not all HTML entities are handled by JTextPane, for example: &mdash; &euro;
            //if (DEBUG.DR) buf.append('[');
            buf.append(StringEscapeUtils.unescapeHtml(desc));
            //if (DEBUG.DR) buf.append(']');
            hasDesc = true;
        }

//         final String media = findProperty(r, data, "media:content@url"); 
//         if (media != null && Resource.looksLikeURLorFile(media) && Resource.looksLikeImageFile(media)) {
//             buf.append(String.format("<center><a href=\"%s\"><img src=\"%s\"></a><br>", media, media));
//         }
        
        String link = findProperty(r, data, "Link");
        if (link != null && Resource.looksLikeURLorFile(link)) {
            if (hasDesc)
                buf.append("<p>");
            else if (!buf.toString().endsWith("<br>"))
                buf.append("<br>");
            //if (DEBUG.DR && hasDesc) buf.append("hadDescription:");
            buf.append(String.format("<a href=\"%s\">%s</a><br>",
                                     link,
                                     Util.decodeURL(link)));
        }
        
        // todo: the lookup of "Cover Image" is temporary hack: add regex lookup for *image* and/or
        // the auto-detection of file/URL encoded fields in data sets.
        final String image = findProperty(r, data, "Cover Image"); 
        if (image != null && Resource.looksLikeURLorFile(image) && Resource.looksLikeImageFile(image)) {
            buf.append(String.format("<center><a href=\"%s\"><img src=\"%s\"></a>", image, image));
        }


        //if (DEBUG.DATA && r != null) r.setProperty("~reformatted", reformatted);
        
        return buf.toString();
                
//         if (! willDoNetworkIO) {
                    
//             htmlText.setText(reformatted);
                    
//         } else {

//             // TODO: this can hang if the network is down while the new
//             // StyledDocument is being created.  Trying to set the new document
//             // in another thread is of no help, in that AWT still then just
//             // hangs when it attempts to paint the component.
                    
//             //mDescription.setText("Loading...");
//             loader =
//                 new Thread("loadHTML " + r) {
//                     @Override
//                     public void run() {
//                         Log.debug("SET-TEXT...");
//                         try {
//                             Document doc = mDescription.getDocument();
//                             doc.remove(0, doc.getLength());
//                             Reader r = new StringReader(reformatted);
//                             EditorKit kit = mDescription.getEditorKit();
//                             kit.read(r, doc, 0); // no help: just hangs here
//                             GUI.setDocumentFont(mDescription, GUI.ContentFace);
//                         } catch (Throwable t) {
//                             Log.error(t);
//                         }
//                         Log.debug("SET-TEXT COMPLETED.");
                                
//                         //mDescription.setText(reformatted);
//                     }
//                 };
//         }

        //mDescription.setToolTipText(reformatted);
    }
    
    
    public void removeAll()
    {
    	super.removeAll();
    	Pane.AllPanes.clear();
    	Pane.AllPanes = null;
    	Pane.AllPanes = new ArrayList<>();
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

        setTitleItem(VueResources.getString("infowindow.nothingselected"));
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

    private void setPanesVisible(int type, boolean visible) {

        //boolean anyVisible = false;
        for (Pane p : Pane.AllPanes) {
            if ((p.bits & type) != 0) {
                Widget.setHidden(p.widget, !visible);
//                 if (visible)
//                     anyVisible = true;
            }
        }

//         if (anyVisible)
//             Widget.setHidden(mSelectionInfo, true);
    }
    
    private void displayPanes(int type) {

        boolean anyVisible = false;
        for (Pane p : Pane.AllPanes) {
            if ((p.bits & type) != 0) {
                Widget.setHidden(p.widget, false);
                anyVisible = true;
            } else
                Widget.setHidden(p.widget, true);
                
        }

        if (anyVisible)
            Widget.setHidden(mSelectionInfo, true);
    }
    
    
    private void showNodePanes(boolean visible) {
        // todo: should be using setPanesVisible here
        Widget.setHidden(mLabelPane, !visible);        
        Widget.setHidden(mNotes, !visible);
        Widget.setHidden(mDataSetData, !visible);

        Widget.setHidden(mKeywords, !visible);
        Widget.setHidden(ontologicalMetadata, !visible);
        
    }
    private void showResourcePanes(boolean visible) {
        setPanesVisible(RESOURCE, visible);
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
            setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

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
        private LWSelection selection;
        private LWComponent dataStyle;
        
        private final VueTextPane labelValue = new VueTextPane() {
                @Override
                protected void applyText(String text) {

                    // TODO: as dataStyle nodes in the Schema/Field are not part of the map model
                    // proper, their edits will not be undoable.  Can we just manually deliver the
                    // event up thru the active map?  Oh, damn... the dataStyle is owned the
                    // Schema, which while saved with the map is actually within the DataTree at
                    // runtime and applies to all open maps, and we have no "global" portion of the
                    // undo queue to handle this...  well, we could try and just ignore the global
                    // aspect and see what happens...
                    
                    super.applyText(text);
                    
                    if (selection != null && text != null && text.trim().length() > 0) {
                        Log.debug("LabelPane: manually applying to " + selection);
                        for (LWComponent c : selection) {
                            c.setLabel(text);
                        }
                        if (dataStyle != null)
                            VUE.markUndo(selection.size() + " Data Labels");
                        else
                            VUE.markUndo(selection.size() + " Labels");
                    }
                }
            };

        LabelPane() {
            super(new BorderLayout());

            labelValue.setFont(tufts.vue.gui.GUI.LabelFace);
            labelValue.setName(getClass().getName());
            final int insetInner = 5;

            if (Util.isMacPlatform()) {
                final int so = 7; // size outer
                // be sure to fetch and include the existing border, in case it's a special mac hilighting border
                labelValue.setBorder(new CompoundBorder(new MatteBorder(so,so,so,so,SystemColor.control),
                                                        new CompoundBorder(labelValue.getBorder(),
                                                                           GUI.makeSpace(insetInner)
                                                                           )));
            } else {
                setBorder(GUI.makeSpace(5));
                labelValue.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED),
                                                        GUI.makeSpace(insetInner)));
            }
            setName("nodeLabel");
            add(labelValue);
        }

        void loadLabel(LWSelection s) {
            if (DEBUG.Enabled) Log.debug("LabelPane LOADING SELECTION w/style " + s.getStyleRecord());
            labelValue.detachProperty();
            if (s.getStyleRecord() != null) {
                labelValue.attachProperty(s.getStyleRecord(), LWKey.Label);
                setName("Multiple Data Labels");
                //selection = null;
                dataStyle = s.getStyleRecord();
            } else {
                dataStyle = null;
                setName(String.format(VueResources.getString("infowindow.multiplelabel"), s.size()));
            }


            // selection = s;
            // The clone is usually overkill, but needed in case selection is cleared before our applyText handler is called.
            // (which can happen if the user manages to click empty space on the map w/out the mouse-entered
            // handler have triggered a save-text).
            selection = s.clone();
            
            //labelValue.loadText(String.format("<changes will apply to all %d nodes>", s.size()));
            //setTypeName(this, null, "Multiple Labels");
            labelValue.setEditable(true);
        }
        
        void loadLabel(LWComponent c) {
            loadLabel(c, c);
        }
        
        private boolean first = true;
        private Border border = null;
       
        
        void loadLabel(LWComponent c, LWComponent editType) {
            selection = null;
            
            if (first)
            {
            	border = labelValue.getBorder();            	
            	first = false;
            }
            
            setTypeName(this, editType, VueResources.getString("inspectorpane.label"));
            if (c instanceof LWText)
            {	
            	labelValue.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            	labelValue.setBackground(this.getBackground());
            	labelValue.setEditable(false);
            }
            else
            {
            	labelValue.setBackground(Color.white);
            	labelValue.setBorder(border);
            	labelValue.setEditable(true);
            }
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
            labelValue.setName(getClass().getSimpleName());            
            
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

    
    public static void main(String[] args) {

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
        
//         if ((VUE.getSelection().size() > 0) && (VUE.getResourceSelection().get() == null))
//             return;
        
        if (VUE.getSelection().size() > 0 && VUE.getActiveResource() == null)
            return;

        showNodePanes(false);
        showResourcePanes(false);
        LWSelection selection = VUE.getSelection();
			
        LWComponent c = selection.first();
        
        if (c != null) {
            if (c.hasResource()) {
                loadResource(c.getResource(), null);
                showNodePanes(true);
                showResourcePanes(true);                
            }
            else
                showNodePanes(true);
            
        }
    }
    
}

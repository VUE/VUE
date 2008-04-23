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

package tufts.vue;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.NodeTool.NodeModeTool;

import java.io.IOException;

import java.util.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.*;
import javax.swing.Icon;

/**
 * Provides for the managing of a list of LWComponents as elements in a "path" through
 * the map as well as ability to render that path on the map.  Includes a current
 * "index", which isn't just the current component, because components can appear
 * in the path at multiple locations (no restrictions, tho we should probably
 * restrict components appearing right next to each other in the path as I don't
 * see how that would be useful).  Also provides for associating path-specific notes
 * for each component in that path (notes are NOT currently index specific, only
 * component specific per path). --SF
 *
 * @author  Scott Fraize
 * @version $Revision: 1.213 $ / $Date: 2008-04-23 14:35:41 $ / $Author: sfraize $
 */
public class LWPathway extends LWContainer
    implements LWComponent.Listener
{
    private static final int NO_INDEX_CHANGE = Short.MIN_VALUE;
    private static boolean ShowSlides = true;
    
    private int mCurrentIndex = -1;
    private MasterSlide mMasterSlide;
    private boolean mLocked = false;
    private boolean mRevealer = false;
    /** For PathwayTable: does this pathway currently show it's entries in the list? */
    private transient boolean mOpen = true;

    /** The pathway Entry's */
    private java.util.List<Entry> mEntries = new java.util.ArrayList(); // could re-use from LWComponent, but may be dangerous
    /** Read-only version of the entry list */
    private java.util.List<Entry> mSecureEntries = java.util.Collections.unmodifiableList(mEntries);

    /** for backward compat with old style saved pathways -- the ordered list of member node ID's */
    private transient java.util.List<String> mOldStyleMemberIDList = new java.util.ArrayList();
    /** for backward compat with old style saved pathways (pre Feb/March 2006) */
    private ArrayList<LWPathwayElementProperty> mOldStyleProperties = new ArrayList();

    /**
     * This encapsulates the data for each pathway entry.  A node can be repeated in a
     * pathway as many times as the user likes, each with it's own notes, slide,
     * etc. Also, an entry in a pathway can just be a slide, and not be associated with
     * a particular node.  A special version of this class is also used as a convenience
     * for the PathwayTableModel and VUE.ActivePathwayEntryListener, so that the rows in
     * PathwayTable can all be instances of this class (e.g., both node entries and the
     * pathways themselves).  This special version of the entry represents the pathway
     * itself.
     */
    // todo: better if this class was in it's own file, including all the auto-slide creation & synchronization code
    public static class Entry implements Transferable {

        public static final String MAP_VIEW_CHANGED = "pathway.entry.mapView";
        
        /** the pathway this entry is in */
        public final LWPathway pathway;
        /** can be null if slide is "pathway only" combination of other nodes
         * Will be the same as pathway if this is a special entry for the pathway itself (for PathwayTableModel)
         * -- should be final, but castor doesn't support that */
        public /*final*/ LWComponent node;
        /** the slide object -- may be null if isMapSlide is true, and we've never created a slide here */
        LWSlide slide;
        /** if true, we don't want to use a slide -- just display the node on the map */
        boolean isMapView = true; // default true so old pre-presentation save files don't immediately create a huge batch of slides
        /** notes for this pathway entry */
        String notes = "";

        /** runtime-only cached MapSlide */
        transient LWSlide mapSlide;

        private Entry(LWPathway pathway, LWComponent node) {
            this.pathway = pathway;
            this.node = node;
            syncNodeEntryRef();
        }
        
//         /** create a merge of multiple nodes */
//         private Entry(LWPathway pathway, Iterable<LWComponent> contents) {
//             this.pathway = pathway;
//             this.node = null;
//             String titleText = "Untitled Slide";
//             this.slide = LWSlide.CreateForPathway(pathway, titleText, null, contents, true);
//             this.slide.enableProperty(LWKey.Label);
//             this.slide.setPathwayEntry(this);
//             this.setLabel(titleText);
//             syncNodeEntryRef();
//         }

        /** for our use during castor restores */
        private Entry(LWPathway pathway, Entry partial) {
            this.pathway = pathway;
            this.node = partial.node;
            this.slide = partial.slide;
            if (slide != null)
                slide.setPathwayEntry(this);
            this.isMapView = partial.isMapView;
            this.notes = partial.notes;
            if (isOffMapSlide() && slide != null)
                slide.enableProperty(LWKey.Label);
            syncNodeEntryRef();
        }

        /** for castor's use during restores */
        public Entry() {
            pathway = null;
        }

        /** @return this index in the pathway, starting at 0 */
        public int index() {
            return pathway.getEntryIndex(this);
        }

        /** @return next entry on this pathway, or null if at end */
        public Entry next() {
            return pathway.getEntry(index() + 1);
        }
        
        /** @return prev entry on this pathway, or null if at beginning */
        public Entry prev() {
            return pathway.getEntry(index() - 1);
        }

        /** @return true if this is the last entry on the pathway */
        public boolean isLast() {
            return pathway.getLast() == this;
        }

        /** @return true if this is the first entry on the pathway */
        public boolean isFirst() {
            return pathway.getFirst() == this;
        }
        
        
        private final boolean restoreUnderway() {
            return pathway == null;
        }
        
        private void syncNodeEntryRef() {
            if (node != null && node instanceof LWPathway == false)
                node.addEntryRef(this);
        }

        /** Make sure the pathway is listening to the given LWComponent, and that our node knows it knows it's in this pathway / has this entry */
        private void ensureModel() {
            if (node == null)
                return;
            if (DEBUG.UNDO && DEBUG.META) pathway.out("ensureModel " + this);
            node.addEntryRef(this);
            if (slide != null)
                slide.setPathwayEntry(this);
            node.addLWCListener(pathway, LWKey.Deleting, LWKey.Label, LWKey.Hidden);
        }
    
        /** Stop the pathway from listening to the given LWComponent, and tell it it's no longer in this pathway / has this entry */
        private void removeFromModel() {
            if (node == null)
                return;
            if (DEBUG.UNDO && DEBUG.META) pathway.out("removeFromModel " + this);
            node.removeEntryRef(this);
            node.removeLWCListener(pathway);

            if (node instanceof LWPortal && node.getEntries().size() == 0) {
                // if this portal has no other entries, remove it
                node.getParent().deleteChildPermanently(node);
            }
            
            // TODO: do we still need to listen to each of our members?
            // if slides stay as children of LWPathway, could handle
            // via broadcastChildEvent
        }
        
        public String getLabel() {
            if (node != null)
                return node.getDisplayLabel();
            else
                return getSlide().getLabel();
        }

        public void setLabel(String s) {
            if (node != null)
                node.setLabel(s);
            else
                getSlide().setLabel(s);
        }

        public LWComponent getFocal() {
            return isMapView() ? node : (canProvideSlide() ? getSlide() : node); // For VUE-967
            //return canProvideSlide() ? getSlide() : node;
        }

        
        public Color getFullScreenFillColor(DrawContext dc) {
            if (isMapView())
                return pathway.getMasterSlide().getFillColor();
            else
                return getSlide().getRenderFillColor(dc);
        }

        public boolean hasSlide() {
            return canProvideSlide();
            //return !isMapView();
        }

        public LWSlide getSlide() {
            return getSlide(false);
        }
        
        /** force a slide no matter what: e.g., for printing */
        public LWSlide produceSlide() {
            return getSlide(true);
        }
        
        private LWSlide getSlide(boolean force) {
            if (isMapView()) {
                if (force || node.supportsSlide()) {
                    //if (slide == null || slide instanceof MapSlide == false)
                    if (mapSlide == null)
                        mapSlide = new MapSlide(this);
                    return mapSlide;
                } else
                    return null;
            }
            
            if (slide == null || slide instanceof MapSlide)
                buildSlide();
            if (node != null && slide.parent != node) {
                if (node instanceof LWContainer)
                    slide.setParent((LWContainer)node);
                else
                    Util.printStackTrace("Non-containers can't have slides: " + node + " can't own " + slide);
            }
            return slide;
        }
        
        private void buildSlide() {
            // TODO: check/test undo -- is it working / the mark happening at the right time?
            final LWSlide oldSlide = slide;
            slide = Slides.CreateForPathwayEntry(this);
            pathway.notify("slide.rebuild", new Undoable() { void undo() {
                slide = oldSlide;
            }});
        }

        public void rebuildSlide() {
            buildSlide();
        }
        
        /** @return what should be selected for this entry: will be node for map-view, slide otherwise, which may
         * currently be null if hasn't been created yet */
        // This could return the node instead of the slide if the slide-icon's aren't
        // currently visible, tho it would be better to force showing the hidden
        // slide icon even if there off...
        public LWComponent getSelectable() {
            return (canProvideSlide() && getSlide().isVisible()) ? getSlide() : node;
            //return canProvideSlide() ? getSlide() : node;
            //return isMapView() ? node : slide;
        }

        public void revertSlideToMasterStyle() {
            if (slide != null)
                slide.revertToMasterStyle();
        }
        
        /** for castor: don't build a slide if we haven't got one */
        public LWSlide getPersistSlide() {
            return isMapView() ? null : slide;
        }
        /** for castor: don't build a slide if we haven't got one */
        public void setPersistSlide(LWSlide s) {
            if (isMapView) {
                if (DEBUG.Enabled)
                    Log.info("skipping restoring slide for an entry marked as map-view: " + this);
                //Util.printStackTrace("skipping restoring slide for an entry marked as map-view: " + this);
                slide = null;
            } else
                slide = s;
        }

        public void setMapView(boolean asMapView) {

            if (isMapView == asMapView)
                return;
            
            final boolean wasMapView = isMapView;
            isMapView = asMapView;

            if (restoreUnderway())
                return;

            // So the PathwayTableModel knows to update / PathwayTable knows to redraw
            pathway.notify(this, MAP_VIEW_CHANGED);

            // Anyone listening to the old focal (e.g., a MapViewer), can watch for the
            // MAP_VIEW_CHANGED event on that old focal (e.g., the user slide, virtual
            // slide, or node itself), to know it's entry type has changed, so they can
            // change views if they want to change when the entry changes.

            if (wasMapView) {
                if (mapSlide != null) {
                    mapSlide.notify(this, MAP_VIEW_CHANGED);
                    if (VUE.getActiveComponent() == mapSlide) {
                        // the MapView slide is no longer the slide for this
                        // entry: select the new slide
                        VUE.setActive(LWComponent.class, this, getSlide());
                    }
                }
                else if (node != null)
                    node.notify(this, MAP_VIEW_CHANGED);
            } else {
                // was regular slide view:
                if (slide != null) slide.notify(this, MAP_VIEW_CHANGED);
                if (VUE.getActiveComponent() == slide) {
                    // the regular slide is no longer the slide for this
                    // entry: select the MapView slide
                    VUE.setActive(LWComponent.class, this, getSlide());
                }
            }
            
// During restores, until node is set, we always think we're a merged slide, and isMapView never gets restored!
// This is just a redundancy check anyway for runtime testing.
//             if (asMapView && isOffMapSlide()) {
//                 tufts.Util.printStackTrace("merged slide can't have map view");
//             } else {
//                 isMapView = asMapView;
//             }
        }

        public final boolean isMapView() {
            // todo: node.isMapViewOnly or node.supportsSlide
            //if (node instanceof LWPortal || node instanceof LWSlide)
            if (node.supportsSlide())
                return isMapView;
            else
                return true;
        }

        public boolean canProvideSlide() {
            if (isMapView())
                return node.supportsSlide();
            else
                return true;
        }

        public final boolean hasVisibleSlide() {

            return pathway.isShowingSlides() && canProvideSlide();
                
//             if (!pathway.isShowingSlides())
//                 return false;

//             if (isMapView())
//                 return node.supportsSlide();
//             else
//                 return true;
            
            //return !isMapView() && pathway.isShowingSlides();
            //return !isMapView && pathway.isShowingSlides();
            
        }

        public final boolean isPortal() {
            return (node instanceof LWPortal)
                || (node != null && node.isTranslucent());
        }

        /** @return false for now: merged slides are not super-special at the moment -- they always have a node behind on the map */
        public final boolean isOffMapSlide() {
            return false;
            //return node == null;
        }

        /** @return true if this entry can support more than one presentation display mode
         * (e.g., a map/"raw node" view and a slide view)
         */
        public boolean allowsMultipleDisplayModes() {
            return node.supportsSlide() && !isOffMapSlide();
            //return !isOffMapSlide() && !(node instanceof LWPortal);
        }

        @Deprecated
        public final boolean hasVariableDisplayMode() {
            return allowsMultipleDisplayModes();
        }
        

        /** @return true if there is a map node associated with this entry, and it should only
         * be visible when the pathway is visible.
         */
        public final boolean hidesWithPathway() {
            return node instanceof LWPortal;
        }

        public boolean hasNotes() {
            return notes != null && notes.length() > 0;
        }

        public String getNotes() {
            return notes;
        }

        public String XMLgetNotes() {
            return LWComponent.escapeWhitespace(this.notes);
        }
        public void XMLsetNotes(String notes) {
            setNotes(LWComponent.decodeCastorMultiLineText(notes));
        }
        
        public void setNotes(String s) {
            if (notes == s || notes.equals(s))
                return;
            final String oldNotes = notes;
            notes = s;
            if (pathway != null) // will be null during restore
                pathway.notify("pathway.entry.notes", new Undoable() { void undo() { setNotes(oldNotes); }});

            // TODO: this isn't updating on undo / remote call
        }

        public boolean isPathway() { return false; }

        public boolean isVisibleOnMap() {
            return pathway.isVisible() && node.isDrawn();
        }

        public String toString() {
            String s = "Entry["
                + (pathway == null ? "<null pathway>" : pathway.getLabel())
                + "#" + (pathway == null ? -999 : index())
                + "; " + node + " ";

            if (isMapView())
                s += "MAP-VIEW";
            else
                s += (slide == null ? "<null-LWSlide>" : slide.getLabel());

            return s + ']';
        }

		public Object getTransferData(java.awt.datatransfer.DataFlavor arg0) throws UnsupportedFlavorException, IOException {
			// TODO Auto-generated method stub
			return this;
		}

		public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
			// TODO Auto-generated method stub
			DataFlavor[] list = new DataFlavor[1];
			try
			{
				list[0] = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType);
			}
			catch(ClassNotFoundException cnf)
			{
				cnf.printStackTrace();
			}
			return list;
		}

		public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor arg0) {
			if (arg0.isFlavorRemoteObjectType())
				return true;
			else
				return false;
		}
    }

    private static final Color AlphaWhite = new Color(255,255,255,128);
    public final Icon mSlideIcon = new Icon() {
            public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
                //g.setColor(getMasterSlide().getFillColor());
                final Color color = Util.alphaMix(AlphaWhite, getMasterSlide().getFillColor());
                g.setColor(color);
                g.fillRect(x, y, getIconWidth(), getIconHeight());
                //g.setColor(Color.gray);
                g.setColor(color.darker());
                g.drawRect(x, y, getIconWidth(), getIconHeight());
//                 g.translate(x + 2, y + 3);
//                 g.drawLine(0, 0,  8, 0);
//                 g.drawLine(0, 2, 10, 2);
//                 g.drawLine(0, 4,  6, 4);
            }
            public int getIconWidth() { return 14; }
            public int getIconHeight() { return 10; }
        };
    

    /**
     * This special pathway entry represents the pathway itself.
     * This is a very handy hack that allows the PathwayTable / PathwayTableModel only deal with Entry objects.
     */
    private final Entry mOurEntry =
        new Entry(LWPathway.this, LWPathway.this) {
            @Override
            public boolean canProvideSlide() { return false; }
            @Override
            public LWComponent getFocal() { return getMasterSlide(); }
            @Override
            public LWComponent getSelectable() { return getMasterSlide(); }
            @Override
            public boolean isPathway() { return true; }
            @Override
            public String getNotes() { return pathway.getNotes(); }
            @Override
            public void setNotes(String s) { pathway.setNotes(s); }
            @Override
            public boolean hasNotes() { return pathway.hasNotes(); }
            @Override
            public LWSlide getSlide() { return null; }
            @Override
            public boolean allowsMultipleDisplayModes() { return false; }
            @Override
            public int index() { return -1; }
            @Override
            public boolean isVisibleOnMap() { return false; }
        };
    
    public Entry asEntry() {
        return mOurEntry;
    }

    LWPathway(String label) {
        this(null, label);
    }

    /** Creates a new instance of LWPathway with the specified label */
    public LWPathway(LWMap map, String label) {
        initPathway();
        setMap(map);
        setLabel(label);
        setStrokeColor(getNextColor());
       
    }

    private static final int PathwayAlpha = (int) (255f * (VueResources.getInt("pathway.alpha.percent", 50) / 100f) + 0.5);

    private void initPathway() {
        disablePropertyTypes(KeyType.STYLE);
        mStrokeColor.setFixedAlpha(PathwayAlpha);
    }
    
    /** @return null -- will prevent participating in auto-styling system */
    @Override
    public Object getTypeToken() {
        return null;
    }

    /** @return false: pathways can't be selected with anything else */
    public boolean supportsMultiSelection() {
        return false;
    }
    

    /** set the global show-slides state */
    public static void setShowSlides(boolean showSlides) {
        LWPathway.ShowSlides = showSlides;
    }
    
    /** toggle the global show-slides state */
    public static void toggleSlideIcons() {
        setShowSlides(!LWPathway.ShowSlides);
    }
    
    /** @return true if the global state for showing slide icons is set */
    public static boolean isShowingSlideIcons() {
        return ShowSlides;
    }

    /** @return true if *this* pathway is currently showing slides (it's currently visible, and slide icons are turned on) */
    public boolean isShowingSlides() {
        return ShowSlides && isDrawn();
    }

    /** @return the first Entry that is for the given LWComponent */
    public Entry getFirstEntry(LWComponent c) {
        for (Entry e : mEntries)
            if (e.node == c)
                return e;
        return null;
    }

    public Entry getFirst() {
        return mEntries.size() > 0 ? mEntries.get(0) : null;
    }
    
    public Entry getLast() {
        return mEntries.size() > 0 ? mEntries.get(mEntries.size()-1) : null;
    }

    /** pathways persist their hidden bit -- this will only persist HideCause.DEFAULT */
    public Boolean getXMLhidden() {
        return isHidden(HideCause.DEFAULT) ? Boolean.TRUE : null;
    }
    

    /**
     * Is this a "reveal"-way?  Members start hidden and are made visible as you move
     * through the pathway.  This value managed by LWPathwayList, as only one Pathway
     * per map is allowed to be an revealer at a time.
     *
     * @deprecated -- this functionality has been removed, at least for now...
     */
    boolean isRevealer() {
        return false;
        //return mRevealer;
    }
    void setRevealer(boolean t) {
        throw new UnsupportedOperationException("re-implement reveal functionality");
//         mRevealer = t;
//         updateMemberVisibility();
    }
    
    @Override
    public boolean isDrawn() {
        return !isRevealer() && super.isDrawn() && mEntries.size() > 0;
    }

    @Override
    public void setVisible(boolean visible) {
        if (DEBUG.PATHWAY) out("setVisible " + visible);
        super.setVisible(visible);

        updateMemberVisibility(visible);


        /*
         * Not currently using the reveal-way feature:
         
        if (isRevealer()) {
            if (visible) {
                // if "showing" a reveal pathway, we actually hide all the
                // elements after the current index
                updateMemberVisibility();
            } else {
                if (DEBUG.PATHWAY) System.out.println(this + " setVisible: showing all items");
                for (Entry e : mEntries) {
                    if (e.node != null) {
                        e.node.clearHidden(HideCause.PATH_UNREVEALED);
                    }
                }
            }
        }
        */
    }

    /**
     * Make sure any nodes that should hide/show with this pathway
     * get a hidden bit set as needed.
     */
    // was: for reveal-way's: show all members up to index, hide all post current index
    private void updateMemberVisibility(boolean visible)
    {
        for (Entry e : mEntries) {
            if (e.hidesWithPathway()) {
                if (!visible) {
                    boolean hideNode = true;
                    // if this node is on any OTHER pathways, and they
                    // are visible, still keep the node visible...
                    for (LWPathway pathway : e.node.getPathways()) {
                        if (pathway.isVisible()) {
                            hideNode = false;
                            break;
                        }
                    }
                    e.node.setHidden(HideCause.HIDES_WITH_PATHWAY, hideNode);
                } else {
                    e.node.clearHidden(HideCause.HIDES_WITH_PATHWAY);
                }
            }
        }

        /*
        if (DEBUG.PATHWAY) System.out.println(this + " setVisible: hiding post-index items, showing all others");
        int index = 0;
        for (Entry e : mEntries) {
            if (e.node == null)
                continue;
            if (isRevealer()) {
                if (index > mCurrentIndex)
                    e.node.setHidden(HideCause.PATH_UNREVEALED);
                else
                    e.node.clearHidden(HideCause.PATH_UNREVEALED);
                index++;
            } else {
                e.node.clearHidden(HideCause.PATH_UNREVEALED);
            }
        }
        */
    }

    private static Color[] ColorTable = {
        new Color(153, 51, 51),
        new Color(204, 51, 204),
        new Color(51, 204, 51),
        new Color(51, 204, 204),
        new Color(255, 102, 51),
        new Color(51, 102, 204),
    };
    
    private static int sColorIndex = 0;
    private static Color getNextColor()
    {
        if (sColorIndex >= ColorTable.length)
            sColorIndex = 0;
        return ColorTable[sColorIndex++];
    }
     
    public int getCurrentIndex() {
        return mCurrentIndex;
    }
    
    public int firstIndexOf(LWComponent c) {
        int index = 0;
        for (Entry e : mEntries) {
            if (e.node == c)
                return index;
            index++;
        }
        return -1;
    }


    public boolean contains(LWComponent c) {
        return firstIndexOf(c) >= 0;
    }
    
    /** @return the number of times the given component appears in the pathway */
    public int count(LWComponent c) {
        int count = 0;
        for (Entry e : mEntries)
            if (e.node == c)
                count++;
        return count;
    }


    /** Set the current index to the first instance of LWComponent @param c in the pathway
     */
    void setCurrentEntry(Entry e) {
        setIndex(mEntries.indexOf(e));
    }

    public Entry getEntry(int index) {
        if (index >= length() || index < 0)
            return null;
        else
            return mEntries.get(index);
    }

    public int getEntryIndex(Entry e) {
        if (e.pathway != this)
            Log.warn("fetching entry index for non-member of ths pathway: " + e);
        
        return mEntries.indexOf(e);
    }
    

    public Entry getCurrentEntry() {
        return mCurrentIndex >= 0 ? getEntry(mCurrentIndex) : null;
    }

    public LWComponent getNodeEntry(int index) {
        Entry e = getEntry(index);
        return e == null ? null : e.node;
    }

    
    /** return the node at the current index -- may be null if current index is a pathway-only slide  */
    public LWComponent getCurrentNode() { 
        if (mCurrentIndex < 0)
            return null;

        return getNodeEntry(mCurrentIndex);
    }

    /*
    public Entry getEntryWithSlide(LWSlide slide) {
        for (Entry e : mEntries)
            if (e.slide == slide)
                return e;
        return null;
    }
    */
    
    
    private List<Entry> cloneEntries() {
        return (List<Entry>) ((ArrayList<Entry>)mEntries).clone();
    }

    public boolean moveEntry(int start, int end)
    {
    //	if (mCurrentIndex <=0)
    //		return false;
    	
    	final List<Entry> newEntries = cloneEntries();
    	Entry moveStart = getEntry(start);
    	newEntries.remove(moveStart);
    	newEntries.add(end,moveStart);
    	setEntries("pathway.reorder",newEntries,newEntries.size()-1);
    	return true;
    }
    public boolean moveCurrentUp()
    {
        if (mCurrentIndex <= 0)
            return false;

        final List<Entry> newEntries = cloneEntries();
        
        Entry moveUp = getEntry(mCurrentIndex);
        Entry moveDown = getEntry(mCurrentIndex - 1);
        newEntries.set(mCurrentIndex, moveDown);
        newEntries.set(mCurrentIndex - 1, moveUp);
        setEntries("pathway.reorder", newEntries, mCurrentIndex-1);
        return true;
    }
    
    public boolean moveCurrentDown()
    {
        if (mCurrentIndex == length()-1)
            return false;

        final List<Entry> newEntries = cloneEntries();
        
        Entry moveDown = getEntry(mCurrentIndex);
        Entry moveUp = getEntry(mCurrentIndex + 1);
        newEntries.set(mCurrentIndex + 1, moveDown);
        newEntries.set(mCurrentIndex, moveUp);
        setEntries("pathway.reorder", newEntries, mCurrentIndex+1);
        return true;
    }
    
    
    /**
     * Set the current index to @param i, and also set the
     * VUE selection to the component at that index.
     * @return the index as a convenience
     */
    public int setIndex(int i)
    {
        if (DEBUG.PATHWAY) out("setIndex " + i);
        if (mCurrentIndex == i)
            return i;

        mCurrentIndex = i;
//         if (isRevealer() && isVisible())
//             updateMemberVisibility();

        broadcastCurrentEntry();

        // No longer need this as it's all handled via the setActive:
        
        //notify("pathway.index"); // we need this so the PathwayTable is eventually told to redraw
        // Although this property is actually saved, it doesn't seem worthy of having
        // it be in the undo list -- it's more of a GUI config. (And FYI, I'm not sure if
        // this property is being properly restored at the moment either).
        //notify("pathway.index", new Undoable(old) { void undo(int i) { setIndex(i); }} );
        
        return mCurrentIndex;
    }


//     /** make sure we're listening to the given LWComponent, and that it knows it's in this pathway */
//     private void ensureMemberRefs(Entry e) {
//         if (e.node == null)
//             return;
//         if (DEBUG.UNDO && DEBUG.META) out("ensureMemberRefs " + e);
//         e.node.addPathwayRef(this);
//         e.node.addEntryRef(e);
//         e.node.addLWCListener(this, LWKey.Deleting, LWKey.Label, LWKey.Hidden);
//     }
    
//     /** Stop listening to the given LWComponent, and tell it it's no longer in this pathway */
//     // TODO: merge with entry.removeModelRefs
//     private void removeMemberRefs(Entry e) {
//         if (e.node == null)
//             return;
//         if (DEBUG.UNDO && DEBUG.META) out("removeMemberRefs " + e);
//         e.node.removePathwayRef(this);
//         e.node.removeEntryRef(e);
//         e.node.removeLWCListener(this);
//         // TODO: do we still need to listen to each of our members?
//         // if slides stay as children of LWPathway, could handle
//         // via broadcastChildEvent
//     }
    
    /** and an entry for the given component at the end of the pathway */
    public void add(LWComponent c) {
        add(new VueUtil.SingleIterator(c));
    }

    /** @return true if the given component can be added to a pathway */
    public static boolean isPathwayAllowed(LWComponent c) {
        if (c instanceof LWPathway ||
            c instanceof LWMap ||       // just in case
            c instanceof LWLink ||      // no longer allowed -- decided in staff 2007-08-29 -- SMF
            c.isPathwayOwned() ||       // e.g.: slides appearing as slide icons
            !c.isMoveable() ||          // just in case, don't allow any non-moveables
            (c instanceof LWImage && ((LWImage)c).isNodeIcon()) ||
            c.hasAncestorOfType(LWSlide.class))
            return false;
        else
            return true;
    }

    /**
     * Add new entries to the end of the pathway for all the components in the iterator.
     */
    public void add(Iterator<LWComponent> i)
    {
        if (DEBUG.PATHWAY||DEBUG.PARENTING) out("add " + i);

        List<Entry> newEntries = cloneEntries();
        int addCount = 0;
        
        while (i.hasNext()) {
            final LWComponent c = i.next();

            if (!isPathwayAllowed(c)) {
                if (DEBUG.PATHWAY||DEBUG.PARENTING) out("DENIED ADDING " + c);
                continue;
            }
                
            if (DEBUG.PATHWAY||DEBUG.PARENTING) out("adding " + c);

//             if (c instanceof LWPathway) {
//                 if (c == this)
//                     Util.printStackTrace(this + ": Can't add a pathway to itself");
//                 else
//                     Util.printStackTrace(this + ": Can't add a pathway to another pathway: " + c);
//                 continue;
//             }
            
            final Entry e = new Entry(this, c);

            // these either require map view, or are likely to want to start that way
            e.setMapView(c.isTranslucent() ||
                         c instanceof LWGroup ||
                         c instanceof LWPortal ||
                         c instanceof LWImage ||
                         c instanceof LWSlide ||
                         c instanceof LWLink
                         );

            newEntries.add(e);
            addCount++;
        }

         if (addCount > 0) {
             int newIndex = NO_INDEX_CHANGE;
             if (addCount == 1)
                 newIndex = newEntries.size() - 1;
             setEntries("pathway.add", newEntries, newIndex);
         }
    }

    /*
    public void addMergedSlide(LWSelection selection)
    {
        final Collection<LWComponent> mergedContents = new LinkedHashSet(); // preserve's insertion order
        
        for (LWComponent c : selection) {
            mergedContents.add(c);
            c.getAllDescendents(ChildKind.PROPER, mergedContents);
        }
        Entry e = new Entry(this, mergedContents);
        List<Entry> newEntries = cloneEntries();
        newEntries.add(e);
        setEntries("pathway.add", newEntries, newEntries.size() - 1);
    }
    */

    public LWComponent createMergedNode(Collection<LWComponent> selection)
    {
        final Collection<LWComponent> mergedContents = new LinkedHashSet(); // preserve's insertion order
        
        for (LWComponent c : selection) {
            mergedContents.add(c);
            c.getAllDescendents(ChildKind.PROPER, mergedContents, Order.TREE);
        }

        final LWNode node = NodeModeTool.createNewNode("Untitled"); // why can't we just use "NodeTool" here?

        node.addChildren(Actions.duplicatePreservingLinks(mergedContents));

        return node;
    }
    


    /** @param index is ignored if toRemove is non-null */
    // TODO: isDeleting currently not used -- do we still need it?
    private void removeEntries(final Iterator<LWComponent> toRemove, int index, boolean isDeleting)
    {
        if (DEBUG.PATHWAY||DEBUG.PARENTING) out("removeEntries " + toRemove + " index=" + index + " isDeleting="+isDeleting);

        final List<Entry> newEntries = cloneEntries();
        boolean didRemove = false;

        if (toRemove == null) {
            Entry e = newEntries.get(index);
            newEntries.remove(index);
            didRemove = true;
        } else {
            while (toRemove.hasNext()) {
                LWComponent c = toRemove.next();
                if (DEBUG.PATHWAY||DEBUG.PARENTING) out("removing " + c);
                Iterator<Entry> i = newEntries.iterator();
                while (i.hasNext()) {
                    if (i.next().node == c) {
                        i.remove();
                        didRemove = true;
                    }
                }
            }
        }
        if (didRemove)
            setEntries("pathway.remove", newEntries, NO_INDEX_CHANGE);
   }
    
    private synchronized void removeIndex(int index, boolean isDeleting)
    {
        if (DEBUG.PATHWAY||DEBUG.PARENTING) out("removeIndex " + index + " isDeleting=" + isDeleting);

        removeEntries(null, index, isDeleting);
    }
    
    /** remove only the  FIRST reference to the given compoent in the pathway */
    public void removeFirst(LWComponent c) {
        removeIndex(firstIndexOf(c), false);
    }
    

    /**
     * As a LWComponent may appear more than once in a pathway, we
     * need to make sure we can remove pathway entries by index, and
     * not just by content.
     */
    public synchronized void remove(int index) {
        removeIndex(index, false);
    }


    /**
     * Pathways aren't true
     * parents, so all we want to do is remove the reference to them
     * and raise a change event.  Removes all items in iterator
     * COMPLETELY from the pathway -- all instances are removed.
     * The iterator may contains elements that are not in this pathway:
     * we just make sure any that are in this pathway are removed.
     */
    public void remove(final Iterator<LWComponent> toRemove) {
        removeEntries(toRemove, -1, false);
    }
    
    
    /**
     * Using this single setEntries call with a variable key allows
     * this to fully support both undo and redo for both add/remove while maintaining
     * list order and current index position throughout.
     */
    private void setEntries(final String keyName, final List<Entry> newEntries, int newIndex)
    {
        final List<Entry> oldEntries = mEntries;
        mEntries = newEntries;
        mSecureEntries = java.util.Collections.unmodifiableList(mEntries);

        // Make sure we're only listening to the right folks, and that they know they're
        // in this pathway. Nodes in the pathway more than once are fine, as the
        // addLWCListener and addPathwayRef calls in ensureMemberRefs only have effect if
        // they haven't already been added.
        
        for (Entry e : oldEntries)
            if (!newEntries.contains(e))
                e.removeFromModel();

        for (Entry e : newEntries)
            e.ensureModel();

        final int oldIndex = mCurrentIndex;

        if (newIndex >= -1)
            setIndex(newIndex);
            //mCurrentIndex = newIndex;
        else if (mCurrentIndex >= newEntries.size())
            setIndex(newEntries.size() - 1);
            //mCurrentIndex = newEntries.size() - 1;

        // no matter what, make sure to broadcast the entry at
        // the current index, as it's possible for the index
        // to stay the same, but the entry change, as on a
        // delete or an undo of a delete.
        broadcastCurrentEntry();
        
        notify(keyName, new Undoable() { void undo() {
            setEntries(keyName, oldEntries, oldIndex);
        }});
    }

    private void broadcastCurrentEntry() {
        if (VUE.getActivePathway() == this) {
            if (mCurrentIndex < 0) {
                VUE.setActive(LWPathway.Entry.class, this, this.asEntry());
            } else {
                VUE.setActive(LWPathway.Entry.class, this, getEntry(mCurrentIndex));
            }
        }
        
    }
    
    
    public synchronized void LWCChanged(LWCEvent e)
    {
        if (e.key == LWKey.Deleting) {
            removeAllOnDelete(e.getComponent());
        } else {
            // rebroadcast our child events so that the LWPathwayList which is
            // listening to us can pass them on to the PathwayTableModel
            mChangeSupport.dispatchEvent(e);
        }
    }

    /**
     * Remove all instances of @param deleted from this pathway
     * Used when a component has been deleted.
     */
    private void removeAllOnDelete(LWComponent deleted)
    {
        removeEntries(new VueUtil.SingleIterator(deleted), -1, true);
    }

    public LWMap getMap(){
        return (LWMap) getParent();
    }
    
    public void setMap(LWMap map) {
        setParent(map);
        ensureID(this);
    }

    public void setLocked(boolean t) {
        final boolean oldValue = mLocked;
        mLocked = t;
        notify("pathway.lock", new Undoable() { void undo() { setLocked(oldValue); }} );
    }
    public boolean isLocked(){
        return mLocked;
    }
    
    public void setOpen(boolean open){
        mOpen = open;
        notify("pathway.open");
        // Although this property is actually saved, it doesn't seem worthy of having
        // it be in the undo list -- it's more of a GUI config.
    }
    
    public boolean isOpen() {
        return mOpen;
    }

    public int length() {
        return mEntries.size();
    }
    
    public void setFirst()
    {
        if (length() > 0)
            setIndex(0);
    }
    
    /** @return true if selected is last item, or none selected */
    public boolean atFirst(){
        return mCurrentIndex <= 0;
    }
    /** @return true if selected is first item, or none selected */
    public boolean atLast(){
        return mCurrentIndex == -1 || mCurrentIndex == (length() - 1);
    }
    
    public void setLast() {
        if (length() > 0)
            setIndex(length() - 1);
    }
    
    public void setPrevious(){
        if (mCurrentIndex > 0)
            setIndex(mCurrentIndex - 1);
    }
    
    public void setNext(){
        if (mCurrentIndex < (length() - 1))
            setIndex(mCurrentIndex + 1);
    }

    /**
     * Make sure we've completely cleaned up the pathway when it's
     * been deleted (must get rid of LWComponent references to this
     * pathway)
     */
    protected void removeFromModel()
    {
        super.removeFromModel();
        for (Entry e : mEntries)
            e.removeFromModel();
    }

    protected void restoreToModel()
    {
        super.restoreToModel();
        for (Entry e : mEntries)
            e.ensureModel();
    }
    
    public MasterSlide getMasterSlide() {
        if (mMasterSlide == null && !mXMLRestoreUnderway)
            mMasterSlide = buildMasterSlide();
        return mMasterSlide;
    }

    /** for persistance only */
    public void setMasterSlide(MasterSlide slide) {
        mMasterSlide = slide;
    }

    protected MasterSlide buildMasterSlide() {
        if (DEBUG.PATHWAY) out("BUILDING MASTER SLIDE:\n");
        return new MasterSlide(this);
    }

    // if we want to use a LWSlide subclass for the master slide, we'll need
    // a lw_mapping entry that subclasses LWSlide so it will know what
    // fields to save, instead of just dumping all of them.

    // TODO: add master slide subclass to lw_mapping which needn't add anything over
    // it's superclass, but it will let us save/restore instances of this that
    // can do stuff like always return 0,0 x/y values.

    private static void getMasterStyle() {

        System.out.println("MASTER SLIDE CSS: " + VueResources.getURL("masterSlide.css"));
        edu.tufts.vue.style.StyleReader.readStyles("masterSlide.css");

        java.util.Set<String> sortedKeys = new java.util.TreeSet<String>(edu.tufts.vue.style.StyleMap.keySet());

        for (String key : sortedKeys) {
            final Object style = edu.tufts.vue.style.StyleMap.getStyle(key);
            System.out.println("Found CSS style key; " + key + ": " + style);
            //System.out.println("Style key: " + se.getKey() + ": " + se.getValue());
        }

    }


    /**
     * A special type of slide used for generating "virtual" slides for viewing
     * a single on-map node in a slide-like presentation.  These are not
     * editable.
     */
    static final class MapSlide extends LWSlide {

        MapSlide(Entry e) {
            setPathwayEntry(e);
            //Util.printStackTrace("new MapSlide " + this + " for entry " + e);
            disableProperty(LWKey.FillColor); // we don't persist map slides, so don't allow this to change: won't be permanent
            disableProperty(LWKey.Label);
            if (e.node instanceof LWContainer)
                setParent((LWContainer)e.node);
            else
                Util.printStackTrace(this + "; Can't yet parent this slide to non-container node: " + e.node);
        }

        @Override
        protected void drawChildren(DrawContext dc)
        {

            // must turn off clip-optimization: as we're drawing the source-node
            // somewhere arbitrary, we must draw everything no matter what -- it or it's
            // children can't check their bounds against the clip region
            
            dc.setClipOptimized(false);
            
            final LWComponent node = getSourceNode();
            node.drawFit(dc, getZeroBounds(), node.getFocalMargin());
            
        }

        /** @return true -- MapSlides always have content (even tho no children) */
        public boolean hasContent() {
            return true;
        }

        /** @return null */
        @Override
        public Object getTypeToken() {
            return null;
        }

        /** @return false */
        public boolean supportsChildren() {
            return false;
        }

        /** @return false: map slides have nothing to sync */
        @Override
        public boolean canSync() {
            return false;
        }

        @Override
        public String getLabel() {
            return "View of " + getEntry().node.getDisplayLabel() + " in " + getEntry().pathway.getDisplayLabel();
        }
        
        @Override
        public String getComponentTypeLabel() {
            return "NodeView";
        }

        @Override
        public boolean hasPicks() { return false; }
        @Override
        public boolean hasChildren() { return false; }
        @Override
        public int numChildren() { return 0; }
        @Override
        public List<LWComponent> getPickList(PickContext pc, List<LWComponent> stored) { return stored; }
        @Override
        public java.util.List<LWComponent> getChildList() { return java.util.Collections.EMPTY_LIST; }
        @Override
        public Collection<LWComponent> getChildren() { return java.util.Collections.EMPTY_LIST; }
        @Override
        protected LWComponent pickChild(PickContext pc, LWComponent c) { return this; }

        @Override
        public LWSlide duplicate(CopyContext cc)
        {
            Util.printStackTrace("MapSlide: illegal duplicate " + this);
            return null;
        }

    }
    

    
    // we don't support standard children: we shouldn't be calling any of these
    @Override
    public void addChildren(Iterable i) {
        Util.printStackTrace("Unsupported: LWPathway.addChildren in " + this);
    }
    
    @Override
    protected void addChildImpl(LWComponent c) { throw new UnsupportedOperationException(); }
    
    @Override
    public void removeChildren(Iterable i) {
        Util.printStackTrace("Unsupported: LWPathway.removeChildren in " + this);
    }
    
    @Override
    protected void setScale(double scale) {}

    /**
     * for persistance: override of LWContainer: pathways never save their children
     * as they don't own them -- they only save ID references to them.  Pathways
     * are only "virtual" containers, not proper parents of their children.
     */
    @Override
    public java.util.List<LWComponent> getChildList() {
        if (DEBUG.XML || DEBUG.PATHWAY) out("getChildList returning EMPTY, as always");
        return java.util.Collections.EMPTY_LIST;
    }

    /** @return Collections.EMPTY_LIST -- the children of pathways are non-proper, and can't be accessed this way */
    @Override
    public Collection<LWComponent> getChildren() {
        if (DEBUG.XML || DEBUG.PATHWAY) out("getChildren returning EMPTY, as always");
        return Collections.EMPTY_LIST;
    }
    

    /** hide children from hierarchy as per getChildList */
    @Override
    public Iterator<LWComponent> getChildIterator() {
        return VueUtil.EmptyIterator;
    }
    
    /** return a read-only list of our Entries */
    public java.util.List<Entry> getEntries() {
        return mSecureEntries;
    }
    /** for castor only -- it needs to modify (build up) the list during restore */
    public java.util.List<Entry> getPersistEntries() {
        return mEntries;
    }

    /** changes in semantics from LWComponent: count entries IN this pathway, not pathways we're a member of */
    @Override
    public boolean hasEntries() {
        return mEntries.size() > 0;
    }
    
    @Override
    public LWComponent getChild(int index) {
        throw new UnsupportedOperationException("pathways don't have proper children");
    }

    @Override
    protected final void addEntryRef(LWPathway.Entry e) {
        Util.printStackTrace(this + " illegal addEntryRef " + e);
    }
    @Override
    protected final void removeEntryRef(LWPathway.Entry e) {
        Util.printStackTrace(this + " illegal removeEntryRef " + e);
    }



//     /* for castor only -- apparently castor's claim to implement this type of access to collections is bogus
//     public Iterator iterateEntries() {
//         return mEntries;
//     }
//     public void castorAddEntry(Entry e) {
//         out("CASTOR ADD ENTRY: " + e);
//     }


    @Override
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection bag, Order order)
    {
        if (kind == ChildKind.ANY) {
            if (mMasterSlide != null) {
                if (order == Order.TREE) {
                    bag.add(mMasterSlide);
                    mMasterSlide.getAllDescendents(kind, bag, order);
                } else {
                    // Order.DEPTH
                    mMasterSlide.getAllDescendents(kind, bag, order);
                    bag.add(mMasterSlide);
                }
            }
            for (Entry e : mEntries) {
                if (!e.isMapView() && e.slide != null) {
                    if (order == Order.TREE) {
                        bag.add(e.slide);
                        e.slide.getAllDescendents(kind, bag, order);
                    } else {
                        e.slide.getAllDescendents(kind, bag, order);
                        bag.add(e.slide);
                        // Order.DEPTH
                    }
                }
            }
        }
        return bag;
    }
    
    
    void completeXMLRestore(LWMap map)
    {
        if (DEBUG.INIT || DEBUG.IO || DEBUG.XML) Log.debug(this + " completeXMLRestore, map=" + map);
        setParent(map);

        if (mMasterSlide != null) {
            mMasterSlide.setParent(this);
            mMasterSlide.completeXMLRestore();
        }

        // Replace the incomplete entries (castor can only use the default constructor) with fully restored entries
        List<Entry> newEntries = new java.util.ArrayList(mEntries.size());
        for (Entry e : mEntries)
            newEntries.add(new Entry(this, e));
        mEntries.clear(); // clear for GC, and setEntries needs no old value at init

        // This is for backward compat with older save files where
        // Pathway elements were stored only by ID
        if (mOldStyleMemberIDList.size() > 0) {
            final Collection<LWComponent> allRestored = map.getAllDescendents(ChildKind.ANY);
            for (String id : mOldStyleMemberIDList) {
                LWComponent c = map.findByID(allRestored, id);
                if (DEBUG.XML || DEBUG.PATHWAY) out("RESTORING old-style path element " + c);
                newEntries.add(new Entry(this, c));
            }
        }

        // setEntries will ensure all of our model pointersd are correctly maintained
        setEntries("pathway.restore", newEntries, 0);

//         // [2007-11-05 -- hasn't been true for a while: slides are parented to their nodes as "slide-icons"]
//         // The parent of a slide tied to an Entry is the LWPathway itself
//         for (Entry e : mEntries) {
//             if (e.slide != null) {
//                 e.slide.setParent(this);
//                 //e.slide.setSourceNode(e.node);
//             }
//         }

        // Now restore old-style notes
        for (LWPathwayElementProperty pep : mOldStyleProperties) {
            for (Entry e : findEntriesWithNodeID(pep.getElementID())) {
                if (DEBUG.XML || DEBUG.PATHWAY) out("RESTORING old style notes for " + e);
                e.setNotes(pep.getElementNotes());
            }
        }

        updateMemberVisibility(isVisible());
        
        if (DEBUG.XML || DEBUG.PATHWAY) out("RESTORED");
        mXMLRestoreUnderway = false;
    }

    
    /** in support of restoring old-style pathway XML */
    private List<Entry> findEntriesWithNodeID(String ID)    
    {
        List<Entry> matches = new java.util.ArrayList();

        for (Entry e : mEntries)
            if (e.node != null && e.node.getID().equals(ID))
                matches.add(e);

        return matches;
    }

    /** for persistance: XML save/restore only for old-style (non Entry) pathway code */
    public java.util.List<String> getElementIDList() {
        if (DEBUG.XML || DEBUG.PATHWAY) out("getElementIDList: " + mOldStyleMemberIDList);
        if (mXMLRestoreUnderway)
            return mOldStyleMemberIDList;
        else
            return null;  // we no longer save this way: only read in
    }

    /** for persistance: XML save/restore only for old-style (non Entry) pathway code */
    public java.util.List getElementPropertyList()
    {
        if (mXMLRestoreUnderway)
            return mOldStyleProperties;
        else
            return null; // we no longer save this way: only read in
    }
    
    

    private static final boolean PathwayAsDots = true;
    private static final int ConnectorStrokeWidth = VueResources.getInt("pathway.stroke.width", 5);
    public static final int PathBorderStrokeWidth = 9;
    // forcing this up no matter what ensure our paint clipping will always work -- can optimize later
    //public static final int BorderStrokeWidth = PathwayAsDots ? 0 : 8; // technically, this is half the stroke, but it's the visible stroke
    
    private static final float[] DashPattern = { 8, 6 };
    //private static final float[] SelectedDash = { 4, 4 };
    //private static final float[] MultiSelectedDash = { 8, 8 };

    private static final float DotSize = VueResources.getInt("pathway.dotSize", 20);
    private static final float DotRadius = DotSize / 2;

    //private static final BasicStroke ConnectorStroke = new BasicStroke();
    private static final BasicStroke ConnectorStroke
        = new BasicStroke(ConnectorStrokeWidth,
                          BasicStroke.CAP_ROUND,
                          //PathwayAsDots ? BasicStroke.CAP_ROUND : BasicStroke.CAP_BUTT,
                          BasicStroke.JOIN_BEVEL, // ignored: always straight (no bends)
                          0f, // ignored (mitre-limit)
                          DashPattern,
                          0f // dash-phase
                          );

    private static final BasicStroke ConnectorStrokePlain
        = new BasicStroke(ConnectorStrokeWidth,
                          BasicStroke.CAP_ROUND,
                          BasicStroke.JOIN_BEVEL);

    
    private static final BasicStroke ConnectorStrokeActive
        = new BasicStroke(10,
                          BasicStroke.CAP_ROUND,
                          BasicStroke.JOIN_BEVEL, // ignored: always straight (no bends)
                          0f, // ignored (mitre-limit)
                          new float[] {8, 12},
                          //null,
                          0f // dash-phase
                          );
    
    
    private static final BasicStroke PathBorderStroke
        = new BasicStroke(PathBorderStrokeWidth,
                          BasicStroke.CAP_ROUND,
                          BasicStroke.JOIN_ROUND);

    private static final BasicStroke PathBorderStrokeDashed
        = new BasicStroke(PathBorderStrokeWidth,
                          BasicStroke.CAP_BUTT,
                          BasicStroke.JOIN_ROUND,
                          10f, // mitre-limit
                          new float[] { 8, 8 },
                          0f
                          );
    
    private static final BasicStroke PathBorderStrokeDashed2
        = new BasicStroke(PathBorderStrokeWidth,
                          BasicStroke.CAP_BUTT,
                          BasicStroke.JOIN_ROUND,
                          10f, // mitre-limit
                          new float[] { 8, 8 },
                          8f
                          );

    
    /** @return the color of the pathway (same as stroke-color) */
    public Color getColor() {
        return mStrokeColor.get();
    }

    public static void decorateOver(final LWComponent node, final DrawContext dc)
    {
        if (PathwayAsDots || node instanceof LWLink)
            drawPathwayDot(node, dc);

    }

    public static void decorateUnder(final LWComponent node, final DrawContext dc)
    {
        if (PathwayAsDots)
            return;

        if (node instanceof LWLink || node instanceof LWPortal || node instanceof LWGroup)
            return; // do nothing: these decorate via fill and/or dot, not border

        drawPathwayBorder(node, dc);
        

// Only draw ONE pathway border...
//         if (true||node.isTransparent()) {
//             for (LWPathway pathway : node.getPathways()) {
//                 //if (!dc.isFocused && path.isDrawn()) {
//                 if (pathway.isDrawn())
//                     pathway.drawPathwayBorder(node, dc.create());
//             }
//         }
    }

    
    /** for drawing just before the component draw's itself -- this is a draw-under,
     * and we're already at the zero transform for the component
     */
    private static void drawPathwayBorder(LWComponent node, DrawContext dc)
    {
        // we're already transformed into the local GC -- just draw the raw shape

        final Color c = node.getPriorityPathwayColor(dc);

        if (c == null) {
//             int count = 0;
//             for (LWPathway p : node.getPathways()) {
//                 if (p.isDrawn()) {
//                     if (count > 0)
//                         dc.g.setStroke(PathBorderStrokeDashed2);
//                     else
//                         dc.g.setStroke(PathBorderStrokeDashed);
//                     dc.g.setColor(p.getColor());
//                     dc.g.draw(node.getZeroShape());
//                     count++;
//                 }
//             }
            return;
        }

        //final Color c = node.getPriorityPathwayColor(dc);
        
        dc.g.setColor(c);
        //dc.g.setColor(getColor());
        
        //final int strokeWidth = PathBorderStrokeWidth + node.getStrokeWidth();

        //dc.g.setStroke(PathBorderStroke);
        dc.g.setStroke(new BasicStroke(PathBorderStrokeWidth + node.getStrokeWidth(),
                                       BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_ROUND));
        
        dc.g.draw(node.getZeroShape());
    }

    private static void drawPathwayDot(LWComponent node, DrawContext dc)
    {
        
        // VUE-892 fix shows dots still drawing even when node is filtered
        if(node.isFiltered())
            return;
        
        LWPathway onlyPathway = null;
        int visiblePathMemberships = 0;

        final LWPathway activePathway = VUE.getActivePathway(); // todo: from draw context

        for (LWPathway p : node.getPathways()) {
            if (p.isDrawn()) {
                onlyPathway = p;
                visiblePathMemberships++;
//                 if (p == activePathway) {
//                     // override for active-pathway:
//                     visiblePathMemberships = 1;
//                     break;
//                 } else {
//                     visiblePathMemberships++;
//                 }
                
            }
        }

        float x, y;

        if (node instanceof LWLink) {
            x = node.getZeroCenterX();
            y = node.getZeroCenterY();
        } else {
            final Point2D corner = node.getZeroNorthWestCorner();
            x = (float) corner.getX();
            y = (float) corner.getY();

// Enable this to move the dot internal to the node if it's inside another node: prevent dot from obscuring neighbors.
// (Will need to update the conncetor code also: merge this into one routine and base the conncetor code on the zero result)
//             if (node.getParent().getTypeToken() == LWNode.class) {
//                 // a node inside an auto-layout node is messy: put the dot on top of it
//                 // checking the type token ensures it's a real LWNode, not a subclass
//                 x += DotRadius * node.getScaleF();
//                 y += DotRadius * node.getScaleF();
//             }

        }

        //dc.g.setComposite(PathTranslucence);

        boolean filledEntirely = false;

//         // DRAW AN OVERLAY
//         if (onlyPathway != null &&
//             (node instanceof LWGroup || node instanceof LWImage)) {

//             final Color fill = node.getPriorityPathwayColor(dc);

//             if (fill != null) {
                
//                 final Rectangle2D.Float bounds = node.getZeroBounds();
            
//                 //if (!c.isTransparent())
//                 grow(bounds, PathBorderStrokeWidth);
                
//                 //dc.g.setColor(onlyPathway.getStrokeColor());
//                 //dc.g.setColor(onlyPathway.getColor());  // TODO: use same logic is LWPortal to get fill color...
//                 //dc.g.setColor(node.getPriorityPathwayColor(dc));
//                 dc.g.setColor(fill);
//                 dc.g.fill(bounds);
//             }
//         }

        if (visiblePathMemberships > 1) {
            final Arc2D.Float arc = new Arc2D.Float(x - DotRadius,
                                                    y - DotRadius,
                                                    DotSize,
                                                    DotSize,
                                                    0, 0, Arc2D.PIE);
                
            final float pieSlice = 360 / visiblePathMemberships;
                
            int i = 0;
            dc.g.setStroke(STROKE_HALF);
            for (LWPathway p : node.getPathways()) {
                if (!p.isDrawn())
                    continue;
                dc.g.setColor(p.getColor());
                arc.setAngleStart(90 + i * pieSlice);
                arc.setAngleExtent(pieSlice);
                dc.g.fill(arc);
                //dc.g.setColor(Color.gray);
                //dc.g.draw(arc);
                i++;
            }

        } else if (onlyPathway != null && !filledEntirely) {
            dc.g.setColor(onlyPathway.getStrokeColor());
            final RectangularShape dot = new java.awt.geom.Ellipse2D.Float(0,0, DotSize,DotSize);
            dot.setFrameFromCenter(x, y, x+DotRadius, y+DotRadius);
            dc.g.fill(dot);
        }
    }

    private static final boolean HEAD_END = true;
    private static final boolean TAIL_END = false;
    private static void setConnectionPoint(Line2D.Float line, boolean end, LWComponent c) {
        final float x, y;

        if (c instanceof LWLink) {
            x = c.getMapCenterX();
            y = c.getMapCenterY();
        } else {
            final Point2D corner = c.getZeroNorthWestCorner();
            x = (float) (c.getMapXPrecise() + corner.getX() * c.getScale());
            y = (float) (c.getMapYPrecise() + corner.getY() * c.getScale());
        }
        
        if (end == HEAD_END) {
            line.x1 = x;
            line.y1 = y;
        } else {
            line.x2 = x;
            line.y2 = y;
        }
                   
    }

    /** used for re-drawing an entire pathway with it's dots -- e.g., for hilighting it */
    public void drawPathwayWithDots(DrawContext dc) {
        drawPathway(dc.push()); dc.pop();
        for (Entry e : getEntries()) {
            if (e.node != null) {
                DrawContext ndc = dc.push();
                e.node.transformZero(ndc.g);
                decorateOver(e.node, ndc);
                dc.pop();
            }
        }
    }
    
    public void drawPathway(DrawContext dc)
    {
        final Line2D.Float connector = new Line2D.Float();

        if (VUE.getActivePathway() == this) {
            dc.g.setStroke(ConnectorStroke);
            dc.g.setColor(getColor());
        } else {
            dc.g.setStroke(ConnectorStrokePlain);
            dc.g.setColor(getColor());
        }
            

//         if (dc.isPresenting()) {
//             if (VUE.getActivePathway() == this) {
//                 dc.g.setStroke(ConnectorStrokeActive);
//                 //dc.g.setColor(Util.alphaMix(getColor(), Color.gray));
//                 dc.g.setColor(getColor());
//             } else {
//                 dc.g.setStroke(ConnectorStrokePlain);
//                 dc.g.setColor(getColor());
//             }
//         } else {
//             dc.g.setStroke(ConnectorStroke);
//             dc.g.setColor(getColor());
//         }

        
        LWComponent last = null;
        for (Entry e : mEntries) {
            if (e.node == null)
                continue;
            final LWComponent next = e.node;
            if (last != null && last.isDrawn() && next.isDrawn()) {
                
                //if (PathwayAsDots || c instanceof LWLink || last instanceof LWLink) {
                if (PathwayAsDots) {
                    setConnectionPoint(connector, HEAD_END, last);
                    setConnectionPoint(connector, TAIL_END, next);
                    // todo: we need to scale the clip for the dot scale, tho this will under-clip
                    // the non-scaled end...
                    VueUtil.clipEnds(connector, DotRadius * Math.min(last.getMapScale(), next.getMapScale()));
                } else {
                    VueUtil.computeConnector(last, next, connector);
                }




//                 if (dc.isPresenting() && dc.getAlpha() != 1f && VUE.getActivePathway() == this) {
//                     DrawContext _dc = dc.create();
//                     Log.debug("REVERTING ALPHA");
//                     _dc.setAlpha(1);
//                     _dc.g.draw(connector);
//                     _dc.dispose();
//                 } else {
//                     dc.g.draw(connector);
//                 }
                
                dc.g.draw(connector);
                    
                if (DEBUG.BOXES) {
                    Ellipse2D dot = new Ellipse2D.Float(0,0, 10,10);
                    Point2D.Float corner = (Point2D.Float) connector.getP1();
                    corner.x+=5; corner.y+=5;
                    dot.setFrameFromCenter(connector.getP1(), corner);
                    dc.g.setColor(Color.green);
                    dc.g.fill(dot);
                    corner = (Point2D.Float) connector.getP2();
                    corner.x+=5; corner.y+=5;
                    dot.setFrameFromCenter(connector.getP2(), corner);
                    dc.g.setColor(Color.red);
                    dc.g.fill(dot);
                    dc.g.setColor(getStrokeColor());
                }
            }
            last = next;
        }
    }
    
    
        /*
        if (DEBUG.PATHWAY) {
        if (dc.getIndex() % 2 == 0)
            dash_phase = 0;
        else
            dash_phase = 0.5f;
        }
        if (DEBUG.PATHWAY&&DEBUG.BOXES) System.out.println("Drawing " + this + " index=" + dc.getIndex() + " phase=" + dash_phase);
        */
        

    /*
          public void drawComponentDecorations(DrawContext dc, LWComponent c)
    {
        //boolean selected = (getCurrentNode() == c && VUE.getActivePathway() == this);
        final boolean selected = false; // turn off the "marching ants" -- don't show the current item on the pathway

        if (selected)
            dc.g.setComposite(PathSelectedTranslucence);
        else
            dc.g.setComposite(PathTranslucence);
        
        dc.g.setColor(getStrokeColor());
        
        if (PathwayAsDots || c instanceof LWLink) {
            int visiblePathMemberships = 0;
            for (LWPathway p : c.getPathways())
                if (p.isVisible())
                    visiblePathMemberships++;

            final float x = c.getZeroCenterX();
            final float y = c.getZeroCenterY();
            
            if (visiblePathMemberships > 1) {
                final Arc2D.Float arc = new Arc2D.Float(x - DotRadius,
                                                        y - DotRadius,
                                                        DotSize,
                                                        DotSize,
                                                        0, 0, Arc2D.PIE);
                
                final float pieSlice = 360 / visiblePathMemberships;
                
                int i = 0;
                for (LWPathway p : c.getPathways()) {
                    if (!p.isVisible())
                        continue;
                    dc.g.setColor(p.getStrokeColor());
                    arc.setAngleStart(90 + i * pieSlice);
                    arc.setAngleExtent(pieSlice);
                    dc.g.fill(arc);
                    i++;
                }

            } else {
                final RectangularShape dot = new java.awt.geom.Ellipse2D.Float(0,0, DotSize,DotSize);
                dot.setFrameFromCenter(x, y, x+DotRadius, y+DotRadius);
                dc.g.fill(dot);
            }
            if (!c.isTransparent())
                return;
        }
        
        int strokeWidth = BorderStrokeWidth;

        //dc = new DrawContext(dc);

        // because we're drawing under the object, only half of the
        // amount we add to to the stroke width is visible outside the
        // edge of the object, except for links, which are
        // one-dimensional, so we use a narrower stroke width for
        // them.
        //if (c instanceof LWLink)
        //    strokeWidth /= 2;
        
        strokeWidth += c.getStrokeWidth();

        if (selected) {
            //if (DEBUG.PATHWAY && dc.getIndex() % 2 != 0) dash_phase = c.getStrokeWidth();

            int visiblePathMemberships = 0;
            for (LWPathway p : c.getPathways())
                if (p.isVisible())
                    visiblePathMemberships++;
        
            //boolean offsetDash = 

            dc.g.setStroke(new BasicStroke(strokeWidth
                                           , BasicStroke.CAP_BUTT
                                           , BasicStroke.JOIN_BEVEL
                                           , 0f
                                           , visiblePathMemberships > 1 ? MultiSelectedDash : SelectedDash
                                           , 0
                                           //, offsetDash ? 8 : 0
                                           ));
        } else {
            dc.g.setStroke(new BasicStroke(strokeWidth));
        }
        // we're already transformed into the local GC -- just draw the raw shape
        dc.g.draw(c.getZeroShape());
        dc.g.setComposite(AlphaComposite.Src);//TODO: restore old composite
    }
    */
    
    
    public String toString()
    {
        return "LWPathway[" + getID()
            + " " + label
            + " n="
            //+ (children==null?-1:children.size())
            + mEntries.size()
            + " i="+mCurrentIndex
            + " " + (getMap()==null?"null":getMap().getLabel())
            + "]";
    }

    
    /** constructor used for un-marshalling only */
    public LWPathway() {
        initPathway();
    }


}



/*
    protected MasterSlide buildMasterSlide() {

        out("BUILDING MASTER SLIDE:\n");
        //final LWSlide m = LWSlide.create();

        //m.setStrokeWidth(0);
        //m.setFillColor(Color.darkGray);
        //m.setLabel("Master Slide on Pathway: " + getLabel());
        //m.setNotes("This is the Master Slide for Pathway \"" + getLabel() + "\"");
        //m.setFillColor(getStrokeColor());

        //LWComponent titleText = NodeTool.createTextNode("Title Text");
        //LWComponent itemText = NodeTool.createTextNode("Item Text");
        //LWComponent footer = NodeTool.createTextNode(getLabel());

        //m.setParent(LWPathway.this); // must set parent before ensureID will work
        //ensureID(m);
        
        //m.addChild(footer);

        // Move the footer to lower right
        //LWSelection s = new LWSelection(footer);
        //Actions.AlignRightEdges.act(s);
        //Actions.AlignBottomEdges.act(s);
        
        //m.addChild(titleText);
        //m.addChild(itemText);

        //return m;
    }
*/


    /*
     * A bit of a hack, but works well: any LWComponent can serveLW
     * as a style holder, and with special notification code, will
     * updating those who point back to us when it changes.
     * Also, making a LWNode allows it to also be an editable
     * node for full node styling, or a "text" node for text styling.
     */

    // Will not persist yet...
    // TODO: consider: do NOT have a separate style object: any LWComponent can
    // be tagged as being a "masterStyle", and if so, it will invoke
    // the below broadcasting code...
    /*
    public static class NodeStyle extends LWNode {
        NodeStyle() {}
        NodeStyle(String label) {
            super(label);
        }

        // Note that would could also do this in MasterSlide.broadcastChildEvent,
        // by checking if e.source is an LWStyle object.  We might want
        // to do this if we end up with a bunch of different LWStyle
        // classes (e.g. TextStyle, LinkStyle, etc)
        protected synchronized void notifyLWCListeners(LWCEvent e) {
            super.notifyLWCListeners(e);

            if ((e.key instanceof Key) == false || getParent() == null) {
                // This only works with real Key's, and if parent is null,
                // we're still initializing.
                return;
            }

            final Key key = (Key) e.key;

            if (key.isStyleProperty) {

                // Now we know a styled property is changing.  Since they Key itself
                // knows how to get/set/copy values, we can now just find all the
                // components "listening" to this style (pointing to it), and copy over
                // the value that just changed on the style object.
                
                out("STYLE OBJECT UPDATING STYLED CHILDREN with " + key);
                //final LWPathway path = ((MasterSlide)getParent()).mOwner;
                
                // We can traverse all objects in the system, looking for folks who
                // point to us.  But once slides are owned by the pathway, we'll have a
                // list of all slides here from the pathway, and we can just traverse
                // those and check for updates amongst the children, as we happen
                // to know that this style object only applies to slides
                // (as opposed to ontology style objects)

                // todo: this not a fast way to traverse & find what we need to change...
                for (LWComponent c : getMap().getAllDescendents(ChildKind.ANY)) {
                    if (c.mParentStyle == this && c != this) { // we should never be point back to ourself, but just in case
                        // Only copy over the style value if was previously set to our existing style value
                        try {
                            if (key.valueEquals(c, e.getOldValue()))
                                key.copyValue(this, c);
                        } catch (Throwable t) {
                            tufts.Util.printStackTrace(t, "Failed to copy value from " + e + " old=" + e.oldValue);
                        }
                    }
                }
            }
        }
    }
    */

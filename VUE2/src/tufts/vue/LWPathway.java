/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import tufts.vue.DEBUG;

import java.io.IOException;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

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
 * @version $Revision: 1.125 $ / $Date: 2007-03-07 18:01:09 $ / $Author: mike $
 */
public class LWPathway extends LWContainer
    implements LWComponent.Listener
{
    private static final int NO_INDEX_CHANGE = Short.MIN_VALUE;
    
    private int mCurrentIndex = -1;
    private MasterSlide mMasterSlide;
    private boolean mLocked = false;
    private boolean mRevealer = false;
    /** For PathwayTable: does this pathway currently show it's entries in the list? */
    private transient boolean mOpen = true;

    /** The pathway Entry's */
    private java.util.List<Entry> mEntries = new java.util.ArrayList();
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
    public static class Entry implements Transferable{
        /** the pathway this entry is in */
        public final LWPathway pathway;
        /** can be null if slide is "pathway only" combination of other nodes
         * Will be the same as pathway if this is a special entry for the pathway itself (for PathwayTableModel)
         * -- should be final, but castor doesn't support that */
        public /*final*/ LWComponent node;
        /** the slide object -- may be null if isMapSlide is true, and we've never created a slide here */
        LWSlide slide;
        /** if true, we don't want to use a slide -- just display the node on the map */
        boolean isMapView;
        /** notes for this pathway entry */
        String notes = "";

        private Entry(LWPathway pathway, LWComponent node) {
            this.pathway = pathway;
            this.node = node;
        }

        /** for our use during castor restores */
        private Entry(LWPathway pathway, Entry partial) {
            this.pathway = pathway;
            this.node = partial.node;
            this.slide = partial.slide;
            this.isMapView = partial.isMapView;
            this.notes = partial.notes;
        }
        

        /** for castor's use during restores */
        public Entry() { pathway = null; }

        public String getLabel() {
            if (node != null)
                return node.getDisplayLabel();
            else
                return "PathwaySlide:" + slide.getDisplayLabel();
        }

        public void setLabel(String s) {
            if (node != null)
                node.setLabel(s);
            else
                getSlide().setLabel(s);
        }

        public LWComponent getFocal() {
            return isMapView ? node : getSlide();
        }

        public LWSlide getSlide() {
            if (slide == null)
                slide = LWSlide.CreateForPathway(pathway, node);
            return slide;
        }
        
        /** for castor: don't build a slide if we haven't got one */
        public LWSlide getPersistSlide() {
            return slide;
        }
        /** for castor: don't build a slide if we haven't got one */
        public void setPersistSlide(LWSlide s) {
            slide = s;
        }
        
        public void setMapView(boolean asMapView) {
            isMapView = asMapView;
        }

        public boolean isMapView() {
            return isMapView;
        }

        public boolean hasNotes() {
            return notes != null && notes.length() > 0;
        }

        public String getNotes() {
            return notes;
        }
        public void setNotes(String s) {
            if (notes == s || notes.equals(s))
                return;
            final String oldNotes = notes;
            notes = s;
            if (pathway != null) // will be null during restore
                pathway.notify("pathway.entry.notes", new Undoable() { void undo() { setNotes(oldNotes); }});
        }

        public int index() {
            return pathway.mEntries.indexOf(this);
        }

        public boolean isPathway() { return false; }

        public String toString() {
            return "Entry[" + node + " isMapView=" + isMapView + "]";
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

    /** This is a very handy hack that allows the PathwayTable / PathwayTableModel only deal with Entry objects */
    private final Entry mOurEntry =
        new Entry(LWPathway.this, LWPathway.this) {
            public final boolean isPathway() { return true; }
            public final String getNotes() { return pathway.getNotes(); }
            public final void setNotes(String s) { pathway.setNotes(s); }
            public final boolean hasNotes() { return pathway.hasNotes(); }
            public int index() { throw new Error("index of pathway itself is meaningless"); }
        };
    
    public Entry asEntry() {
        return mOurEntry;
    }

    LWPathway(String label) {
        this(null, label);
    }

    /** Creates a new instance of LWPathway with the specified label */
    public LWPathway(LWMap map, String label) {
        setMap(map);
        setLabel(label);
        setStrokeColor(getNextColor());
    }

    /** @return the first Entry that is for the given LWComponent */
    public Entry getFirstEntry(LWComponent c) {
        for (Entry e : mEntries)
            if (e.node == c)
                return e;
        return null;
    }

    /**
     * Is this a "reveal"-way?  Members start hidden and are made visible as you move
     * through the pathway.  This value managed by LWPathwayList, as only one Pathway
     * per map is allowed to be an revealer at a time.
     */
    boolean isRevealer() {
        return mRevealer;
    }
    void setRevealer(boolean t) {
        mRevealer = t;
        updateMemberVisibility();
    }
    
    public boolean isDrawn() {
        return !isRevealer() && super.isDrawn() && mEntries.size() > 0;
    }

    public void setVisible(boolean visible) {
        if (DEBUG.PATHWAY) System.out.println(this + " setVisible " + visible);
        super.setVisible(visible);
        if (isRevealer()) {
            if (visible) {
                // if "showing" a reveal pathway, we actually hide all the
                // elements after the current index
                updateMemberVisibility();
            } else {
                if (DEBUG.PATHWAY) System.out.println(this + " setVisible: showing all items");
                for (Entry e : mEntries) {
                    if (e.node != null)
                        e.node.setVisible(true);
                }
            }
        }
    }

    /** for reveal-way's: show all members up to index, hide all post current index */
    private void updateMemberVisibility()
    {
        if (DEBUG.PATHWAY) System.out.println(this + " setVisible: hiding post-index items, showing all others");
        int index = 0;
        for (Entry e : mEntries) {
            if (e.node == null)
                continue;
            if (isRevealer()) {
                if (index > mCurrentIndex)
                    e.node.setVisible(false);
                else
                    e.node.setVisible(true);
                index++;
            } else {
                e.node.setVisible(true);
            }
        }
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
        if (index >= length())
            return null;
        else
            return mEntries.get(index);
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
        if (isRevealer() && isVisible())
            updateMemberVisibility();

        if (VUE.getActivePathway() == this) {
            if (i < 0) {
                VUE.setActivePathwayEntry(this.asEntry());
            } else {
                VUE.setActivePathwayEntry(getEntry(i));
                // TODO: if this node is in pathway more than once,
                // this set-selection is re-triggering a pathway
                // table selection of the FIRST instance of this node,
                // preventing us from ever kbd-arrow navigating
                // down the the second instance of the node in the pathway.
                //VUE.getSelection().setTo(getNodeEntry(i));
            }
        }
        
        notify("pathway.index"); // we need this so the PathwayTable is eventually told to redraw
        
        // Although this property is actually saved, it doesn't seem worthy of having
        // it be in the undo list -- it's more of a GUI config. (And FYI, I'm not sure if
        // this property is being properly restored at the moment either).
        //notify("pathway.index", new Undoable(old) { void undo(int i) { setIndex(i); }} );
        return mCurrentIndex;
    }


    /** make sure we're listening to the given LWComponent, and that it knows it's in this pathway */
    private void ensureMemberRefs(LWComponent c) {
        if (c == null)
            return;
        if (DEBUG.UNDO && DEBUG.META) out("ensureMemberRefs " + c);
        c.addPathwayRef(this);
        c.addLWCListener(this, LWKey.Deleting, LWKey.Label, LWKey.Hidden);
    }
    
    /** Stop listening to the given LWComponent, and tell it it's no longer in this pathway */
    private void removeMemberRefs(LWComponent c) {
        if (c == null)
            return;
        if (DEBUG.UNDO && DEBUG.META) out("removeMemberRefs " + c);
        c.removePathwayRef(this);
        c.removeLWCListener(this);
    }
    
    /** and an entry for the given component at the end of the pathway */
    public void add(LWComponent c) {
        add(new VueUtil.SingleIterator(c));
    }

    /**
     * Add new entries to the end of the pathway for all the components in the iterator.
     */
    public void add(Iterator<LWComponent> i)
    {
        if (DEBUG.PATHWAY||DEBUG.PARENTING) out("add " + i);

        List newEntries = cloneEntries();
        int addCount = 0;
        
        while (i.hasNext()) {
            LWComponent c = i.next();
            if (DEBUG.PATHWAY||DEBUG.PARENTING) out("adding " + c);
            newEntries.add(new Entry(this, c));
            addCount++;
        }

        if (addCount > 0) {
            int newIndex = NO_INDEX_CHANGE;
            if (addCount == 1)
                newIndex = newEntries.size() - 1;
            setEntries("pathway.add", newEntries, newIndex);
        }
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
                removeMemberRefs(e.node);

        for (Entry e : newEntries)
            ensureMemberRefs(e.node);

        final int oldIndex = mCurrentIndex;

        if (newIndex >= -1)
            mCurrentIndex = newIndex;
        else if (mCurrentIndex >= newEntries.size())
            mCurrentIndex = newEntries.size() - 1;
        
        notify(keyName, new Undoable() { void undo() {
            setEntries(keyName, oldEntries, oldIndex);
        }});
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
            removeMemberRefs(e.node);
    }

    protected void restoreToModel()
    {
        super.restoreToModel();
        for (Entry e : mEntries)
            ensureMemberRefs(e.node);
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
    
    public static class MasterSlide extends LWSlide
    {
        final static String StyleLabel = "Sample Text";
        // need to figure out how to make these non-deletable
        LWComponent textStyle;

        /** for castor persistance */
        public MasterSlide() {}

        void completeXMLRestore() {
            // this is a hack for now: just grab the first
            // child that is a style parent, and assume
            // it's our desired text style.
            for (LWComponent c : children) {
                // check the label is a temporary hack
                if (c.isStyleParent() || StyleLabel.equals(c.getLabel())) {
                    textStyle = c;
                    break;
                }
            }
        }

        
        MasterSlide(final LWPathway owner)
        {
            setStrokeWidth(0);
            if (owner != null)
                setFillColor(owner.getStrokeColor()); // TODO: debugging for now: use the pathway stroke as slide color
            setSize(SlideWidth, SlideHeight);

            // Create the default items for the master slide:
            
            textStyle = NodeTool.initAsTextNode(new LWNode(StyleLabel));
            textStyle.setFont(new Font("SansSerif", Font.BOLD, 48));
            textStyle.setPersistIsStyleParent(Boolean.TRUE);
            
            //titleStyle.setLocation(40,30);
            //textStyle.setLocation(45,140);
            
            if (owner != null) {
                setParent(owner);
                ensureID(this);
            }
            
            LWComponent footer = NodeTool.buildTextNode(getLabel());
            footer.setFont(new Font("SansSerif", Font.ITALIC+Font.BOLD, 48));
            footer.setTextColor(new Color(255,255,255,64));

            //tufts.Util.printStackTrace("inPath=" + owner + " pathMAP=" + owner.getMap());

            
            addChild(footer);
            //addChild(titleStyle);
            addChild(textStyle);
            
            // Now that the footer is parented, move it to lower right in it's parent
            LWSelection s = new LWSelection(footer);
            Actions.AlignRightEdges.act(s);
            Actions.AlignBottomEdges.act(s);

            /*
            s.setTo(titleStyle);
            Actions.AlignCentersRow.act(s);
            Actions.AlignCentersColumn.act(s);
            */

            s.setTo(textStyle);
            Actions.AlignCentersRow.act(s);
            Actions.AlignCentersColumn.act(s);

            //titleStyle.translate(0, -100);
            //textStyle.translate(0, +50);
        }

        // we could not have a special master slide object if we could handle
        // this draw-skipping in some other way (and arbitrary nodes can be style master's)
        // Tho having a special master-slide object isn't really that big a deal.
        public void drawChild(LWComponent child, DrawContext dc) {
            //if (!dc.isEditMode() && (child == titleStyle || child == textStyle))
            if (!dc.isEditMode() && child == textStyle)
            //if (!dc.isEditMode() && child.isParentStyle())
                return;
            else
                super.drawChild(child, dc);
        }

        /*
        void broadcastChildEvent(LWCEvent e) {
            super.broadcastChildEvent(e);
            // could handle style broadcasts here, tho
            // that would mean that styles could only be style master's
            // when on master slides.
        }
        */
        
        public String getLabel() {
            return "Master Slide: " + (getParent() == null ?" <unowned>" : getParent().getDisplayLabel());
        }
        
        public String getComponentTypeLabel() { return "Slide<Master>"; }

    }

    
    // we don't support standard children: we shouldn't be calling any of these
    public void addChildren(Iterator i) { throw new UnsupportedOperationException(); }
    protected void addChildImpl(LWComponent c) { throw new UnsupportedOperationException(); }
    public void removeChildren(Iterator i) { throw new UnsupportedOperationException(); }
    
    void setScale(float scale) {}

    /**
     * for persistance: override of LWContainer: pathways never save their children
     * as they don't own them -- they only save ID references to them.  Pathways
     * are only "virtual" containers, not proper parents of their children.
     */
    public java.util.List<LWComponent> getChildList() {
        if (DEBUG.XML || DEBUG.PATHWAY) out("getChildList returning EMPTY, as always");
        return java.util.Collections.EMPTY_LIST;
    }

    /** hide children from hierarchy as per getChildList */
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
    public LWComponent getChild(int index) {
        throw new UnsupportedOperationException("pathways don't have proper children");
    }

    /* for castor only -- apparently castor's claim to implement this type of access to collections is bogus
    public Iterator iterateEntries() {
        return mEntries;
    }
    public void castorAddEntry(Entry e) {
        out("CASTOR ADD ENTRY: " + e);
    }
    */
        
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection bag) {
        if (kind == ChildKind.ANY) {
            if (mMasterSlide != null)
                bag.add(mMasterSlide);
            if (mMasterSlide != null)
                mMasterSlide.getAllDescendents(kind, bag);
            for (Entry e : mEntries) {
                if (e.slide != null) {
                    bag.add(e.slide);
                    e.slide.getAllDescendents(kind, bag);
                }
            }
        }
        return bag;
    }
    
    
    void completeXMLRestore(LWMap map)
    {
        if (DEBUG.INIT || DEBUG.IO || DEBUG.XML) System.out.println(this + " completeXMLRestore, map=" + map);
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

        setEntries("pathway.restore", newEntries, 0);

        // The parent of a slide tied to an Entry is the LWPathway itself
        for (Entry e : mEntries)
            if (e.slide != null)
                e.slide.setParent(this);

        // Now restore old-style notes
        for (LWPathwayElementProperty pep : mOldStyleProperties) {
            for (Entry e : findEntriesWithNodeID(pep.getElementID())) {
                if (DEBUG.XML || DEBUG.PATHWAY) out("RESTORING old style notes for " + e);
                e.setNotes(pep.getElementNotes());
            }
        }
        
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
    
    
    private static final AlphaComposite PathTranslucence = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f);
    private static final AlphaComposite PathSelectedTranslucence = PathTranslucence;
    //private static final AlphaComposite PathSelectedTranslucence = AlphaComposite.Src;
    //private static final AlphaComposite PathSelectedTranslucence = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);

    private static float dash_length = 4;
    private static float dash_phase = 0;

    public static final int PathwayStrokeWidth = 8; // technically, this is half the stroke, but it's the visible stroke

    /** for drawing just before the component draw's itself -- this is a draw-under */
    public void drawComponentDecorations(DrawContext dc, LWComponent c)
    {
        int strokeWidth = PathwayStrokeWidth;
        boolean selected = (getCurrentNode() == c && VUE.getActivePathway() == this);

        //dc = new DrawContext(dc);

        // because we're drawing under the object, only half of the
        // amount we add to to the stroke width is visible outside the
        // edge of the object, except for links, which are
        // one-dimensional, so we use a narrower stroke width for
        // them.
        
        if (c instanceof LWLink)
            strokeWidth /= 2;
        
        if (selected)
            dc.g.setComposite(PathSelectedTranslucence);
        else
            dc.g.setComposite(PathTranslucence);
        
        dc.g.setColor(getStrokeColor());
        
        strokeWidth += c.getStrokeWidth();
        if (!selected) {
            dc.g.setStroke(new BasicStroke(strokeWidth));
        } else {
            if (DEBUG.PATHWAY && dc.getIndex() % 2 != 0) dash_phase = c.getStrokeWidth();
            dc.g.setStroke(new BasicStroke(strokeWidth
                                           , BasicStroke.CAP_BUTT
                                           , BasicStroke.JOIN_BEVEL
                                           , 0f
                                           , new float[] { dash_length, dash_length }
                                           , dash_phase));
        }
        dc.g.draw(c.getShape());
        dc.g.setComposite(AlphaComposite.Src);//TODO: restore old composite
    }
    
    public void drawPathway(DrawContext dc)
    {
        /*
        if (DEBUG.PATHWAY) {
        if (dc.getIndex() % 2 == 0)
            dash_phase = 0;
        else
            dash_phase = 0.5f;
        }
        */
        //dc = new DrawContext(dc);

        if (DEBUG.PATHWAY&&DEBUG.BOXES) System.out.println("Drawing " + this + " index=" + dc.getIndex() + " phase=" + dash_phase);
        Line2D.Float connector = new Line2D.Float();

        /*
        BasicStroke connectorStroke =
            new BasicStroke(4,
                            BasicStroke.CAP_BUTT
                            , BasicStroke.JOIN_BEVEL
                            , 0f
                            , new float[] { dash_length, dash_length }
                            , dash_phase);
        */

        BasicStroke connectorStroke = new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);

        dc.g.setColor(getStrokeColor());
        dc.g.setStroke(connectorStroke);
        
        LWComponent last = null;
        for (Entry e : mEntries) {
            if (e.node == null)
                continue;
            final LWComponent c = e.node;
            if (last != null) {
                dc.g.setComposite(PathTranslucence);
                //connector.setLine(last.getCenterPoint(), c.getCenterPoint());
                dc.g.draw(VueUtil.computeConnector(last, c, connector));
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
            last = c;
        }
    }
    
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

    
    /** @deprecated - default constructor used for marshalling ONLY */
    public LWPathway() {
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

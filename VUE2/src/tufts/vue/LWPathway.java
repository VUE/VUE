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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

/**
 * LWPathway.java
 *
 * Provides for the managing of a list of LWComponents as elements in a "path" through
 * the map as well as ability to render that path on the map.  Includes a current
 * "index", which isn't just the current component, because components can appear
 * in the path at multiple locations (no restrictions, tho we should probably
 * restrict components appearing right next to each other in the path as I don't
 * see how that would be useful).  Also provides for associating path-specific notes
 * for each component in that path (notes are NOT currently index specific, only
 * component specific per path). --SF
 *
 * @author  Jay Briedis
 * @author  Scott Fraize
 * @version  March 2004
 */

public class LWPathway extends LWContainer
    implements LWComponent.Listener
{
    private int weight = 1;
    private boolean ordered = false;
    private int mCurrentIndex = -1;
    private boolean locked = false;
    private boolean reveal = false;
    private ArrayList elementPropertyList = new ArrayList();

    private transient boolean open = true;
    private transient boolean mXMLRestoreUnderway = false;

    private static Color[] ColorTable = {
        new Color(153, 51, 51),
        new Color(204, 51, 204),
        new Color(51, 204, 51),
        new Color(51, 204, 204),
        new Color(255, 102, 51),
        new Color(51, 102, 204),
    };
    private static int sColorIndex = 0;

    LWPathway(String label) {
        this(null, label);
    }

    /** Creates a new instance of LWPathway with the specified label */
    public LWPathway(LWMap map, String label) {
        setMap(map);
        setLabel(label);
        setStrokeColor(getNextColor());
    }

    /**
     * Is this a "reveal"-way?  Members start hidden and are made visible as you move
     * through the pathway.  This value managed by LWPathwayList, as only one Pathway
     * per map is allowed to be an revealer at a time.
     */
    boolean isRevealer() {
        return this.reveal;
    }
    void setRevealer(boolean t) {
        this.reveal = t;
        updateMemberVisibility();
    }
    
    public boolean isDrawn() {
        return !isRevealer() && super.isDrawn();
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
                Iterator i = children.iterator();
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    c.setVisible(true);
                }
            }
        }
    }

    /** for reveal-way's: show all members up to index, hide all post current index */
    private void updateMemberVisibility()
    {
        if (DEBUG.PATHWAY) System.out.println(this + " setVisible: hiding post-index items, showing all others");
        int index = 0;
        Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (isRevealer()) {
                if (index > mCurrentIndex)
                    c.setVisible(false);
                else
                    c.setVisible(true);
                index++;
            } else {
                c.setVisible(true);
            }
        }
    }

    private static Color getNextColor()
    {
        if (sColorIndex >= ColorTable.length)
            sColorIndex = 0;
        return ColorTable[sColorIndex++];
        /*
        LWPathwayManager manager = VUE.getActiveMap().getPathwayManager();
        System.out.println("manager: " + manager.toString());
        if(manager != null && manager.getPathwayList() != null){
            int num = manager.getPathwayList().size();
            borderColor = (Color)colorArray.get(num % 6);
        }
        System.out.println("pathway border color: " + borderColor.toString());
        */
    }
     
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public int getCurrentIndex() {
        return mCurrentIndex;
    }
    public void setCurrentIndex(int i) {
        setIndex(i);
    }

    /** Set the current index to the first instance of LWComponent @param c in the pathway
     */
    void setCurrentElement(LWComponent c) {
        setIndex(children.indexOf(c));
    }
    
    /** return the current element */
    public LWComponent getCurrent() { 
        LWComponent c = null;
        if (mCurrentIndex < 0)
            return null;
        /*
        if (mCurrentIndex < 0 && length() > 0) {
            System.out.println(this + " lazy default of index to 0");
            mCurrentIndex = 0;
        }
        */
        try {
            c = (LWComponent) children.get(mCurrentIndex);
        } catch (IndexOutOfBoundsException ie){
            c = null;
        }      
        return c;
    }

    public boolean bringForward(LWComponent c)
    {
        boolean moved = super.bringForward(c);
        if (moved)
            setCurrentElement(c);
        return moved;
    }
    public boolean sendBackward(LWComponent c)
    {
        boolean moved = super.sendBackward(c);
        if (moved)
            setCurrentElement(c);
        return moved;
    }
    
    /**
     * Set the current index to @param i, and also set the
     * VUE selection to the component at that index.
     * @return the index as a convenience
     */
    private int setIndex(int i)
    {
        if (DEBUG.PATHWAY) System.out.println(this + " setIndex " + i);
        if (mCurrentIndex == i)
            return i;
        //Object oldValue = new Integer(mCurrentIndex);
        if (i >= 0 && VUE.getActivePathway() == this)
            VUE.getSelection().setTo(getElement(i));
        mCurrentIndex = i;
        if (isRevealer() && isVisible())
            updateMemberVisibility();
        notify("pathway.index");
        // Although this property is actually saved, it doesn't seem worthy of having
        // it be in the undo list -- it's more of a GUI config. (And FYI, I'm not sure if
        // this property is being properly restored at the moment either).
        //notify("pathway.index", new Undoable(old) { void undo(int i) { setIndex(i); }} );
        return mCurrentIndex;
    }

    /**
     * Overrides LWContainer addChildren.  Pathways aren't true
     * parents, so all we want to do is add a reference to them,
     * and raise a change event.
     */
    public void addChildren(Iterator i)
    {
        if (DEBUG.PATHWAY||DEBUG.PARENTING) System.out.println(this + " addChildren " + VUE.getSelection());

        notify(LWKey.HierarchyChanging);
        
        ArrayList added = new java.util.ArrayList();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (DEBUG.PATHWAY||DEBUG.PARENTING) System.out.println(this + " addChild " + added.size() + " " + c);
            addChildRefs(c);
            super.children.add(c);

            // For now you can only have one set of properties per element in the list,
            // even if the element is in the path more than once.  We probably
            // want to ultimately support a set of properties for each index
            // within the pathway.
            if (!hasElementProperties(c))
                elementPropertyList.add(new LWPathwayElementProperty(c));
            
            added.add(c);
        }
        if (DEBUG.PATHWAY||DEBUG.PARENTING) System.out.println(this + " ADDEDALL " + added);
        //if (mCurrentIndex == -1) setIndex(length() - 1);
        if (added.size() > 0) {
            if (added.size() == 1)
                setIndex(length()-1);
            notify("pathway.add", new Undoable(added) { void undo() { undoAddChildren((List)old); }} );
            // todo: although should not ever see more than one pathway.add or pathway.delete
            // per user action, it's a theoretically possible combination for the UndoManager to
            // see, and this is not a compressable event, it's cumulative (if we do see more
            // than one of them, we don't want to throw away all the events after first), so
            // would be better to have some way of marking the event as cumulative (e.g., starts
            // with "*") and have the UndoManager handle it.
        }
    }

    private void removeChildRefs(LWComponent c) {
        // We only do the ref removes if we don't still contain it, which is possible
        // since pathway's can contain multiple entries for the same LWComponent.
        if (DEBUG.UNDO) System.out.println(this + " removeChildRefs " + c);
        if (!contains(c)) {
            c.removePathwayRef(this);
            c.removeLWCListener(this);
        }
    }
    void addChildRefs(LWComponent c) {
        // We only do the ref add's if the component doesn't already think it's in us,
        // which can happen since pathway's can contain multiple entries for the same LWComponent.
        if (DEBUG.UNDO) System.out.println(this + " addChildRefs " + c);
        if (!c.inPathway(this)) {
            c.addPathwayRef(this);
            c.addLWCListener(this);
        }
    }
    
    // In support of undo.  As LWPathway is an LWContainer, the references
    // to the children and their order is handled by the hierarchy change event,
    // but that won't patch up the special pathway refs's that any component
    // that's a member of a pathway has.
    private void undoAddChildren(List list)
    {
        Iterator i = list.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (DEBUG.UNDO) System.out.println(this + " undoAddChildren undoing " + c);
            removeChildRefs(c);
        }
    }
    
    private void undoRemoveChildren(List list)
    {
        Iterator i = list.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (DEBUG.UNDO) System.out.println(this + " undoRemoveChildren undoing " + c);
            addChildRefs(c);
        }
    }

    public void add(LWComponent c) {
        addChild(c);
    }
    public void remove(LWComponent c) {
        removeChild(c);
    }
    public void add(Iterator i) {
        addChildren(i);
    }

    /**
     * As a LWComponent may appear more than once in a pathway, we
     * need to make sure we can remove pathway entries by index, and
     * not just by content.
     */
    public synchronized void remove(int index) {
        remove(index, false);
    }

    /**
     * Overrides LWContainer removeChildren.  Pathways aren't true
     * parents, so nobody should be calling this LWContainer method on us.
     * Always @throws UnsupportedOperationException
     */
    public void removeChildren(Iterator i) {
        throw new UnsupportedOperationException(this + ".removeChildren");
    }
    
    /**
     * Pathways aren't true
     * parents, so all we want to do is remove the reference to them
     * and raise a change event.  Removes all items in iterator
     * COMPLETELY from the pathway -- all instances are removed.
     * The iterator may contains elements that are not in this pathway:
     * we just make sure any that are in this pathway are removed.
     */
    //  Todo: factor & combine with remove(int index, bool deleting)
    public void remove(Iterator i)
    {
        if (DEBUG.PATHWAY||DEBUG.PARENTING) System.out.println(this + " removeChildren " + VUE.getSelection());

        notify(LWKey.HierarchyChanging);
        
        ArrayList removed = new java.util.ArrayList();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            boolean contained = false;
            while (children.contains(c)) {
                if (DEBUG.PATHWAY||DEBUG.PARENTING) System.out.println(this + " removeChild " + removed.size() + " " + c);
                contained |= children.remove(c);
            }
            if (contained) {
                c.removePathwayRef(this);
                c.removeLWCListener(this);       
                //disposeElementProperties(c);
                removed.add(c);
            }
        }
        if (DEBUG.PATHWAY||DEBUG.PARENTING) System.out.println(this + " REMOVEDALL " + removed);
        if (removed.size() > 0) {
            if (mCurrentIndex >= length())
                setIndex(length() - 1);
            else
                setIndex(mCurrentIndex);
            notify("pathway.remove", new Undoable(removed) { void undo() { undoRemoveChildren((List)old); }} );
        }
    }

    private LWComponent removingComponent = null;
    private synchronized void remove(int index, boolean deleting)
    {
        if (DEBUG.PATHWAY||DEBUG.PARENTING) System.out.println(this + " remove index " + index + " deleting=" + deleting);

        notify(LWKey.HierarchyChanging);
        LWComponent c = (LWComponent) children.remove(index);
        
        if (DEBUG.PATHWAY||DEBUG.PARENTING) System.out.println(this + " removed " + c);

        if (length() == 0)
            mCurrentIndex = -1;

        if (!contains(c)) { // in case in multiple times
            c.removePathwayRef(this);
            if (!deleting) {
                // If deleting, component will remove us as listener itself.
                // If we remove it here while deleting, we'll get a concurrent
                // modification exception from LWCompononent.notifyLWCListeners
                c.removeLWCListener(this);
            }
            //disposeElementProperties(c);  // only remove property if last time appears in list.
            // For now, we don't dispose at all for undo.
        }

        // If what we just deleted was the current item, the currentIndex
        // doesn't change, but we call this to make sure we set the selection.
        // Or, if we just deleted the last item in the list, mCurrentIndex
        // needs to shrink by one.
        if (mCurrentIndex >= length())
            setIndex(length() - 1);
        else
            setIndex(mCurrentIndex);

        //removingComponent = c; // todo: can we remove this now that we don't deliver events back to source?
        //
        // This is now handled via special case in UndoManager, which we need there anyway so we don't set
        // pathway children's parent to the pathway.  If fix that, could handle addChildRefs by
        // making this an uncompressable event (e.g., "*pathway.remove" or something) so that
        // it keeps each call in the undo list.
        //
        //notify("pathway.remove-index", new Undoable(c) { void undo() { addChildRefs((LWComponent)old); }} );
        //
        //removingComponent = null;
    }
    
    public synchronized void LWCChanged(LWCEvent e)
    {
        /*
        if (e.getComponent() == removingComponent) {
            //if (DEBUG.PATHWAY || DEBUG.EVENTS) System.out.println(e + " ignoring: already deleting in " + this);
            new Throwable(e + " ignoring: already deleting in " + this).printStackTrace();
            return;
        }
        */
        
        if (e.getWhat() == LWKey.Deleting) {
            removeAll(e.getComponent());
        } else {
            // rebroadcast our child events so that the LWPathwayList which is
            // listening to us can pass them on to the PathwayTableModel
            mChangeSupport.dispatchEvent(e);
        }
        /*else if (super.listeners != null) {
            // rebroadcast our child events so that the LWPathwayList which is
            // listening to us can pass them on to the PathwayTableModel
            dispatchLWCEvent(this, super.listeners, e);
            }*/
    }

    /**
     * Remove all instances of @param deleted from this pathway
     * Used when a component has been deleted.
     */
    protected void removeAll(LWComponent deleted)
    {
        while (contains(deleted))
            remove(children.indexOf(deleted), true);
    }

    public LWMap getMap(){
        return (LWMap) getParent();
    }
    public void setMap(LWMap map) {
        setParent(map);
    }

    public void setLocked(boolean t) {
        Object old = Boolean.valueOf(locked);
        this.locked = t;
        notify("pathway.lock", new Undoable(old) { void undo(boolean b) { setLocked(b); }} );
    }
    public boolean isLocked(){
        return locked;
    }
    
    public void setOpen(boolean open){
        //Object old = Boolean.valueOf(open);
        this.open = open;
        notify("pathway.open");
        // Although this property is actually saved, it doesn't seem worthy of having
        // it be in the undo list -- it's more of a GUI config.
        //notify("pathway.open", new Undoable(old) { void undo(boolean b) { setOpen(b); }} );
    }
    
    public boolean isOpen() {
        return open;
    }

    public boolean contains(LWComponent c) {
        return children.contains(c);
    }
    public boolean containsMultiple(LWComponent c) {
        return children.indexOf(c) != children.lastIndexOf(c);
    }

    public int length() {
        return children.size();
    }
    
    public LWComponent setFirst()
    {
        if (length() > 0) {
            setIndex(0);
            return getElement(0);
        }
        return null;
    }
    
    /** @return true if selected is last item, or none selected */
    public boolean atFirst(){
        return mCurrentIndex <= 0;
    }
    /** @return true if selected is first item, or none selected */
    public boolean atLast(){
        return mCurrentIndex == -1 || mCurrentIndex == (length() - 1);
    }
    
    public LWComponent setLast() {
        if (length() > 0) {
            setIndex(length() - 1);
            return getElement(length() - 1);
        }
        return null;
    }
    
    public LWComponent setPrevious(){
        if (mCurrentIndex > 0)
            return getElement(setIndex(mCurrentIndex - 1));
        else
            return null;
    }
    
    public LWComponent setNext(){
        if (mCurrentIndex < (length() - 1))
            return getElement(setIndex(mCurrentIndex + 1));
        else 
            return null;
    }

    public LWComponent getElement(int index){
        LWComponent c = null;
        try {
            c = (LWComponent) children.get(index);
        } catch (IndexOutOfBoundsException e) {
            System.err.println(this + " getElement " + index + " " + e);
        }    
        return c;
    }

    /**
     * Make sure we've completely cleaned up the pathway when it's
     * been deleted (must get rid of LWComponent references to this
     * pathway)
     */
    protected void removeFromModel()
    {
        super.removeFromModel();
        Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.removePathwayRef(this);
            c.removeLWCListener(this);
       }
    }

    protected void restoreToModel()
    {
        super.restoreToModel();
        Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.addPathwayRef(this);
            c.addLWCListener(this);
       }
    }
    
    public boolean getOrdered() {
        return ordered;
    }
    
    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }

    /**
     * for persistance: override of LWContainer: pathways never save their children
     * as they don't own them -- they only save ID references to them.
     */
    public List getChildList() {
        return null;
    }
    /** hide children from hierarchy as per getChildList */
    public Iterator getChildIterator() {
        return VueUtil.EmptyIterator;
    }
    
    /** Pathway interface */
    public java.util.Iterator getElementIterator() {
        return super.children.iterator();
    }

    public java.util.List getElementList() {
        //System.out.println(this + " getElementList type  ="+elementList.getClass().getName()+"  size="+elementList.size());
        return super.children;
    }

    private LWComponent findElementByID(String ID)    
    {
        java.util.Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.getID().equals(ID))
                return c;
        }
        System.err.println(this + " couldn't find ID [" + ID + "] in " + children);
        return null;
    }
    
    void completeXMLRestore(LWMap map)
    {
        System.out.println(this + " completeXMLRestore, map=" + map);
        setParent(map);
        for (Iterator i = this.idList.iterator(); i.hasNext();) {
            String id = (String) i.next();
            LWComponent c = getMap().findChildByID(id);
            if (DEBUG.PATHWAY) System.out.println("\tpath adding " + c);
            add(c);
        }
        for (Iterator i = this.elementPropertyList.iterator(); i.hasNext();) {
            LWPathwayElementProperty pep = (LWPathwayElementProperty) i.next();
            LWComponent c = findElementByID(pep.getElementID());
            if (c == null) // shouldn't normally happen, but just in case
                i.remove();
            else
                pep.setComponent(c);
        }
        if (DEBUG.PATHWAY) System.out.println(this + " restored. elementPropertyList= " + elementPropertyList);
        mXMLRestoreUnderway = false;
    }

    
    private List idList = new ArrayList();
    /** for persistance: XML save/restore only */
    public List getElementIDList() {
        if (mXMLRestoreUnderway) {
            return idList;
        } else {
            idList.clear();
            Iterator i = getElementIterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                idList.add(c.getID());
            }
        }
        System.out.println(this + " getElementIDList: " + idList);
        return idList;
    }


    /** for persistance: XML save/restore only */
    public java.util.List getElementPropertyList()
    {
        //if (DEBUG.PATHWAY) System.out.println(this + " getElementPropertyList0 " + elementPropertyList);
        if (!mXMLRestoreUnderway) {
            // cull any entries for components that have been deleted
            // or are no longer in the pathway.
            for (Iterator i = elementPropertyList.iterator(); i.hasNext();) {
                LWPathwayElementProperty pep = (LWPathwayElementProperty) i.next();
                if (pep.getComponent().isDeleted() || !pep.getComponent().inPathway(this))
                    i.remove();
            }
        }
        
        //if (DEBUG.PATHWAY) System.out.println(this + " getElementPropertyList1 " + elementPropertyList);
        return elementPropertyList;
    }
    
    private LWPathwayElementProperty getPep(LWComponent c) {
        if (c == null) return null;
        for (Iterator i = elementPropertyList.iterator(); i.hasNext();) {
            LWPathwayElementProperty pep = (LWPathwayElementProperty) i.next();
            if (mXMLRestoreUnderway) {
                if (pep.getElementID().equals(c.getID()))
                    return pep;
            } else {
                if (pep.getComponent() == c)
                    return pep;
            }
        }
        return null;
        //return new LWPathwayElementProperty(c);
    }
    
    public String getElementNotes(LWComponent c) {
        return getPep(c).getElementNotes();
    }
    
    public boolean hasElementProperties(LWComponent c) {
        return getPep(c) != null;
    }

    public void setElementNotes(LWComponent c, String notes)
    {
        if (c == null)
            throw new IllegalArgumentException(this + " setElementNotes: LWComponent is null");
        if (notes == null)
            notes = "";
        
        LWPathwayElementProperty pep = getPep(c);
        Object[] undoInfo = { c, pep.getElementNotes() };
        pep.setElementNotes(notes);
        notify("pathway.element.notes",
               new Undoable(undoInfo) {
                   void undo(Object[] a) {
                       setElementNotes((LWComponent) a[0], (String) a[1]);
                   }
               });
    }

    private void disposeElementProperties(LWComponent c)
    {
        LWPathwayElementProperty pep = getPep(c);
        if (pep != null) {
            elementPropertyList.remove(pep);
            if (DEBUG.PATHWAY&&DEBUG.META) System.out.println(this + " dumped properties " + pep);
        }
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
        boolean selected = (getCurrent() == c && VUE.getActivePathway() == this);

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
        dc.g.setComposite(AlphaComposite.Src);//todo: restore old composite
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
        Iterator i = this.getElementIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
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
        return "LWPathway[" + label
            + " n="
            + (children==null?-1:children.size())
            + " i="+mCurrentIndex
            + " " + (getMap()==null?"null":getMap().getLabel())
            + "]";
    }

    
    /** @deprecated - default constructor used for marshalling ONLY */
    public LWPathway() {
        mXMLRestoreUnderway = true;
    }


}

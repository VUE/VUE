package tufts.vue;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.awt.geom.Rectangle2D;

/**
 * LWContainer.java
 *
 * Manage a group of children within a parent that maintains it's size.
 *
 * @author Scott Fraize
 * @version 6/1/03
 */
public abstract class LWContainer extends LWComponent
{
    protected ArrayList children = new java.util.ArrayList();
    
    /** for use during restore */
    private int idStringToInt(String idStr)
    {
        int id = -1;
        try {
            id = Integer.parseInt(idStr);
        } catch (Exception e) {
            System.err.println(e + " invalid ID: '" + idStr + "'");
        }
        return id;
    }
    /** for use during restore */
    protected LWComponent findChildByID(String ID)
    {
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.getID().equals(ID))
                return c;
            if (c instanceof LWContainer) {
                c = ((LWContainer)c).findChildByID(ID);
                if (c != null)
                    return c;
            }
        }
        return null;
    }

    /**
     * For restore: To be called once after a persisted map
     * is restored.
     */
    protected void resolvePersistedLinks(LWContainer topLevelContainer)
    {
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWContainer) {
                ((LWContainer)c).resolvePersistedLinks(topLevelContainer);
                continue;
            }
            if (!(c instanceof LWLink))
                continue;
            LWLink l = (LWLink) c;
            try {
                l.setEndPoint1(topLevelContainer.findChildByID(l.getEndPoint1_ID()));
                l.setEndPoint2(topLevelContainer.findChildByID(l.getEndPoint2_ID()));
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("*** removing bad link " + l);
                i.remove();
            }
        }
    }
    
    /** for use during restore */
    protected int findGreatestChildID()
    {
        int maxID = -1;
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            int curID = idStringToInt(c.getID());
            if (curID > maxID)
                maxID = curID;
            if (c instanceof LWContainer) {
                curID = ((LWContainer)c).findGreatestChildID();
                if (curID > maxID)
                    maxID = curID;
            }
        }
        return maxID;
    }
    /** for use during restore */
    protected void setChildParentReferences()
    {
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setParent(this);
            if (c instanceof LWContainer)
                ((LWContainer)c).setChildParentReferences();
        }
    }

    /** for use during restore */
    protected void setChildScaleValues()
    {
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setScale(c.getScale());
        }
    }
    
    /*
     * Child handling code
     */
    public boolean hasChildren()
    {
        return this.children.size() > 0;
    }
    public ArrayList getChildList()
    {
        return this.children;
    }
    public java.util.Iterator getChildIterator()
    {
        return this.children.iterator();
    }

    public Iterator getNodeIterator()
    {
        return getNodeList().iterator();
    }
    public Iterator getLinkIterator()
    {
        return getLinkList().iterator();
    }
    public Iterator getPathwayIterator()
    {
        return getPathwayList().iterator();
    }
    
    // todo: temporary for html? 
    private List getNodeList()
    {
        ArrayList list = new ArrayList();
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWNode)
                list.add(c);
        }
        return list;
    }
    // todo: temporary for html?
    private List getLinkList()
    {
        ArrayList list = new ArrayList();
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWLink)
                list.add(c);
        }
        return list;
    }
    
    private List getPathwayList()
    {
        ArrayList list = new ArrayList();
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWPathway)
                list.add(c);
        }
        return list;
    }

    protected void addChildInternal(LWComponent c)
    {
        if (DEBUG_PARENTING) System.out.println("["+getLabel() + "] ADDING   " + c);
        if (c.getParent() != null) {
            if (c.getParent() == this) {
                if (DEBUG_PARENTING) System.out.println("["+getLabel() + "] ADD-BACK " + c + " (already our child)");
                // this okay -- in fact useful for child node re-drop on existing parent to trigger
                // re-ordering & re-layout
            }
            c.getParent().removeChild(c);
        }
        if (c.getFont() == null)//todo: really want to do this? only if not manually set?
            c.setFont(getFont());
        this.children.add(c);
        c.setParent(this);
        c.ensureLinksPaintOnTopOfAllParents();
        
        // todo: raise all in getLinkRefs to the top of the stack if we're
        // being added as a child of anything other than the top level map
    }

    protected void ensureLinksPaintOnTopOfAllParents()
    {
        super.ensureLinksPaintOnTopOfAllParents();
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.ensureLinksPaintOnTopOfAllParents();
            if (c instanceof LWContainer)
                ((LWContainer)c).ensureLinksPaintOnTopOfAllParents();
        }
    }

    public void addChild(LWComponent c)
    {
        addChildInternal(c);
        c.notify("added", this);
        notify("childAdded", c);
    }
    
    protected void removeChild(LWComponent c)
    {
        removeChild0(c);
        //c.notify("removed", this); // don't need to let anyone know this
        notify("childRemoved", c);
        layout();
    }

    private void removeChild0(LWComponent c)
    {
        if (DEBUG_PARENTING) System.out.println("["+getLabel() + "] REMOVING " + c);
        if (!this.children.remove(c))
            throw new IllegalStateException(this + " didn't contain child for removal: " + c);
        c.setParent(null);

        // gunk to handle scale stuff
        if (c.isManagedColor())
            c.setFillColor(COLOR_NODE_DEFAULT);
        c.setScale(1f);

        //if (c instanceof LWContainer)
        //  ((LWContainer)c).layout();//why doing this here instead of setScale?
        // todo: check layout diags with it at end of setScale to see if
        // too many layouts happening is a problem
    }
    
    /**
     * Remove any children in this iterator from this container.
     * Items not already children of this container are ignored.
     */
    public void removeChildren(Iterator i)
    {
        int count = 0;
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.getParent() == this) {
                removeChild0(c);
                count++;
            }
        }
        if (count > 0) {
            notify("childrenRemoved");
            //todo: change all these child events to a structureChanged event
            layout();
        }
    }

    public void deleteChild(LWComponent c)
    {
        if (DEBUG_PARENTING) System.out.println("["+getLabel() + "] DELETING " + c);
        c.notify("deleting");
        removeChild(c);
        // note that parents of c will not get this event as
        // c.parent has been nulled by now -- but listeners will get it.
        //c.notify("deleted");
        c.removeFromModel();
    }
    
    public java.util.List getAllConnectedNodes()
    {
        // todo opt: could cache this list, or organize as giant iterator tree
        // see LWComponent comment for where to go next
        // Could also make this faster with a cached bounds
        // and explicitly call a getRepaintRegion here that
        // just returns that + any nodes(links) connected to any children
        java.util.List list = super.getAllConnectedNodes();
        list.addAll(children);
        java.util.Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            list.addAll(c.getAllConnectedNodes());
        }
        return list;
    }

    /**
     * Only called AFTER we've determined that mapX, mapY already
     * lie within the bounds of this LWComponent via contains(x,y)
     */

    public LWComponent findLWComponentAt(float mapX, float mapY)
    {
        if (DEBUG_CONTAINMENT) System.out.println("LWContainer.findLWComponentAt[" + getLabel() + "]");
        // hit detection must traverse list in reverse as top-most
        // components are at end
        java.util.ListIterator i = children.listIterator(children.size());
        while (i.hasPrevious()) {
            LWComponent c = (LWComponent) i.previous();
            if (c.contains(mapX, mapY)) {
                if (c.hasChildren())
                    return ((LWContainer)c).findLWComponentAt(mapX, mapY);
                else
                    return c;
            }
        }
        return this;
    }

    /** Code is duplicated from above here, but subclasses (e.g.,
     * LWGroup) handle this differently, but we can't reuse
     * above code due to recursive usage.
     */
    public LWComponent findLWSubTargetAt(float mapX, float mapY)
    {
        if (DEBUG_CONTAINMENT) System.out.println("LWContainer.findLWSubTargetAt[" + getLabel() + "]");
        // hit detection must traverse list in reverse as top-most
        // components are at end
        java.util.ListIterator i = children.listIterator(children.size());
        while (i.hasPrevious()) {
            LWComponent c = (LWComponent) i.previous();
            if (c.contains(mapX, mapY)) {
                if (c.hasChildren())
                    return ((LWContainer)c).findLWSubTargetAt(mapX, mapY);
                else
                    return c;
            }
        }
        return this;
    }

    
    //    public LWNode findLWNodeAt(float mapX, float mapY, LWComponent excluded)
    public LWNode findLWNodeAt(float mapX, float mapY)
    {
        // hit detection must traverse list in reverse as top-most
        // components are at end
        
        if (children.size() > 0) {
            java.util.ListIterator i = children.listIterator(children.size());
            while (i.hasPrevious()) {
                LWComponent c = (LWComponent) i.previous();
                if (!(c instanceof LWNode))
                    continue;
                //if (c != excluded && c.contains(mapX, mapY)) {
                if (!c.isSelected() && c.contains(mapX, mapY)) {
                    if (c.hasChildren())
                        return ((LWContainer)c).findLWNodeAt(mapX, mapY);
                    else
                        return (LWNode) c;
                }
            }
        }
        if (this instanceof LWNode)
            return (LWNode) this;
        else
            return null;
    }

    public boolean isOnTop(LWComponent c)
    {
        // todo opt: has to on avg scan half of list every time
        // (will slow down selection in checks to enable front/back actions)
        return children.indexOf(c) == children.size()-1;
    }
    public boolean isOnBottom(LWComponent c)
    {
        // todo opt: has to on avg scan half of list every time
        // (will slow down selection in checks to enable front/back actions)
        return children.indexOf(c) == 0;
    }

    public int getLayer(LWComponent c)
    {
        return getIndex(c);
    }
    private int getIndex(Object c)
    {
        return children.indexOf(c);
    }
        
    /* To preseve the relative display order of a group of elements
     * we're moving forward or sending back, we need to move them in a
     * particular order depending on the operation and how the
     * operation functions.  Note that when moving a group
     * forward/back one move at a time, once the group moves all the
     * way to the front or the back of the list, it will start cycling
     * the order of the components if they're all right next to each
     * other.
     */

    private static Comparator ForwardOrder = new Comparator() {
            public int compare(Object o1, Object o2) {
                LWComponent c1 = (LWComponent) o1;
                LWComponent c2 = (LWComponent) o2;
                return c2.getParent().getIndex(c2) - c1.getParent().getIndex(c1);
            }};
    private static Comparator ReverseOrder = new Comparator() {
            public int compare(Object o1, Object o2) {
                LWComponent c1 = (LWComponent) o1;
                LWComponent c2 = (LWComponent) o2;
                return c1.getParent().getIndex(c1) - c2.getParent().getIndex(c2);
            }};

    private static LWComponent[] sort(List selection, Comparator comparator)
    {
        LWComponent[] array = new LWComponent[selection.size()];
        selection.toArray(array);
        java.util.Arrays.sort(array, comparator);
        // Note that it's okay that components with different
        // parents are in this list, as they only need to move
        // relative to layers of any other siblings in the list,
        // and it doesn't matter if re-layering is done to
        // a parent (LWContainer) at a time -- only that the movement
        // order of siblings within a parent is enforced.
        return array;
    }
    
    /** 
     * Make component(s) paint first & hit last (on bottom)
     */
    public static void bringToFront(List selectionList)
    {
        LWComponent[] comps = sort(selectionList, ReverseOrder);
        for (int i = 0; i < comps.length; i++)
            comps[i].getParent().bringToFront(comps[i]);
    }
    public static void bringForward(List selectionList)
    {
        LWComponent[] comps = sort(selectionList, ForwardOrder);
        for (int i = 0; i < comps.length; i++)
            comps[i].getParent().bringForward(comps[i]);
    }
    /** 
     * Make component(s) paint last & hit first (on top)
     */
    public static void sendToBack(List selectionList)
    {
        LWComponent[] comps = sort(selectionList, ForwardOrder);
        for (int i = 0; i < comps.length; i++)
            comps[i].getParent().sendToBack(comps[i]);
    }
    public static void sendBackward(List selectionList)
    {
        LWComponent[] comps = sort(selectionList, ReverseOrder);
        for (int i = 0; i < comps.length; i++)
            comps[i].getParent().sendBackward(comps[i]);
    }

    boolean bringToFront(LWComponent c)
    {
        // Move to END of list, so it will paint last (visually on top)
        int idx = children.indexOf(c);
        int idxLast = children.size() - 1;
        if (idx == idxLast)
            return false;
        //System.out.println("bringToFront " + c);
        children.remove(idx);
        children.add(c);
        return true;
    }
    boolean sendToBack(LWComponent c)
    {
        // Move to FRONT of list, so it will paint first (visually on bottom)
        int idx = children.indexOf(c);
        if (idx == 0)
            return false;
        //System.out.println("sendToBack " + c);
        children.remove(idx);
        children.add(0, c);
        return true;
    }
    boolean bringForward(LWComponent c)
    {
        // Move toward the END of list, so it will paint later (visually on top)
        int idx = children.indexOf(c);
        int idxLast = children.size() - 1;
        if (idx == idxLast)
            return false;
        //System.out.println("bringForward " + c);
        swap(idx, idx + 1);
        return true;
    }
    boolean sendBackward(LWComponent c)
    {
        // Move toward the FRONT of list, so it will paint sooner (visually on bottom)
        int idx = children.indexOf(c);
        if (idx == 0) 
            return false;
        //System.out.println("sendBackward " + c);
        swap(idx, idx - 1);
        return true;
    }

    private void swap(int i, int j)
    {
        //System.out.println("swapping positions " + i + " and " + j);
        children.set(i, children.set(j, children.get(i)));
    }

    // essentially this implements an "insert-after" of top relative to bottom
    void ensurePaintSequence(LWComponent onBottom, LWComponent onTop)
    {
        if (onBottom.getParent() != this || onTop.getParent() != this)
            throw new IllegalArgumentException(this + " both aren't children " + onBottom + " " + onTop);
        int bottomIndex = getIndex(onBottom);
        int topIndex = getIndex(onTop);
        if (bottomIndex < 0 || topIndex < 0)
            throw new IllegalArgumentException(this + " both aren't in list! " + bottomIndex + " " + topIndex);
        //if (DEBUG_PARENTING) System.out.println("ENSUREPAINTSEQUENCE: " + onBottom + " " + onTop);
        if (topIndex == (bottomIndex - 1)) {
            swap(topIndex, bottomIndex);
            if (DEBUG_PARENTING) System.out.println("ensurePaintSequence: swapped " + onTop);
        } else if (topIndex < bottomIndex) {
            children.remove(topIndex);
            // don't forget that after above remove the indexes have all been shifted down one
            if (bottomIndex >= children.size())
                children.add(onTop);
            else
                children.add(bottomIndex, onTop);
            if (DEBUG_PARENTING) System.out.println("ensurePaintSequence: inserted " + onTop);
        } else {
            //if (DEBUG_PARENTING) System.out.println("ensurePaintSequence: already sequenced");
        }
        //if (DEBUG_PARENTING) System.out.println("ensurepaintsequence: " + onBottom + " " + onTop);
        
    }

    // todo: this really wants to be in LWNode, not general to container,
    // or even better, support this feature in a much cleaner way
    // (e.g., tweak the font size or something)
    public void setScale(float scale)
    {
        super.setScale(scale);
        //System.out.println("Scale set to " + scale + " in " + this);
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            // todo: temporary hack color change for children
            if (c.isManagedColor()) {
                if (COLOR_NODE_DEFAULT.equals(getFillColor()))
                    c.setFillColor(COLOR_NODE_INVERTED);
                else
                    c.setFillColor(COLOR_NODE_DEFAULT);
            }
            c.setScale(scale * ChildScale);
        }
        layout(); // todo: if we leave scaling in, see if can do this less frequently
    }
    
    /**
     * If this container supports special layout for it's children,
     * do it here.
     */
    protected void layout() {}
    
    public void draw(java.awt.Graphics2D g)
    {
        if (this.children.size() > 0) {
            java.util.Iterator i = getChildIterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                c.draw((java.awt.Graphics2D) g.create());
            }
        }
        if (DEBUG_CONTAINMENT) {
            g.setColor(java.awt.Color.green);
            g.setStroke(STROKE_ONE);
            g.draw(getBounds());
        }
    }
    
    
}

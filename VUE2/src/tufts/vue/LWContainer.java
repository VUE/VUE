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
    static boolean DEBUG_PARENTING = true;
    
    class CList extends java.util.ArrayList {
        public boolean add(Object obj) {
            boolean tv = super.add(obj);
            //System.out.println("added " + obj);
            return tv;
        }
    }
    
    protected ArrayList children = new CList();
    
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
        if (DEBUG_PARENTING) System.out.println("["+getLabel() + "] ADDS " + c);
        if (c.getParent() != null)
            c.getParent().removeChild(c);
        this.children.add(c);
        if (c.getFont() == null)//todo: really want to do this? only if not manually set?
            c.setFont(getFont());
        c.setParent(this);
    }
        
    public void addChild(LWComponent c)
    {
        addChildInternal(c);
        c.notify("added", this);
    }
    
    protected void removeChild(LWComponent c)
    {
        if (DEBUG_PARENTING) System.out.println("["+getLabel() + "] REMOVING " + c);
        if (!this.children.remove(c))
            throw new RuntimeException(this + " DIDN'T CONTAIN CHILD FOR REMOVAL: " + c);
        c.setParent(null);
        notify("childRemoved", c);
    }

    public void deleteChild(LWComponent c)
    {
        if (DEBUG_PARENTING) System.out.println("["+getLabel() + "] DELETING " + c);
        c.notify("deleting");
        removeChild(c);
        // note that parents of c will not get this event as
        // c.parent has been nulled by now -- but listeners will get it.
        //c.notify("deleted");
        c.removeAllLWCListeners();
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

    
    // existed only for MapDropTarget -- drop event should
       // make this obsolete
    public LWNode findLWNodeAt(float mapX, float mapY, LWComponent excluded)
    {
        // hit detection must traverse list in reverse as top-most
        // components are at end
        java.util.ListIterator i = children.listIterator(children.size());
        while (i.hasPrevious()) {
            LWComponent c = (LWComponent) i.previous();
            if (!(c instanceof LWNode))
                continue;
            if (c != excluded && c.contains(mapX, mapY)) {
                if (c.hasChildren())
                    return ((LWContainer)c).findLWNodeAt(mapX, mapY, excluded);
                else
                    return (LWNode) c;
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

    public void setScale(float scale)
    {
        super.setScale(scale);
        //System.out.println("Scale set to " + scale + " in " + this);
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            // todo: temporary hack color change for children
            if (c.isManagedColor()) {
                if (getFillColor() == COLOR_NODE_DEFAULT)
                    c.setFillColor(COLOR_NODE_INVERTED);
                else
                    c.setFillColor(COLOR_NODE_DEFAULT);
            }
            c.setScale(scale * ChildScale);
        }
    }
    
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

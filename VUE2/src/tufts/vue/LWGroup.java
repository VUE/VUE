package tufts.vue;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * LWGroup.java
 *
 * @author Scott Fraize
 * @version 6/1/03
 */
public class LWGroup extends LWComponent
{
    class CList extends java.util.ArrayList {
        public boolean add(Object obj) {
            boolean tv = super.add(obj);
            //System.out.println("added " + obj);
            return tv;
        }
    }
    
    protected ArrayList children = new CList();
    
    public LWGroup()
    {
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

    public void addChild(LWComponent c)
    {
        System.out.println(getLabel() + " ADDS " + c);
        if (c.getParent() != null)
            c.getParent().removeChild(c);
        this.children.add(c);
        c.setParent(this);
        if (c.getFont() == null)
            c.setFont(getFont());
    }
    public void removeChild(LWComponent c)
    {
        System.out.println(getLabel() + " REMOVES " + c);
        if (!this.children.remove(c))
            throw new RuntimeException(this + " DIDN'T CONTAIN CHILD FOR REMOVAL: " + c);
        c.notify("removed");
        c.removeAllLWCListeners();
        c.setParent(null);
    }

    public LWComponent findLWComponentAt(float mapX, float mapY)
    {
        // hit detection must traverse list in reverse as top-most
        // components are at end
        java.util.ListIterator i = children.listIterator(children.size());
        while (i.hasPrevious()) {
            LWComponent c = (LWComponent) i.previous();
            if (c.contains(mapX, mapY)) {
                if (c.hasChildren())
                    return ((LWGroup)c).findLWComponentAt(mapX, mapY);
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
                    return ((LWGroup)c).findLWNodeAt(mapX, mapY, excluded);
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
                //if (c1.getParent() != c2.getParent()) return 0;
                return c2.getParent().getIndex(c2) - c1.getParent().getIndex(c1);
            }};
    private static Comparator ReverseOrder = new Comparator() {
            public int compare(Object o1, Object o2) {
                LWComponent c1 = (LWComponent) o1;
                LWComponent c2 = (LWComponent) o2;
                //if (c1.getParent() != c2.getParent()) return 0;
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
        // a parent (LWGroup) at a time -- only that the movement
        // order of siblings within a parent is enforced.
        /*
        for (int i = 0; i < array.length; i++) {
            LWComponent c = (LWComponent) array[i];
            System.out.println(i + " " + c.getParent().getIndex(c) + " " + c);
            }*/
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
    
    /*
    private Rectangle2D getAllChildrenBounds()
    {
        // compute bounds based on a vertical stacking layout
        java.util.Iterator i = getChildIterator();
        float height = 0;
        float maxWidth = 0;
        float width;
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            //height += c.getHeight() + VerticalChildGap;
            //width = c.getWidth();
            height += c.height + VerticalChildGap;
            width = c.width;
            if (width > maxWidth)
                maxWidth = width;
            
        }
        height *= ChildScale;
        maxWidth *= ChildScale;
        return new Rectangle2D.Float(0f, 0f, maxWidth, height);
        }*/
        
    public void draw(java.awt.Graphics2D g)
    {
        if (this.children.size() > 0) {
            java.util.Iterator i = getChildIterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                c.draw((java.awt.Graphics2D) g.create());
            }
        }
    }
    
    
}

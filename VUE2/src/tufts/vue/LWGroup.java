package tufts.vue;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Vector;

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
        this.children.remove(c);
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


    /** 
     * Make component paint last & hit first (on top)
     */
    public void bringToFront(LWComponent c)
    {
        // Move to END of list, so it will paint last (visually on top)
        int idx = children.indexOf(c);
        children.remove(idx);
        children.add(c);
    }
    /** 
     * Make component paint first & hit last (on bottom)
     */
    public void sendToBack(LWComponent c)
    {
        // Move to FRONT of list, so it will paint first (visually on bottom)
        int idx = children.indexOf(c);
        children.remove(idx);
        children.add(0, c);
    }
    public void bringForward(LWComponent c)
    {
        int idx = children.indexOf(c);
        children.remove(idx);
        children.add(idx + 1, c);
    }
    public void sendBackward(LWComponent c)
    {
        int idx = children.indexOf(c);
        children.remove(idx);
        children.add(idx - 1, c);
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

package tufts.vue;

import java.util.Iterator;

//todo: make singleton
public class LWSelection extends java.util.ArrayList
{
    void add(LWComponent c)
    {
        System.out.println("LWSelection adding " + c);
        super.add(c);
    }
    void remove(LWComponent c)
    {
        System.out.println("LWSelection removing " + c);
        super.remove(c);
    }

    public java.awt.geom.Rectangle2D getBounds()
    {
        java.awt.geom.Rectangle2D bounds = Vue2DMap.getBounds(iterator());
        //System.out.println("SELECTION BOUNDS=" + bounds);
        return bounds;
    }

    public LWComponent first()
    {
        return (LWComponent) get(0);
    }
    
    public int countTypes(Class clazz)
    {
        int count = 0;
        Iterator i = iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (clazz.isInstance(c))
                count++;
        }
        return count;
    }
    
    public boolean allOfSameType()
    {
        LWComponent oc = null;
        Iterator i = iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (oc != null && oc.getClass() != c.getClass())
                return false;
            oc = c;
        }
        return true;
    }
    
    public boolean allHaveSameParent()
    {
        LWComponent oc = null;
        Iterator i = iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (oc != null && oc.getParent() != c.getParent())
                return false;
            oc = c;
        }
        return true;
    }
}

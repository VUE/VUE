package tufts.vue;

import java.util.Iterator;

public class LWSelection extends java.util.ArrayList
{
    static final boolean DEBUG_SELECTION = true;

    java.util.ArrayList listeners = new java.util.ArrayList();

    public interface Listener extends java.util.EventListener {
        void selectionChanged(LWSelection selection);
    }

    public void addListener(Listener l)
    {
        listeners.add(l);
    }
    public void removeListener(Listener l)
    {
        listeners.remove(l);
    }

    private void notifyListeners()
    {
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            Listener l = (Listener) i.next();
            l.selectionChanged(this);
        }
    }
       
    
    void setTo(LWComponent c)
    {
        clear0();
        add(c);
    }
     
    void add(LWComponent c)
    {
        if (!c.isSelected()) {
            add0(c);
        } else
            if (DEBUG_SELECTION) System.out.println("addToSelection(already): " + c);
        notifyListeners();
    }
    
    public boolean add(Object o)
    {
        throw new RuntimeException("LWSelection can't add " + o.getClass() + ": " + o);
    }
    
    void add(Iterator i)
    {
        LWComponent c;
        while (i.hasNext()) {
            c = (LWComponent) i.next();
            add0(c);
        }
        notifyListeners();
    }
    
    private void add0(LWComponent c)
    {
        if (DEBUG_SELECTION) System.out.println("LWSelection adding " + c);
        c.setSelected(true);
        super.add(c);
    }
    
    public void remove(LWComponent c)
    {
        if (DEBUG_SELECTION) System.out.println("LWSelection removing " + c);
        c.setSelected(false);
        if (!super.remove(c))
            throw new RuntimeException("LWSelection remove: doesn't contain! " + c);
        notifyListeners();
    }
    
    public void clear()
    {
        clear0();
        notifyListeners();
    }

    private void clear0()
    {
        if (DEBUG_SELECTION) System.out.println("LWSelection clear " + this);
        java.util.Iterator i = iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setSelected(false);
        }
        super.clear();
    }

    public java.awt.geom.Rectangle2D getBounds()
    {
        if (size() == 0)
            return null;
        // todo opt: cache bounds
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

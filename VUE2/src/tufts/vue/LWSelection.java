package tufts.vue;

import java.util.Iterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

public class LWSelection extends java.util.ArrayList
{
    public static final boolean DEBUG_SELECTION = VueResources.getBool("selection.debug");

    private java.util.List listeners = new java.util.ArrayList();
    private java.util.List controlListeners = new java.util.LinkedList();
    private Rectangle2D bounds = null;
    
    public interface Listener extends java.util.EventListener {
        void selectionChanged(LWSelection selection);
    }
    public static class ControlPoint extends Point2D.Float
    {
        java.awt.Color color;
        public ControlPoint(float x, float y)
        {
            this(x, y, VueConstants.COLOR_SELECTION_CONTROL);
            //this(x, y, VueConstants.COLOR_SELECTION_HANDLE);
        }
        public ControlPoint(java.awt.geom.Point2D p)
        {
            this((float)p.getX(), (float)p.getY());
        }
        public ControlPoint(java.awt.geom.Point2D p, java.awt.Color c)
        {
            this((float)p.getX(), (float)p.getY(), c);
        }
        public ControlPoint(float x, float y, java.awt.Color c)
        {
            super(x,y);
            setColor(c);
        }
        public void setColor(java.awt.Color c)
        {
            this.color = c;
        }
        public java.awt.Color getColor()
        {
            return this.color;
        }
    }
    public interface ControlListener extends java.util.EventListener {
        void controlPointMoved(int index, MapMouseEvent e);
        void controlPointDropped(int index, MapMouseEvent e);
        //Point2D.Float[] getControlPoints();
        ControlPoint[] getControlPoints();
    }
    

    //public void addSelectionControl(java.awt.geom.Point2D mapLocation, ControlListener listener)
    private void addControlListener(ControlListener listener)
    {
        if (DEBUG_SELECTION) System.out.println("LWSelection: adding control listener " + listener);
        controlListeners.add(listener);
    }
    
    private void removeControlListener(ControlListener listener)
    {
        if (DEBUG_SELECTION) System.out.println("LWSelection: removing control listener " + listener);
        if (!controlListeners.remove(listener))
            throw new IllegalStateException("LWSelection: didn't contain control listener " + listener);
    }

    java.util.List getControlListeners()
    {
        return controlListeners;
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
    
    void setTo(Iterator i)
    {
        clear0();
        add(i);
    }
     
    void add(LWComponent c)
    {
        if (!c.isSelected()) {
            add0(c);
            notifyListeners();
        } else
            if (DEBUG_SELECTION) System.out.println("addToSelection(already): " + c);
    }
    
    public boolean add(Object o)
    {
        throw new RuntimeException("LWSelection can't add " + o.getClass() + ": " + o);
    }
    
    /** Make sure all in iterator are in selection & do a single change notify at the end */
    void add(Iterator i)
    {
        LWComponent c;
        boolean changed = false;
        while (i.hasNext()) {
            c = (LWComponent) i.next();
            if (!c.isSelected()) {
                add0(c);
                changed = true;
            }
        }
        if (changed)
            notifyListeners();
    }
    
    /** Change the selection status of all LWComponents in iterator */
    void toggle(Iterator i)
    {
        LWComponent c;
        boolean changed = false;
        while (i.hasNext()) {
            c = (LWComponent) i.next();
            if (c.isSelected())
                remove0(c);
            else
                add0(c);
            changed = true;
        }
        if (changed)
            notifyListeners();
    }
    
    private void add0(LWComponent c)
    {
        if (DEBUG_SELECTION) System.out.println("LWSelection: adding " + c);
        
        if (!c.isSelected()) {
            c.setSelected(true);
            bounds = null;
            super.add(c);
            if (c instanceof ControlListener)
                addControlListener((ControlListener)c);
        } else
            throw new RuntimeException("LWSelection: attempt to add already selected component " + c);
    }
    
    public void remove(LWComponent c)
    {
        remove0(c);
        notifyListeners();
    }

    private void remove0(LWComponent c)
    {
        if (DEBUG_SELECTION) System.out.println("LWSelection: removing " + c);
        c.setSelected(false);
        if (c instanceof ControlListener)
            removeControlListener((ControlListener)c);
        bounds = null;
        if (!super.remove(c))
            throw new RuntimeException("LWSelection remove: list doesn't contain " + c);
    }
    
    /**
     * clearAndNotify
     * This emthod clears teh selection and always notifies
     * listeners of a change.
     *
     **/
    public void clearAndNotify() {
    	clear0();
    	notifyListeners();
    }
    
    public void clear()
    {
        if (clear0())
            notifyListeners();
    }

    private boolean clear0()
    {
        if (isEmpty())
            return false;
        if (DEBUG_SELECTION) System.out.println("LWSelection clear " + this);
        java.util.Iterator i = iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setSelected(false);
        }
        controlListeners.clear();
        bounds = null;
        super.clear();
        return true;
    }

    /** return bounds of map selection in map (not screen) coordinates */
    public Rectangle2D getBounds()
    {
        if (size() == 0)
            return null;
        //todo:not really safe to cache as we don't know if anything in has has moved?
        //if (bounds == null) {
            bounds = LWMap.getBounds(iterator());
            //System.out.println("COMPUTED SELECTION BOUNDS=" + bounds);
            //}
        return bounds;
    }

    void flushBounds()
    {
        bounds = null;
    }

    public boolean contains(float mapX, float mapY)
    {
        if (size() == 0)
            return false;
	return getBounds().contains(mapX, mapY);
    }

    public LWComponent first()
    {
        return (LWComponent) get(0);
    }
    
    public int countTypes(Class clazz)
    {
        int count = 0;
        Iterator i = iterator();
        while (i.hasNext())
            if (clazz.isInstance(i.next()))
                count++;
        return count;
    }
    
    public boolean containsType(Class clazz)
    {
        Iterator i = iterator();
        while (i.hasNext())
            if (clazz.isInstance(i.next()))
                return true;
        return false;
    }
    
    public boolean allOfType(Class clazz)
    {
        Iterator i = iterator();
        while (i.hasNext())
            if (!clazz.isInstance(i.next()))
                return false;
        return true;
    }
    
    public boolean allOfSameType()
    {
        Iterator i = iterator();
        Object first = i.next();
        while (i.hasNext())
            if (i.next().getClass() != first.getClass())
                return false;
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

    public LWComponent[] getArray()
    {
        LWComponent[] array = new LWComponent[size()];
        super.toArray(array);
        return array;
    }
}

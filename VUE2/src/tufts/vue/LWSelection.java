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
import java.util.HashMap;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

public class LWSelection extends java.util.ArrayList
{
    private List listeners = new java.util.ArrayList();
    private List controlListeners = new java.util.LinkedList();
    private Rectangle2D bounds = null;

    private boolean isClone = false;
    
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
        public ControlPoint(java.awt.Color c)
        {
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
        void controlPointPressed(int index, MapMouseEvent e);
        void controlPointMoved(int index, MapMouseEvent e);
        void controlPointDropped(int index, MapMouseEvent e);
        ControlPoint[] getControlPoints();
    }
    

    //public void addSelectionControl(java.awt.geom.Point2D mapLocation, ControlListener listener)
    private void addControlListener(ControlListener listener)
    {
        if (DEBUG.SELECTION) System.out.println(this + " adding control listener " + listener);
        controlListeners.add(listener);
    }
    
    private void removeControlListener(ControlListener listener)
    {
        if (DEBUG.SELECTION) System.out.println(this + " removing control listener " + listener);
        if (!controlListeners.remove(listener))
            throw new IllegalStateException(this + " didn't contain control listener " + listener);
    }

    java.util.List getControlListeners()
    {
        return controlListeners;
    }

    public synchronized void addListener(Listener l)
    {
        if (DEBUG.SELECTION&&DEBUG.META) System.out.println(this + " adding listener   " + l);
        listeners.add(l);
    }
    public synchronized void removeListener(Listener l)
    {
        if (DEBUG.SELECTION&&DEBUG.META) System.out.println(this + " removing listener " + l);
        listeners.remove(l);
    }

    private Listener[] listener_buf = new Listener[128];
    private boolean inNotify = false;
    private synchronized void notifyListeners()
    {
        if (isClone) throw new IllegalStateException(this + " clone's can't notify listeners! " + this);

        if (notifyUnderway())
            return;
        
        try {
            inNotify = true;
        
            if (DEBUG.SELECTION) {
                System.out.println("-----------------------------------------------------------------------------");
                System.out.println(this + " NOTIFYING " + listeners.size() + " LISTENERS");
            }
            Listener[] listener_iter = (Listener[]) listeners.toArray(listener_buf);
            int nlistener = listeners.size();
            long start = 0;
            for (int i = 0; i < nlistener; i++) {
                if (DEBUG.SELECTION) System.out.print(this + " notifying: #" + (i+1) + " " + (i<9?" ":""));
                Listener l = listener_iter[i];
                try {
                    if (DEBUG.SELECTION) {
                        System.out.print(l + "...");
                        start = System.currentTimeMillis();
                    }
                    l.selectionChanged(this);
                    if (DEBUG.SELECTION) {
                        long delta = System.currentTimeMillis() - start;
                        System.out.println(delta + "ms");
                    }
                } catch (Exception ex) {
                    System.err.println(this + " notifyListeners: exception during selection change notification:"
                                       + "\n\tselection: " + this
                                       + "\n\tfailing listener: " + l);
                    ex.printStackTrace();
                    //java.awt.Toolkit.getDefaultToolkit().beep();
                }
            }
        } finally {
            inNotify = false;
        }
    }

    private boolean notifyUnderway() {
        if (inNotify) {
            new Throwable(this + " attempt to change selection during selection change notification: denied.").printStackTrace();
            java.awt.Toolkit.getDefaultToolkit().beep();            
            return true;
        } else {
            return false;
        }
    }

    /** for Actions.java */
    java.util.List getListeners()
    {
        return this.listeners;
    }

    synchronized void setTo(LWComponent c)
    {
        if (size() == 1 && first() == c)
            return;
        if (notifyUnderway())
            return;
        clear0();
        add(c);
    }
    
    synchronized void setTo(Iterator i)
    {
        if (notifyUnderway())
            return;
        clear0();
        add(i);
    }
     
    synchronized void add(LWComponent c)
    {
        if (notifyUnderway())
            return;
        if (!c.isSelected()) {
            add0(c);
            notifyListeners();
        } else
            if (DEBUG.SELECTION) System.out.println(this + " addToSelection(already): " + c);
    }
    
    public boolean add(Object o)
    {
        throw new RuntimeException(this + " can't add " + o.getClass() + ": " + o);
    }
    
    /** Make sure all in iterator are in selection & do a single change notify at the end */
    synchronized void add(Iterator i)
    {
        if (notifyUnderway())
            return;
        
        LWComponent c;
        boolean changed = false;
        while (i.hasNext()) {
            c = (LWComponent) i.next();
            if (!c.isSelected() && c.isDrawn()) {
                add0(c);
                changed = true;
            }
        }
        if (changed)
            notifyListeners();
    }
    
    /** Change the selection status of all LWComponents in iterator */
    synchronized void toggle(Iterator i)
    {
        if (notifyUnderway())
            return;
        
        LWComponent c;
        boolean changed = i.hasNext();
        while (i.hasNext()) {
            c = (LWComponent) i.next();
            if (c.isSelected())
                remove0(c);
            else
                add0(c);
        }
        if (changed)
            notifyListeners();
    }
    
    private synchronized void add0(LWComponent c)
    {
        if (DEBUG.SELECTION) System.out.println(this + " adding " + c);

        if (notifyUnderway())
            return;
        
        if (!c.isSelected()) {
            if (!isClone) c.setSelected(true);
            bounds = null;
            super.add(c);
            if (!isClone && c instanceof ControlListener)
                addControlListener((ControlListener)c);
        } else
            throw new RuntimeException(this + " attempt to add already selected component " + c);
    }
    
    public synchronized void remove(LWComponent c)
    {
        remove0(c);
        notifyListeners();
    }

    private synchronized void remove0(LWComponent c)
    {
        if (DEBUG.SELECTION) System.out.println(this + " removing " + c);
        if (notifyUnderway())
            return;
        if (!isClone) c.setSelected(false);
        if (!isClone && c instanceof ControlListener)
            removeControlListener((ControlListener)c);
        bounds = null;
        if (!super.remove(c))
            throw new RuntimeException(this + " remove: list doesn't contain " + c);
    }
    
    /**
     * clearAndNotify
     * This emthod clears teh selection and always notifies
     * listeners of a change.
     *
     **/
    public synchronized void clearAndNotify() {
    	clear0();
        if (DEBUG.SELECTION) System.out.println(this + " clearAndNotify: forced notification after clear");
    	notifyListeners();
    }
    
    public synchronized void clear()
    {
        if (clear0())
            notifyListeners();
    }

    private synchronized boolean clear0()
    {
        if (isEmpty())
            return false;
        if (notifyUnderway())
            return false;
        if (DEBUG.SELECTION) System.out.println(this + " clear0");

        if (!isClone) {
            java.util.Iterator i = iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                c.setSelected(false);
            }
        }
        controlListeners.clear();
        bounds = null;
        super.clear();
        return true;
    }

    /** Remove from selection anything that's been deleted */
    synchronized void clearDeleted()
    {
        if (DEBUG.SELECTION) System.out.println(this + " clearDeleted");
        boolean removed = false;
        LWComponent[] elements = new LWComponent[size()];
        toArray(elements);
        for (int i = 0; i < elements.length; i++) {
            LWComponent c = elements[i];
            if (c.isDeleted()) {
                if (DEBUG.SELECTION) System.out.println(this + " clearDeleted: clearing " + c);
                remove0(c);
                removed = true;
            }
        }
        if (removed)
            notifyListeners();
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

    /** return shape bounds of map selection in map (not screen) coordinates
     * Does NOT inclde any stroke widths. */
    public Rectangle2D getShapeBounds()
    {
        if (size() == 0)
            return null;
        return LWMap.getShapeBounds(iterator());
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
        return size() == 0 ? null : (LWComponent) get(0);
    }
    
    public LWComponent last()
    {
        return size() == 0 ? null : (LWComponent) get(size()-1);
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
        return size() != 0;
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

    public Object clone()
    {
        LWSelection copy = (LWSelection) super.clone();
        copy.isClone = true;
        // if anybody tries to use these we want a NPE
        copy.listeners = null;
        copy.controlListeners = null;
        return copy;
    }

    public String toString()
    {
        return "LWSelection[" + size() + (isClone?" CLONE]":"]");
    }
    
}

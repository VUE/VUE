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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;


public class ResourceSelection extends java.util.ArrayList
{
    public static final boolean DEBUG_SELECTION = VueResources.getBool("selection.debug");

    private java.util.List listeners = new java.util.ArrayList();

    private  boolean isClone = false;
    
    public interface Listener extends java.util.EventListener {
        void selectionChanged(ResourceSelection selection);
    }

    //public void addSelectionControl(java.awt.geom.Point2D mapLocation, ControlListener listener)
 

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
        if (isClone) throw new IllegalStateException("Resource: clone's can't notify listeners! " + this);
        
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            Listener l = (Listener) i.next();
            l.selectionChanged(this);
        }
    }
       
    void setTo(Resource r)
    {
        clear0();
        add(r);
    }
    
    void setTo(Iterator i)
    {
        clear0();
        add(i);
    }
     
    void add(Resource r)
    {
        if (!r.isSelected()) {
            add0(r);
            notifyListeners();
        } else
            if (DEBUG_SELECTION) System.out.println("addToSelection(already): " + r);
    }
    
    public boolean add(Object o)
    {
        throw new RuntimeException("ResourceSelection can't add " + o.getClass() + ": " + o);
    }
    
    /** Make sure all in iterator are in selection & do a single change notify at the end */
    void add(Iterator i)
    {
        Resource r;
        boolean changed = false;
        while (i.hasNext()) {
            r = (Resource) i.next();
            if (!r.isSelected()) {
                add0(r);
                changed = true;
            }
        }
        if (changed)
            notifyListeners();
    }
    
    /** Change the selection status of all ResourceComponents in iterator */
    void toggle(Iterator i)
    {
        Resource r;
        boolean changed = false;
        while (i.hasNext()) {
            r = (Resource) i.next();
            if (r.isSelected())
                remove0(r);
            else
                add0(r);
            changed = true;
        }
        if (changed)
            notifyListeners();
    }
    
    private void add0(Resource r)
    {
        if (DEBUG_SELECTION) System.out.println("ResourceSelection: adding " + r);
        
        if (!r.isSelected()) {
            if (!isClone) r.setSelected(true);
            super.add(r);
        } else
            throw new RuntimeException("ResourceSelection: attempt to add already selected resource " + r);
    }
    
    public void remove(Resource r)
    {
        remove0(r);
        notifyListeners();
    }

    private void remove0(Resource r)
    {
        if (DEBUG_SELECTION) System.out.println("ResourceSelection: removing " + r);
        if (!isClone) r.setSelected(false);
        if (!super.remove(r))
            throw new RuntimeException("ResourceSelection remove: list doesn't contain " + r);
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
        if (DEBUG_SELECTION) System.out.println("ResourceSelection clear " + this);

        if (!isClone) {
            java.util.Iterator i = iterator();
            while (i.hasNext()) {
                Resource r= (Resource) i.next();
                r.setSelected(false);
            }
        }
       
  
        super.clear();
        return true;
    }

    /** return bounds of map selection in map (not screen) coordinates */
   

  
    
    public Resource first()
    {
        return (Resource) get(0);
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


    public Resource[] getArray()
    {
        Resource[] array = new Resource[size()];
        super.toArray(array);
        return array;
    }

    public Object clone()
    {
        ResourceSelection copy = (ResourceSelection) super.clone();
        copy.isClone = true;
        // if anybody tries to use these we want a NPE
        copy.listeners = null;
        return copy;
    }
    
}

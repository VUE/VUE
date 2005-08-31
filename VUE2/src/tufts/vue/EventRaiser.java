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

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.Frame;

/**
 * Generic awt containment event raiser.
 * Experimental until we're sure the performance is okay.
 */

public abstract class EventRaiser
{
    java.awt.Component source;

    public EventRaiser(java.awt.Component source)
    {
        this.source = source;
    }

    public java.awt.Component getSource()
    {
        return source;
    }

    public abstract Class getListenerClass();

    public void raise()
    {
        // get the root parent -- todo: only
        // need to do this once / listen to AWT hierarcy events
        // really: move to a registration system
        /*
        Component parent = source;
        while (parent.getParent() != null)
            parent = parent.getParent();
        deliverToChildren((Container)parent);
        if (parent instanceof Window) {
            Window[] owned = ((Window)parent).getOwnedWindows();
            //System.err.println(java.util.Arrays.asList(owned);
            for (int i = 0; i < owned.length; i++) {
                deliverToChildren(owned[i]);
            }
        }
        */
        //long start = System.currentTimeMillis();
        Frame frames[] = Frame.getFrames();
        for (int fi = 0; fi < frames.length; fi++) {
            Frame frame = frames[fi];
            deliverToChildren((Container)frame);
            if (frame instanceof Window) {
                Window[] owned = ((Window)frame).getOwnedWindows();
                //System.err.println(java.util.Arrays.asList(owned);
                for (int i = 0; i < owned.length; i++) {
                    deliverToChildren(owned[i]);
                }
            }
        }
        //long delta = System.currentTimeMillis() - start;
        //System.out.println("EventRaiser: " + delta + "ms delivery");
    }

    abstract void dispatch(Object listener);

    private void doDispatch(Object target)
    {
        //System.out.println("DISPATCHING " + this + " to " + target);
        dispatch(target);
    }

    protected void deliverToChildren(Container parent)
    {
        Class clazz = getListenerClass();
        if (clazz.isInstance(parent))
            doDispatch(parent);
        Component[] comps = parent.getComponents();
        for (int i = 0; i < comps.length; i++) {
            //System.err.println("checking child " + comps[i]);
            if (comps[i] instanceof Container)
                deliverToChildren((Container)comps[i]);
            else if (clazz.isInstance(comps[i]))
                doDispatch(comps[i]);
        }
    }
    
}


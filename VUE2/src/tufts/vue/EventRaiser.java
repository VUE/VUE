package tufts.vue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;

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
    }

    abstract void dispatch(Object listener);

    private void doDispatch(Object target)
    {
        //System.out.println("DISPATCHING " + this + " to " + target);
        dispatch(target);
    }

    private void deliverToChildren(Container parent)
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


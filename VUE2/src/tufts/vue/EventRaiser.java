/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import tufts.Util;
import tufts.vue.gui.GUI;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.Frame;
import javax.swing.JComponent;
import javax.swing.JMenu;

/**
 *
 * Generic AWT containment event raiser.  Will deliver an event to ANY
 * Component in the entire hierarchy for which
 * getTargetClass().isInstance(Component) returns true.  Event
 * delivery is done by an override of the abstrict dispatch(Object
 * listener).
 *
 * Alternatively, you may override visit(Component c), and perform
 * whatever tests you'd like for dispatching, or just use it for AWT
 * tree traversal.
 *
 * May be started at arbitrary points in the AWT tree
 * by using raiseStartingAt(Contaner) instead if raise()
 *
 * After the event has been raised, traversal (and subsequent event dispatch) may be stopped
 * by calling stop() (e.g., from within dispatch() or visit()).
 *
 * Although this runs surprisingly fast (order of 0.3ms to 3ms), 
 * it's probably wise to use this with care.
 * 
 * Note that if targetClass is null, visit(Component) will throw
 * a NullPointerException, so only do this if you are overriding visit(Component).
 *
 * Does not currently traverse into children of popup menus.
 *
 * @version $Revision: 1.17 $ / $Date: 2010-02-03 19:17:41 $ / $Author: mike $
 * @author Scott Fraize
 */

public abstract class EventRaiser<T>
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(EventRaiser.class);

    private int depth = 0;

    public static boolean INCLUDE_MENUS = true;

    public final Class targetClass;
    public final Object source;
    private final boolean includeMenus;
    
    public EventRaiser(Object source, Class clazz, boolean includeMenus)
    {
        this.source = source;
        this.targetClass = clazz;
        this.includeMenus = includeMenus;
    }
    
    public EventRaiser(Object source, Class clazz)
    {
        this(source, clazz, false);
    }

    public EventRaiser(Object source)
    {
        this.source = source;
        this.targetClass = getTargetClass();
        this.includeMenus = false;
    }

    public Object getSource() {
        return source;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public void raise() {
        traverse(null);
    }
    
    public void raiseStartingAt(Container start) {
        traverse(start);
    }

    public abstract void dispatch(T target);

    
    /**
     * Stop traversal: nothing further can be dispatched.
     *
     * This works by throwing a special completion exception, so
     * it must be called from within the traversal (e.g., dispatch(), or visit()).
     *
     */
    protected static void stop() {
        throw new Completion();
    }
    
    private static class Completion extends RuntimeException {}
    
    private void traverse(Container start)
    {
        long startTime = 0;

        // startTime = System.nanoTime(); // java 1.5
            
        if (DEBUG.EVENTS && DEBUG.META) {
            startTime = System.currentTimeMillis();
            out("raising " + this + " "
                + (start == null ? "everywhere" : ("starting at: " + GUI.name(start))));
        }
        
        try {
            if (start == null) {
                traverseFrames();
            } else {
                traverseContainer(start);
            }
        } catch (Completion e) {
            if (DEBUG.EVENTS) out("traversal stopped; done dispatching events");
        }

        if (DEBUG.EVENTS && DEBUG.META) {
            long delta = System.currentTimeMillis() - startTime;
            //double delta = (System.nanoTime() - startTime) / 1000000.0; // java 1.5
            out(this + " " + delta + "ms delivery");
        }
        
    }
    
    private void traverseFrames()
    {
        Frame frames[] = Frame.getFrames();

        if (DEBUG.EVENTS && DEBUG.META) out("FRAMES: " + java.util.Arrays.asList(frames));

        traverseChildren(frames);
    }

    protected void dispatchSafely(Component target)
    {
        if (DEBUG.EVENTS) out("DISPATCHING "
                              + this
                              + " to " + tufts.vue.gui.GUI.name(target)
                              //+ " " + (target instanceof JComponent ? ((JComponent)target).getUIClassID() : "")
                              );
        try {
            dispatch((T) target);
        } catch (Completion e) {
            throw e;
        } catch (Throwable e) {
            Util.printStackTrace(e, EventRaiser.class.getName() + " exception during dispatch: "
                                 + "\n\t  event source: " + source + " (" + Util.objectTag(source) + ")"
                                 + "\n\t  event raised: " + this
                                 + "\n\t   targetClass: " + targetClass
                                 + "\n\tdispatching to: " + GUI.name(target) + " (" + Util.objectTag(target) + ")"
                                 + "\n\t also known as: " + target
                                 );
        }
    }

    protected void visit(Component c) {
        if (targetClass.isInstance(c)) {
            dispatchSafely(c);
        }
    }

    protected void traverseContainer(Container parent)
    {
        visit(parent);

        if (parent.getComponentCount() > 0)
            traverseChildren(parent.getComponents());

        if (parent instanceof Window) {

            Window[] owned = ((Window)parent).getOwnedWindows();

            if (owned.length > 0) {
                if (DEBUG.EVENTS && DEBUG.META)
                    eoutln("    OWNED by " + GUI.name(parent) + ": " + java.util.Arrays.asList(owned));
                traverseChildren(owned);
            }
        }
        else if (parent instanceof javax.swing.JMenu) {

            traverseChildren( ((javax.swing.JMenu)parent).getMenuComponents() );

        }
    }
    

    protected void traverseChildren(Component[] children)
    {
        depth++;
        
        for (int i = 0; i < children.length; i++) {
            if (DEBUG.EVENTS && DEBUG.META && DEBUG.FOCUS) eoutln(getType(children[i]) + i + " " + GUI.name(children[i]));
            if (children[i] instanceof Container)
                traverseContainer((Container)children[i]);
            else
                visit(children[i]);
        }

        depth--;
    }

    private static String getType(Component c) {
        if (c instanceof Frame)
            return "FRAME";
        if (c instanceof Window)
            return "owned";
        return "child";
    }
                                                  

    private void out(String s) {
        Log.debug(s);
    }

    protected void eout(String s) {
        for (int x = 0; x < depth; x++) System.out.print("    ");
        System.out.print(s);
    }
    protected void eoutln(String s) {
        eout(s + "\n");
    }
    
    
}


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

import tufts.Util;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.Icon;

/**
 * Base class for VueActions that don't use the selection.
 * @see Actions.LWCAction for actions that use the selection
 *
 * @version $Revision: 1.31 $ / $Date: 2007-11-05 11:46:22 $ / $Author: sfraize $ 
 */
public class VueAction extends javax.swing.AbstractAction
{
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueAction.class);
    
    public static final boolean EnableSmallIconsForMenus = false;
    public static final String LARGE_ICON = "LargeIcon";

    private static List<VueAction> AllActionList = new ArrayList();

    private static boolean allIgnored = false;
    private static boolean allEditIgnored = false;

    private final String permanentName;

    static class DeniedException extends RuntimeException {
        DeniedException(String msg) { super(msg); }
    }

    /** Set's all action events to be temporarily ignored.
        E.g., used while a TextBox edit is active */
    // todo: may want to allow NewItem actions as they automatically
    // activate an edit, thus preventing a quick series of NewItem
    // actions to be done.
    static void setAllActionsIgnored(boolean tv)
    {
        if (DEBUG.Enabled) {
            Log.debug("allIgnored=" + tv);
            if (DEBUG.META) tufts.Util.printStackTrace("ALL ACTIONS IGNORED: " + tv);
        }
        
        allIgnored = tv;
    }

//     // todo: need to update Actions.java for all actions
//     static void setAllEditActionsIgnored(boolean tv)
//     {
//         if (DEBUG.Enabled) {
//             System.out.println("VueAction: allIEditgnored=" + tv);
//             if (DEBUG.META) tufts.Util.printStackTrace("ALL EDIT ACTIONS ENABLED: " + tv);
//         }
        
//         allEditIgnored = tv;
//     }
    

    public VueAction(String name, String shortDescription, KeyStroke keyStroke, Icon icon)
    {
        super(name);
        this.permanentName = name;
        if (shortDescription != null)
            putValue(SHORT_DESCRIPTION, shortDescription);
        else
            putValue(SHORT_DESCRIPTION, name);
        if (keyStroke != null)
            putValue(ACCELERATOR_KEY, keyStroke);

        setSmallIcon(icon);
        //if (DEBUG.Enabled) System.out.println("Constructed: " + this + " icon=" + getValue(SMALL_ICON));
        AllActionList.add(this);
    }
    public VueAction(String name, KeyStroke keyStroke, String iconSpec) {
        this(name, null, keyStroke, null);
        setIcon(iconSpec);
    }
    public VueAction(String name, KeyStroke keyStroke) {
        this(name, null, keyStroke, null);
    }
    public VueAction(String name) {
        this(name, null, null, null);
    }
    public VueAction(String name, Icon icon) {
        this(name, null, null, icon);
    }

    private static int anonIndex = 0;
    public VueAction() {
        this(null, null, null, null);
	putValue(Action.NAME, getClass().getName() + anonIndex++);
    }    

    public List<VueAction> getAllActions() {
        return Collections.unmodifiableList(AllActionList);
    }

    private void setIcon(String iconSpec) {
        Icon icon = null;
        if (iconSpec.startsWith(":")) {
            if (EnableSmallIconsForMenus) {
                icon = VueResources.getImageIconResource("/toolbarButtonGraphics/" + iconSpec.substring(1) + "16.gif");
                setSmallIcon(icon);
            }
            icon = VueResources.getImageIconResource("/toolbarButtonGraphics/" + iconSpec.substring(1) + "24.gif");
            putValue(LARGE_ICON, icon);
        } else {
            if (EnableSmallIconsForMenus) {
                icon = VueResources.getImageIconResource(iconSpec);
                setSmallIcon(icon);
            }
        }
    }

    private void setSmallIcon(Icon icon) {
        if (EnableSmallIconsForMenus) {
            if (icon != null) {
                if (icon != tufts.vue.gui.GUI.NO_ICON)
                    putValue(SMALL_ICON, icon);
            }
        }
    }


    /** undoable: the undo manager already won't bother to create an
     * undo action if it didn't detect any changes.  This method is
     * here as a backup just in case we know for sure we don't even
     * want to talk to the undo manager during an action, such as the
     * undo actions.
     */
    boolean undoable() { return true; }

    /** @return false (the default) if this is an editing action - overide and return false
     * to enable as a non-editing action
     */
    boolean isEditAction() {
        return false;
    }

    public String getActionName()
    {
        return (String) getValue(Action.NAME);
    }
    /**
     * @return the base name for this action that never changes.  E.g.:,
     * for UndoAction, return just "Undo" instead of "Undo <most recent action>"
     */
    public String getPermanentActionName()
    {
        return this.permanentName;
    }
    public void setActionName(String s)
    {
        putValue(Action.NAME, s);
    }

    public boolean overrideIgnoreAllActions() { return false; }
        
    public void actionPerformed(ActionEvent ae)
    {
        if (DEBUG.EVENTS)
            Log.debug("\n-----------------------------------------------------------------------------\n"
                      + this
                      + " START OF actionPerformed: " + getClass().getName()
                      + ";\n\tActionEvent: (" + ae.paramString()
                      + ")\n\t     source: " + ae.getSource());
        if (allIgnored && !isUserEnabled()) {
            if (DEBUG.Enabled) Log.debug("ALL ACTIONS DISABLED; " + this + "; " + ae);
            return;
        }
//         if (allEditIgnored && isEditAction()) {
//             if (DEBUG.Enabled) System.out.println("ALL EDIT ACTIONS DISABLED; " + this + "; " + ae);
//             return;
//         }
        boolean hadException = false;
        try {
            /*
              String msg = "VueAction: " + getActionName();
              if (!ae.getActionCommand().equals(getActionName()))
              msg += " (" + ae.getActionCommand() + ")";
              msg += " n=" + VUE.getSelection().size();
              System.out.println(msg);
            */
            if (isUserEnabled()) {
                act();
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
                System.err.println(getActionName() + ": Not currently enabled");
            }
        } catch (DeniedException e) {
            Log.info("Denied: " + this + "; " + e.getMessage());
        } catch (Throwable t) {
            synchronized (System.err) {
                System.err.println("*** VueAction: exception during action [" + getActionName() + "]");
                System.err.println("*** VueAction: " + getClass());
                System.err.println("*** VueAction: selection is " + VUE.getSelection());
                System.err.println("*** VueAction: event was " + ae);
                tufts.Util.printStackTrace(t);
            }
            hadException = true;
        }
        //if (DEBUG.EVENTS) System.out.println("\n" + this + " UPDATING JUST THE ACTION LISTENERS FOR ENABLED STATES");
        if (VUE.getUndoManager() != null && undoable()) {
            String undoName = ae.getActionCommand();
            if (undoName == null)
                undoName = getActionName();
            if (hadException && DEBUG.Enabled)
                undoName += " (!)";
            VUE.getUndoManager().markChangesAsUndo(undoName);
        }
        //Actions.Undo.putValue(NAME, "Undo " + ae.getActionCommand());
        updateActionListeners();
        if (DEBUG.EVENTS) Log.debug(this + " END OF actionPerformed: ActionEvent=" + ae.paramString() + "\n");
        // normally handled by updateActionListeners, but if someone
        // has actually defined "enabled" instead of enabledFor, we'll
        // need this.
        // setEnabled(enabled());
        // Okay, do NOT do this -- enabled sometimes use to just
        // ring the bell when an action is attempted that you
        // can't actually do right now -- problem is if we
        // disable the action based on enabled(), it has
        // no way of ever getting turned back on!
    }

    public void fire(Object source) {
        actionPerformed(new ActionEvent(source, 0, (String) getValue(NAME)));
    }

    // To update action's enabled state after an action is performed.

    // todo: create a single VueAction LWSelection listener that then
    // dispatches selection updates to all the actions (and then see
    // how many ms all together are taking), and for performance can
    // break down selection once into a selection info object for
    // action's to check against (size, #nodes, #links, etc)
    // also consider just putting all that info directly in
    // the selection.
    
    private void updateActionListeners()
    {
        Iterator i = VUE.getSelection().getListeners().iterator();
        while (i.hasNext()) {
            LWSelection.Listener l = (LWSelection.Listener) i.next();
            if (l instanceof javax.swing.Action) {
                l.selectionChanged(VUE.getSelection());
                //System.out.println("Notifying action " + l);
            }
            //else System.out.println("Skipping listener " + l);
        }
    }

    /** Note that overriding enabled() will not update the action's enabled
     * state based on what's in the selection -- you need to subclass
     * Actions.LWCAction and override enabledFor(LWSelection s) for
     * that -- it gets called whenever the selection changes and will
     * update the actions enabled state based on what it returns.  If
     * you want an action to update it's enabled state based on any
     * other VUE application state, all enabled states are updated
     * after every action is performed, but if you need more than
     * that, the action will need it's own listener for whatever event
     * it's interested in.
     */
    boolean enabled() { return VUE.getActiveViewer() != null; }

    /** public access enabled checker that also checks master action enabled states */
    public boolean isUserEnabled() {
        if (allIgnored && !overrideIgnoreAllActions())
            return false;
        else
            return enabled();
    }

    public void act() {
        System.err.println("Unhandled VueAction: " + getActionName());
    }

    protected void out(String s) {
        if (DEBUG.Enabled) Log.debug(this + ": " + s);
    }

    protected void info(String s) {
        Log.info(this + " " + s);
    }
    
    public String toString() {
        Class c = getClass();
        return Util.TERM_GREEN
            + (c.isAnonymousClass() ? c.getSuperclass().getSimpleName() : c.getSimpleName())
            + "[" + getActionName() +  "]"
            + Util.TERM_CLEAR;
    }
}


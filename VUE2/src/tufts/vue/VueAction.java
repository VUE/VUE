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
 */
public class VueAction extends javax.swing.AbstractAction
{
    public static final String LARGE_ICON = "LargeIcon";

    private static List AllActionList = new ArrayList();

    private static boolean allIgnored = false;
    /** Set's all action events to be temporarily ignored.
        E.g., used while a TextBox edit is active */
    // todo: may want to allow NewItem actions as they automatically
    // activate an edit, thus preventing a quick series of NewItem
    // actions to be done.
    static void setAllIgnored(boolean tv)
    {
        allIgnored = tv;
    }
        
    public VueAction(String name, String shortDescription, KeyStroke keyStroke, Icon icon)
    {
        super(name, icon);
        if (shortDescription != null)
            putValue(SHORT_DESCRIPTION, shortDescription);
        else
            putValue(SHORT_DESCRIPTION, name);
        if (keyStroke != null)
            putValue(ACCELERATOR_KEY, keyStroke);
        if (icon != null)
            putValue(SMALL_ICON, icon);
        //if (DEBUG.Enabled) System.out.println("Constructed: " + this);
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

    public List getAllActions() {
        return Collections.unmodifiableList(AllActionList);
    }

    private void setIcon(String iconSpec) {
        Icon icon = null;
        if (iconSpec.startsWith(":")) {
            icon = VueResources.getImageIconResource("/toolbarButtonGraphics/" + iconSpec.substring(1) + "16.gif");
            putValue(SMALL_ICON, icon);
            icon = VueResources.getImageIconResource("/toolbarButtonGraphics/" + iconSpec.substring(1) + "24.gif");
            putValue(LARGE_ICON, icon);
        } else {
            icon = VueResources.getImageIconResource(iconSpec);
            putValue(SMALL_ICON, icon);
        }
    }


    /** undoable: the undo manager already won't bother to create an
     * undo action if it didn't detect any changes.  This method is
     * here as a backup just in case we know for sure we don't even
     * want to talk to the undo manager during an action, such as the
     * undo actions.
     */
    boolean undoable() { return true; }

    public String getActionName()
    {
        return (String) getValue(Action.NAME);
    }
    public void setActionName(String s)
    {
        putValue(Action.NAME, s);
    }
    public void actionPerformed(ActionEvent ae)
    {
        if (DEBUG.EVENTS) System.out.println("\n-----------------------------------------------------------------------------\n"
                                             + this
                                             + " START OF actionPerformed: ActionEvent="
                                             + ae.paramString()
                                             + " src=" + ae.getSource());
        if (allIgnored) {
            if (DEBUG.EVENTS) System.out.println("ACTIONS DISABLED.");
            return;
        }
        boolean hadException = false;
        try {
            /*
              String msg = "VueAction: " + getActionName();
              if (!ae.getActionCommand().equals(getActionName()))
              msg += " (" + ae.getActionCommand() + ")";
              msg += " n=" + VUE.getSelection().size();
              System.out.println(msg);
            */
            if (enabled()) {
                act();
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
                System.err.println(getActionName() + ": Not currently enabled");
            }
        } catch (Exception e) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            e.printStackTrace();
            System.err.println("*** VueAction: exception during action [" + getActionName() + "]");
            System.err.println("*** VueAction: selection is " + VUE.getSelection());
            System.err.println("*** VueAction: event was " + ae);
            hadException = true;
        }
        //if (DEBUG.EVENTS) System.out.println("\n" + this + " UPDATING JUST THE ACTION LISTENERS FOR ENABLED STATES");
        if (VUE.getUndoManager() != null && undoable()) {
            String undoName = ae.getActionCommand();
            if (hadException && DEBUG.Enabled)
                undoName += " (!)";
            VUE.getUndoManager().markChangesAsUndo(undoName);
        }
        //Actions.Undo.putValue(NAME, "Undo " + ae.getActionCommand());
        updateActionListeners();
        if (DEBUG.EVENTS) System.out.println(this + " END OF actionPerformed: ActionEvent=" + ae.paramString() + "\n");
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

    void act() {
        System.err.println("Unhandled VueAction: " + getActionName());
    }

    protected void out(String s) {
        System.out.println(this + ": " + s);
    }

    public String toString() { return "VueAction[" + getActionName() + "]"; }
}


package tufts.vue;

import java.util.Iterator;
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
        if (keyStroke != null)
            putValue(ACCELERATOR_KEY, keyStroke);
        if (DEBUG.Enabled) System.out.println("Constructed: " + this);
    }
    public VueAction(String name, KeyStroke keyStroke) {
        this(name, null, keyStroke, null);
    }
    public VueAction(String name) {
        this(name, null, null, null);
    }

    // todo: should be able to get rid of this flag: undo manager
    // now just does nothing if it detected no changes
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
        if (DEBUG.EVENTS) System.out.println("\n-------------------------------------------------------\n"
                                             + this + " START OF actionPerformed: ActionEvent=" + ae.paramString());
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
            if (hadException)
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

    /** note that overriding this will not update the action's enabled
     * state based on what's in the selection -- you need to use
     * enabledFor(LWSelection s) for that -- it gets called whenever
     * the selection changes and will update the actions enabled
     * state based on the return value.
     */
    boolean enabled() { return VUE.openMapCount() > 0; }

    void act() {
        System.err.println("Unhandled VueAction: " + getActionName());
    }
    void Xact() {}// for commenting convenience

    public String toString() { return "VueAction[" + getActionName() + "]"; }
}


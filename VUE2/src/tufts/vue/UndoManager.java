package tufts.vue;

import java.util.*;
import javax.swing.Action;

/**
 * Records all changes that take place in a map and
 * provides for arbitrarily marking named points of rollback.
 */

public class UndoManager
    implements LWComponent.Listener
{
    //private static final String kUndoActionName = "undoActionName";
    
    ArrayList mChanges;
    ArrayList mUndoActions = new ArrayList();
    Map mPropertyChanges = new HashMap();

    /**
     * A list (map) of components each with a list (map) of property changes to them.
     */
    static class UndoAction {
        String name;
        Map propertyChanges;
        UndoAction(String name, Map propertyChanges) {
            this.name = name;
            this.propertyChanges = propertyChanges;
        }

        void undoPropertyChanges()
        {
            if (DEBUG.UNDO) System.out.println(this + " undoPropertyChanges");
        }

        public String toString() {
            return "UndoAction[" + name + " propertyChanges=" + propertyChanges.size() + "]";
        }
    }

    public UndoManager(LWMap map)
    {
        map.addLWCListener(this);
        mChanges = new ArrayList();
    }

    public void undo()
    {
        UndoAction undoAction = (UndoAction) mUndoActions.get(mUndoActions.size() - 1);
        if (DEBUG.UNDO) System.out.println(undoAction + ": UNDO");
        undoAction.undoPropertyChanges();
    }
    
    public void markChangesAsUndoable(String name)
    {
        if (DEBUG.UNDO) System.out.println("UNDO: marking " + name);
        UndoAction undoAction = new UndoAction(name, mPropertyChanges);
        mUndoActions.add(undoAction);
        mPropertyChanges = new HashMap();
        String undoName = "Undo " + name;
        if (DEBUG.EVENTS||DEBUG.UNDO) undoName += " (" + undoAction.propertyChanges.size() + ")";
        Actions.Undo.putValue(Action.NAME, undoName);
    }

    public void LWCChanged(LWCEvent e) {
        if (DEBUG.UNDO) System.out.println("UNDO: tracking " + e);
        String propName = e.getWhat();
        LWComponent c = e.getComponent(); // can be list...
        Object oldValue = e.getOldValue();
        if (oldValue != null) {
            if (DEBUG.UNDO) System.out.println("\t   got old value " + oldValue);
            Map propList = (Map) mPropertyChanges.get(c);
            if (propList != null) {
                if (DEBUG.UNDO) System.out.println("\tfound existing component");
                Object value = propList.get(propName);
                if (value != null) {
                    if (DEBUG.UNDO) System.out.println("\tIGNORING: found existing property value: " + value);
                } else {
                    propList.put(propName, oldValue);
                    if (DEBUG.UNDO) System.out.println("\tstored old value " + oldValue);
                }
            } else {
                propList = new HashMap();
                propList.put(propName, oldValue);
                mPropertyChanges.put(c, propList);
                if (DEBUG.UNDO) System.out.println("\tstored old value " + oldValue);
            }
        } else {
            if (DEBUG.UNDO) System.out.println("\tunhandled");
        }
    }

    public String toString()
    {
        return "UndoManager[]";
    }
    
}


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
    ArrayList mChanges;
    ArrayList mNamedChangeGroups = new ArrayList();
    ArrayList mPropertyChanges = new ArrayList();

    public UndoManager(LWMap map)
    {
        map.addLWCListener(this);
        mChanges = new ArrayList();
    }

    public static void undo()
    {
        if (DEBUG.UNDO) System.out.println("UNDO");
    }
    
    public void markChangesAsUndoable(String name)
    {
        if (DEBUG.UNDO) System.out.println("UNDO: marking " + name);
        Actions.Undo.putValue(Action.NAME, "Undo " + name);
    }

    public void LWCChanged(LWCEvent e) {
        if (DEBUG.UNDO) System.out.println("UNDO: tracking " + e);
        String prop = e.getWhat();
        LWComponent c = e.getComponent(); // can be list...
        Object old = e.getOldValue();
        if (old != null) {
            if (DEBUG.UNDO) System.out.println("\tgot old value " + old);
            if (mPropertyChanges.get
        } else {
            if (DEBUG.UNDO) System.out.println("\tunhandled");
        }
        
        
    }

    public String toString()
    {
        return "UndoManager[]";
    }
    
}


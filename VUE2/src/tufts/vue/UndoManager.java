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

    public UndoManager(LWMap map)
    {
        map.addLWCListener(this);
        mChanges = new ArrayList();
    }
    
    public void markChangesAsUndoable(String name)
    {
        if (DEBUG.UNDO) System.out.println("UNDO: marking " + name);
        Actions.Undo.putValue(Action.NAME, "Undo " + name);
    }

    public void LWCChanged(LWCEvent e) {
        if (DEBUG.UNDO) System.out.println("UNDO: tracking " + e);
        String t = e.getWhat();
        Object old = e.getOldValue();
        if (DEBUG.UNDO && old != null)
            System.out.println("\tgot old value " + old);
        
    }
    
}


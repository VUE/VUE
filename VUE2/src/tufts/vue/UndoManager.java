package tufts.vue;

import javax.swing.Action;

/**
 * Records all changes that take place in a map and
 * provides for arbitrarily marking named points of rollback.
 */

public class UndoManager
    implements LWComponent.Listener
{
    public UndoManager(LWMap map)
    {
        map.addLWCListener(this);
    }
    
    public void markChangesAsUndoable(String name)
    {
        Actions.Undo.putValue(Action.NAME, "Undo " + name);
    }

    public void LWCChanged(LWCEvent e) {
        if (DEBUG.UNDO) System.out.println("UNDO: " + e);
    }
    
}


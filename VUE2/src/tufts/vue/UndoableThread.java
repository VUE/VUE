package tufts.vue;

/**
 *
 * A thread from which the  UndoManager can track asynchronous model updates.
 *
 * @version $Revision: 1.3 $ / $Date: 2006-01-23 17:24:51 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
public class UndoableThread extends Thread
{
    private Object undoActionMarker;

    public UndoableThread(String name, UndoManager undoManager) {
        super("UndoableThread: " + name);
        setDaemon(true);
        if (undoManager != null)
            undoManager.attachThreadToNextMark(this);
        // it's okay if there was no UndoManger: this happens
        // during map loading.
    }


    // called back by undo manager during attach
    void setMarker(Object o) {
        this.undoActionMarker = o;
    }

    // called by undo manager
    Object getMarker() {
        return this.undoActionMarker;
    }

    public void start() {
        if (false && DEBUG.CASTOR)
            run(); // run synchronously
        else
            super.start();
    }
        

}


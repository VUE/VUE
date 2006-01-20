package tufts.vue;

/**
 *
 * A thread from which the  UndoManager can track asynchronous model updates.
 *
 * @version $Revision: 1.2 $ / $Date: 2006-01-20 20:15:39 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
public class UndoableThread extends Thread
{
    private Object undoActionMarker;

    public UndoableThread(String name) {
        super("UndoableThread: " + name);
        setDaemon(true);
        UndoManager undoManager = VUE.getUndoManager();
        if (undoManager != null) {
            undoManager.attachThreadToUpcomingMark(this);
        }
        // it's okay if there was no UndoManger: this happens
        // during map loading.
    }

    void setMarker(Object o) {
        this.undoActionMarker = o;
    }

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


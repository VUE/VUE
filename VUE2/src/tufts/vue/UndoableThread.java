package tufts.vue;

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
        if (DEBUG.CASTOR)
            run(); // run synchronously
        else
            super.start();
    }
        

}


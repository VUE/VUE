package tufts.vue;

abstract class Undoable {
    protected Object old; // protected so is visible in sub-classed undo method
    public Undoable(Object old) {
        this.old = old;
    }
    abstract void undo();

    public String toString() {
        return getClass().getName() + ".undo[" + old + "]";
    }
    /*
      // if can figure out how to make this a static class when created in-line,
      // can make this a bit more memory efficient as each subclass instance doesn't
      // need to keep a "this" reference to it's containing class as the undo
      // manager is already doing that, tho it sure makes the code look 
      // short and sweet to do it this way.
    void doUndo(LWComponent c) {
        if (DEBUG.UNDO) System.out.println(this + " restoring " + old);
        undo(c);
    }
    abstract void undo(LWComponent c);
    */
}


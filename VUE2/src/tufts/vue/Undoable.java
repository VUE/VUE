package tufts.vue;

abstract class Undoable {
    protected Object old; // old property value: protected so is visible in sub-classed undo method
    public Undoable(Object old) {
        this.old = old;
    }
    public Undoable() {}

    /**
     * If your type isn't one we've provided an automatic convenience
     * conversion for, just override undo(), grab the old value
     * yourself (it's not private so subclasses can get it) and do a
     * cast.  The convenience conversions just keep the inline class
     * definitions cleaner & less cluttered.
     */

    void undo()
    {
             if (old instanceof Boolean)        undo(((Boolean) old).booleanValue());
        else if (old instanceof Integer)        undo(((Integer) old).intValue());
        else if (old instanceof String)         undo(((String) old));
        else if (old instanceof Float)          undo(((Float) old).floatValue());
        else if (old instanceof Double)         undo(((Double) old).doubleValue());
        else if (old instanceof Object[])       undo((Object[]) old);
        else
            throw new TypeException(this
                                    + ": no automatic type converter for old value of type "
                                    + old.getClass()
                                    + ": override undo() instead.");
    }

    /**
     * Only one of these will ever be called.  Your subclass
     * should either override one of these, or the generic
     * undo() above and do your own cast of the old value.
     */
    void undo(boolean b) { throw new TypeException(); }
    void undo(int i) { throw new TypeException(); }
    void undo(float f) { throw new TypeException(); }
    void undo(double d) { throw new TypeException(); }
    void undo(String s) { throw new TypeException(); }
    void undo(Object[] a) { throw new TypeException(); }

    class TypeException extends RuntimeException {
        TypeException(String msg) {
            super(msg);
        }
        TypeException() {
            this(Undoable.this + ": must override undo(<type> arg) with argument matching old value type " + old.getClass());
        }
    }

    public String toString() {
        String s = getClass().getName() + "<undoable>[";
        if (old instanceof Object[])
            s += java.util.Arrays.asList((Object[])old);
        else
            s += old;
        return s + "]";
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


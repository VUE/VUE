package tufts.vue;

class DEBUG
{
    // Can leave these as static for runtime tweaking, or
    // make final to have compiler strip them out entirely.
    static boolean CONTAINMENT = false;
    static boolean PARENTING = false;
    static boolean LAYOUT = false;
    static boolean BOXES = false;
    static boolean ROLLOVER = false;
    static boolean EVENTS = false;
    static boolean SCROLL = false;
    static boolean SELECTION = false;

    //Mapper pSELECTION = new Mapper("selection") { void set(boolean v) { selection=v; } boolean get() { return selection; } }

    /*
    abstract class Mapper {
        String mName;
        Mapper(String name) { mName = name; }
        abstract void set(boolean v);
        abstract boolean get();
    }
    Mapper[] props = {
        new Mapper("selection") { void set(boolean v) { SELECTION=v; } boolean get() { return SELECTION; } },
        new Mapper("scroll") { void set(boolean v) { SCROLL=v; } boolean get() { return SCROLL; } }
    };
    // use introspection instead
    */
}

package tufts.vue;

public class DEBUG
{
    // Can leave these as static for runtime tweaking, or
    // make final to have compiler strip them out entirely.
    public static boolean CONTAINMENT = false;
    public static boolean PARENTING = false;
    public static boolean LAYOUT = false;
    public static boolean BOXES = false;
    public static boolean ROLLOVER = false;
    public static boolean EVENTS = false;
    public static boolean SCROLL = false;
    public static boolean SELECTION = false;
    public static boolean FOCUS = false;
    public static boolean UNDO = false;
    public static boolean PATHWAY = false;
    public static boolean PROPERTIES = true;

    public static boolean META = false; // generic toggle to use in combination with other flags

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

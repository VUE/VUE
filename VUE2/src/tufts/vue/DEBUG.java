package tufts.vue;

public class DEBUG
{
    public static boolean Enabled = false; // can user turn debug switches on
    
    // Can leave these as static for runtime tweaking, or
    // make final to have compiler strip them out entirely.
    public static boolean CONTAINMENT = false;
    public static boolean PARENTING = false;
    public static boolean LAYOUT = false;
    public static boolean BOXES = false;
    public static boolean SELECTION = false;
    public static boolean UNDO = false;
    public static boolean PATHWAY = false;
    public static boolean DND = false; // drag & drop
    public static boolean MOUSE = false;
    public static boolean VIEWER = false; // MapViewer
    public static boolean PAINT = false; // painting
    public static boolean ROLLOVER = false; // MapViewer auto-zoom rollover
    public static boolean SCROLL = false; // MapViewer scroll-bars / scrolling
    public static boolean FOCUS = false; // AWT focus events, VUE MapViewer application focus
    public static boolean EVENTS = false; // VUE LWCEvents & Action Events
    public static boolean INIT = false; // startup / initializations
    public static boolean MARGINS = true; // turn off bounds margin adjustments for testing
    public static boolean DYNAMIC_UPDATE = false; // components process all LWCEvent's immediately

    public static boolean DR = false; // digital repository & data sources
    
    public static boolean META = false; // generic toggle to use in combination with other flags

    public static  void setAllEnabled(boolean t) {
        CONTAINMENT=PARENTING=LAYOUT=BOXES=ROLLOVER=EVENTS=
            SCROLL=SELECTION=FOCUS=UNDO=PATHWAY=DND=MOUSE=VIEWER=PAINT=MARGINS=INIT=t;
        if (t == false)
            META = false;
    }

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

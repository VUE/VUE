/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

/**
 * Global VUE application debug flags.
 * 
 * @version $Revision: 1.1 $ / $Date: 2005/12/02 15:50:23 $ / $Author: sfraize $ 
 */
public class DEBUG
{
    public static boolean Enabled = false; // can user turn debug switches on
    
    // Can leave these as static for runtime tweaking, or
    // make final static to have compiler strip them out entirely.
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
    public static boolean EVENTS = false; // VUE LWCEvents & Action Events (not tool events)
    public static boolean INIT = false; // startup / initializations
    public static boolean MARGINS = false; // turn off bounds margin adjustments for testing
    public static boolean DYNAMIC_UPDATE = false; // components process all LWCEvent's immediately
    public static boolean KEYS = false; // keyboard input
    public static boolean TOOL = false; // toolbars & tool events
    public static boolean EDGE = false; // window edges & sticking
    public static boolean IMAGE = false; // images
    public static boolean CASTOR = false; // castor persist (save/restore)
    public static boolean XML = false; // castor persist (save/restore)
    public static boolean THREAD = false; // threading
    public static boolean TEXT = false; // text objects
    public static boolean IO = false; // file and network i/o
    public static boolean DOCK = false; // DockWindow's
    public static boolean WIDGET = false; // Widget's
    public static boolean DATA = false; // data production / meta-data
    public static boolean RESOURCE = false; // Resources
    
    public static boolean WORK = true; // work-in-progress

    public static boolean DR = false; // digital repository & data sources
    
    public static boolean META = false; // generic toggle to use in combination with other flags

    public static  void setAllEnabled(boolean t) {
        Enabled=CONTAINMENT=PARENTING=LAYOUT=BOXES=ROLLOVER=EVENTS=
            SCROLL=SELECTION=FOCUS=UNDO=PATHWAY=DND=MOUSE=VIEWER=
            PAINT=MARGINS=INIT=DYNAMIC_UPDATE=KEYS=TOOL=DR=IMAGE=
            CASTOR=XML=THREAD=TEXT=EDGE=IO=DOCK=WIDGET=DATA=t;

        // only turn META & WORK off, not on
        if (t == false)
            META = WORK = false;
    }

    public static void parseArg(String a) {
             if (a.equals("-debug_meta"))       DEBUG.META = true;
        else if (a.equals("-debug_work"))       DEBUG.WORK = !DEBUG.WORK;
        else if (a.equals("-debug_init"))       DEBUG.INIT = true;
        else if (a.equals("-debug_focus"))      DEBUG.FOCUS = true;
        else if (a.equals("-debug_dr"))         DEBUG.DR = true;
        else if (a.equals("-debug_tool"))       DEBUG.TOOL = true;
        else if (a.equals("-debug_drop"))       DEBUG.DND = true;
        else if (a.equals("-debug_undo"))       DEBUG.UNDO = true;
        else if (a.equals("-debug_castor"))     DEBUG.CASTOR = true;
        else if (a.equals("-debug_xml"))        DEBUG.XML = true;
        else if (a.equals("-debug_paint"))      DEBUG.PAINT = true;
        else if (a.equals("-debug_mouse"))      DEBUG.MOUSE = true;
        else if (a.equals("-debug_keys"))       DEBUG.KEYS = true;
        else if (a.equals("-debug_layout"))     DEBUG.LAYOUT = true;
        else if (a.equals("-debug_text"))       DEBUG.TEXT = true;
        else if (a.equals("-debug_io"))         DEBUG.IO = true;
        else if (a.equals("-debug_data"))       DEBUG.DATA = true;
        else if (a.equals("-debug_selection"))  DEBUG.SELECTION = true;
        else if (a.equals("-debug_resource"))   DEBUG.RESOURCE = true;
        else if (a.startsWith("-debug_edge"))   DEBUG.EDGE = true;
        else if (a.startsWith("-debug_event"))  DEBUG.EVENTS = true;
        else if (a.startsWith("-debug_thread")) DEBUG.THREAD = true;
        else if (a.startsWith("-debug_image"))  DEBUG.IMAGE = true;
        else if (a.startsWith("-debug_box"))    DEBUG.BOXES = true;
        else if (a.startsWith("-debug_dock"))   DEBUG.DOCK = true;
        else if (a.startsWith("-debug_widget"))   DEBUG.WIDGET = true;
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

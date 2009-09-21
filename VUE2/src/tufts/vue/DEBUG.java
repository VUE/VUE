/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import java.lang.reflect.Field;

/**
 * Global VUE application debug flags.
 * 
 * @version $Revision: 1.61 $ / $Date: 2007/10/06 06:40:34 $ / $Author: sfraize $ 
 */
public class DEBUG
{
    public static boolean Enabled = false; // can user turn debug switches on

    public static boolean INIT = false; // startup / initializations
    
    // Can leave these as static for runtime tweaking, or
    // make final static to have compiler strip them out entirely.
    public static boolean CONTAINMENT;
    public static boolean PARENTING;
    public static boolean LAYOUT;
    public static boolean BOXES;
    public static boolean SELECTION;
    public static boolean UNDO;
    public static boolean PATHWAY;
    public static boolean DND; // drag & drop
    public static boolean MOUSE;
    public static boolean VIEWER; // MapViewer
    public static boolean PAINT; // painting
    public static boolean ROLLOVER; // MapViewer auto-zoom rollover
    public static boolean SCROLL; // MapViewer scroll-bars / scrolling
    public static boolean FOCUS; // AWT focus events, VUE MapViewer application focus
    public static boolean EVENTS; // VUE LWCEvents & Action Events (not tool events)
    public static boolean MARGINS; // turn off bounds margin adjustments for testing
    public static boolean DYNAMIC_UPDATE = true; // components process all LWCEvent's immediately
    public static boolean KEYS; // keyboard input
    public static boolean TOOL; // toolbars & tool events
    public static boolean EDGE; // window edges & sticking
    public static boolean IMAGE; // images
    public static boolean CASTOR; // castor persist (save/restore)
    public static boolean XML; // castor persist (save/restore)
    public static boolean THREAD; // threading
    public static boolean TEXT; // text objects
    public static boolean IO; // file and network i/o
    public static boolean DOCK; // DockWindow's
    public static boolean WIDGET; // Widget's
    public static boolean DATA; // data production / meta-data
    public static boolean RESOURCE; // Resources
    public static boolean PRESENT;
    public static boolean NAV; // presentation non-linear navigation
    public static boolean PICK;
    public static boolean LISTS; //for debugging UL and OL in HTML textbox code
    public static boolean LINK; // for LWLinks
    public static boolean STYLE; // for Styles
    public static boolean HTML; // for Styles
    public static boolean WEBSHOTS; // for Styles
    public static boolean PDF; // for PDF output
    public static boolean PROPERTY;
    public static boolean IM;
    //If you set LISTS to true you'll get the HTML code for the node in the Info Label
    //instead of the rendered HTML this should be useful for debugging, at least I hope so.
    //see my note in InspectorPane for more info. -MK
    public static boolean WORK; // work-in-progress

    public static boolean DR; // digital repository & data sources
    
    public static boolean META; // generic toggle to use in combination with other flags
    
    public static boolean TRACE; // enhanced (yet slow) log4j logger tracing
    
    public static boolean RDF;
    
    public static boolean PERF; // performance
    
    public static boolean SCHEMA; // schema's
    
    public static boolean QUARTILE; // quartile import

    public static  void setAllEnabled(boolean enabled) {
//         Enabled=CONTAINMENT=PARENTING=LAYOUT=BOXES=ROLLOVER=EVENTS=
//             SCROLL=SELECTION=FOCUS=UNDO=PATHWAY=DND=MOUSE=VIEWER=
//             PAINT=MARGINS=INIT=DYNAMIC_UPDATE=KEYS=TOOL=DR=IMAGE=
//             CASTOR=XML=THREAD=TEXT=EDGE=IO=DOCK=WIDGET=DATA=PRESENT=
//             PICK=LINK=STYLE=NAV=HTML=WEBSHOTS=PDF=TRACE=PROPERTY=PERF=SCHEMA=t;

        for (Field f : Fields)
            setFlag(f, enabled);

        // only turn META & WORK off, not on
        if (enabled == false)
            META = WORK = false;
    }

    public static void parseArg(String arg) {
        if (arg == null || !arg.toLowerCase().startsWith("-debug"))
            return;

        if (arg.length() < 7)
            return;

        arg = arg.substring(6);

        //System.out.println("parsing arg group[" + arg + "]");

        String[] args;

        if (arg.charAt(0) == ':')
            args = arg.substring(1).split(",");
        else if (arg.charAt(0) == '_')
            args = new String[] { arg.substring(1) };
        else
            return;

        for (int i = 0; i < args.length; i++) {
            String a = args[i].toUpperCase();

            boolean handled = false;
            for (Field f : Fields) {
                if (f.getName().toUpperCase().startsWith(a)) {
                    try {
                        f.setBoolean(null, true);
                        handled = true;
                        System.out.println(DEBUG.class.getName() + " enable: " + f.getName());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                
            }
            if (!handled)
                System.err.format("%s didn't understand debug flag \"%s\"\n", DEBUG.class.getName(), args[i]);
        }
    }

    private static void setFlag(Field f, boolean enabled) {
        try {
            f.setBoolean(null, enabled);
            System.out.println(DEBUG.class.getName() + " set: " + f.getName() + " = " + enabled);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

//         for (int i = 0; i < args.length; i++) {
//             String a = args[i].toLowerCase();
//             //System.out.println("parsing arg [" + a + "]");
//                  if (a.equals("meta"))       DEBUG.META = true;
//             else if (a.equals("work"))       DEBUG.WORK = !DEBUG.WORK;
//             else if (a.equals("init"))       DEBUG.INIT = true;
//             else if (a.equals("focus"))      DEBUG.FOCUS = true;
//             else if (a.equals("dr"))         DEBUG.DR = true;
//             else if (a.equals("tool"))       DEBUG.TOOL = true;
//             else if (a.equals("drop"))       DEBUG.DND = true;
//             else if (a.equals("undo"))       DEBUG.UNDO = true;
//             else if (a.equals("castor"))     DEBUG.CASTOR = true;
//             else if (a.equals("xml"))        DEBUG.XML = true;
//             else if (a.equals("paint"))      DEBUG.PAINT = true;
//             else if (a.equals("mouse"))      DEBUG.MOUSE = true;
//             else if (a.equals("keys"))       DEBUG.KEYS = true;
//             else if (a.equals("layout"))     DEBUG.LAYOUT = true;
//             else if (a.equals("text"))       DEBUG.TEXT = true;
//             else if (a.equals("io"))         DEBUG.IO = true;
//             else if (a.equals("data"))       DEBUG.DATA = true;
//             else if (a.equals("selection"))  DEBUG.SELECTION = true;
//             else if (a.equals("resource"))   DEBUG.RESOURCE = true;
//             else if (a.equals("scroll"))     DEBUG.SCROLL = true;
//             else if (a.equals("pick"))       DEBUG.PICK = true;
//             else if (a.startsWith("parent")) DEBUG.PARENTING = true;
//             else if (a.startsWith("contain"))DEBUG.CONTAINMENT = true;
//             else if (a.startsWith("path"))   DEBUG.PATHWAY = true;
//             else if (a.startsWith("edge"))   DEBUG.EDGE = true;
//             else if (a.startsWith("event"))  DEBUG.EVENTS = true;
//             else if (a.startsWith("thread")) DEBUG.THREAD = true;
//             else if (a.startsWith("image"))  DEBUG.IMAGE = true;
//             else if (a.startsWith("box"))    DEBUG.BOXES = true;
//             else if (a.startsWith("dock"))   DEBUG.DOCK = true;
//             else if (a.startsWith("widget")) DEBUG.WIDGET = true;
//             else if (a.startsWith("pres"))   DEBUG.PRESENT = true;
//             else if (a.startsWith("nav"))    DEBUG.NAV = true;
//             else if (a.startsWith("link"))   DEBUG.LINK = true;
//             else if (a.startsWith("style"))  DEBUG.STYLE = true;
//             else if (a.startsWith("rdf")) DEBUG.RDF = true;
//             else if (a.startsWith("pdf"))  DEBUG.PDF = true;
//             else if (a.startsWith("trace"))  DEBUG.TRACE = true;
//             else if (a.startsWith("prop"))  DEBUG.PROPERTY = true;
//             else if (a.startsWith("perf"))  DEBUG.PERF = true;
//             else if (a.startsWith("schema"))  DEBUG.SCHEMA = true;
//             else if (a.startsWith("im"))  DEBUG.IM = true;
//         }

    private static final Field[] Fields = DEBUG.class.getFields();

    public static void main(String[] args)
    {
        for (Field f : Fields) {
            System.out.format("%-26s %s\n", tufts.Util.tags(f.getName()), f);
        }
        
    }
}

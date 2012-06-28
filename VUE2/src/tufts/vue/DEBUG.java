/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DEBUG.class);

    public static boolean Enabled; // can user turn debug switches on

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
    public static boolean SINGLE_THREAD; // where can, run with as few threads as possible for debugging (e.g., image loaders)
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
    public static boolean TWITTER=false;
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
    
    public static boolean ANNOTATE; // data annotations
    
    public static boolean SEARCH; // overlaps with RDF
    
    public static boolean MERGE; // Merge Maps

    public static  void setAllEnabled(boolean enabled) {
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

            String symbol = args[i];
            boolean handled = false;
            
            if (symbol.charAt(0) == '+') {
                symbol = symbol.substring(1);
                Log.info("attempting debug for class " + symbol);
                Class clazz;
                try {
                    clazz = Class.forName(symbol);
                    Log.info("found: " + clazz);
                    try {
                        Field debugField = clazz.getDeclaredField("DEBUG");
                        Log.info("found: " + debugField);
                        if (setFlag(debugField, true))
                            handled = true;
                    } catch (Exception e) {
                        Log.info(e);
                    }
                    if (false)  {
                        // currently meaningless as we already set to Level.DEBUG for all classes
                        // tufts.vue.* and edu.tufts.*, where we use the "Log" convention, and
                        // it's also normally private, not public.  Could try changing this
                        // to looking up the logger itself via class name.
                        Field logField = clazz.getDeclaredField("Log");
                        Log.info("found: " + logField);
                        //org.apache.log4j.Logger log = logField.get(null);
                        ((org.apache.log4j.Logger) logField.get(null))
                            .setLevel(org.apache.log4j.Level.DEBUG);
                    }
                } catch (Exception e) {
                    Log.info(e);
                }
            } else {
                symbol = symbol.toUpperCase();
                for (Field f : Fields) {
                    if (f.getName().toUpperCase().startsWith(symbol) && setFlag(f, true)) {
                        handled = true;
                        break;
                    }
                }
            }
            if (!handled)
                Log.info(String.format("couldn't handle debug flag \"%s\"", args[i]));
        }
    }

    private static boolean setFlag(Field f, boolean enabled) {
        boolean success = false;
        try {
            f.setBoolean(null, enabled);
            success = true;
            Log.info(f.getDeclaringClass().getName() + "/" + f.getName() + " = " + enabled);
            //Log.info(f.getName() + " = " + enabled);
        } catch (IllegalAccessException e) {
            Log.warn("reflection problem", e);
        }
        return success;
    }

    private static final Field[] Fields = DEBUG.class.getFields();

    public static void main(String[] args)
    {
        for (Field f : Fields) {
            System.out.format("%-26s %s\n", tufts.Util.tags(f.getName()), f);
        }
        
    }
}

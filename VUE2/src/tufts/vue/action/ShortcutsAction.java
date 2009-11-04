package tufts.vue.action;

import tufts.Util;
import tufts.vue.VUE;
import tufts.vue.DEBUG;
import tufts.vue.VueResources;
import tufts.vue.VueTool;
import tufts.vue.VueAction;
import tufts.vue.Actions;
import tufts.vue.VueConstants;
import tufts.vue.VueToolbarController;
import tufts.vue.gui.GUI;
import tufts.vue.gui.DockWindow;

import java.awt.Event;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.JScrollPane;

/**
 * Produce a shortcuts window.
 *
 * @version $Revision: 1.18 $ / $Date: 2009-11-04 18:53:10 $ / $Author: mike $
 * @author Scott Fraize
 */
public class ShortcutsAction extends tufts.vue.VueAction
{
    private static DockWindow window;

    public ShortcutsAction() {
        super(VueResources.getString("shortcutactions.keyboardshortcuts"));
    }
    
    @Override
    public boolean isUserEnabled() { return true; }
        
    private boolean wasDebug;
    private JComponent content;

    /** display the shortcuts DockWindow (create it if needed) */
    public void act() {
        if (window == null)
            window = GUI.createDockWindow(VUE.getName() + " " + VueResources.getString("shortcutactions.shortcutkeys"));

        if (content == null || (wasDebug != DEBUG.Enabled)) {
            wasDebug = DEBUG.Enabled;
            if (DEBUG.Enabled)
                tufts.vue.VueAction.checkForDupeStrokes();
            content = buildShortcutsComponent();
            window.setContent(content);
        }
        window.pack(); // fit to widest line
        GUI.makeVisibleOnScreen(content);
      //  window.setVisible(true);
        window.raise();
    }

    private static String keyCodeChar(int keyCode) {
        return keyCodeChar(keyCode, false);
    }
        
    private static String keyCodeChar(int keyCode, boolean lowerCase) {
            
        if (lowerCase && keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
            return String.valueOf((char)keyCode).toLowerCase();
        }
            
        if (keyCode == KeyEvent.VK_OPEN_BRACKET)
            return "[";
        else if (keyCode == KeyEvent.VK_CLOSE_BRACKET)
            return "]";
        else if (keyCode == 0)
            return "";
        else if (Util.isMacLeopard()) {
            if (keyCode == KeyEvent.VK_SPACE) // override the little "u" glyph
                return "Space";
            else if (keyCode == KeyEvent.VK_BACK_QUOTE) // override the hard to see '`'
                return "Back Quote";
        }

        return KeyEvent.getKeyText(keyCode);
    }

    static StringBuffer html;

    private static int BOLD = 1;
    private static int ITAL = 2;
    private static int RIGHT = 4;
    private static int CENTER = 8;
    private static int SPAN2 = 16;
    private static int SPAN3 = 32;
    private static int NO_EAST_GAP = 64;
    private static int NO_WEST_GAP = 128;
    private static int NO_GAP = NO_EAST_GAP + NO_WEST_GAP;

    private static final String NBSP = "&nbsp;";
    //private static final String NBSP = "X";
    private static final String BIG_NBSP =
        " " + NBSP
        + " " + NBSP
        + " " + NBSP
        //+ " " + NBSP // these if tool lines are bold on Leopard
        //+ " " + NBSP
        //+ " " + NBSP
        ;
;
        
    private static void add(int bits, Object o, String... attr) {

        html.append("<td");

        if ((bits & SPAN2) != 0)
            html.append(" colspan=2");
        else if ((bits & SPAN3) != 0)
            html.append(" colspan=3");
            
        if ((bits & CENTER) != 0) // CENTER takes priority over RIGHT
            html.append(" align=center");
        else if ((bits & RIGHT) != 0)
            html.append(" align=right");

        if (attr != null) {
            for (String s : attr) {
                html.append(' ');
                html.append(s);
            }
        }
        html.append('>');
        
        if ((bits & NO_WEST_GAP) == 0)
            html.append(NBSP);

        if ((bits & BOLD) != 0) html.append("<b>");
        if ((bits & ITAL) != 0) html.append("<i>");
                
        html.append(o == null ? (DEBUG.Enabled?"null":"") : o.toString());
            
   //      if ((bits & BOLD) != 0) html.append("</b>"); // implied
    //     if ((bits & ITAL) != 0) html.append("</i>"); // implied
            
        if ((bits & NO_EAST_GAP) == 0)
            html.append(NBSP);
	 //MK : enable for file output
     //   html.append("</td>"); // implied
    }
    private static void add(Object o) {
        add(0, o);
    }

    private static void addRow(int row) {
        addRow(row, false);
    }
    
    private static void addRow(int row, boolean debug) {

		// MK - user this comment rest for file output
        html.append("\n<tr");
        
        if (debug) {
            html.append(" bgcolor=#FF0000");
        } else if (row % 2 == 0) {
            if (Util.isMacPlatform()) {
                if (DEBUG.Enabled)
                    html.append(" bgcolor=#DDDDFF");
                else
                    html.append(" bgcolor=#EEEEEE"); // VUE-1036
            } else {
                html.append(" bgcolor=#FFFFFF");
            }
        } else {
            html.append('>');
        }
    }


    private static void addTable(String... attr) {

        html.append('\n');
        html.append("<table");
        html.append(" cellpadding=2");
        
        if (DEBUG.Enabled)
            html.append(" border=1");
        else
            html.append(" border=0");

        //html.append(" rules=rows bordercolor=red"); // Java does not support these

        if (attr != null) {
            for (String s : attr) {
                html.append(' ');
                html.append(s);
            }
        }
        html.append('>');
        html.append('\n');
        
    }
        
        

    private static final int SHIFT = Event.SHIFT_MASK + InputEvent.SHIFT_DOWN_MASK;
    private static final int CTRL = Event.CTRL_MASK + InputEvent.CTRL_DOWN_MASK;
    private static final int ALT = Event.ALT_MASK + InputEvent.ALT_DOWN_MASK;

    private static boolean hasOnlyShift(int mods)   { return (mods & SHIFT) == (mods | SHIFT); }
    private static boolean hasOnlyCtrl(int mods)    { return (mods & CTRL) == (mods | CTRL); }
    private static boolean hasOnlyAlt(int mods)     { return (mods & ALT) == (mods | ALT); }
        
    private static boolean hasOnlyOne(int mods) {
        return hasOnlyShift(mods) || hasOnlyAlt(mods) || hasOnlyCtrl(mods);
    }

    private static final String TitleColor = "#C5C5C5"; // As per VUE-1036

        
    static JComponent buildShortcutsComponent()
    {
        if (html == null) {
            if (DEBUG.Enabled)
                html = new StringBuffer(65536);
            else
                html = new StringBuffer(8192);
        }
        html.setLength(0);
        html.append("<html>");
            
        addTable("width=100%"); // fill to wider width of actions below
        //addTable();
            
        int row = 0;
            
        if (DEBUG.Enabled) {
                
            //=============================================================================
            // DEBUG TOOLS Title/Header Line
            //=============================================================================
                
            html.append("<tr bgcolor=#00FFFF>");
            add(BOLD+ITAL, "TOOL ID");
            add(BOLD+ITAL, "ShortCut");
            add(BOLD+ITAL, "DownKey");
            add(BOLD+ITAL, "Name");
            add(BOLD+ITAL, VueTool.class);
            html.append("</tr>");
                
        } else {

            //=============================================================================
            // Production TOOLS Title/Header Line
            //=============================================================================

            html.append("<tr bgcolor=" + TitleColor + ">");
            add(BOLD, VueResources.getString("shortcutactions.tool"));
            add(BOLD+CENTER, VueResources.getString("shortcutactions.key"));
            add(BOLD+CENTER, VueResources.getString("shortcutactions.auick-key"));
            html.append("</tr>");
                
        }

        for (VueTool t : VueTool.getTools()) {

            if (t.getShortcutKey() == 0)
                continue;

            final char downKey = (char) t.getActiveWhileDownKeyCode();

            addRow(row++);
                
            if (DEBUG.Enabled) {
                    
                //-------------------------------------------------------
                // DEBUG TOOLS 
                //-------------------------------------------------------

                add(t.getID());
                add(BOLD+CENTER, t.getShortcutKey());
                add(BOLD+CENTER, keyCodeChar(downKey));
                //add(BOLD+CENTER, KeyStroke.getKeyStroke((char)downKey));
                add(BOLD, t.getToolName());
                add(t.getClass().getName());
                //MK - enable for file output
                //html.append("</tr>");
                
            } else {

                //=======================================================
                // Production TOOLS 
                //=======================================================
                
                add(t.getToolName());
                
                add(CENTER, t.getShortcutKey());

                if (downKey == 0)
                    add("");
                else
                    add(CENTER, keyCodeChar(downKey, true));
                
                //add(BOLD+CENTER, t.getShortcutKey(), "bgcolor=black color=white");
                //MK - enable for fileoutput
                //html.append("</tr>");
            }
        }

        html.append("</table><br>");

        addTable();
        
        if (DEBUG.Enabled) {

            //-------------------------------------------------------
            // DEBUG ACTION Title/Header Line
            //-------------------------------------------------------
                
            //                 html.append("</table><p>");
            //                 addTable(0);

            html.append("<tr bgcolor=#00FFFF>");
            add(BOLD+ITAL, "row");
            add(BOLD+ITAL, "mod bits");
            add(BOLD+ITAL, "mod text");
            if (Util.isMacLeopard()) add(BOLD, "<font size=-3>Leopard<br>&nbsp;Glyphs");
            add(BOLD+ITAL+CENTER, "Key");
            add(BOLD+ITAL, "ACTION NAME");
            add(BOLD+ITAL, KeyStroke.class);
            add(BOLD+ITAL, VueAction.class);
            html.append("</tr>");
        } else {

            //=======================================================
            // Production ACTION Title/Header Line
            //=======================================================
                
            html.append("<tr bgcolor=" + TitleColor + ">");

            if (Util.isMacLeopard())
                add(NO_GAP, " " + NBSP + " " + NBSP + " " + NBSP + " ");

            add(BOLD, VueResources.getString("shortcutactions.action"));
            
            if (Util.isMacLeopard())
                add(BOLD+SPAN3+NO_WEST_GAP, VueResources.getString("shortcutactions.macshortcut"));
            else 
                add(BOLD+SPAN2, VueResources.getString("shortcutactions.shortcutkey"));
            
            html.append("</tr>");
                
        }

        //=============================================================================
        // Find all short-cuts
        //=============================================================================

        
        if (DEBUG.Enabled) {
        
            //------------------------------------------------------------------
            //
            // Find all javax.swing.AbstractButton's in the AWT hierarchy (which
            // includes allJMenuItem's), and record any accelerator found on their
            // action.
            //
            // todo: don't just check action: check the actual accelerator keys on the
            // JMenuItems (could someday even check all input maps?)
            //
            //------------------------------------------------------------------

            new tufts.vue.EventRaiser<AbstractButton>
                (ShortcutsAction.class,
                 AbstractButton.class,
                 tufts.vue.EventRaiser.INCLUDE_MENUS)
                {
                    int row = 0;
                    public void dispatch(AbstractButton b) {
                        if (reportKeyStroke(b.getAction(), row))
                            row++;
                    }
                }
            .raiseStartingAt(VUE.getRootWindow());
            

            addRow(0, true);

            // todo: keep map of all found here, and below, if NOT found in
            // that hash map, debug-mode report the "dangling" vue-action
            // (not found in the UI)

        }
            
        //------------------------------------------------------------------
        // Find accelerator keys set in any instaniated VueActions
        // This is the meat of everything we do here.
        //------------------------------------------------------------------
        
        row = 0;
        for (VueAction a : VueAction.getAllActions())
            if (reportKeyStroke(a, row))
                row++;

            
        //=============================================================================
        // Load up the produced HTML text
        //=============================================================================
        
        final javax.swing.JLabel t = new javax.swing.JLabel();
            
//         if (DEBUG.Enabled)
//             t.setFont(VueConstants.LargeFont);
//         else
            t.setFont(VueConstants.MediumFont);

        if (DEBUG.Enabled) Log.debug("HTML size: " + ShortcutsAction.html.length());
         t.setText(html.toString());
        /*
		Since this list can't be cut and pasted from the screen
		added this to dump it as a file as necessary so we can keep
		a list on the wiki
		FileOutputStream fos;
		try {
			fos = new FileOutputStream("/Users/mkorcy01/Desktop/mike2.html");
			try {
				fos.write(html.toString().getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        */
        t.setOpaque(false);
        //t.setFocusable(false);
       
        return new JScrollPane(t,
                               JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                               JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                               );
        
    }


    /** if a keystroke is found on the action, add another HTML table-data line for it */
    private static boolean reportKeyStroke(Action a, int row)
    {
        if (a == null)
            return false;
        
        KeyStroke k = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
        if (k == null && !(DEBUG.Enabled && DEBUG.WORK))
            return false;
                
        String modNames = "";

        if (k != null) {
            modNames = KeyEvent.getKeyModifiersText(k.getModifiers());
            //if (modNames != null && modNames.length() > 0)
            //modNames += " ";
        }

        if (DEBUG.Enabled) {
            if (tufts.vue.VueAction.isDupeStrokeAction(a))
                addRow(row, true);
            else
                addRow(row, false);
        } else {
            addRow(row);
        }

        row++;
                    
        final int mods = k == null ? 0 : k.getModifiers();
        int goRight = hasOnlyOne(mods) ? RIGHT : 0;

        if (goRight != 0 && (mods & Actions.COMMAND) != 0) // not for the platform primary
            goRight = 0;
                    
        if (DEBUG.Enabled) {

            //-----------------------------------------------------------------------------
            // DEBUG ACTIONS
            //-----------------------------------------------------------------------------
                    
            add(RIGHT, row);
            if (k == null) {
                add("");
                add("");
                if (Util.isMacPlatform()) add("");
                add("");                        
            } else {
                add(RIGHT+BOLD, Integer.toBinaryString(mods));

                if (Util.isMacLeopard()) {
                    add(BOLD+goRight, get_MacOSX_Leopard_Modifier_Names(mods));
                    add(BOLD+goRight+(DEBUG.Enabled?0:CENTER), KeyEvent.getKeyModifiersText(mods));
                } else {
                    add(BOLD+goRight, KeyEvent.getKeyModifiersText(mods)); 
                }
                add(BOLD+CENTER, keyCodeChar(k.getKeyCode()));
            }

            if (a instanceof VueAction)
                add(BOLD, ((VueAction)a).getPermanentActionName());
            else
                add(BOLD+ITAL, (String) a.getValue(Action.NAME));
                
            add(k == null ? "" : k);
            add(a.getClass().getName());

        } else {

            //=============================================================================
            // Production ACTIONS
            //=============================================================================
                    
            if (Util.isMacLeopard())
                add(NO_GAP, "");
                
            if (a instanceof VueAction)
                add(((VueAction)a).getPermanentActionName());
            else
                add(ITAL, (String) a.getValue(Action.NAME));
                
            if (Util.isMacLeopard()) {
                add(NO_GAP + goRight, get_MacOSX_Leopard_Modifier_Glyphs(mods));
                add(NO_GAP, keyCodeChar(k.getKeyCode()));
            } else {
                add(goRight, KeyEvent.getKeyModifiersText(mods));
                add(NO_WEST_GAP, keyCodeChar(k.getKeyCode()));
            }

            if (row == 1 && Util.isMacLeopard()) 
                add(NO_GAP, BIG_NBSP); // for colspan 3 in header
                
            //=============================================================================

        }

        return true;

    }

    

    /** @return a standard, short and unqiue description of the given KeyStroke */
    public static String getDescription(KeyStroke k) {

        if (Util.isMacLeopard()) {
            //return get_MacOSX_Leopard_Modifier_Names(k.getModifiers()) + "+" + keyCodeChar(k.getKeyCode()); // longest
            //return KeyEvent.getKeyModifiersText(k.getModifiers()) + "+" + keyCodeChar(k.getKeyCode()); // much shorter
            return get_MacOSX_Leopard_Modifier_Glyphs(k.getModifiers()) + keyCodeChar(k.getKeyCode()); // shortest
        } else {
            return KeyEvent.getKeyModifiersText(k.getModifiers()) + "+" + keyCodeChar(k.getKeyCode());
        }
        
    }

    // The Mac OSX Leopard JVM impl changed KeyEvent.getKeyModifiersText(mods) to return the actual
    // special mac glyphs representing these keys.  This replaces the old functionality
    // (swiped from the java source), in case we want to use it.
    private static String get_MacOSX_Leopard_Modifier_Names(int modifiers) {
        StringBuffer buf = new StringBuffer();
        if ((modifiers & InputEvent.META_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.meta", "Meta"));
            //buf.append("Command");
            buf.append("Apple");
            buf.append("+");
        }
        if ((modifiers & InputEvent.CTRL_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.control", "Ctrl"));
            buf.append("Ctrl");
            buf.append("+");
        }
        if ((modifiers & InputEvent.ALT_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.alt", "Alt"));
            buf.append("Alt");
            buf.append("+");
        }
        if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.shift", "Shift"));
            buf.append("Shift");
            buf.append("+");
        }
        if ((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.altGraph", "Alt Graph"));
            buf.append("Alt Graph");
            buf.append("+");
        }
        if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.button1", "Button1"));
            buf.append("Button1");
            buf.append("+");
        }
        if (buf.length() > 0) {
            buf.setLength(buf.length()-1); // remove trailing '+'
        }
        return buf.toString();
    }
    

    private static String get_MacOSX_Leopard_Modifier_Glyphs(int modifiers) {
        return KeyEvent.getKeyModifiersText(modifiers).replace('+', (char)0);
    }
            
    

    
    public static void main(String args[])
    {
        VUE.init(args);

        // Ensure the tools are loaded to we can see their shortcuts:
        VueToolbarController.getController();
        
        javax.swing.JFrame frame = new javax.swing.JFrame("vueParentWindow");

        // Ensure that all the Actions are instantiated so we can see them:
        tufts.vue.Actions.Delete.toString();
	         
        // Let us see the actual menu bar:
        frame.setJMenuBar(new tufts.vue.gui.VueMenuBar());
        frame.setVisible(true); // do this or we can't see the menu bar

        new ShortcutsAction().act();

        //         Log.info("creating...");
        //         DockWindow shortcuts = ShortcutsAction.createWindow();
        //         Log.info("showing...");
        //         shortcuts.pack(); // fit to HTML content
        //         shortcuts.setVisible(true);

        if (args.length > 1) {
            System.out.println(ShortcutsAction.html);
            System.out.println("length=" + ShortcutsAction.html.length());
        }

    }
}
    

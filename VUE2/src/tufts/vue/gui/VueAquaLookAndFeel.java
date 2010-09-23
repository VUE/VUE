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
package tufts.vue.gui;

import tufts.vue.VUE;
import tufts.vue.VueConstants; // todo: rename GUI constants & move to GUI
import tufts.vue.DEBUG;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

      
// We can find the class apple.laf.AquaLookAndFeel on Mac OS X systems in:
// /System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Classes/laf.jar
// We've extracted just the AquaLookAndFeel class and put it in lib/apple-laf.jar
// so this will compile on other platforms.  Note that apple-laf.jar may need
// to be updated for new releases of the JVM on the Mac.  Currently we're using
// the classes from JVM 1.4.2.

/**
 * We extend apple.laf.AquaLookAndFeel and install it as our own
 * look and feel on the mac in order to override certian items,
 * such as the default font sizes.  Note that in order for Aqua
 * tabbed-pane's to look right, we have to have an "icons"
 * directory at the same level as this class containing Right.gif,
 * Left.gif and Both.gif, which are needed for Aqua tabbed-pane's
 * when there isn't enough room to display all the tabs.
 *
 * @version $Revision: 1.10 $ / $Date: 2010-02-03 19:15:47 $ / $Author: mike $ 
 */
// superclass of AquaJideLookAndFeel is apple.laf.AquaLookAndFeel
//public class VueAquaLookAndFeel extends com.jidesoft.plaf.aqua.AquaJideLookAndFeel { // JIDE
public class VueAquaLookAndFeel extends apple.laf.AquaLookAndFeel
{
    public final static Font SystemFont = new Font("Lucida Grande", Font.PLAIN, 13);
    public final static Font SmallSystemFont = new Font("Lucida Grande", Font.PLAIN, 11);
    public final static Font SmallSystemFont12 = new Font("Lucida Grande", Font.PLAIN, 12);
    public final static Font MiniSystemFont = new Font("Lucida Grande", Font.PLAIN, 9);
    public final static Font EmphasizedMiniSystemFont = new Font("Lucida Grande", Font.BOLD, 9);

    public String getDescription() { return super.getDescription() + " (VUE Derivative)"; }
    public void initComponentDefaults(UIDefaults table)
    {
        super.initComponentDefaults(table);
        //table.put("TitledBorder.font", fontMedium.deriveFont(Font.BOLD));
        //table.put("Button.font", getFont());

        Font font = table.getFont("Label.font");
        //System.out.println("GUI: default label font: " + table.getFont("Label.font"));
        font = makeFont(font.deriveFont(11f));
        //System.out.println(font);
            
        table.put("Label.font", font);
        table.put("Label.foreground", new ColorUIResource(new Color(61, 61, 61)));
        table.put("TextField.font", font);
        table.put("TextArea.font", font);
        table.put("TextPane.font", font);
        table.put("Tree.font", font);
        table.put("Table.font", font);

        //table.put("TextArea.border", table.get("TextField.border"));
        //table.put("TextPane.border", table.get("TextField.border"));


        Font menuFont = table.getFont("MenuItem.font");
        //System.out.println("GUI: default menu item font " + table.getFont("MenuItem.font"));
        menuFont = menuFont.deriveFont(13f);
        table.put("MenuItem.font", menuFont);
        //table.put("MenuItem.foreground", new ColorUIResource(Color.red));
        
        //table.put("TableHeader.font", font);

        //table.put("ComboBox.font", font);

        //table.put("ToolBarUI", "tufts.vue.VueAquaLookAndFeel$ToolBarUI");

        // the background colors seem to be having no effect -- only foreground:
        // background -- appears to work only when manually calling
        // getCaret().setSelectionVisible as we do in our hacked KeyboardFocusManager.
        if (false)
            table.put("textHighlight", new javax.swing.plaf.ColorUIResource(255, 255, 0));
        //table.put("TextField.selectionBackground", new javax.swing.plaf.ColorUIResource(0, 0, 255));
        //table.put("TextField.selectionForeground", new javax.swing.plaf.ColorUIResource(0, 255, 0));

        /*
        // from JIDE  com.jidesoft.plaf.basic.BasicCommandBarTitleBarUI:
        UIManager.getInt("CommandBar.titleBarSize");
        UIManager.getInt("CommandBar.titleBarButtonGap");
        UIManager.getColor("CommandBar.titleBarBackground");
        UIManager.getColor("CommandBar.titleBarForeground");
        UIManager.getFont("CommandBar.titleBarFont");
        */

        // Settings for JIDE components:
        if (false) {
            table.put("CommandBarUI", "tufts.vue.VueAquaLookAndFeel$CommandBarUI");
            table.put("CommandBarTitleBarUI", "tufts.vue.VueAquaLookAndFeel$FloatingToolbarTitleUI");
            table.put("CommandBar.titleBarSize", new Integer(8));
            table.put("CommandBar.titleBarBackground", SystemColor.control);
            //table.put("CommandBar.titleBarBackground", new javax.swing.plaf.ColorUIResource(SystemColor.window));
            //table.put("CommandBar.titleBarBackground", SystemColor.window); // does NOT work as a ColorUIResource for some reason
            table.put("CommandBar.titleBarForeground", new javax.swing.plaf.ColorUIResource(Color.black));
            table.put("CommandBar.titleBarFont", font);
        }
            

        //System.out.println(table);

        //System.out.println("Created " + this + " pp=" + getClass().getSuperclass().getSuperclass()); 


        /*
          Object newFolderIcon = LookAndFeel.makeIcon(getClass(), "icons/NewFolder.gif");
          Object upFolderIcon = LookAndFeel.makeIcon(getClass(), "icons/UpFolder.gif");
          Object homeFolderIcon = LookAndFeel.makeIcon(getClass(), "icons/HomeFolder.gif");
          Object detailsViewIcon = LookAndFeel.makeIcon(getClass(), "icons/DetailsView.gif");
          Object listViewIcon = LookAndFeel.makeIcon(getClass(), "icons/ListView.gif");
          Object directoryIcon = LookAndFeel.makeIcon(getClass(), "icons/Directory.gif");
          Object fileIcon = LookAndFeel.makeIcon(getClass(), "icons/File.gif");
          Object computerIcon = LookAndFeel.makeIcon(getClass(), "icons/Computer.gif");
          Object hardDriveIcon = LookAndFeel.makeIcon(getClass(), "icons/HardDrive.gif");
          Object floppyDriveIcon = LookAndFeel.makeIcon(getClass(), "icons/FloppyDrive.gif");
        */
    }

    /*
      public static Object makeIcon(final Class baseClass, final String gifFile) {
      System.err.println("VueAquaLookAndFeel.makeIcon " + baseClass + " " + gifFile);
      return apple.laf.AquaLookAndFeel.makeIcon(baseClass, gifFile);
      }
    */
        
    private static javax.swing.plaf.FontUIResource makeFont(Font font) {
        return new javax.swing.plaf.FontUIResource(font);
    }
    private javax.swing.plaf.FontUIResource getFont() {
        return new javax.swing.plaf.FontUIResource(VueConstants.MediumFont);
    }

    /** this only matters for java 1.5 */
    public static void installProperty(JComponent c, String propertyName, Object propertyValue) {
        // will need to reflect for 1.4 compilation to work
        //super.installProperty(c, propertyName, propertyValue);
    }
    
    /*
      apparently unused in apple.laf.AquaLookAndFeel
    public static void installBorder(JComponent c, String borderName) {
        if (DEBUG.INIT) System.out.println("VueAquaLookAndFeel: installBorder " + borderName + " on " + c);
        apple.laf.AquaLookAndFeel.installBorder(c, borderName);
    }
    */

    /*
    public static class CommandBarUI extends com.jidesoft.plaf.basic.BasicCommandBarUI {
        public static javax.swing.plaf.ComponentUI createUI(JComponent x) {
            CommandBarUI ui = new CommandBarUI();
            return ui;
        }

        public Component getTitleBar() {
            Component c = super.getTitleBar();
            System.out.println("getTitleBar=" + c);
            c.setBackground(Color.red);
            return c;
        }
        public Component getGripper() {
            Component c = super.getGripper();
            System.out.println("getGripper=" + c);
            return c;
        }
    }

    public static class FloatingToolbarTitleUI extends com.jidesoft.plaf.basic.BasicCommandBarTitleBarUI {
        public FloatingToolbarTitleUI(com.jidesoft.action.CommandBarTitleBar titleBar) {
            super(titleBar);
            //titleBar.setTitle("l");
            //titleBar.setTitle(titleBar.getTitle() + " (hacked)");
            //super._title = null;
            //super._title = new JLabel("foo");
        }
        public static javax.swing.plaf.ComponentUI createUI(JComponent x) {
            FloatingToolbarTitleUI ui = new FloatingToolbarTitleUI((com.jidesoft.action.CommandBarTitleBar) x);
            //com.jidesoft.plaf.basic.BasicCommandBarTitleBarUI ui =
            //(com.jidesoft.plaf.basic.BasicCommandBarTitleBarUI) = super.createUI(x);
            System.out.println("Created FloatingToolbarTitleUI for " + x);
            return ui;
        }

        protected java.awt.LayoutManager createLayout() {
            java.awt.LayoutManager mgr = super.createLayout();
            System.out.println("createLayout = " + mgr);
            return mgr;
        }

    }
    */

        
//     public static class ToolBarUI extends javax.swing.plaf.basic.BasicToolBarUI {

//         public static javax.swing.plaf.ComponentUI createUI( JComponent x ) {
//             System.out.println("\nCREATEUI\n");
//             javax.swing.plaf.ComponentUI o = new ToolBarUI();
//             if (true||DEBUG.INIT) System.out.println("Created " + o.getClass());
//             return o;
//         }

//         protected RootPaneContainer createFloatingWindow(JToolBar toolbar) {
//             System.err.print("\nCREATE FLOATING WINDOW = ");
//             //RootPaneContainer rpc = super.createFloatingWindow(toolbar);
//             RootPaneContainer rpc = GUI.createDockWindow("Test ToolBar");
//             //RootPaneContainer rpc = VUE.createToolPalette("Test ToolBar").getRootPaneContainer();
//             System.err.println(rpc);
//             return rpc;
//         }

//         protected JFrame createFloatingFrame(JToolBar toolbar) {
//             System.err.print("\nCREATE FLOATING FRAME = ");
//             JFrame f = super.createFloatingFrame(toolbar);
//             System.err.println(f);
//             return f;
//         }

//         protected DragWindow createDragWindow(JToolBar toolbar) {
//             System.err.print("\nCREATE DRAG WINDOW = ");
//             DragWindow dw = super.createDragWindow(toolbar);
//             System.err.println(dw);
//             return dw;
//         }
//     }
}

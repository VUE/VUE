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

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

class VueTheme extends javax.swing.plaf.metal.DefaultMetalTheme
{
    private static VueTheme singleton;
    public static VueTheme getTheme() {
        if (singleton == null)
            singleton = new VueTheme();
        return singleton;
    }

    VueTheme()
    {
        System.out.println("VueTheme: constructed.");
        if (DEBUG.INIT) new Throwable("VueTheme created").printStackTrace();
        //System.out.println("VueTheme: LookAndFeel: " + javax.swing.UIManager.getLookAndFeel().getName());
        if (isMacMetalLAF()) {
            System.out.println("VueTheme: Mac Aqua Brush Metal Look");
            ToolbarColor = SystemColor.window;
        }
    }

    public static boolean isMacMetalLAF() {
        String p = System.getProperty("apple.awt.brushMetalLook");
        return p != null && p.equals("true");
    }
    
    protected FontUIResource fontMedium = new FontUIResource("SansSerif", Font.PLAIN, 12);
    protected FontUIResource fontSmall  = new FontUIResource("SansSerif", Font.PLAIN, 11);
    protected FontUIResource fontTiny  = new FontUIResource("SansSerif", Font.PLAIN, 8);
    protected FontUIResource fontControl  = new FontUIResource("SansSerif", Font.PLAIN, 12);
    //private FontUIResource fontWindowTitle  = new FontUIResource(FixedFont);
    //private FontUIResource fontSubText  = new FontUIResource(FONT_TINY);
    
    private static Color VueColor = new ColorUIResource(VueResources.getColor("menubarColor"));    
    private static Color ToolbarColor = new ColorUIResource(VueResources.getColor("toolbar.background"));
    private Color FocusColor = new ColorUIResource(Color.red);
    
    // these are gray in Metal Default Theme
    private static ColorUIResource VueSecondary1 = new ColorUIResource(VueColor.darker().darker());
    private static ColorUIResource VueSecondary2 = new ColorUIResource(VueColor.darker());
    private static ColorUIResource VueSecondary3 = new ColorUIResource(VueColor);

    private Color VueLowContrast = new ColorUIResource(VueUtil.factorColor(VueColor, 0.95));

    private Color TestColor1 = new ColorUIResource(Color.red);
    private Color TestColor2 = new ColorUIResource(Color.green);
    private Color TestColor3 = new ColorUIResource(Color.blue);
    
    public String getName() {
        return super.getName() + " (VUE)";
    }

    protected FontUIResource getSmallFont() { return fontSmall; }
    
    //public static Color getVueColor() { return VueUtil.isMacAquaLookAndFeel() ? SystemColor.control : VueColor;  }
    //public static Color getToolbarColor() { return VueUtil.isMacAquaLookAndFeel() ? SystemColor.control : ToolbarColor; }
    public static Color getVueColor() { return getTheme().VueColor;  }
    public static Color getToolbarColor() { return getTheme().ToolbarColor;  }
    
    public FontUIResource getMenuTextFont() { return fontMedium;  }
    public FontUIResource getUserTextFont() { return fontSmall; }
    // controls: labels, buttons, tabs, tables, etc.
    public FontUIResource getControlTextFont() { return fontControl; }
    //public FontUIResource getWindowTitleFont() { return fontWindowTitle; } // internal frames?
    //public FontUIResource getSubTextFont() { return fontSubText; } // accelerator names
    
    protected ColorUIResource getSecondary1() { return VueSecondary1; }
    protected ColorUIResource getSecondary2() { return VueSecondary2; }
    protected ColorUIResource getSecondary3() { return VueSecondary3; }
    
    //protected ColorUIResource getPrimary1() { return TestColor1; }
    //protected ColorUIResource getPrimary2() { return TestColor2; }
    //protected ColorUIResource getPrimary3() { return TestColor3; }
    
    //public ColorUIResource getFocusColor() { return FocusColor; }
    
    //public ColorUIResource getMenuBackground() {
    //    return new ColorUIResource(Color.green);
    //}

    /** Check java impl: how is this different? Border or color I think */
    public static class VueTabbedPaneUI extends MetalTabbedPaneUI {
        public static ComponentUI createUI( JComponent x ) {
            if (DEBUG.Enabled) System.out.println("Creating VueTabbedPaneUI");
            return new VueTabbedPaneUI();
        }  
        protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                           Rectangle[] rects, int tabIndex, 
                                           Rectangle iconRect, Rectangle textRect,
                                           boolean isSelected) {}
        
        protected void paintTabBackground( Graphics g, int tabPlacement,
                                           int tabIndex, int x, int y, int w, int h, boolean isSelected ) {
            int slantWidth = h / 2;
            if ( isSelected ) {
                g.setColor( selectColor );
            } else {
                //g.setColor( tabPane.getBackgroundAt( tabIndex ) );
                Color c = tabPane.getBackgroundAt( tabIndex );
                // for now, allow toolbar color as optional tabbed bg, but all others override
                if (ToolbarColor.equals(c))
                    g.setColor(c);
                else
                    g.setColor(VueSecondary2);
            }
            switch ( tabPlacement ) {
            case LEFT:
                g.fillRect( x + 5, y + 1, w - 5, h - 1);
                g.fillRect( x + 2, y + 4, 3, h - 4 );
                break;
            case BOTTOM:
                g.fillRect( x + 2, y, w - 2, h - 4 );
                g.fillRect( x + 5, y + (h - 1) - 3, w - 5, 3 );
                break;
            case RIGHT:
                g.fillRect( x + 1, y + 1, w - 5, h - 1);
                g.fillRect( x + (w - 1) - 3, y + 5, 3, h - 5 );
                break;
            case TOP:
            default:
                g.fillRect( x + 4, y + 2, (w - 1) - 3, (h - 1) - 1 );
                g.fillRect( x + 2, y + 5, 2, h - 5 );
            }
        }
    }
        
    
    public void addCustomEntriesToTable(UIDefaults table)
    {
        table.put("ComboBox.background", Color.white);
        table.put("Button.font", getSmallFont());
        table.put("Label.font", getSmallFont());
        table.put("TitledBorder.font", fontMedium.deriveFont(Font.BOLD));

        table.put("TabbedPaneUI", "tufts.vue.VueTheme$VueTabbedPaneUI");
        
        //table.put("TabbedPane.focus", VueSecondary3); // annoying focus border: same as selected hides
        //table.put("TabbedPane.focus", VueLowContrast); // annoying focus border: same as selected hides

        // From BasicLookAndFeel:
        //table.put("TabbedPane.selected", Color.white); // selected tab fill & content border
        //table.put("TabbedPane.foreground", Color.green); // text color
        //table.put("TabbedPane.background", Color.red); // ???
        //table.put("TabbedPane.highlight", Color.red);
        //table.put("TabbedPane.light", Color.green); // edge border
        //table.put("TabbedPane.shadow", Color.red); / /???
        //table.put("TabbedPane.darkShadow", Color.green); // edge border
        // from MetalLookAndFeel:
        //table.put("TabbedPane.tabAreaBackground", Color.red); // ???
        //table.put("TabbedPane.selectHighlight", Color.red); // edge border
        
        //new Throwable("addCustomEntriesToTable " + table).printStackTrace();

        /*
        //-------------------------------------------------------
        // from java.swing.plaf.basic.BasicLookAndFeel.java:
        //-------------------------------------------------------
        
        "TabbedPane.font", dialogPlain12,
        "TabbedPane.background", table.get("control"),
        "TabbedPane.foreground", table.get("controlText"),
        "TabbedPane.highlight", table.get("controlLtHighlight"),
        "TabbedPane.light", table.get("controlHighlight"),
        "TabbedPane.shadow", table.get("controlShadow"),
        "TabbedPane.darkShadow", table.get("controlDkShadow"),
        "TabbedPane.selected", null,
        "TabbedPane.focus", table.get("controlText"),
        "TabbedPane.textIconGap", four,
        "TabbedPane.tabInsets", tabbedPaneTabInsets,
        "TabbedPane.selectedTabPadInsets", tabbedPaneTabPadInsets,
        "TabbedPane.tabAreaInsets", tabbedPaneTabAreaInsets,
        "TabbedPane.contentBorderInsets", tabbedPaneContentBorderInsets,
        "TabbedPane.tabRunOverlay", new Integer(2),
         
        "ComboBox.font", sansSerifPlain12,
        "ComboBox.background", table.get("window"),
        "ComboBox.foreground", table.get("textText"),
        "ComboBox.buttonBackground", table.get("control"),
        "ComboBox.buttonShadow", table.get("controlShadow"),
        "ComboBox.buttonDarkShadow", table.get("controlDkShadow"),
        "ComboBox.buttonHighlight", table.get("controlLtHighlight"),
        "ComboBox.selectionBackground", table.get("textHighlight"),
        "ComboBox.selectionForeground", table.get("textHighlightText"),
        "ComboBox.disabledBackground", table.get("control"),
        "ComboBox.disabledForeground", table.get("textInactiveText"),
         
        "PopupMenu.font", dialogPlain12,
        "PopupMenu.background", table.get("menu"),
        "PopupMenu.foreground", table.get("menuText"),
        "PopupMenu.border", popupMenuBorder,
         
        //-------------------------------------------------------
        // from java.swing.plaf.metal.MetalLookAndFeel.java:
        //-------------------------------------------------------
        "TabbedPane.font", controlTextValue,
        "TabbedPane.tabAreaBackground", getControl(),
        "TabbedPane.background", getControlShadow(),
        "TabbedPane.light", getControl(),
        "TabbedPane.focus", getPrimaryControlDarkShadow(),
        "TabbedPane.selected", getControl(),
        "TabbedPane.selectHighlight", getControlHighlight(),
        "TabbedPane.tabAreaInsets", tabbedPaneTabAreaInsets,
        "TabbedPane.tabInsets", tabbedPaneTabInsets,
         
        "ComboBox.background", table.get("control"),
        "ComboBox.foreground", table.get("controlText"),
        "ComboBox.selectionBackground", getPrimaryControlShadow(),
        "ComboBox.selectionForeground", getControlTextColor(),
        "ComboBox.font", controlTextValue,
         
        */
        
        //Color toolbarColor = VueResources.getColor("toolbar.background");
            
        /*
          // these work
        table.put("Label.font", FONT_MEDIUM);
        table.put("Button.font", FONT_MEDIUM);
        table.put("Menu.font", FONT_MEDIUM);
        table.put("MenuItem.font", FONT_MEDIUM);
        table.put("TabbedPane.font", FONT_MEDIUM);
        table.put("CheckBox.font", FONT_MEDIUM);
        table.put("RadioButton.font", FONT_MEDIUM);
        table.put("ToggleButton.font", FONT_MEDIUM);
        */

        /*
          // these work
        table.put("MenuBar.background", menuColor);
        table.put("MenuItem.background", menuColor);
        */
            
        // This is doing nothing I can see:
        //table.put("Menu.background", Color.white);
            
        // This tweaks the bg, but the buttons all appear to paint their on bg,
        // so we only see a thin border of this:
        //table.put("PopupMenu.background", Color.white);
            
        // the rest of these are tests
            
        //table.put("ComboBox.foreground", Color.red);
            
        // this doesn't do anything I can see (it does in windows L&F, but not Metal)
        //if (debug) table.put("ComboBox.buttonBackground", Color.yellow);
        // Okay: this works to change selected bg -- the one thing we didn't want to change.
        //table.put("ComboBox.selectionBackground", Color.white);
            
        // Effect in Windows L&F, but not metal:
        //if (debug) table.put("ComboBox.buttonShadow", Color.green);
        // Effect in Windows L&F, but not metal:
        //if (debug) table.put("ComboBox.buttonDarkShadow", Color.red);
            
            
        // Affects tabs but not tab contents background, so looks broken:
        //table.put("TabbedPane.selected", toolbarColor);
            
        //table.put("TabbedPane.tabAreaBackground", Color.green);
        // Why, in metal, is the default window "gray" background color neither lightGray
        // nor equal to the SystemColor.control???
        //table.put("TabbedPane.background", Color.blue);
        //table.put("TabbedPane.light", Color.orange);
        //table.put("TabbedPane.focus", Color.yellow);
        //table.put("TabbedPane.selected", Color.magenta);
        //table.put("TabbedPane.selectHighlight", Color.red);
    }
    
}

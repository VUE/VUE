package tufts.vue;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;

class VueTheme extends javax.swing.plaf.metal.DefaultMetalTheme
{
    private static VueTheme singleton;
    public static VueTheme getTheme() {
        if (singleton == null)
            singleton = new VueTheme();
        return singleton;
    }

    private VueTheme() {}
    
    private FontUIResource fontMedium = new FontUIResource("SansSerif", Font.PLAIN, 12);
    private FontUIResource fontSmall  = new FontUIResource("SansSerif", Font.PLAIN, 11);
    private FontUIResource fontControl  = new FontUIResource("SansSerif", Font.PLAIN, 12);
    //private FontUIResource fontWindowTitle  = new FontUIResource(FixedFont);
    //private FontUIResource fontSubText  = new FontUIResource(FONT_TINY);
    
    private ColorUIResource VueColor = new ColorUIResource(VueResources.getColor("menubarColor"));    
    private ColorUIResource FocusColor = new ColorUIResource(Color.red);
    
    // these are gray in Metal Default Theme
    private ColorUIResource VueSecondary1 = new ColorUIResource(VueColor.darker().darker());
    private ColorUIResource VueSecondary2 = new ColorUIResource(VueColor.darker());
    private ColorUIResource VueSecondary3 = new ColorUIResource(VueColor);

    private ColorUIResource TestColor1 = new ColorUIResource(Color.red);
    private ColorUIResource TestColor2 = new ColorUIResource(Color.green);
    private ColorUIResource TestColor3 = new ColorUIResource(Color.blue);
    
    public String getName() {
        return super.getName() + " (VUE)";
    }
    
    public static Color getVueColor() { return getTheme().VueColor;  }
    
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
    
    public void addCustomEntriesToTable(UIDefaults table)
    {
        table.put("ComboBox.background", Color.white);
        table.put("Button.font", fontSmall);
        table.put("Label.font", fontSmall);
        table.put("TitledBorder.font", fontMedium.deriveFont(Font.BOLD));

        // From BasicLookAndFeel:
        //table.put("TabbedPane.selected", Color.white); // selected tab fill & content border
        //table.put("TabbedPane.foreground", Color.green); // text color
        //table.put("TabbedPane.background", Color.red); // ???
        //table.put("TabbedPane.highlight", Color.red);
        //table.put("TabbedPane.light", Color.green); // edge border
        //table.put("TabbedPane.shadow", Color.red); / /???
        //table.put("TabbedPane.darkShadow", Color.green); // edge border
        table.put("TabbedPane.focus", VueColor); // annoying focus border: same as selected hides
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

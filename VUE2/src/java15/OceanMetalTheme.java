/*
 * This file only compiles in java 1.5 (due to it's reference to
 * OceanTheme), which is why isn't in the main source tree (until we
 * move over to 1.5 completely).  It in manually compiled and jar'd
 * into VUE-Java15.lib when modified.
 */

package tufts.vue.gui;

import tufts.vue.gui.GUI;

import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/**
 * Extension of the Java 1.5 Ocean Theme for VUE.
 *
 * @version $Revision: 1.2 $ / $Date: 2006-01-20 16:48:28 $ / $Author: sfraize $
 * @author Scott Fraize
 */
  
public class OceanMetalTheme extends javax.swing.plaf.metal.OceanTheme
{
    private GUI.CommonMetalTheme common;
        
    public OceanMetalTheme(GUI.CommonMetalTheme common) { this.common = common; }

    OceanMetalTheme() {}
    // We only have this so we can have a null constructor so Class.newInstance can be used.
    public void setCommonTheme(GUI.CommonMetalTheme common) {
        this.common = common;
    }
        
    public FontUIResource getMenuTextFont() { return common.fontMedium;  }
    public FontUIResource getUserTextFont() { return common.fontSmall; }
    public FontUIResource getControlTextFont() { return common.fontControl; }
        
    protected ColorUIResource getSecondary1() { return common.VueSecondary1; }
    protected ColorUIResource getSecondary2() { return common.VueSecondary2; }
    protected ColorUIResource getSecondary3() { return common.VueSecondary3; }

    public void addCustomEntriesToTable(UIDefaults table) {
        super.addCustomEntriesToTable(table);
        common.addCustomEntriesToTable(table);

        // these are the lighter blue #c8ddf2 in Ocean
        table.put("TabbedPane.contentAreaColor", GUI.VueColor);
        table.put("TabbedPane.selected", GUI.VueColor);

        java.awt.Color toolbar = new java.awt.Color(175,182,198);

        //table.put("Menu.opaque", Boolean.TRUE);
        //table.put("MenuBar.gradient", null);
        table.put("MenuBar.gradient",
                  java.util.Arrays.asList(new Object[] {
                    new Float(1f),
                    new Float(0f),
                    //getWhite(),
                    tufts.Util.factorColor(GUI.VueColor, 1.1),
                    toolbar,
                    new ColorUIResource(toolbar) })
                  );
        
        table.put("MenuBar.borderColor", new ColorUIResource(toolbar));
    }
        
    public String getName() { return super.getName() + " (VUE)"; }

}

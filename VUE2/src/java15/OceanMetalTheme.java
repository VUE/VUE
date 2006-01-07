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
 * @version $Revision: 1.1 $ / $Date: 2006-01-07 15:43:03 $ / $Author: sfraize $
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
    }
        
    public String getName() { return super.getName() + " (VUE)"; }

}

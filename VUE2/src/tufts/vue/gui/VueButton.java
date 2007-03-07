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


package tufts.vue.gui;

import tufts.vue.DEBUG;
import tufts.vue.VueResources;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.*;

/**
 *
 * This class is a wrapper around JButton to get the look and feel for
 * VUE buttons displaying icons only (no text).  The button sets the
 * disabled, up & down icons.  All the icons must be present in
 * VueResources in format buttonName.Up, buttonName.down,
 * buttonName.disabled, or .raw for generated buttons.
 *
 * @version $Revision: 1.8 $ / $Date: 2007-03-07 18:01:10 $ / $Author: mike $
 * @author  akumar03
 * @author  Scott Fraize
 */

public class VueButton extends JButton
//public class VueButton extends com.jidesoft.swing.JideButton
//implements com.jidesoft.swing.Alignable // JIDE
{
    public void setOrientation(int o) {
        System.out.println(this + " so " + o);
    }
    public int getOrientation() {return 0;}
    public boolean supportHorizontalOrientation() { return true; }
    public boolean supportVerticalOrientation() { return true; }

    
    protected String key;
    private boolean isToolbarButton;

    public VueButton(String name, ActionListener l)
    {
        init(name);
        if (l != null)
            addActionListener(l);
    }
    
    public VueButton(String name) {
        init(name);
    }

    public VueButton(Action a) {
        //setButtonStyle(HYPERLINK_STYLE); // JIDE
        
        setAction(a);
        Icon largeIcon = (Icon) a.getValue(tufts.vue.VueAction.LARGE_ICON);
        if (largeIcon != null)
            setIcon(largeIcon);
        init((String) a.getValue(Action.ACTION_COMMAND_KEY));
    }

    public void setAsToolbarButton(boolean tb) {
        isToolbarButton = tb;
        if (isToolbarButton)
            super.setText(null);
    }

    public void setText(String text) {
        if (isToolbarButton)
            setToolTipText(text);
        else
            super.setText(text);
    }

    public void addNotify() {
        super.addNotify();
        setBackground(getParent().getBackground());
    }

    public static class Toggle extends JToggleButton {
        // todo: make an LWPropertyHandler for boolean's
        protected String key;
        public Toggle(String name, ActionListener l) {
            init(name);
            if (l != null)
                addActionListener(l);
        }
        public Toggle(String name) {
            init(name);
        }
        
        public Toggle(Action a) {
            setAction(a);
            init((String) a.getValue(Action.ACTION_COMMAND_KEY));
        }

        /*
        public Toggle(String name, ActionListener l) {
            super(name, l);
            // apparently need more than this to get to work as a toggle
            //setModel(new JToggleButton.ToggleButtonModel());
            // if can get working, can subclass VueButton
        }
        */

        public void addNotify() {
            super.addNotify();
            setBackground(getParent().getBackground());
        }
        
        private void init(String key) {
            this.key = key;
            VueButton.init(this, key);
        }
        public String toString() {
            return "VueButton.Toggle[" + key + "]";
        }
    }
    

    private void init(String key) {
        this.key = key;
        init(this, key);
    }
    
    private static void init(AbstractButton b, String key)
    {
        //System.out.println(GUI.name(b) + "\tINIT0 " + key + "\tTTT[" + b.getToolTipText() + "]");
        
        if (key == null) {
            // from an action init w/no action command
            if (b.getName() == null)
                b.setName(b.getText());
            if (b.getToolTipText() == null || b.getToolTipText().length() < 1) {
                b.setToolTipText(b.getText()); 
               if (DEBUG.Enabled) System.out.println(GUI.name(b) + " lazy setToolTipText " + b.getText());
            }
            VueButtonIcon.installGenerated(b, b.getIcon(), null);
        } else {
            b.setName(key);
            installResourceConfiguration(b, key);
        }

        b.setRolloverEnabled(true);

        //System.out.println(GUI.name(b) + "\tINIT1 " + key + "\tTTT[" + b.getToolTipText() + "]");
        
        b.setFocusable(false);
        
        if (false && GUI.isOceanTheme()) {
            //b.setRolloverEnabled(true);
            // todo: need some kind of border, but then will need
            // to change rollover icon (or maybe change border
            // on rollover instead of changing icon)
            b.setBorder(new LineBorder(Color.blue));
        } else {
            b.setBorder(null);
            b.setBorderPainted(false);
            b.setOpaque(false);
        }

        if (b.getIcon() != null) {
            Dimension imageSize = new Dimension(b.getIcon().getIconWidth(), b.getIcon().getIconHeight());
            //System.out.println(b + " icon size is " + VueUtil.out(imageSize));
            b.setPreferredSize(imageSize);
        } else {
            //if (DEBUG.Enabled) System.out.println(b + " init");
        }

        //setBackground(Color.white);
        //setBackground(Color.red);
        if (DEBUG.INIT) System.out.println("Created new " + b);
        //if (true||DEBUG.SELECTION&&DEBUG.META) new Throwable().printStackTrace();
    }


    public static final String kRAW = ".raw";
    public static final String kUP = ".up";
    public static final String kDOWN = ".down";
    public static final String kDISABLED = ".disabled";
    public static final String kSIZE = ".size";
    public static final String kSELECTED = ".selected";

    public static void installResourceConfiguration(AbstractButton b, String key)
    {    	
        Icon i = null;
        Icon i2 = null;
        if ((i = VueResources.getImageIcon(key + kRAW)) != null) {
        	if ((i2 = VueResources.getImageIcon(key + kSELECTED)) != null)
        		VueButtonIcon.installGenerated(b, i,i2, VueResources.getSize(key + kSIZE));
        	else
        		VueButtonIcon.installGenerated(b, i, VueResources.getSize(key + kSIZE));
        } else {
        	if ((i = VueResources.getImageIcon(key+kSELECTED)) != null){ b.setSelectedIcon(i);System.out.println("MIKEK");}
            if ((i = VueResources.getImageIcon(key + kUP)) != null)       b.setIcon(i);
            if ((i = VueResources.getImageIcon(key + kDOWN)) != null)     b.setPressedIcon(i);
            if ((i = VueResources.getImageIcon(key + kDISABLED)) != null) b.setDisabledIcon(i);
        }
        String tt = VueResources.getString(key + ".tooltip");
        if (tt != null) {
            b.setToolTipText(tt);
        }
    }

    

    public String toString() {
        String label;
        if (key == null) {
            if (getAction() == null)
                label = "txt=" + getText();
            else
                label = getAction().toString();
        } else
            label = key;
        return "VueButton[" + label + "]";
    }

   
}

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
 * @author  akumar03
 * @author  Scott Fraize
 * @version March 2004
 */

public class VueButton extends JButton
{
    protected String key;

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
        setAction(a);
        init((String) a.getValue(Action.ACTION_COMMAND_KEY));
    }

    public void addNotify() {
        super.addNotify();
        setBackground(getParent().getBackground());
    }

    private void init(String key) {
        this.key = key;
        init(this, key);
    }
    
    public static class Toggle extends JToggleButton {
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
    

    static void init(AbstractButton b, String key)
    {
        installResourceConfiguration(b, key);

        if (true) {
            b.setBorder(null);
            b.setBorderPainted(false);
            b.setFocusable(false);
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
        System.out.println("Created new " + b);
        //if (true||DEBUG.SELECTION&&DEBUG.META) new Throwable().printStackTrace();
    }


    public static final String kRAW = ".raw";
    public static final String kUP = ".up";
    public static final String kDOWN = ".down";
    public static final String kDISABLED = ".disabled";
    public static final String kSIZE = ".size";

    public static void installResourceConfiguration(AbstractButton b, String key)
    {
        Icon i;
        if ((i = VueResources.getImageIcon(key + kRAW)) != null) {
            
            VueButtonIcon.installGenerated(b, i, VueResources.getSize(key + kSIZE));
        } else {
            if ((i = VueResources.getImageIcon(key + kUP)) != null)       b.setIcon(i);
            if ((i = VueResources.getImageIcon(key + kDOWN)) != null)     b.setPressedIcon(i);
            if ((i = VueResources.getImageIcon(key + kDISABLED)) != null) b.setDisabledIcon(i);
        }
        String tt = VueResources.getString(key + ".tooltip");
        if (tt != null)
            b.setToolTipText(tt);
    }

    

    public String toString() {
        return "VueButton[" + key + "]";
    }

   
}

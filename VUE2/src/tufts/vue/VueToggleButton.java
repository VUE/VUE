package tufts.vue;

import javax.swing.*;
import java.awt.*;

/**
 * VueToggleButton
 *
 * This class is a wrapper around JToggleButton to get the look and feel for VUE.
 *
 * @author  Scott Fraize
 * @version March 2004
 */
public class VueToggleButton extends JToggleButton
{
    private static final String UP = ".up";
    private static final String DOWN = ".down";
    private static final String DISABLED = ".disabled";
    private static final String RAW = ".raw";

    private String key;

    public VueToggleButton(String name, java.awt.event.ActionListener listener)
    {
        init(name);
        if (listener != null)
            addActionListener(listener);
    }
    
    public VueToggleButton(String name) {
        init(name);
    }

    public VueToggleButton(Action a) {
        setAction(a);
        init((String) a.getValue(Action.ACTION_COMMAND_KEY));
    }

    private void init(String key)
    {
        this.key = key;
        if (DEBUG.Enabled) System.out.println("initializing " + this);
        
        Icon i;
        
        if ((i = VueResources.getImageIcon(key + RAW)) != null) {
            VueButtonIcon.installGenerated(this, i);
        } else {
            if ((i = VueResources.getImageIcon(key + UP)) != null)       setIcon(i);
            if ((i = VueResources.getImageIcon(key + DOWN)) != null)     setPressedIcon(i);
            if ((i = VueResources.getImageIcon(key + DISABLED)) != null) setDisabledIcon(i);
        }

        if (true) {
            setBorder(null);
            setBorderPainted(false);
            setFocusable(false);
            setOpaque(false);
        }

        if (getIcon() != null) {
            Dimension imageSize = new Dimension(getIcon().getIconWidth(), getIcon().getIconHeight());
            System.out.println("VueToggleButton: icon size is " + VueUtil.out(imageSize) + " on " + key);
            setPreferredSize(imageSize);
        }

        //setBackground(Color.white);
        //setBackground(Color.red);
        if (DEBUG.SELECTION&&DEBUG.META) new Throwable().printStackTrace();

    }

    public String toString() {
        return "VueToggleButton[" + key + "]";
    }


   
}

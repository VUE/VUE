/*
 * VueButton.java
 *
 * Created on February 13, 2004, 10:15 AM
 *
 * This claas is a wrapper around JButton to get the look and feel for VUE.
 *VueButtons are currently used in Pathway Panel and Advanced Search.  The button sets the disabled, Up and Down icons.
 * All the icons must be present in VueResources in format buttonName.Up, buttonName.down, buttonName.disabled.
 *
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 */

import javax.swing.*;
import java.awt.*;

public class VueButton extends JButton {
    
    /** Creates a new instance of VueButton */
    public static String UP = "up";
    public static String DOWN = "down";
    public static String DISABLED = "disabled";

    public VueButton(String name, java.awt.event.ActionListener listener)
    {
        super(VueResources.getImageIcon(name+"."+UP));
        Icon i;
        if ((i = VueResources.getImageIcon(name+"."+DOWN)) != null)     setPressedIcon(i);
        if ((i = VueResources.getImageIcon(name+"."+DISABLED)) != null) setDisabledIcon(i);
        setBorderPainted(false);
        //setBackground(Color.white);
        //setBackground(Color.red);
        setOpaque(false);
        Dimension imageSize = new Dimension(getIcon().getIconWidth(), getIcon().getIconHeight());
        System.out.println("VueButton: icon size is " + VueUtil.out(imageSize) + " on " + name);
        setPreferredSize(imageSize);
        if (listener != null)
            addActionListener(listener);
        setFocusable(false);
        
        //new Throwable().printStackTrace();
    }
    
    public VueButton(String name) {
        this(name, null);
    }
   
}

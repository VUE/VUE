/*
 * VueButton.java
 *
 * Created on February 13, 2004, 10:15 AM
 *
 * This claas is a wrapper around JButton to get the look and feel for VUE.
 * VueButtons are currently used in Pathway Panel and Advanced Search.  The button sets the disabled, Up and Down icons.
 * All the icons must be present in VueResources in format buttonName.Up, buttonName.down, buttonName.disabled.
 *
 */

package tufts.vue;

/**
 * @author  akumar03
 * @author  Scott Fraize
 * @version March 2004
 */

import javax.swing.*;
import java.awt.*;

public class VueButton extends JButton
{
    private static final String UP = ".up";
    private static final String DOWN = ".down";
    private static final String DISABLED = ".disabled";

    public VueButton(String name, java.awt.event.ActionListener listener)
    {
        init(name);
        if (listener != null)
            addActionListener(listener);
    }
    
    public VueButton(String name) {
        init(name);
    }

    public VueButton(Action a) {
        setAction(a);
        init((String) a.getValue(Action.ACTION_COMMAND_KEY));
    }

    private void init(String key)
    {
        Icon i;
        
        if ((i = VueResources.getImageIcon(key + UP)) != null)       setIcon(i);
        if ((i = VueResources.getImageIcon(key + DOWN)) != null)     setPressedIcon(i);
        if ((i = VueResources.getImageIcon(key + DISABLED)) != null) setDisabledIcon(i);

        setBorder(null);
        setBorderPainted(false);
        setFocusable(false);
        setOpaque(false);

        if (getIcon() != null) {
            Dimension imageSize = new Dimension(getIcon().getIconWidth(), getIcon().getIconHeight());
            System.out.println("VueButton: icon size is " + VueUtil.out(imageSize) + " on " + key);
            setPreferredSize(imageSize);
        }

        //setBackground(Color.white);
        //setBackground(Color.red);
        //new Throwable().printStackTrace();
        
    }
   
}

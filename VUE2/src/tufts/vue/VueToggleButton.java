package tufts.vue;

import javax.swing.*;
import java.awt.*;

/**
 * VueToggleButton
 *
 * This class is a wrapper around JToggleButton to get the look and feel for VUE.
 *
 * @deprecated in favor of VueButton.Toggle
 */
public class VueToggleButton extends JToggleButton
{
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

    public void addNotify() {
        super.addNotify();
        setBackground(getParent().getBackground());
        System.out.println(this + " setting bg to " + getParent() + " " + getParent().getBackground());
    }
    
    private void init(String key)
    {
        this.key = key;
        if (DEBUG.Enabled) System.out.println(this + " init");
        
        VueButtonIcon.installResourceConfiguration(this, key);

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

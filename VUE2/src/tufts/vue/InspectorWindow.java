/*
 * InspectorWindow.java
 *
 * Created on June 25, 2003, 10:27 AM
 *
 * This class can be used to create a stand alone window thats always
 * on top of the main app. It can be used instead of the ToolWindow class
 * to allow editing of fields and the ability for the user to close the
 * window.
 */

package tufts.vue;

import javax.swing.*;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.*;

/**
 *
 * @author  Jay Briedis
 */
public class InspectorWindow extends JDialog {
    
    private JFrame owner = null;
    private Point location = new Point(300, 300);
    private Dimension size = new Dimension(200, 200);

    public InspectorWindow() {
        this((JFrame)null, "Default Window");
        //setUndecorated(true); // no effect
    }
    
    public InspectorWindow(JFrame owner, String title, Point loc){
        this(owner, title);
        this.location = loc;
    }
    
    public InspectorWindow(JFrame owner, String title) {
        super(owner, title);
        this.owner = owner;
        this.setModal(false);
        this.setLocation(location);
        this.setSize(size);
    }
    
}

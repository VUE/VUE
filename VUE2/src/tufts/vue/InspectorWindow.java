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

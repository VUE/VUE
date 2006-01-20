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

import java.awt.event.MouseEvent;

public class MouseAdapter implements javax.swing.event.MouseInputListener
{
    private String name = "";
    public MouseAdapter() {}
    public MouseAdapter(String name) { this.name = name + ": "; }
    public MouseAdapter(Class clazz) { this(clazz.getName()); }
    
    public static final MouseAdapter debug = new MouseAdapter("MouseAdapter");

// The MouseListener methods
// implements java.awt.event.MouseListener
    
    public void mouseClicked(MouseEvent e) {if (DEBUG.MOUSE&&DEBUG.META) System.out.println(name + e);}
    public void mousePressed(MouseEvent e) {if (DEBUG.MOUSE&&DEBUG.META) System.out.println(name + e);}
    public void mouseReleased(MouseEvent e) {if (DEBUG.MOUSE&&DEBUG.META) System.out.println(name + e);}
    public void mouseEntered(MouseEvent e) {if (DEBUG.MOUSE&&DEBUG.META) System.out.println(name + e);}
    public void mouseExited(MouseEvent e) {if (DEBUG.MOUSE&&DEBUG.META) System.out.println(name + e);}

// The MouseMotionListener methods
// implements java.awt.event.MouseMotionListener
    
    public void mouseDragged(MouseEvent e)
    {
        if (DEBUG.MOUSE && DEBUG.META)
            System.out.println(name + "[" + e.paramString() + "] on " + e.getSource().getClass().getName());
    }
    public void mouseMoved(MouseEvent e)
    {
        if (DEBUG.MOUSE && DEBUG.META)
            System.err.println(name + "[" + e.paramString() + "] on " + e.getSource().getClass().getName());
    }
}



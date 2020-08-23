/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import java.awt.event.MouseEvent;

public class MouseAdapter implements javax.swing.event.MouseInputListener
{
    private String name = "";
    public MouseAdapter() {}
    public MouseAdapter(String name) { this.name = name + ": "; }
    public MouseAdapter(Class clazz) { this(clazz.getName()); }
    public MouseAdapter(Object o) { this(o.getClass()); }

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

    public String toString() {
        return name;
    }
}



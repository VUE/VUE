package tufts.vue;

import java.awt.event.MouseEvent;

class MouseAdapter implements javax.swing.event.MouseInputListener
{
    private String name = "";
    public MouseAdapter() {}
    public MouseAdapter(String name) { this.name = name + ": "; }
    
    public static final MouseAdapter debug = new MouseAdapter("MouseAdapter");

// The MouseListener methods
// implements java.awt.event.MouseListener
    
    public void mouseClicked(MouseEvent e) {if (DEBUG.MOUSE) System.out.println(name + e);}
    public void mousePressed(MouseEvent e) {if (DEBUG.MOUSE) System.out.println(name + e);}
    public void mouseReleased(MouseEvent e) {if (DEBUG.MOUSE) System.out.println(name + e);}
    public void mouseEntered(MouseEvent e) {if (DEBUG.MOUSE) System.out.println(name + e);}
    public void mouseExited(MouseEvent e) {if (DEBUG.MOUSE) System.out.println(name + e);}

// The MouseMotionListener methods
// implements java.awt.event.MouseMotionListener
    
    public void mouseDragged(MouseEvent e)
    {
        if (DEBUG.MOUSE)
            System.out.println(name + "[" + e.paramString() + "] on " + e.getSource().getClass().getName());
    }
    public void mouseMoved(MouseEvent e)
    {
        if (DEBUG.MOUSE&&DEBUG.META)
            System.err.println(name + "[" + e.paramString() + "] on " + e.getSource().getClass().getName());
    }
}



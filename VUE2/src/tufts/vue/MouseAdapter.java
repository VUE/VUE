package tufts.vue;

import java.awt.event.MouseEvent;

class MouseAdapter implements javax.swing.event.MouseInputListener
//sf
{
    String name = "";
    public MouseAdapter() {}
    public MouseAdapter(String name) { this.name = name + ": "; }
    
    public static final MouseAdapter debug = new MouseAdapter();

// The MouseListener methods
// implements java.awt.event.MouseListener
    
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {System.err.println(name + e);}
    public void mouseReleased(MouseEvent e) {System.err.println(name + e);}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

// The MouseMotionListener methods
// implements java.awt.event.MouseMotionListener
    
    public void mouseDragged(MouseEvent e)
    {
        System.err.println(name + "[" + e.paramString() + "] on " + e.getSource().getClass().getName());
    }
    public void mouseMoved(MouseEvent e)
    {
        //System.err.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());
    }
}



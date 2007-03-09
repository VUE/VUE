package tufts.vue.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;

public class TimedASComponent extends JComponent{

	  private Autoscroller autoscroller;

	  public void addMouseListener(MouseListener ml)
	  {
		  super.addMouseListener(ml);
	  }
	  
	  public void setAutoscrolls(boolean autoscrolls) {
	        if (autoscrolls) {
	            if (autoscroller == null) {
	                autoscroller = new Autoscroller(this);
	            }
	        } else {
	            if (autoscroller != null) {
	                autoscroller.dispose();
	                autoscroller = null;
	            }
	        }
	    }
	  
	  public boolean getAutoscrolls() {
	        return autoscroller != null;
	    }
	
	  public void superProcessMouseMotionEvent(MouseEvent e) {
	        super.processMouseMotionEvent(e);
	    }
	 public void processMouseMotionEvent(MouseEvent e)
	  {
		  boolean dispatch = true;
	        if (autoscroller != null) {
	            if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
	                // We don't want to do the drags when the mouse moves if we're
	                // autoscrolling.  It makes it feel spastic.
	                dispatch = !autoscroller.timer.isRunning();
	                autoscroller.mouseDragged(e);
	            }
	        }
	        if (dispatch) {
	            super.processMouseMotionEvent(e);
	        }
	  }
}

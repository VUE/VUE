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

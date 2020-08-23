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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.util.*;
import javax.swing.JWindow;
import javax.swing.Popup;
import javax.swing.SwingUtilities;

class VueHeavyweightPopup extends Popup {
    
    private Point p;
    Component owner;
    Component child;
    
    private static List cache = null;
    
    public static Popup getInstance (Component owner, Component child, Point p) 
    {
        if (cache == null) {
            cache = new LinkedList();
        }
        VueHeavyweightPopup result;
        synchronized (cache) {
            if (cache.size() > 0) {
                result = (VueHeavyweightPopup) cache.get(0);
                cache.remove(0);
            } else {
                result = new VueHeavyweightPopup(owner, child, p);
            }
        }
        result.owner = owner;
        result.child = child;
        result.p = p;
        return result;
    }
    
    private static void recycle(Popup popup) 
    {
        synchronized(cache) {
            if (cache.size() < 5) {
                cache.add(popup);
            }
        }
    }
    
    private VueHeavyweightPopup(Component owner, Component child, Point p) 
    {
        this.owner = owner;
        this.p = p;
        this.child = child;
    }
    
    public void hide() 
    {
       // super.hide();
        Component component = getComponent();
        if (component instanceof JWindow) {
            component.hide();
            ((JWindow)component).getContentPane().removeAll();
        }        
      
        owner = null;
        child = null;
        p = null;
        recycle(this);
    }
    
    public void show() 
    {                    
        Point pt = new Point(this.p);
        
        Component component = getComponent();
        ((JWindow)component).getContentPane().add(child,BorderLayout.CENTER);        
        
        component.setLocation(pt.x, pt.y);
        
        component.setVisible(true);
        
        
        component.validate();
    }
    
    /**
     * The Component representing the Popup.
     */
    private Component component;

    Component createComponent() 
    {     
        return new HeavyWeightWindow(getParentWindow(tufts.vue.VUE.getDialogParent()));
    }
    
    Component getComponent() {
    	if (component == null)
    	{
    		component= createComponent();
    		return component;
    	}
    	else
    		return component;
    }
    /**
     * Returns the <code>Window</code> to use as the parent of the
     * <code>Window</code> created for the <code>Popup</code>. This creates
     * a new <code>Frame</code> each time it is invoked. Subclasses that wish
     * to support a different <code>Window</code> parent should override
     * this.
     */
    private Window getParentWindow(Component owner) {
        Window window = null;

        if (owner instanceof Window) {
            window = (Window)owner;
        }
        else if (owner != null) {
            window = SwingUtilities.getWindowAncestor(owner);
        }
        if (window == null) {
            window = new DefaultFrame();
        }
        return window;
    }

    /**
     * Component used to house window.
     */
    static class HeavyWeightWindow extends JWindow 
    {  private Component child;
        HeavyWeightWindow(Window parent) {
            super(parent);
            setFocusableWindowState(false);
            setName("###overrideRedirect###");
            
        }

        public void update(Graphics g) {
            paint(g);
        }

	public void show() {
	    super.show();
	    this.pack();
	}	  
   }
 
    static class DefaultFrame extends Frame {
    }
}
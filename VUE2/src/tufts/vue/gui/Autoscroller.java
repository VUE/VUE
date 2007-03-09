package tufts.vue.gui;

/*
 * @(#)Autoscroller.java	1.12 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import java.awt.*;
import java.awt.event.*;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.Timer;


/**
 * @version 1.12 01/23/03
 * @author Dave Moore
 */

class Autoscroller extends MouseAdapter implements Serializable
{
    transient MouseEvent event;
    transient Timer timer;
    TimedASComponent component;


    Autoscroller(TimedASComponent c) {
	if (c == null) {
	    throw new IllegalArgumentException("component must be non null");
	}
	component = c;
	timer = new Timer(tufts.vue.VueResources.getInt("autoscroller.delay"), new AutoScrollTimerAction());
	
	component.addMouseListener(this);
    }

    class AutoScrollTimerAction implements ActionListener {
	public void actionPerformed(ActionEvent x) {
	
	    if(!component.isShowing() || (event == null)) {
		stop();
		return;
	    }
	    Point screenLocation = component.getLocationOnScreen();
	    MouseEvent e = new MouseEvent(component, event.getID(),
					  event.getWhen(), event.getModifiers(),
					  event.getX() - screenLocation.x,
					  event.getY() - screenLocation.y,
					  event.getClickCount(), event.isPopupTrigger());
	    component.superProcessMouseMotionEvent(e);
	   
	}
    }

    void stop() {
	timer.stop();
	event = null;
    }

    void dispose() {
	stop();
	component.removeMouseListener(this);
    }

    public void mouseReleased(MouseEvent e) {
	stop();
    }

    public void mouseDragged(MouseEvent e) {
	Rectangle visibleRect = component.getVisibleRect();
	boolean contains = visibleRect.contains(e.getX(), e.getY());

	if (contains) {
	    if (timer.isRunning()) {
		stop();
	    }
	} else {
	    Point screenLocation = component.getLocationOnScreen();

	    event = new MouseEvent(component, e.getID(), e.getWhen(), e.getModifiers(),
				   e.getX() + screenLocation.x,
				   e.getY() + screenLocation.y,
				   e.getClickCount(), e.isPopupTrigger());
	    if (!timer.isRunning()) {
		timer.start();
	    }
	}
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
	s.defaultWriteObject();
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException 
    {
	s.defaultReadObject();
	timer = new Timer(100, new AutoScrollTimerAction());
    }
}


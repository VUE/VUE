package edu.tufts.vue.preferences;

import javax.swing.event.ChangeEvent;

public class VuePrefEvent extends ChangeEvent {

	private Object oldValue;
	private Object newValue;
	private static final long serialVersionUID = 1L;
	
	public VuePrefEvent(Object arg0, Object oldValue, Object newValue) {
		super(arg0);
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	public Object getOldValue()
	{
		return oldValue;
	}
	
	public Object getNewValue()
	{
		return newValue;
	}		
}

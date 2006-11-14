package edu.tufts.vue.preferences;

import java.util.EventListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public interface VuePrefListener extends EventListener{

	public void preferenceChanged(VuePrefEvent prefEvent);

}

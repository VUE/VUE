package tufts.vue;

import java.util.EventListener;


public interface ExpandSelectionListener extends EventListener {


	public abstract void depthChanged(ExpandSelectionEvent event);	
}

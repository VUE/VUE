

package tufts.vue;

import java.lang.*;
import java.util.*;
import javax.swing.*;

public class HandTool extends VueTool {

	public static final String kName = "hand";

	public HandTool(   ) {
		super();
	}
    public boolean handleKeyPressed(java.awt.event.KeyEvent e)  {
		return false;
	}
	
	public void handleSelection() {
	
	}
	
	public JPanel getContextualPanel() {
		return null;
	}

}
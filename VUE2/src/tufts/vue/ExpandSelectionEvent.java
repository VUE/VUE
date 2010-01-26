package tufts.vue;

import java.util.EventObject;


public class ExpandSelectionEvent extends EventObject {
	public static final long		serialVersionUID = 1;
	protected int					depth = -1;


	public ExpandSelectionEvent(Object source, int depth) {
		super(source);

		this.depth = depth;
	}


	public int getDepth() {
		return depth;
	}
}

package tufts.vue;


/**
 * VueToolSelectionListener
 * This interface is used by Objects interested in
 * VueToolbar tool selection events.
 **/
public interface VueToolSelectionListener {

	/**
	 * toolSelected
	 * This method is called when a tool is selected
	 * @param VueTool - the selected tool
	 **/
	public void toolSelected( VueTool pTool);
}
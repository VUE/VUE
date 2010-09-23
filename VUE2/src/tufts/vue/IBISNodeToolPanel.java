package tufts.vue;

import tufts.vue.NodeToolPanel.LinkMenuButton;
import tufts.vue.NodeToolPanel.ShapeMenuButton;

public class IBISNodeToolPanel extends NodeToolPanel {
	
    private final ShapeMenuButton mShapeButton;
    private final LinkMenuButton mLinkButton;

	public IBISNodeToolPanel() {
        this.mShapeButton = new ShapeMenuButton();
        this.mLinkButton = new LinkMenuButton();   
	}

}

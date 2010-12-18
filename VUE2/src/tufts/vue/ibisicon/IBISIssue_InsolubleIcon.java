package tufts.vue.ibisicon;

import java.io.File;
import java.awt.*;

import tufts.vue.*;

public class IBISIssue_InsolubleIcon extends IBISImageIcon {
	private static Image mImage = VueResources.getImage("IBISNodeTool.issue_insoluble.raw");
	
	public IBISIssue_InsolubleIcon() {
		super(mImage);
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

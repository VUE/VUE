package tufts.vue.ibisicon;

import java.io.File;
import java.awt.*;

import tufts.vue.*;

public class IBISIssue_ResolvedIcon extends IBISImageIcon {
	private static Image mImage = VueResources.getImage("IBISNodeTool.issue_resolved.raw");
	
	public IBISIssue_ResolvedIcon() {
		super(mImage);
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

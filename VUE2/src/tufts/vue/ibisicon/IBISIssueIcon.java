package tufts.vue.ibisicon;

import java.awt.Image;
import java.io.File;

import tufts.vue.*;

public class IBISIssueIcon extends IBISImageIcon {
	
	private static Image mImage = VueResources.getImage("IBISNodeTool.issue.raw");
	
	public IBISIssueIcon() {
		super(mImage);
		
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

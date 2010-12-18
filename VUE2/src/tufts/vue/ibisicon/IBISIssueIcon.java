package tufts.vue.ibisicon;

import java.awt.Image;
import java.io.File;

import tufts.vue.*;

public class IBISIssueIcon extends IBISImageIcon {
	
	// HO 17/12/2010 EMERGENCY BEGIN ********
	//private static File mImageFile = VueResources.getFile("IBISNodeTool.issue.raw");
	private static Image mImage = VueResources.getImage("IBISNodeTool.issue.raw");
	// HO 17/12/2010 EMERGENCY END ********
	
	public IBISIssueIcon() {
		//super(mImageFile.toString());
		super(mImage);
		
	}
	
	/* public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	} */
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

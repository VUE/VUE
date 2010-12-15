package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISIssueIcon extends IBISImageIcon {
	
	private static File mImageFile = VueResources.getFile("IBISNodeTool.issue.raw");
	
	public IBISIssueIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

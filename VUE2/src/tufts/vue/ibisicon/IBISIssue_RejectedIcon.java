package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISIssue_RejectedIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.issue_rejected.raw");
	
	public IBISIssue_RejectedIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

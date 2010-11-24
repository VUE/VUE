package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISAcceptedIssueIcon extends IBISImageIcon {
	
	private static File mImageFile = VueResources.getFile("IBISNodeTool.accepted_issue.raw");
	
	public IBISAcceptedIssueIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

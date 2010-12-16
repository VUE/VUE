package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISIssue_InsolubleIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.issue_insoluble.raw");
	
	public IBISIssue_InsolubleIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

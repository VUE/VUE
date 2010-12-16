package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISIssue_ResolvedIcon extends IBISImageIcon {
	// HO 13/12/2010 BEGIN ************
	private static File mImageFile = VueResources.getFile("IBISNodeTool.issue_resolved.raw");
	//private static File mImageFile = VueResources.getFile("IBISNodeTool.neptune.icon");
	// HO 13/12/2010 END ************
	
	public IBISIssue_ResolvedIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

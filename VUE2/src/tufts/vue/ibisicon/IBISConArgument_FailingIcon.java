package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISConArgument_FailingIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.con_argument_failing.raw");
	
	public IBISConArgument_FailingIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

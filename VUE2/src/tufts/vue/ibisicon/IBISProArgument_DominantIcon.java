package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISProArgument_DominantIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.pro_argument_dominant.raw");
	
	public IBISProArgument_DominantIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISConArgument_DominantIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.con_argument_dominant.raw");
	
	public IBISConArgument_DominantIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

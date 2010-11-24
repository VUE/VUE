package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISUranusIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.uranus.raw");
	
	public IBISUranusIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

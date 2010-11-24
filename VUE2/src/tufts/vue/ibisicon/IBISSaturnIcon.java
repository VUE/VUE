package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISSaturnIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.saturn.raw");
	
	public IBISSaturnIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISProArgumentIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.pro_argument.raw");
	
	public IBISProArgumentIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

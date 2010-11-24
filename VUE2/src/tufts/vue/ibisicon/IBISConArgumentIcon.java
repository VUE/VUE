package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISConArgumentIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.con_argument.raw");
	
	public IBISConArgumentIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

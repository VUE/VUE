package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISPlutoIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.pluto.raw");
	
	public IBISPlutoIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISNeptuneIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.neptune.raw");
	
	public IBISNeptuneIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISAnswer_LikelyIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.answer_likely.raw");
	
	public IBISAnswer_LikelyIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

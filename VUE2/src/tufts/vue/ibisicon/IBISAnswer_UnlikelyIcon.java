package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISAnswer_UnlikelyIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.answer_unlikely.raw");
	
	public IBISAnswer_UnlikelyIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

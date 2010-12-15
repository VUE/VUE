package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISAnswerIcon extends IBISImageIcon {
	
	private static File mImageFile = VueResources.getFile("IBISNodeTool.answer.raw");
	
	public IBISAnswerIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

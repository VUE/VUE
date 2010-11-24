package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISAcceptedAnswerIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.accepted_answer.raw");
	
	public IBISAcceptedAnswerIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}
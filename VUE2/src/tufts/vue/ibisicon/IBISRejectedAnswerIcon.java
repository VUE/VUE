package tufts.vue.ibisicon;

import java.awt.Image;
import java.io.File;

import tufts.vue.*;

public class IBISRejectedAnswerIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.rejected_answer.raw");
	
	public IBISRejectedAnswerIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

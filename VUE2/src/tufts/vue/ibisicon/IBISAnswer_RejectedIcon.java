package tufts.vue.ibisicon;

import java.awt.Image;
import java.io.File;

import tufts.vue.*;

public class IBISAnswer_RejectedIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.answer_rejected.raw");
	
	public IBISAnswer_RejectedIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

package tufts.vue.ibisicon;

import java.io.File;

import tufts.vue.*;

public class IBISAnswer_AcceptedIcon extends IBISImageIcon {
	private static File mImageFile = VueResources.getFile("IBISNodeTool.answer_accepted.raw");
	
	public IBISAnswer_AcceptedIcon() {
		super(mImageFile.toString());
	}
	
	public void setImageFile(File f) {
		
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	}
}

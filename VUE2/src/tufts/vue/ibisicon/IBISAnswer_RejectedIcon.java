package tufts.vue.ibisicon;

import java.awt.Image;
import java.io.File;

import tufts.vue.*;

public class IBISAnswer_RejectedIcon extends IBISImageIcon {
	private static Image mImage = VueResources.getImage("IBISNodeTool.answer_rejected.raw");
	
	public IBISAnswer_RejectedIcon() {
		super(mImage);
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

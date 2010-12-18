package tufts.vue.ibisicon;

import java.io.File;
import java.awt.*;

import tufts.vue.*;

public class IBISAnswer_UnlikelyIcon extends IBISImageIcon {
	private static Image mImage = VueResources.getImage("IBISNodeTool.answer_unlikely.raw");
	
	public IBISAnswer_UnlikelyIcon() {
		super(mImage);
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

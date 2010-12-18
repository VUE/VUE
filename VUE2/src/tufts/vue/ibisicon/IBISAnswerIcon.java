package tufts.vue.ibisicon;

import java.io.File;
import java.awt.*;

import tufts.vue.*;

public class IBISAnswerIcon extends IBISImageIcon {
	
	private static Image mImage = VueResources.getImage("IBISNodeTool.answer.raw");
	
	public IBISAnswerIcon() {
		super(mImage);
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

package tufts.vue.ibisicon;

import java.io.File;
import java.awt.*;

import tufts.vue.*;

public class IBISConArgumentIcon extends IBISImageIcon {
	private static Image mImage = VueResources.getImage("IBISNodeTool.con_argument.raw");
	
	public IBISConArgumentIcon() {
		super(mImage);
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}
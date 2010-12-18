package tufts.vue.ibisicon;

import java.io.File;
import java.awt.*;

import tufts.vue.*;

public class IBISProArgumentIcon extends IBISImageIcon {
	private static Image mImage = VueResources.getImage("IBISNodeTool.pro_argument.raw");
	
	public IBISProArgumentIcon() {
		super(mImage);
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

package tufts.vue.ibisicon;

import java.io.File;
import java.awt.*;

import tufts.vue.*;

public class IBISProArgument_DominantIcon extends IBISImageIcon {
	private static Image mImage = VueResources.getImage("IBISNodeTool.pro_argument_dominant.raw");
	
	public IBISProArgument_DominantIcon() {
		super(mImage);
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

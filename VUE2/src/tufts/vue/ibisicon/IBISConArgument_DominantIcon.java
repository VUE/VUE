package tufts.vue.ibisicon;

import java.io.File;
import java.awt.*;

import tufts.vue.*;

public class IBISConArgument_DominantIcon extends IBISImageIcon {
	private static Image mImage = VueResources.getImage("IBISNodeTool.con_argument_dominant.raw");
	
	public IBISConArgument_DominantIcon() {
		super(mImage);
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

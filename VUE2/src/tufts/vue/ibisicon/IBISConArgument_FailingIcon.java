package tufts.vue.ibisicon;

import java.io.File;
import java.awt.*;

import tufts.vue.*;

public class IBISConArgument_FailingIcon extends IBISImageIcon {
	private static Image mImage = VueResources.getImage("IBISNodeTool.con_argument_failing.raw");
	
	public IBISConArgument_FailingIcon() {
		super(mImage);
	}
	
	public void setImage(Image i) {
		
		mImage = i;
	}
	
	public Image getImage() {
		
		return mImage;
	}
}

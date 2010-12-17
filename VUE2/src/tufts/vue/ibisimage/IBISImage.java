package tufts.vue.ibisimage;

import java.awt.*;
import java.io.*;

import tufts.vue.*;
import tufts.vue.LWComponent.Flag;
import tufts.vue.ibisicon.*;

public abstract class IBISImage extends LWImage {
	
	// HO 18/11/2010 BEGIN ***************
	//private LWImage mImage = null;
	// HO 18/11/2010 END ***************
	private IBISImageIcon mIcon = null;
	
	public IBISImage() {
		
	}
	
	public IBISImage(Resource r) {
		super(r);
		//super.setSizeImpl(64,64, true);
	}
	
	// HO 18/11/2010 BEGIN ***************
	/* public abstract void setImage(Resource r);
	
	public abstract LWImage getImage(); */
	// HO 18/11/2010 END ***************
	
	public abstract void setIcon();
	
	public abstract IBISImageIcon getIcon();

	
}

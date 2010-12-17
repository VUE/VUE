package tufts.vue.ibisimage;

import java.awt.*;
import java.io.*;

import tufts.vue.*;
import tufts.vue.LWComponent.Flag;
import tufts.vue.ibisicon.*;

public abstract class IBISImage extends LWImage {
	
	private IBISImageIcon mIcon = null;
	
	public IBISImage() {
		
	}
	
	public IBISImage(Resource r) {
		super(r);
	}
	
	public abstract void setIcon();
	
	public abstract IBISImageIcon getIcon();

	
}

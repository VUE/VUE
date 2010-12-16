package tufts.vue.ibisimage;

import java.awt.*;
import java.io.*;

import tufts.vue.*;
import tufts.vue.ibisicon.*;

public class IBISConArgument_FailingImage extends IBISImage {
	
	private static File mImageFile = VueResources.getFile("IBISNodeTool.con_argument_failing.raw");
	private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	
	private IBISImageIcon mIcon = null;
	
	public IBISConArgument_FailingImage() {
		super(mImageResource);
		this.setIcon();
	}
	
	public void setImageFile(File f) {
	
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	} 
	
	public void setImageResource(Resource r) {
		
		mImageResource = r;
	}
	
	public Resource getImageResource() {
		
		return mImageResource;
	} 
	
	public void setIcon() {
		
		mIcon = new IBISProArgumentIcon();
	}
	
	public IBISImageIcon getIcon() {
		
		return mIcon;
	}
}
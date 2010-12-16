package tufts.vue.ibisimage;

import java.awt.*;
import java.io.*;

import tufts.vue.*;
import tufts.vue.ibisicon.*;

public class IBISAnswer_UnlikelyImage extends IBISImage {
	
	private static File mImageFile = VueResources.getFile("IBISNodeTool.answer_unlikely.raw");
	private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	
	private IBISImageIcon mIcon = null;
	
	public IBISAnswer_UnlikelyImage() {
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
		
		mIcon = new IBISAnswer_AcceptedIcon();
	}
	
	public IBISImageIcon getIcon() {
		
		return mIcon;
	}
}
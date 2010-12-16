package tufts.vue.ibisimage;

import java.awt.*;
import java.io.*;

import tufts.vue.*;
import tufts.vue.ibisicon.*;

public class IBISIssue_InsolubleImage extends IBISImage {
	
	private static File mImageFile = VueResources.getFile("IBISNodeTool.issue_insoluble.raw");
	private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	
	private IBISImageIcon mIcon = null;
	
	public IBISIssue_InsolubleImage() {
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
		
		mIcon = new IBISIssue_InsolubleIcon();
	}
	
	public IBISImageIcon getIcon() {
		
		return mIcon;
	}
}
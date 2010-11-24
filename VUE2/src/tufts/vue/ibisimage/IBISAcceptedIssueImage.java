package tufts.vue.ibisimage;

import java.awt.*;
import java.io.*;

import tufts.vue.*;
import tufts.vue.ibisicon.*;

public class IBISAcceptedIssueImage extends IBISImage {
	
	private static File mImageFile = VueResources.getFile("IBISNodeTool.accepted_issue.raw");
	private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	
	private IBISImageIcon mIcon = null;
	
	public IBISAcceptedIssueImage() {
		super(mImageResource);
		this.setIcon();
		
		// HO 18/11/2010 BEGIN ***********
		Image img = this.getIcon().getImage();
		System.out.println(img);
		// HO 18/11/2010 END ***********
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
		
		mIcon = new IBISAcceptedIssueIcon();
	}
	
	public IBISImageIcon getIcon() {
		
		return mIcon;
	}
}

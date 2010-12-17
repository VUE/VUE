package tufts.vue.ibisimage;

import java.awt.*;
import java.io.*;

import tufts.vue.*;
import tufts.vue.ibisicon.*;

public class IBISProArgumentImage extends IBISImage {
	
	private static File mImageFile = VueResources.getFile("IBISNodeTool.pro_argument.image");
	private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	
	private IBISImageIcon mIcon = null;
	
	// HO 17/12/2010 BEGIN ***********
	private String saveImageFile = "";
	// HO 17/12/2010 END *************
	
	public IBISProArgumentImage() {
		super(mImageResource);
		this.setIcon();
		// HO 17/12/2010 BEGIN ***********
		// persistence only
		this.setSaveImageFile(mImageFile.toString());
		// HO 17/12/2010 END *************
	}
	
	public void setImageFile(File f) {
	
		mImageFile = f;
	}
	
	public File getImageFile() {
		
		return mImageFile;
	} 
	
    /** persistance only */
    public String getSaveImageFile() {
        return saveImageFile == null ? null : saveImageFile.toString();
    }

    /** persistance only */
    public void setSaveImageFile(String path) {
        saveImageFile = path;
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
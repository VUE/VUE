package tufts.vue.ibisimage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import tufts.vue.*;
import tufts.vue.ibisicon.*;

public class IBISConArgument_FailingImage extends IBISImage {
	
	//private static File mImageFile = VueResources.getFile("IBISNodeTool.con_argument_failing.raw");
	//private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	private static BufferedImage mImage = VueResources.getBufferedImage("IBISNodeTool.con_argument_failing.image");
	private static File mImageFile = createImageFile(VueResources.getString("IBISNodeTool.con_argument_failing.image.filename"), mImage);
	private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	
	
	private IBISImageIcon mIcon = null;
	
	// HO 17/12/2010 BEGIN ***********
	private String saveImageFile = "";
	// HO 17/12/2010 END *************
	
	public IBISConArgument_FailingImage() {
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
		
	public void setImageResource(File f) {
		mImageResource = new LWMap("dummy map").getResourceFactory().get(f);
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
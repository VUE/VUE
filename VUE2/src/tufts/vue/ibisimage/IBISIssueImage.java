package tufts.vue.ibisimage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import tufts.vue.*;
import tufts.vue.ibisicon.*;

public class IBISIssueImage extends IBISImage {
	
	
	
	// HO 17/12/2010 BEGIN EMERGENCY ******
	//private static File mImageFile = VueResources.getFile("IBISNodeTool.issue.image");
	private static BufferedImage mImage = VueResources.getBufferedImage("IBISNodeTool.issue.image");
	// HO 17/12/2010 END EMERGENCY ******
	//private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	private static File mImageFile = createImageFile(VueResources.getString("IBISNodeTool.issue.image.filename"), mImage);
	private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	
	private IBISImageIcon mIcon = null;
	
	// HO 17/12/2010 BEGIN ***********
	private String saveImageFile = "";
	// HO 17/12/2010 END *************
	
	public IBISIssueImage() {
		super(mImageResource);
		this.setIcon();

		// HO 17/12/2010 BEGIN ***********
		// persistence only
		this.setSaveImageFile(mImageFile.toString());
		// HO 17/12/2010 END *************
	}
	
	public IBISIssueImage(Resource r) {
		super(r);
	}
	
	/* private File createImageFile() {
		// create a new file in a cache directory under the home directory
		File imgFile = new File(super.getCacheDir(), VueResources.getString("IBISNodeTool.issue.image.filename"));
		try {
			// use the image to create a file, 
			// or get the existing one
			if (super.writeImageToJPG(imgFile, mImage)) 
				return imgFile;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
			return imgFile;
		}
	} */
	
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
		
		mIcon = new IBISIssueIcon();
	}
	
	public IBISImageIcon getIcon() {
		
		return mIcon;
	}
}

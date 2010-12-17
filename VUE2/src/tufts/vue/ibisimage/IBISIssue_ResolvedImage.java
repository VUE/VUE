package tufts.vue.ibisimage;

import java.awt.*;
import java.io.*;

import tufts.vue.*;
import tufts.vue.ibisicon.*;

public class IBISIssue_ResolvedImage extends IBISImage {
	
	// HO 13/12/2010 BEGIN ************
	//private static File mImageFile = VueResources.getFile("IBISNodeTool.neptune.raw");
	private static File mImageFile = VueResources.getFile("IBISNodeTool.issue_resolved.image");
	// HO 13/12/2010 END ************
	private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	
	private IBISImageIcon mIcon = null;
	
	// HO 17/12/2010 BEGIN ***********
	private String saveImageFile = "";
	// HO 17/12/2010 END *************
	
	public IBISIssue_ResolvedImage() {
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
		
		mIcon = new IBISIssue_ResolvedIcon();
	}
	
	public IBISImageIcon getIcon() {
		
		return mIcon;
	}
}
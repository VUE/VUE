/*
 * This addition Copyright 2010-2012 Design Engineering Group, Imperial College London
 * Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
 * @author  Helen Oliver, Imperial College London 
 */

package tufts.vue.ibisimage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import tufts.vue.*;
import tufts.vue.ibisicon.*;

public class IBISConArgument_DominantImage extends IBISImage {
	
	//private static File mImageFile = VueResources.getFile("IBISNodeTool.con_argument_dominant.raw");
	//private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	private static BufferedImage mImage = VueResources.getBufferedImage("IBISNodeTool.con_argument_dominant.image");
	private static File mImageFile = createImageFile(VueResources.getString("IBISNodeTool.con_argument_dominant.image.filename"), mImage);
	private static Resource mImageResource = new LWMap("dummy map").getResourceFactory().get(mImageFile);
	
	private IBISImageIcon mIcon = null;
	
	// HO 17/12/2010 BEGIN ***********
	private String saveImageFile = "";
	// HO 17/12/2010 END *************
	
	public IBISConArgument_DominantImage() {
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
		
		mIcon = new IBISConArgument_DominantIcon();
	}
	
	public IBISImageIcon getIcon() {
		
		return mIcon;
	}
}
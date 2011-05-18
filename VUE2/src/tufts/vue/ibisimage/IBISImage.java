package tufts.vue.ibisimage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import tufts.vue.*;
import tufts.vue.LWComponent.Flag;
import tufts.vue.ibisicon.*;

public abstract class IBISImage extends LWImage {
	
	protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(IBISImage.class);
	
	// HO 18/05/2011 BEGIN *******
	protected static final int DefaultIconMaxSide = 32;
    protected static final int DefaultWidth = 32;
    protected static final int DefaultHeight = 32;
    
    public static int getDefaultIconMaxSide() {
    	return DefaultIconMaxSide;
    }
    
    public static int getDefaultWidth() {
    	return DefaultWidth;
    }
    
    public static int getDefaultHeight() {
    	return DefaultHeight;
    }
    // HO 18/05/2011 END *******
    
	private IBISImageIcon mIcon = null;
	
	public IBISImage() {
		
	}
	
	public IBISImage(Resource r) {
		// HO 18/05/2011 BEGIN *******
		//super(r);
		super(r, DefaultIconMaxSide, DefaultWidth, DefaultHeight);
		// HO 18/05/2011 END *******
	}
	
	public abstract void setIcon();
	
	public abstract IBISImageIcon getIcon();
	
    private static File CacheDir;
    private static File createCacheDirectory()
    {
        if (CacheDir == null) {
            File dir = VueUtil.getDefaultUserFolder();
            CacheDir = new File(dir, "ibiscache");
            if (!CacheDir.exists()) {
                Log.debug("creating cache directory: " + CacheDir);
                if (!CacheDir.mkdir())
                    Log.warn("couldn't create cache directory " + CacheDir);
            } else if (!CacheDir.isDirectory()) {
                Log.warn("couldn't create cache directory (is a file) " + CacheDir);
                return CacheDir = null;
            }
            Log.debug("Got cache directory: " + CacheDir);
        }
        return CacheDir;
    }
    
    public void setCacheDir(File theDir) {
    	CacheDir = theDir;
    }
    
    public File getCacheDir() {
    	return createCacheDirectory();
    }
    
    public static boolean writeImageToJPG (File file,BufferedImage bufferedImage) 
       throws IOException {
    	// if the file doesn't already exist, create it	
    	if (readImageFromFile(file) == null)
    			return ImageIO.write(bufferedImage,"jpg",file);
    	else
    		return true;
    }

    public static BufferedImage readImageFromFile(File file) 
    throws IOException {
    	BufferedImage theImage = null;
    	try {
    		theImage = ImageIO.read(file);
    		//return theImage;
    	} catch(Exception e) {
    		//e.printStackTrace(); // do nothing, output nothing
    		//return theImage;
    	} finally {
    		return theImage;
    	}
    }
    
	public static File createImageFile(String theFile, BufferedImage theImage) {
		// create a new file in a cache directory under the home directory
		File imgFile = new File(createCacheDirectory(), theFile);
		try {
			// use the image to create a file, 
			// or get the existing one
			if (writeImageToJPG(imgFile, theImage)) 
				return imgFile;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			imgFile = null;
		} finally {
			return imgFile;
		}
	}
	
	// HO 18/05/2011 BEGIN **********

	// HO 18/05/2011 END ************
	
}

package tufts.vue.ibisimage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import tufts.vue.*;
import tufts.vue.LWComponent.Flag;
import tufts.vue.ibisicon.*;

import org.apache.batik.dom.svg.*;
import org.apache.batik.*;

public abstract class IBISImage extends LWImage {
	
	protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(IBISImage.class);
	
	// HO 18/05/2011 BEGIN *******
	protected static int IBISIconMaxSide = 32;
    protected static int IBISDefaultWidth = 32;
    protected static int IBISDefaultHeight = 32;
    
    public static int getIBISIconMaxSide() {
    	return IBISIconMaxSide;
    }
    
    public static int getIBISDefaultWidth() {
    	return IBISDefaultWidth;
    }
    
    public static int getIBISDefaultHeight() {
    	return IBISDefaultHeight;
    }
    // HO 18/05/2011 END *******
    
	private IBISImageIcon mIcon = null;
	
	public IBISImage() {
		
	}
	
	public IBISImage(Resource r) {
		// HO 18/05/2011 BEGIN *******
		//super(r);
		super(r, IBISIconMaxSide, IBISDefaultWidth, IBISDefaultHeight, false);
		// HO 18/05/2011 END *******
	}
	
    // HO 20/05/2011 BEGIN *******
    public IBISImage(Resource r, int iconMaxSide, int width, int height, boolean unsized) {
    	super(r, iconMaxSide, width, height, unsized);
    	IBISIconMaxSide = iconMaxSide;
    	IBISDefaultWidth = width;
    	IBISDefaultHeight = height;
    }
    // HO 20/05/2011 END *********
	
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
    
    // HO 26/05/2011 BEGIN **********  
    private static String getFileExtension(File file) {
    	// input validation
    	if (file == null)
    		return "";

    	// get file extension
    	String fileName = file.getName();
    	int mid= fileName.lastIndexOf(".");
    	String ext=fileName.substring(mid+1,fileName.length()); 
    	
    	return ext;
    }
    
    public static boolean writeImageToFile (File file,BufferedImage bufferedImage) 
    throws IOException {
    	// input validation
    	if (file == null)
    		return false;
    	
    	String strExtn = getFileExtension(file);
    	
	 	// if the file doesn't already exist, create it	
	 	if (readImageFromFile(file) == null)
	 			return ImageIO.write(bufferedImage,strExtn,file);
	 	else
	 		return true;
    }
    
    /* public static BufferedImage readImageFromSVGFile(File file) {
    	BufferedImage theImage = null;
    	
		try {
			//SVGRasterizer r = new SVGRasterizer(file.toURI().toString());
			// HO 31/05/2011 BEGIN *****
			//theImage = r.createBufferedImage();
			//theImage = r.createJPG(file);
			MyTranscoder transcoder = new MyTranscoder();
		    BufferedImage image = transcoder.getThatImage(file.toString());
		} catch(Exception e) {
			
		} finally {
			return theImage;
		}
    } */
    
    public static BufferedImage readImageFromImageIO(File file) {
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
    // HO 26/05/2011 END ************

    public static BufferedImage readImageFromFile(File file) 
    throws IOException {
    	BufferedImage theImage = null;
    	// HO 26/05/2011 BEGIN ************
    	/* if (file.getName().endsWith(".svg")) {
    		theImage = readImageFromSVGFile(file);
    		return theImage;
    	} else { */   	
	    	theImage = readImageFromImageIO(file);
	    	return theImage;
    	// }
    	// HO 26/05/2011 END ************
    }
    
	public static File createImageFile(String theFile, BufferedImage theImage) {
		// create a new file in a cache directory under the home directory
		File imgFile = new File(createCacheDirectory(), theFile);
		try {
			// use the image to create a file, 
			// or get the existing one
			// HO 26/05/2011 BEGIN **********
			//if (writeImageToJPG(imgFile, theImage)) 
			if (writeImageToFile(imgFile, theImage))
				return imgFile;
			// HO 26/05/2011 END **********
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

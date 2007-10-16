/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue.action;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import tufts.vue.*;

/**
 * @version $Revision: 1.21 $ / $Date: 2007-10-16 20:35:38 $ / $Author: mike $ *
 * @author  Daisuke Fujiwara
 */

/**a class which constructs a JPEG image of the current concept map*/
public class ImageConversion extends VueAction {
    
    /** Creates a new instance of ImageConversion */
    public ImageConversion() {
    }
    
    /**A constructor */
    public ImageConversion(String label)
    {
        super(label);
    }
    
    /**A method which takes in the image object and the location of the file along with the file format
     and saves the current map to the given file*/
    public static void convert(BufferedImage image, File location, String format)
    {   
        try
        {
            if (DEBUG.IO || DEBUG.IMAGE)
                System.out.println("ImageIO.write " + image + " fmt=" + format + " to " + location);        
            ImageIO.write(image, format, location);
        }
        catch (Exception e)
        {
            System.out.println("Couldn't write to the file:" + e);
        }
    }
    
    /**A method which sets up for converting the active viewer to a Jpeg file*/
    public static void createActiveMapJpeg(File location)
    {
        convert(VUE.getActiveMap().getAsImage(1.0), location, "jpeg");
    }
    
    /**A method which sets up for converting the active viewer to a Jpeg file*/
    public static void createActiveMapPng(File location)
    {
        convert(VUE.getActiveMap().getAsImage(1.0), location, "png");
    }
    
    public void act() {
       try {

           File selectedFile = ActionUtil.selectFile("Saving JPEG", "jpeg");
           
           if (selectedFile != null)
               createActiveMapJpeg(selectedFile);

       } catch (Throwable t) {
            System.out.println("Couldn't convert to jpeg:" + t);
       }   
    }
    
}

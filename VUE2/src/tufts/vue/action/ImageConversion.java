/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

package tufts.vue.action;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import tufts.vue.*;

/**
 * @version $Revision: 1.25 $ / $Date: 2008-06-30 20:44:59 $ / $Author: sfraize $ *
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
    public static void createActiveMapJpeg(File location, double zoomFactor)
    {
        // todo: jpeg output quality is poor; see:
        // http://www.universalwebservices.net/web-programming-resources/java/adjust-jpeg-image-compression-quality-when-saving-images-in-java
        convert(VUE.getActiveMap().getAsImage(zoomFactor), location, "jpeg");
    }
    
    /**A method which sets up for converting the active viewer to a Jpeg file*/
    public static Dimension createActiveMapPng(File location,double zoomFactor)
    {
    	BufferedImage bi = VUE.getActiveMap().getAsImage(zoomFactor);
        convert(bi, location, "png");
        Dimension d = new Dimension(bi.getWidth(),bi.getHeight());
        return d;
    }
    
    public void act() {
       try {

           File selectedFile = ActionUtil.selectFile("Saving JPEG", "jpeg");
           
           if (selectedFile != null)
               createActiveMapJpeg(selectedFile,tufts.vue.VueResources.getDouble("imageExportFactor"));

       } catch (Throwable t) {
            System.out.println("Couldn't convert to jpeg:" + t);
       }   
    }
    
}

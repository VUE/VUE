package tufts.vue.action;

/*
 * ImageConversion.java
 *
 * Created on May 28, 2003, 11:08 AM
 */

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import tufts.vue.*;
/**
 *
 * @author  Daisuke Fujiwara
 */

/**a class which constructs a JPEG image of the current concept map*/
public class ImageConversion extends AbstractAction {
    
    /** Creates a new instance of ImageConversion */
    public ImageConversion() {
    }
    
    /**A constructor */
    public ImageConversion(String label)
    {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);        
    }
    
    /**A method which takes in the image object and the location of the file along with the file format
     and saves the current map to the given file*/
    public void convert(BufferedImage image, File location, String format)
    {   
        //the conversion is done using the ImageIO class's static method
        try
        {
            ImageIO.write(image, format, location);
        }
        catch (Exception e)
        {
            System.out.println("Couldn't write to the file:" + e);
        }
    }
    
    /**A default draw function for the testing purpose */
    private void drawImage(Graphics2D g2, Dimension size)
    {
        g2.setColor(Color.white);
        g2.fill(new Rectangle(0, 0, size.width - 1, size.height - 1));
        g2.setColor(Color.red);
        g2.draw(new Rectangle(0, 0, size.width - 1, size.height - 1));
        
    }
    
    /** A main function for the testing purpose */
    public static void main (String[] args)
    {
        BufferedImage defaultImage = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);       
        ImageConversion test = new ImageConversion();
        
        test.drawImage((Graphics2D)defaultImage.getGraphics(), new Dimension(300, 400));
        test.convert(defaultImage, new File("c:\\test.jpeg"), "jpeg");
    }
    
    /**A method defined in the interface */
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        
        try 
        {
           File selectedFile = ActionUtil.selectFile("Saving JPEG", "jpeg");
           
           if (selectedFile != null)
             {
                //retrives the current map and gets its size
                MapViewer currentMap = VUE.getActiveViewer();
                Dimension size = currentMap.getSize();
        
                //creates an image object and sets up the graphics object of the image
                BufferedImage mapImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
                Graphics g = mapImage.getGraphics();
                g.setClip(0, 0, size.width, size.height);
        
                //let the map draws to the image object's graphic object
                currentMap.paintComponent(g);
                
                //outlining the returned image
                g.setColor(Color.black);
                g.drawRect(0, 0, size.width - 1, size.height - 1);
        
                //begins the conversion to the file
                convert(mapImage, selectedFile, "jpeg");
            }
       }
        
       catch(Exception ex) 
       {
            System.out.println("Couldn't convert to JPEG:" + ex);
       }   
    }
    
}

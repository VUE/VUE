package tufts.vue;

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
/**
 *
 * @author  Daisuke Fujiwara
 */
public class ImageConversion extends AbstractAction {
    
    /** Creates a new instance of ImageConversion */
    public ImageConversion() {
    }
    
    public ImageConversion(String label)
    {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);        
    }
    
    public void convert(BufferedImage image, String location, String format)
    {   
        try
        {
            //String[] files = ImageIO.getReaderFormatNames();
            //  for (int i = 0; i < files.length; i++)
            //    System.out.println(files[i]);
            
            System.out.println(location);
            ImageIO.write(image, format, new File(location));
        }
        catch (Exception e)
        {
            System.out.println("Couldn't write to the file:" + e);
        }
    }
    
    private void drawImage(Graphics2D g2, Dimension size)
    {
        g2.setColor(Color.white);
        g2.fill(new Rectangle(0, 0, size.width - 1, size.height - 1));
        g2.setColor(Color.red);
        g2.draw(new Rectangle(0, 0, size.width - 1, size.height - 1));
        
    }
    
    public static void main (String[] args)
    {
        BufferedImage defaultImage = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);       
        ImageConversion test = new ImageConversion();
        
        test.drawImage((Graphics2D)defaultImage.getGraphics(), new Dimension(300, 400));
        test.convert(defaultImage, "c:\\test.jpeg", "jpeg");
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        System.out.println("Performing Conversion:" + actionEvent.getActionCommand());
        
        MapViewer currentMap = (MapViewer)VUE.tabbedPane.getSelectedComponent();
        Dimension size = currentMap.getSize();
        
        BufferedImage mapImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics g = mapImage.getGraphics();
        g.setClip(0, 0, size.width, size.height);
        currentMap.paintComponent(g);
        
        g.setColor(Color.black);
        g.drawRect(0, 0, size.width - 1, size.height - 1);
        
        convert(mapImage, "c:\\" + currentMap.getMap().getLabel() + ".jpeg", "jpeg");
    }
    
}

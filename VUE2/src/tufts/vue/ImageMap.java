/*
 * ImageMap.java
 *
 * Created on May 29, 2003, 5:14 PM
 */

package tufts.vue;

/**
 *
 * @author  Jay Briedis
 */
import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;

public class ImageMap extends AbstractAction {
    
    /** Creates a new instance of ImageConversion */
    public ImageMap() {
    }
    
    public ImageMap(String label)
    {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);        
    }
    
    public void convert(BufferedImage image, String location, String format)
    {   
        try
        {
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
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        System.out.println("Performing Conversion for ImageMap:" + actionEvent.getActionCommand());
        
        MapViewer currentMap = (MapViewer)VUE.tabbedPane.getSelectedComponent();
        Dimension size = currentMap.getSize();
        
        BufferedImage mapImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics g = mapImage.getGraphics();
        g.setClip(0, 0, size.width, size.height);
        currentMap.paintComponent(g);
        
        g.setColor(Color.black);
        g.drawRect(0, 0, size.width - 1, size.height - 1);
        String filename = "c:\\example.jpeg";
        convert(mapImage, filename, "jpeg");
        String imageName = "example.jpeg";
        createHtml(imageName, currentMap);
    }

    private void createHtml(String file, MapViewer map){
            
            String out = "<HTML><HEAD><TITLE>Image Map Test</TITLE></HEAD><BODY>";
            Dimension size = map.getSize();
            out += "<img src=\""+file+"\" border=0 usemap=\"#map\" HEIGHT="+size.height+" WIDTH="+size.width+">";
            out += "<map name=\"map\">";
            
            java.util.List list = (java.util.List) map.getAllLWNodes();
            java.util.Iterator iter = list.iterator();
            while(iter.hasNext()){
                LWNode node = (LWNode)iter.next();
                String shape = "rect";
                MapItem item = node.getMapItem();
                String label = item.getLabel();
                String res = "";
                if(item.getResource() != null){
                    Resource resource = item.getResource();
                    res = resource.toString();
                    if(!res.startsWith("http://")) res = "file:///" + res;
                } 
                else res = "null";
                int ox = (int)node.getX();
                int oy = (int)node.getY();
                int ow = (int)node.getWidth();
                int oh = (int)node.getHeight();
                String href = "";
                if(res.equals("null")) href = "nohref";
                else href = "href=" + res;
                out += "<area " + href
                    +" alt=\""+label
                    +"\" shape=\""+shape
                    +"\" coords=\""+ox
                    +","+oy
                    +","+(ox + ow)
                    +","+(oy + oh)
                    +"\">";
            }
            out += "</map></BODY></HTML>"; 
            System.out.println("out: \n   " +out); 
            try{         
                File outputFile = new File("C:\\ImageMap.html");
                FileWriter output = new FileWriter(outputFile);
                output.write(out);
                output.close();
                System.out.println("wrote to the file...");
            }catch(IOException ioe){
                System.out.println("Error trying to write to html file: " + ioe);
            }    
    }
}

package tufts.vue.action;

/*
 * SVGConversion.java
 *
 * Created on June 3, 2003, 3:29 PM
 */

import java.awt.*;
import javax.swing.*;
import java.io.*;
import org.apache.xerces.dom.*;
import org.apache.batik.svggen.*;
import org.w3c.dom.*;
import tufts.vue.*;
/**
 *
 * @author  Daisuke Fujiwara 
 */

/**A class which converts the current map image into SVG format and saves the content to a file*/
public class SVGConversion extends AbstractAction {
    
    /** Creates a new instance of SVGConversion */
    public SVGConversion() {
    }
    
    public SVGConversion(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }
    
    /**A method which converts the given Java graphics into the SVG form and writes the output
       to a given file*/
    public void createSVG(MapViewer map, File location)
    {
        //sets up the document object model
        Document document = new DocumentImpl();
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
       
        Dimension size = map.getSize();
        svgGenerator.setClip(0, 0, size.width, size.height);
        
        //renders the map image into the SVGGraphics object
        map.paintComponent(svgGenerator);
        
        svgGenerator.setColor(Color.black);
        svgGenerator.drawRect(0, 0, size.width - 1, size.height - 1);
        
        try
        {
            //using the SVGGraphics object, write the SVG content to the given file
            FileWriter out = new FileWriter(location);
            svgGenerator.stream(out, false);
            out.flush();
            out.close();
        }
        
        catch (IOException e)
        {
            System.err.println("Couldn't convert to SVG:" + e);
        }
        
        //there might be a better way to do this
        replaceHeader(location);
    }
    
    /**A class which replaces the encoding of the xml to be utf - 8*/
    public void replaceHeader(File file)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(file));
                        
            String line, content = null;
            boolean firstLine = true; 
            
            //replaces the first line - xml declaration 
            while ((line = reader.readLine()) != null)
            {
                if (firstLine)
                {
                    content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
                    firstLine = false;
                }
                
                else
                    content += line;
            }
            
            reader.close();
            
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
            writer.close();
        }
        
        catch (Exception e)
        {
            System.err.println("Something went wront when replacing the header:" + e);
        }        
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        
       //displays the file chooser so that the user can select the file to save the SVG image into
       try 
       {
          File selectedFile = ActionUtil.selectFile("Saving SVG", "svg");
          
          if (selectedFile != null)
            {
                //gets the currently selected map
                MapViewer currentMap = VUE.getActiveViewer();
                createSVG(currentMap, selectedFile);
            }
       }
        
       catch(Exception ex) 
       {
            System.out.println("Couldn't convert to SVG:" + ex);
       }   
    }
}

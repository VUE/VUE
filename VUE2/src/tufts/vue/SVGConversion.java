package tufts.vue;

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
    public void createSVG(MapViewer map, String location)
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
    public void replaceHeader(String location)
    {
        try
        {
            File file = new File(location);
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
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save SVG File");
            //chooser.setFileFilter(new VueFileFilter());
            
            if(VueUtil.isCurrentDirectoryPathSet()) 
                chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        
            int option = chooser.showDialog(tufts.vue.VUE.frame, "Save");
        
            if (option == JFileChooser.APPROVE_OPTION) {
                String fileName = chooser.getSelectedFile().getAbsolutePath();
                // if they choose nothing, fileName will be null -- detect & abort
                
                //if the file name doesn't have the right extension
                if (!fileName.endsWith(".svg"))
                    fileName += ".svg";
                
                VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
                
                //gets the currently selected map
                MapViewer currentMap = (MapViewer)VUE.tabbedPane.getSelectedComponent();
                createSVG(currentMap, fileName);
            }
       }
        
       catch(Exception ex) 
       {
            System.out.println("Couldn't convert to SVG:" + ex);
       }   
    }
}

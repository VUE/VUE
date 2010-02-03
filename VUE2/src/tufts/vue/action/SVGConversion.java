/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

/*
 * SVGConversion.java
 *
 * Created on June 3, 2003, 3:29 PM
 */

import java.awt.*;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import java.io.*;
import org.apache.xerces.dom.*;
import org.apache.batik.dom.GenericDOMImplementation;
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
    
  
    public static void createSVG(File location)
    {
        LWMap currentMap = VUE.getActiveMap();
        createSVG(location,currentMap);
    }
    
    /**A method which converts the given Java graphics into the SVG form and writes the output
       to a given file*/
    public static void createSVG(File location,LWMap currentMap)
    {
        
        //sets up the document object model
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd";
        Document document = domImpl.createDocument(svgNS, "svg", null);//new DocumentImpl();
        //SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);
        ctx.setEmbeddedFontsOn(true);
        
       // ctx.set
        SVGGraphics2D svgGenerator = new SVGGraphics2D(ctx, true);
    //    svgGenerator.setUnsupportedAttributes(arg0)
        //Rectangle2D bounds = map.getAllComponentBounds();
        //int xLocation = (int)bounds.getX() + 5, yLocation = (int)bounds.getY() + 5;
        //Dimension size = new Dimension((int)bounds.getWidth() + xLocation, (int)bounds.getHeight() + yLocation);
        
        Rectangle2D bounds = currentMap.getBounds();
   //     Dimension size = new Dimension((int)bounds.getWidth(), (int)bounds.getHeight());
        
        //draws the background and the border of the image
   /*     svgGenerator.setColor(Color.white);
        svgGenerator.fillRect(0, 0, size.width, size.height);
        svgGenerator.setColor(Color.black);
        svgGenerator.drawRect(0, 0, size.width-1, size.height-1);        
        //translate and set the clip for the map content
        svgGenerator.translate(-(int)bounds.getX(), -(int)bounds.getY());    
     */
        svgGenerator.setClip(bounds);   
        
        //renders the map image into the SVGGraphics object
        //map.paintComponent(svgGenerator);
          
        DrawContext dc = new DrawContext(svgGenerator);
        dc.setMapDrawing();
        dc.setDraftQuality();
        dc.setAntiAlias(false);
        
        dc.setClipOptimized(false);	
      //  dc.setRawDrawing();
        //dc.setClipOptimized(false);
        
        dc.setInteractive(false);
        dc.setDrawPathways(false);

        // render the map
        LWPathway.setShowSlides(false);
        currentMap.draw(dc);
        LWPathway.setShowSlides(true);
        
        
        try
        {
            //using the SVGGraphics object, write the SVG content to the given file
        //    FileWriter out = new FileWriter(location);
        	
        	Writer out = new OutputStreamWriter(new FileOutputStream(location),"UTF-8");
            svgGenerator.stream(out, true);
            out.flush();
            out.close();
        }
        
        catch (IOException e)
        {
            System.err.println("Couldn't convert to SVG:" + e);
        }
        catch (java.lang.OutOfMemoryError error)
        {
        	System.err.println("Couldn't convert to SVG:" + error);
        }
        /*
         * There's no explanation to why this is being done, I'm not really sure
         * why you would do this.  Every application I tried appeared to open the
         * file fine without this so I'm removing it, and it causes VUE to hang. -MK
         */
        //there might be a better way to do this
      //  replaceHeader(location);
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
            createSVG(selectedFile);    
       }
        
       catch(Exception ex) 
       {
            System.out.println("Couldn't convert to SVG:" + ex);
       }   
    }
}

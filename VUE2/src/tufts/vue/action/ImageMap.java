/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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


import java.io.*;
import java.util.Iterator;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import tufts.vue.*;

/**
 * @version $Revision: 1.24 $ / $Date: 2008-09-15 14:19:33 $ / $Author: dan $ *
 * @author  Jay Briedis
 */
public class ImageMap extends VueAction {
    
	private Dimension imageDimensions;
    private int xOffset, yOffset;
   
    /** Creates a new instance of ImageConversion */
    public ImageMap() {}
    
    public ImageMap(String label) {
        super(label);
    }
    
    public void act()
    {
        File selectedFile = ActionUtil.selectFile("Saving Imap", "html");
        
        if (selectedFile != null)
            createImageMap(selectedFile);
    }

    public void createImageMap(File file)
    {    
       // See: VUE-536 in JIRA, If SaveAction Class still chooses "html" as the file type for image maps
       // html file will already not be overwritten
       //String imageLocation = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-5)+"-for-image-map"+".jpeg";
       //String imageName = file.getName().substring(0, file.getName().length()-5)+"-for-image-map"+".jpeg";
       String imageLocation = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-5)+".png";
       String imageName = file.getName().substring(0, file.getName().length()-5)+".png";
       String fileName = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-5)+".html";
       
       File imageLocationFile = new File(imageLocation);
       
       if(imageLocationFile.exists())
       {
          int confirm = VueUtil.confirm("png image already exists, overwrite?","File Already Exists");
          if(confirm == javax.swing.JOptionPane.NO_OPTION)
          {
              VueUtil.alert("Image map not saved","Image Map");
              return;
          }
       }
       
       //createJpeg(imageLocation, "jpeg", currentMap, size);
       //ImageConversion.createActiveMapJpeg(new File(imageLocation));
       imageDimensions = ImageConversion.createActiveMapPng(imageLocationFile,1.0);
       createHtml(imageName, fileName);
    }
    
  
    private int nodeCounter = 0;
    private String writeOutCss(String imageName,LWContainer container)
    {
        String out = new String();
         
        //java.util.Iterator iter =  container.getNodeIterator();
        Iterator iter = container.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
       
        
        while(iter.hasNext())
        {
        	LWNode node = null;
        	
            LWComponent comp = (LWComponent)iter.next();
            
            if (comp instanceof LWNode && (!(comp instanceof LWGroup)))
            	node = (LWNode)comp;
            else
            	continue;
            
            // seemed to be creating duplicates (getAllDescendants already traverses entire tree..)
            //if (node.hasChildren())
            //  out += writeOutCss(imageName,(LWContainer)node);
                
            String altLabel = null;
            
            if ((altLabel = node.getNotes()) == null)
            {
            	 if(node.getResource() != null){
                     Resource resource = node.getResource();
                     //res = resource.toString();
                     altLabel=resource.getSpec();
                     
                     // see VUE-873 getSpec() should be fine for now
                     //if(!(res.startsWith("http://") || res.startsWith("https://"))) res = "file:///" + res;
                 } 
                 else
                	 continue;
            }
            int groupX=0;
            int groupY=0;
            
            if (node.getParent() instanceof LWGroup)
            {
            	groupX =(int)node.getParent().getX();
            	groupY =(int)node.getParent().getY();
            }	
            String res = "";
            int ox = (int)node.getMapX() + groupX -  xOffset;
            int oy = (int)node.getMapY() + groupY -  yOffset;
            int ow = (int)node.getWidth();
            int oh = (int)node.getHeight();
                        
            out+="#map a.node" + nodeCounter +"{\n";
            out+="	top:"+ oy + "px;\n";
            out+="	left:"+ ox + "px;\n";
            out+="	width:" +ow + "px;\n";
            out+="	height:" + oh + "px;\n";
    		out+="}\n\n";
    	//	System.out.println("Write out CSS : " + nodeCounter + " "+ node.getLabel());            
    		nodeCounter++;
        }
        
        return out;
    }
    private String writeOutLi(LWContainer container)
    {
        String out = new String();
         
        //java.util.Iterator iter =  container.getNodeIterator();
        Iterator iter = container.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        
        while(iter.hasNext())
        {
        	LWNode node = null;
        	
            LWComponent comp = (LWComponent)iter.next();
            
            if (comp instanceof LWNode && (!(comp instanceof LWGroup)))
            	node = (LWNode)comp;
            else
            	continue;
            
            // seemed to be creating duplicates (getAllDescendants already traverses entire tree..)
            //if (node.hasChildren())
            //  out += writeOutLi((LWContainer)node);
            
            String shape = "rect";
            String altLabel = null;
            
            if ((altLabel = node.getNotes()) == null)
            {
            	 if(node.getResource() != null){
                     Resource resource = node.getResource();
                     //res = resource.toString();
                     altLabel=resource.getSpec();
                     
                     // see VUE-873 getSpec() should be fine for now
                     //if(!(res.startsWith("http://") || res.startsWith("https://"))) res = "file:///" + res;
                 } 
                 else
                	 continue;
            }
            	
            
            String res = "";
            String href = "";
            
            if(node.getResource() != null){
                Resource resource = node.getResource();
                //res = resource.toString();
                res = resource.getSpec();
                
                // see VUE-873 getSpec() should be fine for now
                //if(!(res.startsWith("http://") || res.startsWith("https://"))) res = "file:///" + res;
            } 
            else res = "null";
            
            if(res.equals("null")) href = "nohref";
            else href = "href=\"" + res + "\" target=\"_blank\"";
            
            	if (href.equals("nohref"))
            		out+="<li><a class=\"node" + nodeCounter+"\" href=\"#\" ><span><b>";
            	else
            		out+="<li><a class=\"node" + nodeCounter+"\"" + " " + href+" ><span><b>";            		
            	out+=altLabel +"\n";
            	out+="</b><br></span></a></li>\n";
    		//System.out.println("Write out CSS : " + nodeCounter + " "+ node.getLabel());      
            nodeCounter++;
        }
         
        return out;
    }
    
    private void createHtml(String imageName, String fileName){
        
        LWMap currentMap = VUE.getActiveMap();   
        Rectangle2D bounds = currentMap.getBounds();
        xOffset = (int)bounds.getX()-30; 
        yOffset = (int)bounds.getY()-30;
        Dimension size = new Dimension((int)bounds.getWidth(), (int)bounds.getHeight());
        
        String out = "<HTML><HEAD><TITLE>" + currentMap.getLabel();
        out += "</TITLE>";
        //write out css
        
        out +="<style type=\"text/css\">\n";
        out+="#map {\n";
        out+="	margin:0;\n";
        out+="	padding:0;\n";
        out+="	width:"+(int)imageDimensions.getWidth()+"px;\n";
        out+="	height:"+(int)imageDimensions.getHeight()+"px;\n";
        out+="	background:url(" + imageName +") top left no-repeat #fff;\n";
        out+="	font-family:arial, helvetica, sans-serif;\n";
        out+="	font-size:8pt;}\n\n";
        out+="#map li {\n";
        out+="	margin:0;\n";
        out+="	padding:0;\n";
        out+="	list-style:none;}\n\n";
        out+="#map li a {\n";
        out+="	position:absolute;\n";
        out+="	width:50px;\n";
        out+="	height:50px;\n";
        out+="	display:block;\n";
        out+="	text-decoration:none;\n";
        out+="	color:#000;}\n\n";
        out+="#map li a span { width:300px;\n";
        out+="	display:block;\n";
        out+="	filter: alpha(opacity=0);\n";
        out+="	opacity:0.0; }\n\n";
        out+="#map li a:hover span {\n";
        out+="	position:relative;\n";
        out+="	display:block;\n";
        out+="	width:350px;\n";
        out+="	left:10px;\n";
        out+="	top:15px;\n";
        out+="	border:1px solid #000;\n";
        out+="	background:#fff;\n";
        out+="	padding:5px;\n";
        out+="	filter:alpha(opacity=80);\n";
        out+="	opacity:0.8;}\n\n";
  		
        out += writeOutCss(imageName,currentMap);
        out += "</style></head><body>\n";
        out += "<ul id =\"map\">";
        nodeCounter =0;
        out += writeOutLi(currentMap);
        
        out += "\n</ul></body></html>"; 
        
        //write out to the selected file
        try{
            FileWriter output = new FileWriter(fileName);
            output.write(out);
            output.close();
            System.out.println("wrote to the file...");
        }
        catch(IOException ioe){
            System.out.println("Error trying to write to html file: " + ioe);
        }    
    }
}

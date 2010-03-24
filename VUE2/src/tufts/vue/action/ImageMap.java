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

import static tufts.Util.reverse;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import tufts.vue.*;
import tufts.vue.LWComponent.ChildKind;
import tufts.vue.LWComponent.Order;

/**
 * @version $Revision: 1.38 $ / $Date: 2010-03-24 14:57:03 $ / $Author: mike $ *
 * @author Jay Briedis
 * 
 * Major revision: 2/17/09 -MK
 * The image map now uses a bunch of JQuery files stored on vue.tufts.edu, to enhance
 * the image map a bit.  The map itself now supports nested nodes, and groups for the first time.
 * Intersecting areas of the image map are priortized by what appears first in this list, 
 * so ideallly we want the lowest level descendants to appear first with the top level nodes
 * appearing last, also you want a groups descedents to appear before the actual group.
 * 
 * The current tooltip code doesn't quite work right in IE7 so I cased it to only work in FF, and 
 * use the standard tooltips in IE.  I'll revisit this after the Feb. 09 release.  I'd also like
 * to visit adding a zoom slider similar to google maps and scaling the image map appropriately.
 * 
 * 3rd level children don't seem to size quite right, need to revisit that as well.  Also, the tooltip
 * code lets us include Image Previews so we shoud look at using that to provide resource previews when
 * its as simple as pointing to a URL.
 * 
 * Also I'd really like to add some simple templating as having the HTML in java is really cumbersome.
 */
public class ImageMap extends VueAction {

	public static final int UPPER_LEFT_MARGIN = 30;
	private int nodeCounter = 0;
	static final float ChildScale = VueResources.getInt("node.child.scale", 75) / 100f;
	private Dimension imageDimensions;
	private int xOffset, yOffset;

	/** Creates a new instance of ImageConversion */
	public ImageMap() {
	}

	public ImageMap(String label) {
		super(label);
	}

	public void act() {
		File selectedFile = ActionUtil.selectFile("Saving Imap", "html");

		if (selectedFile != null)
			createImageMap(selectedFile,1.0);
	}
	
	public void createImageMap(File file,LWMap map,double zoom) {
		String imageLocation = file.getAbsolutePath().substring(0,
				file.getAbsolutePath().length() - 5)
				+ ".png";
		String imageName = file.getName().substring(0,
				file.getName().length() - 5)
				+ ".png";
		String fileName = file.getAbsolutePath().substring(0,
				file.getAbsolutePath().length() - 5)
				+ ".html";

		File imageLocationFile = new File(imageLocation);

		if (imageLocationFile.exists()) {
			int confirm = VueUtil.confirm(
					VueResources.getString("imagemap.fileexists.warning"),
					VueResources.getString("imagemap.fileexists.title"));
			if (confirm == javax.swing.JOptionPane.NO_OPTION) {
				VueUtil.alert(VueResources.getString("imagemap.mapnotsaved.error"), VueResources.getString("imagemap.mapnotsaved.title"));
				return;
			}
		}
 	 	imageDimensions = ImageConversion.createActiveMapPng(imageLocationFile,map,1.0);
		createHtml(imageName, fileName,map,zoom);
	}

	public void createImageMap(File file, double zoom) {
		// See: VUE-536 in JIRA, If SaveAction Class still chooses "html" as the
		// file type for image maps
		// html file will already not be overwritten
		// String imageLocation = file.getAbsolutePath().substring(0,
		// file.getAbsolutePath().length()-5)+"-for-image-map"+".jpeg";
		// String imageName = file.getName().substring(0,
		// file.getName().length()-5)+"-for-image-map"+".jpeg";
		String imageLocation = file.getAbsolutePath().substring(0,
				file.getAbsolutePath().length() - 5)
				+ ".png";
		String imageName = file.getName().substring(0,
				file.getName().length() - 5)
				+ ".png";
		String fileName = file.getAbsolutePath().substring(0,
				file.getAbsolutePath().length() - 5)
				+ ".html";

		File imageLocationFile = new File(imageLocation);

		if (imageLocationFile.exists()) {
			int confirm = VueUtil.confirm(
					VueResources.getString("imagemap.fileexists.warning"),
					VueResources.getString("imagemap.fileexists.title"));
			if (confirm == javax.swing.JOptionPane.NO_OPTION) {
				VueUtil.alert(VueResources.getString("imagemap.mapnotsaved.error"), VueResources.getString("imagemap.mapnotsaved.title"));
				return;
			}
		}

		// createJpeg(imageLocation, "jpeg", currentMap, size);
		// ImageConversion.createActiveMapJpeg(new File(imageLocation));
		imageDimensions = ImageConversion.createActiveMapPng(imageLocationFile,
				zoom);
		createHtml(imageName, fileName,zoom);
	}
	
	/**
	 * Returns a string containing the coordinates (x1, y1, x2, y2) for a given
	 * rectangle. This string is intended for use in an image map.
	 * 
	 * @param rectangle
	 *            the rectangle
	 * 
	 * @return Upper left and lower right corner of a rectangle.
	 */
	private String getRectCoords(Rectangle2D rectangle, double zoom) {
		if (rectangle == null) {
			throw new IllegalArgumentException("Null 'rectangle' argument.");
		}
		int x1 = (int) (rectangle.getX()* zoom);
		int y1 = (int) (rectangle.getY()* zoom);
		int x2 = (int) (rectangle.getWidth()* zoom);
		int y2 = (int) (rectangle.getHeight()* zoom);
		
		if (x2 == x1) {
			x2++;
		}
		if (y2 == y1) {
			y2++;
		}
		
		return x1 + "," + y1 + "," + x2 + "," + y2;
	}

	private Rectangle getRectNode(LWComponent node) {
		int groupX = 0;
		int groupY = 0;
		int ox;
		int oy;
		int ow;
		int oh;

		oy = (int) node.getMapY() + groupY - yOffset - 13;
		ox = (int) node.getMapX() + groupX - xOffset - 14;

		ow = (int) node.getWidth() + ox;
		oh = (int) node.getHeight() + oy;

		if (node.getParent() instanceof LWNode) {
			System.out.println(node.getDepth());
			ow = (int) (ow - ((ow - ox) * (1 - Math.pow(ChildScale,(node.getDepth()-2)))));
					/// (node.getDepth() - 2))));
			oh = (int) (oh - ((oh - oy) *  (1 - Math.pow(ChildScale,(node.getDepth()-2)))));
					/// (node.getDepth() - 2))));

		}
		return new Rectangle(ox, oy, ow, oh);
	}

	private String writeMapforContainer(LWContainer container,LWMap map, double zoom) {
		/*
		 * I'm using an array list to gather all the lines of the image map
		 * so that I can push things to the top.  Intersecting areas of the image map
		 * are priortized by what appears first in this list, so ideallly we want 
		 * the lowest level descendants to appear first with the top level nodes
		 * appearing last, also you want a groups descedents to appear before the 
		 * actual group -MK 2/16/09
		 */
	
		java.util.List<String> arrayList = new ArrayList<String>();
		//java.util.ArrayList<LWComponent> comps = new ArrayList<LWComponent>();
		
		//get the current map.
 
		 // handle in reverse order (top layer on top)
        for (LWComponent layer : reverse(map.getChildren())) {         
                //for (LWComponent c : reverse(layer.getChildren()))
//                for (LWComponent c : reverse()
                	
            
        
		
		Iterator<LWComponent> iter = layer.getAllDescendents(ChildKind.PROPER, new ArrayList(), Order.DEPTH).iterator();
			//comps.iterator();
			//container.getAllDescendents(
			//	LWComponent.ChildKind.PROPER).iterator();

		while (iter.hasNext()) {
			LWComponent comp = (LWComponent) iter.next();

			String type = "node";
			if (container instanceof LWGroup) 
				type = "group";

			if (!(comp instanceof LWNode) && !(comp instanceof LWGroup))
				continue;

			  String href = null;
			  String altLabel = null;
	          Resource res = null;
	            if (comp instanceof LWNode)
	            {
	            	 if(comp.getResource() != null){
	                     res = comp.getResource();
	                     //res = resource.toString();
	                     altLabel=res.getSpec();
	                     
	                     // see VUE-873 getSpec() should be fine for now
	                     //if(!(res.startsWith("http://") || res.startsWith("https://"))) res = "file:///" + res;
	                 } 
	            }
	           
			  
		        if (res == null)
		        	href ="";
	            else if(res.equals("null"))
	        	   href = "";
	           else 
	        	   href = "href=\"" + res.getSpec() + "\" target=\"_blank\"";
            
            String notes ="";
            
            notes = comp.getNotes();
	             
            if (notes == null)
            	notes ="";
            else
            {
            	notes = VueUtil.formatLines(notes, 20);
            	notes ="class=\"tooltip\" title=\""+notes+"\" "; 
            }
			if (!comp.hasChildren()) {
				arrayList.add(" <area "+href+" "+notes+" id=\"" + type
						+ (nodeCounter++) + "\" shape=\"rect\" coords=\""
						+ getRectCoords(getRectNode(comp),zoom) + "\"></area>\n");
			} else {
				Collection<LWComponent> children = comp.getAllDescendents();
				LWComponent[] array = new LWComponent[children.size()];
				children.toArray(array);
				for (int i = array.length - 1; i >= 0; i--) 
				{
					if (array[i] instanceof LWNode)
					{
						  String childHref;
						  Resource childRes = array[i].getResource();
						   if (childRes == null)
					        	childHref ="";
				            else if(childRes.equals("null"))
				        	   childHref = "";
				           else 
				        	   childHref = "href=\"" + childRes.getSpec() + "\" target=\"_blank\"";	
						   
						arrayList.add(0, " <area "+childHref+" "+notes+" id=\"" + type
								+ (nodeCounter++)
								+ "\" shape=\"rect\" coords=\""
								+ getRectCoords(getRectNode(array[i]),zoom)
								+ "\"></area>\n");
					}
				}

				arrayList.add(" <area "+href+" "+notes+" id=\"" + type
						+ (nodeCounter++) + "\" shape=\"rect\" coords=\""
						+ getRectCoords(getRectNode(comp),zoom) + "\"></area>\n");

				if (comp instanceof LWGroup) {
					String groupOutput = writeMapforContainer((LWGroup) comp,map,zoom);
					arrayList.add(groupOutput);
				}
			}// end else

		}// end while
		
        }
		String buf = "";
		Iterator<String> iter2 = arrayList.iterator();
		while (iter2.hasNext()) {
			String st = iter2.next();
			buf += st;
		}
		return buf;
	}
    private  void createHtml(String imageName, String fileName,double zoom) {
    	LWMap currentMap = VUE.getActiveMap();
    	createHtml(imageName,fileName,currentMap,zoom);
    }
    
	private  void createHtml(String imageName, String fileName,LWMap currentMap,double zoom) {

 		Rectangle2D bounds = currentMap.getMapBounds();

		xOffset = (int) bounds.getX() - UPPER_LEFT_MARGIN;
		yOffset = (int) bounds.getY() - UPPER_LEFT_MARGIN;
		
		String out = "<html><head><title>" + currentMap.getLabel();
		out += "</title>";
		out += "<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.3.1/jquery.min.js\" type=\"text/javascript\"></script>";
		out += "<script  type=\"text/javascript\">\n";
		out += "jQuery.noConflict();\n";
		out += "</script>\n";
		out += "<script src=\"http://vue.tufts.edu/htmlexport-includes/jquery.maphilight.min.js\" type=\"text/javascript\"></script>";
		out += "<script src=\"http://vue.tufts.edu/htmlexport-includes/v3/tooltip.min.js\" type=\"text/javascript\"></script>";
		out += "<script type=\"text/javascript\">";
		out += "jQuery(function() {jQuery.fn.maphilight.defaults = {\n";
		out += "         fill: false,\n";
		out += "         fillColor: '000000',\n";
		out += "         fillOpacity: 0.2,\n";
		out += "             stroke: true,\n";
		out += "         strokeColor: '282828',\n";
		out += "         strokeOpacity: 1,\n";
		out += "         strokeWidth: 4,\n";
		out += "         fade: true,\n";
		out += "         alwaysOn: false\n";
		out += "     }\n";
		out += "jQuery('.example2 img').maphilight();\n";
		out += "});\n";
		out += "</script>\n";
		out += "<style type=\"text/css\">\n";
		out += "#tooltip{\n";
		out += "position:absolute;\n";
		out += "border:1px solid #333;\n";
		out += "background:#f7f5d1;\n";
		out += "padding:2px 5px;\n";
		out += "color:#333;\n";
		out += "display:none;\n";
		out += "}\n";	
		out +="</style>\n";
		out += "</head><body>\n";
		out += "<div class=\"example2\">";
		out += "<img class=\"map\" src=\"" + imageName + "\" width=\""
				+ imageDimensions.getWidth() + "\" height=\""
				+ imageDimensions.getHeight() + "\" usemap=\"#vuemap\">";
		out += "<map name=\"vuemap\">";
		nodeCounter = 0;
		out += writeMapforContainer(currentMap,currentMap,zoom);
		out += "\n</map></div></body></html>";
		// write out to the selected file
		FileWriter output = null;
		try {
			output = new FileWriter(fileName);
			output.write(out);

			System.out.println("wrote to the file...");
		} catch (IOException ioe) {
			System.out.println("Error trying to write to html file: " + ioe);
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

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
package tufts.vue.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
 

public class DashBorder extends AbstractBorder {

	public static final int[] DASH = { 4, 4 };
 
	private int thickness = 1;
 	
	private Color colorFG = null;
 
	private Color colorBG = null;
    
	private boolean bottom,top,left,right;
  
	public DashBorder(Color fg) {
		this(1,  fg, null,true,true,true,true);
	}
 
	public DashBorder(int thickness) {
		this(thickness, null, null,true,true,true,true);
	}
 
	public DashBorder(int thickness, Color fg) {
		this(thickness,  fg, null,true,true,true,true);
	}
 
	public DashBorder(Color fg, boolean bottom,boolean top,boolean left,boolean right)
	{
		this (1,fg,null,bottom,top,left,right);
	}
	
	public DashBorder(int thickness, Color fg, Color bg, boolean bottom, boolean top, boolean left, boolean right) {
		
		if(thickness <= 0) {
			throw new IllegalArgumentException("Thickness cannot be <= 0.");
		}
		this.thickness = thickness;
		this.colorFG = fg;
		this.colorBG = bg;
		this.bottom=bottom;
		this.top=top;
		this.right=right;
		this.left=left;
	}
 
	
	public Insets getBorderInsets(Component c) {
		return new Insets(thickness, thickness, thickness, thickness);
	}
 
	
	public Insets getBorderInsets(Component c, Insets insets) {
		return new Insets(thickness, thickness, thickness, thickness);
	}
 
	
	public boolean isBorderOpaque() {
		return true;
	}
 
	/**
	 * Paint the border.
	 * @param  Component c: the component the border is for
	 * @param  Graphics g: the graphics object to draw on
	 * @param  x  int: the border y position
	 * @param  y  int: the border x position
	 * @param  width  int: the border width
	 * @param  height  int: the border height
	 */
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Color colorFGX = c.getBackground();
		if(colorFG != null) {
			colorFGX = colorFG;
		}
	
		Color colorBGX = c.getBackground();
		if(colorBG != null) {
			colorBGX = colorBG;
		}
		g.setColor(colorFGX);
		if(top)
			g.fillRect(x, x, width, thickness);				// top
		if(bottom)
			g.fillRect(x, y+height-thickness, width, thickness);	// bottom
		if(left)
			g.fillRect(x, y, thickness, height);				// left
		if(right)
			g.fillRect(x+width-thickness, y, thickness, height);	// right
		
		g.setColor(colorBGX);
		// top/bottom
		int cx = 0;		
		

		for(int i = 0, j = 0; i < width; i++, j+=2) {
			if(j >= DASH.length) {
				j = 0;
			}
			cx += DASH[j];
			g.fillRect(cx, y, DASH[j+1], thickness);				// top
			g.fillRect(cx, y+height-thickness, DASH[j+1], thickness);	// bottom
			cx += DASH[j+1];
		}
		// left/right
		int cy = 0;
		for(int i = 0, j = 0; i < height; i++, j+=2) {
			if(j >= DASH.length) {
				j = 0;
			}
			cy += DASH[j];
			g.fillRect(x, cy, thickness, DASH[j+1]);				// left
			g.fillRect(x+width-thickness, cy, thickness, DASH[j+1]);	// right
			cy += DASH[j+1];
		}
	}
}
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

package tufts.vue;

import tufts.Util;
import static tufts.Util.*;
import java.awt.Dimension;
import java.awt.geom.RectangularShape;
import java.awt.geom.Rectangle2D;

public final class Size extends java.awt.geom.Dimension2D {

    public static final Size None = new Size();
    
    /**
     * The width dimension; negative values can be used. 
     */
    public float width;

    /**
     * The height dimension; negative values can be used. 
     */
    public float height;

    public Size(float width, float height) {
	this.width = width;
	this.height = height;
    }

    public Size(double width, double height) {
	this.width = (float) width;
	this.height = (float) height;
    }
    
    public Size() {
	this(0, 0);
    }
    public Size(Size s) {
	this(s.width, s.height);
    }
    public Size(int[] wh) {
	this(wh[0], wh[1]);
    }
    public Size(Dimension d) {
	this(d.width, d.height);
    }
    public Size(Rectangle2D.Float r) {
	this(r.width, r.height);
//         if (r.x != 0 || r.y != 0) Util.printStackTrace("non-zero offset in " + fmt(r));
//         // todo: handle adjustment and/or caller should be using Rectangle2D instead of Size
    }
    public Size(Rectangle2D r) {
	this((float)r.getWidth(), (float)r.getHeight());
//         if (r.getX() != 0 || r.getY() != 0) Util.printStackTrace("non-zero offset in " + fmt(r));
//         // todo: handle adjustment and/or caller should be using Rectangle2D instead of Size
    }

    public Dimension dim() {
        return new Dimension((int)width, (int)height);
    }

    public double getWidth() {
	return width;
    }

    public double getHeight() {
	return height;
    }

    public int pixelWidth() {
        return (int) Math.ceil(width);
    }

    public int pixelHeight() {
        return (int) Math.ceil(height);
    }

    public boolean equals(int w, int h) {
        return w == pixelWidth() && h == pixelHeight();
    }

    public boolean equals(float w, float h) {
        return w == width && height == height;
    }

    public void fit(Size s) {
        fitWidth(s.width);
        fitHeight(s.height);
    }

    public void fit(RectangularShape s) {
        fitWidth(s.getWidth());
        fitHeight(s.getHeight());
    }

    public void fitHeight(float h) {
        if (height < h)
            height = h;
    }
    public void fitWidth(float w) {
        if (width < w)
            width = w;
    }

    public void fitHeight(double h) {
        if (height < h)
            height = (float) h;
    }
    public void fitWidth(double w) {
        if (width < w)
            width = (float) w;
    }    

    public void setSize(float width, float height) {
	this.width = width;
	this.height = height;
    }
    public void setSize(double width, double height) {
	this.width = (float) width;
	this.height = (float) height;
    }

    public Size getSize() {
	return new Size(width, height);
    }	
    public void setSize(Size s) {
	setSize(s.width, s.height);
    }	
    public void setSize(Dimension d) {
	setSize(d.width, d.height);
    }
    /*
    public void setSize(int width, int height) {
    	this.width = width;
    	this.height = height;
    }
    */

    public boolean equals(Object obj) {
	if (obj instanceof Size) {
	    Size d = (Size)obj;
	    return (width == d.width) && (height == d.height);
	}
	return false;
    }

    public int hashCode() {
        int sum = (int) (width + height);
        return sum * (sum + 1)/2 + (int) width;
    }

    public String toString() {
	return "[" + width + "x" + height + "]";
    }
}

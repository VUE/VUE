/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import java.awt.Dimension;
import java.awt.geom.RectangularShape;
import java.awt.geom.Rectangle2D;

public class Size {

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
    
    public Size() {
	this(0, 0);
    }
    public Size(Size s) {
	this(s.width, s.height);
    }
    public Size(Dimension d) {
	this(d.width, d.height);
    }
    public Size(Rectangle2D.Float r) {
	this(r.width, r.height);
    }

    public float getWidth() {
	return width;
    }

    public float getHeight() {
	return height;
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

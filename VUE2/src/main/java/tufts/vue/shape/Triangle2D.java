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

package tufts.vue.shape;

import java.awt.geom.RectangularShape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;

/**
 * This class implements a triangular polygon shape.
 * @author Scott Fraize
 */
public class Triangle2D extends RectangularShape
{
    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected boolean upsidedown = false;
    
    public Triangle2D(double x, double y, double width, double height, boolean upsidedown) {
        setFrame(x, y, width, height);
        this.upsidedown = upsidedown;
    }
    
    public Triangle2D(double x, double y, double width, double height) {
        this(x, y, width, height, false);
    }
    
    /** does shape contain point? */
    public boolean contains(double px, double py)
    {
        // first, do a simple bounds check
        if (px < x || py < y || px > x + width || py > y + height)
            return false;
        //for each segment:if CCW < 0 for all segments (and all segments ordered "clockwise")
        // then point is in this polygon
        //Line2D.relativeCCW(x1, y1, x2, y2, px, py);
        return true;
    }

    /** does shape entirely contain rectangle? */
    public boolean contains(double x, double y, double w, double h)
    {
        return contains(x, y) && contains(x+w, y+h);
        // todo: should work to check opposite corners for regular polygons,
        // but will need to check other corners if shape is irregular
    }
    
    /** does any part of shape intersect rectangle? */
    public boolean intersects(double x, double y, double w, double h)
    {
        return contains(x, y) || contains(x+w, y+h);
        // todo: need to do intersection check for each of the 4 lines in the rectangle
    }
    
    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Double(x, y, width, height);
    }
    
    public double getHeight() { return height; }
    public double getWidth() { return width; }
    public double getX() { return x; }
    public double getY() { return y; }
    
    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }
    
    public void setFrame(double x, double y, double width, double height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

        
    public PathIterator getPathIterator(AffineTransform affineTransform)
    {
        return new TriIterator(affineTransform);
    }

class TriIterator implements PathIterator
{
    int index = 0;
    AffineTransform affine;

    public TriIterator(AffineTransform affine)
    {
        this.affine = affine;
    }
    
    double[][] dCurrentPoint = {
        {x + width/2, y},
        {x, y + height},
        {x + width, y + height},
        {0,0},
    };
    // upsidedown triangle
    double[][] dCurrentPointUD = {
        {x, y},
        {x + width/2, y + height},
        {x + width, y},
        {0,0}
    };
    final int[] segment = { SEG_MOVETO, SEG_LINETO, SEG_LINETO, SEG_CLOSE };
    
    public int currentSegment(double[] coords) { 
        if (upsidedown) {
            coords[0] = dCurrentPointUD[index][0];  
            coords[1] = dCurrentPointUD[index][1];
        } else {
            coords[0] = dCurrentPoint[index][0];  
            coords[1] = dCurrentPoint[index][1];
        }
	if (affine != null)
	    affine.transform(coords, 0, coords, 0, 1);
        //System.out.println("i"+index + " TriIterator(double)coords=" +coords[0] + "," + coords[1]);
        return segment[index];
    }
			
    public int currentSegment(float[] coords){ 
        if (upsidedown) {
            coords[0] = (float)dCurrentPointUD[index][0];  
            coords[1] = (float)dCurrentPointUD[index][1];
        } else {
            coords[0] = (float)dCurrentPoint[index][0];  
            coords[1] = (float)dCurrentPoint[index][1];
        }
	if (affine != null)
	    affine.transform(coords, 0, coords, 0, 1);
        //System.out.println("i"+index + " TriIterator(float)coords=" +coords[0] + "," + coords[1]);
        return segment[index];
    } 
			
    public int getWindingRule() { return PathIterator.WIND_NON_ZERO; }
    public boolean isDone() { return index == 4; } 
    public void next() { index++ ; } 
}
    
    
}

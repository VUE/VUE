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

package tufts.vue.shape;

import java.awt.geom.RectangularShape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;

/**
 * This class implements a diamond like Shape object.
 * @author  mueller
 * @version 1.0
 */
public class Diamond2D extends RectangularShape {

    protected double x, y, width, height;

    /** Creates a Diamond shape with the specified coordinates. */
    public Diamond2D(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
	
    /** Returns the height. */
    public double getHeight() {
        return height;
    }
	
    /** Returns the width. */
    public double getWidth() {
        return width;
    }
	
    /** Returns the x-coordinate. */
    public double getX() {
        return x;
    }
	
    /** Returns the y-coordinate. */
    public double getY() {
        return y;
    }
	
    /** Returns true if the bounding box is empty. */
    public boolean isEmpty() {
        return (width <= 0.0) || (height <= 0.0);
    }
	
    /** Sets the framing rectangle. */
    public void setFrame(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
	
    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Double(x, y, height, width);
    }
	
    /** Returns the object's PathIterator. */
    public PathIterator getPathIterator(AffineTransform at) {
        return new PathIterator() {
                int state = 0;
                int maxstate = 4;
                float[][] fcurrentSegment = { {(float)(x + width/2), (float)y, 0f, 0f, 0f, 0f},
                                              {(float)x, (float)(y + height/2), 0f, 0f, 0f, 0f},
                                              {(float)(x + width/2), (float)(y + height), 0f, 0f, 0f, 0f},
                                              {(float)(x + width), (float)(y + height/2), 0f, 0f, 0f, 0f},
                                              {0f, 0f, 0f, 0f, 0f, 0f} };
										  
			
                double[][] dcurrentSegment = {{x + width/2, y, 0.0, 0.0, 0.0, 0.0},
                                              {x, y + height/2, 0.0, 0.0, 0.0, 0.0},
                                              {x + width/2, y + height, 0.0, 0.0, 0.0, 0.0},
                                              {x + width, y + height/2, 0.0, 0.0, 0.0, 0.0},
                                              {0.0, 0.0, 0.0, 0.0, 0.0, 0.0} };

                int[] segment = { PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO, PathIterator.SEG_LINETO, PathIterator.SEG_LINETO, PathIterator.SEG_CLOSE};
			
                public int currentSegment(double[] coords) { 
                    coords[0] = dcurrentSegment[state][0];  
                    coords[1] = dcurrentSegment[state][1];
                    return segment[state];
                }
			
                public int currentSegment(float[] coords){ 
                    coords[0] = fcurrentSegment[state][0];  
                    coords[1] = fcurrentSegment[state][1];
                    return segment[state];
                } 
			
                public int getWindingRule() { return PathIterator.WIND_NON_ZERO; }
                public boolean isDone() { return (state == maxstate); } 
                public void next() { state++ ; } 
            };
    }
    
    public boolean contains(double x, double y, double w, double h) {
        return false;
    }
    
    public boolean contains(double x, double y)  {
        return false;
    }
    
    public boolean intersects(double x, double y, double w, double h) {
        return false;
    }
    
}

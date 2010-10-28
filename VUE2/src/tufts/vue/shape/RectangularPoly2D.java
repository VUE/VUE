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
 * This class implements a polygon shape fit into a specified rectanular region.
 * @author Scott Fraize
 */
public abstract class RectangularPoly2D extends RectangularShape
{
    public static final int CENTER = 0;
    public static final int NORTH      = 1;
    public static final int NORTH_EAST = 2;
    public static final int EAST       = 3;
    public static final int SOUTH_EAST = 4;
    public static final int SOUTH      = 5;
    public static final int SOUTH_WEST = 6;
    public static final int WEST       = 7;
    public static final int NORTH_WEST = 8;
        
    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected int sides;
    protected int layout;
    
    protected double[] xpoints;
    protected double[] ypoints;
    
    public RectangularPoly2D(int sides, double x, double y, double width, double height)
    {
        setSides(sides);
        setFrame(x, y, width, height);
    }

    public RectangularPoly2D(int sides)
    {
        setSides(sides);
    }

    /** For persistance */
    public RectangularPoly2D() {}

    public int getContentGravity() {
        return CENTER;
    }

    protected abstract void computeVertices();

    /** a point-up triangle */
    public static class Triangle extends RectangularPoly2D {
        public Triangle() { setSides(3); }
        public int getContentGravity() { return SOUTH; }
        protected void computeVertices()
        {
            xpoints[0] = x + width/2;
            ypoints[0] = y;
            
            xpoints[1] = x;
            ypoints[1] = y + height;
            
            xpoints[2] = x + width;
            ypoints[2] = y + height;
        }
        
    }
    /** a point-down triangle */
    public static class Shield extends RectangularPoly2D {
        public Shield() { setSides(3); }
        public int getContentGravity() { return NORTH; }
        protected void computeVertices()
        {
            xpoints[0] = x;
            ypoints[0] = y;
            
            xpoints[1] = x + width;
            ypoints[1] = y;

            xpoints[2] = x + width/2;
            ypoints[2] = y + height;
        }
        
    }
    /** a point to the right triangle */
    public static class Flag extends RectangularPoly2D {
        public Flag() { setSides(3); }
        public int getContentGravity() { return EAST; }
        protected void computeVertices()
        {
            xpoints[0] = x;
            ypoints[0] = y;
            
            xpoints[1] = x;
            ypoints[1] = y + height;
            
            xpoints[2] = x + width;
            ypoints[2] = y + height / 2;
        }
    }
    /** a point to the left triangle */
    public static class Flag2 extends RectangularPoly2D {
        public Flag2() { setSides(3); }
        public int getContentGravity() { return WEST; }
        protected void computeVertices()
        {
            xpoints[0] = x;
            ypoints[0] = y + height / 2;
            
            xpoints[1] = x + width;
            ypoints[1] = y;
            
            xpoints[2] = x + width;
            ypoints[2] = y + height;
            
        }
    }
    

    /** a 4 sided polygon */
    public static class Diamond extends RectangularPoly2D {
        public Diamond() { setSides(4); }
        protected void computeVertices()
        {
            xpoints[0] = x + width/2;
            ypoints[0] = y;
            
            xpoints[1] = x + width;
            ypoints[1] = y + height/2;
            
            xpoints[2] = x + width/2;
            ypoints[2] = y + height;
            
            xpoints[3] = x;
            ypoints[3] = y + height/2;
        }
    }
    
    /** a 4 sided polygon */
    public static class Rhombus extends RectangularPoly2D {
        public Rhombus() { setSides(4); }
        protected void computeVertices()
        {
        

            xpoints[0] = x + ((double)width-(double)width*0.8);
            ypoints[0] = y;
            
            xpoints[1] = x + width;
            ypoints[1] = y;
            
            xpoints[3] = x;
            ypoints[3] = y + height;
            
            xpoints[2] = x + ((double)width-(double)width*0.2);
            ypoints[2] = y + height;
        }
    }

    /** a 5 sided polygon */
    public static class Pentagon extends RectangularPoly2D {
        public Pentagon() { setSides(5); }
        protected void computeVertices()
        {
            xpoints[0] = x + width/2;
            ypoints[0] = y;
            
            xpoints[1] = x + width;
            ypoints[1] = y + height/2;
            
            xpoints[2] = x + width*3/4;
            ypoints[2] = y + height;
            
            xpoints[3] = x + width/4;
            ypoints[3] = y + height;
            
            xpoints[4] = x;
            ypoints[4] = y + height/2;
        }
    }

    /** a 6 sided polygon */
    public static class Hexagon extends RectangularPoly2D {
        public Hexagon() { setSides(6); }
        protected void computeVertices()
        {
            // tan(30) = inset/halfH
            // halfH*tan(30) = inset
            double halfH = height/2;
            //double inset = halfH * Math.tan(Math.PI/6);
            double inset = 0.2257085*width;
        
            //System.out.println("HEXAGON size=" + width + "x" + height);
            //System.out.println("HEXAGON HALFH=" + halfH);
            //System.out.println("HEXAGON INSET=" + inset);
            //System.out.println("HEXAGON SEGSIZE  TOP=" + (width-(inset*2)));
            //System.out.println("HEXAGON SEGSIZE LEFT=" + Math.sqrt(inset*inset+halfH*halfH));
            
            xpoints[0] = x + inset;
            ypoints[0] = y;

            xpoints[1] = x + (width - inset);
            ypoints[1] = y;

            xpoints[2] = x + width;
            ypoints[2] = y + halfH;

            xpoints[3] = x + (width - inset);
            ypoints[3] = y + height;

            xpoints[4] = x + inset;
            ypoints[4] = y + height;
        
            xpoints[5] = x;
            ypoints[5] = y + halfH;
        }
    }

    /** an 8 sided polygon */
    public static class Octagon extends RectangularPoly2D {
        public Octagon() { setSides(8); }
        protected void computeVertices()
        {
            double xInset = width / 3.4;
            double yInset = height / 3.4;
            
            xpoints[0] = x + xInset;
            ypoints[0] = y;
            
            xpoints[1] = x + (width - xInset);
            ypoints[1] = y;
            
            xpoints[2] = x + width;
            ypoints[2] = y + yInset;
            
            xpoints[3] = x + width;
            ypoints[3] = y + (height - yInset);
            
            xpoints[4] = x + (width - xInset);
            ypoints[4] = y + height;
            
            xpoints[5] = x + xInset;
            ypoints[5] = y + height;
            
            xpoints[6] = x;
            ypoints[6] = y + (height - yInset);
            
            xpoints[7] = x;
            ypoints[7] = y + yInset;
        }
    }
    /** a point sideways Chevron */  
    public static class Chevron extends RectangularPoly2D {  
    	  public Chevron() { setSides(6); }  
    	  public int getContentGravity() { return CENTER; }  
          protected void computeVertices()  
           {  
            
             xpoints[0] = x + ((double)width-(double)width*0.2);  
             ypoints[0] = y;         
             xpoints[1] = x + width;  
             ypoints[1] = y + (height/2);     
             xpoints[2] = x + ((double)width-(double)width*0.2);  
             ypoints[2] = y + height;   
             xpoints[3] = x;  
             ypoints[3] = y + height;  
             xpoints[4] = x + (double)width*0.2;  
             ypoints[4] = y + (height/2);  
             xpoints[5] = x;  
             ypoints[5] = y;             
           }      
     }  
    public void setSides(int sides)
    {
        if (sides < 3 || sides > 8)
            throw new IllegalArgumentException("RectangularPoly2D: sides not >=3 && <=8: "+sides);
        this.sides = sides;
        xpoints = new double[sides+1];
        ypoints = new double[sides+1];
        xpoints[sides] = 0;
        ypoints[sides] = 0;
    }

    public int getSides()
    {
        return this.sides;
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
    
    /** does this polygon contain the given point? */
    public boolean contains(double px, double py)
    {
        // fast-reject: check if outside bounding box
        if (px < x || py < y || px > x + width || py > y + height)
            return false;

        // below adapted from java.awt.Polygon
        
	int hits = 0;

	double lastx = xpoints[sides - 1];
	double lasty = ypoints[sides - 1];
	double curx, cury;

	// Walk the edges of the polygon
	for (int i = 0; i < sides; lastx = curx, lasty = cury, i++)
        {
	    curx = xpoints[i];
	    cury = ypoints[i];

	    if (cury == lasty)
		continue;

	    double leftx;
	    if (curx < lastx) {
		if (x >= lastx) {
		    continue;
		}
		leftx = curx;
	    } else {
		if (px >= curx) {
		    continue;
		}
		leftx = lastx;
	    }

	    double test1, test2;
	    if (cury < lasty) {
		if (py < cury || py >= lasty) {
		    continue;
		}
		if (px < leftx) {
		    hits++;
		    continue;
		}
		test1 = px - curx;
		test2 = py - cury;
	    } else {
		if (py < lasty || py >= cury) {
		    continue;
		}
		if (px < leftx) {
		    hits++;
		    continue;
		}
		test1 = px - lastx;
		test2 = py - lasty;
	    }

	    if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
		hits++;
	    }
	}

	return ((hits & 1) != 0);
    }
    

    /** does shape entirely contain rectangle? */
    public boolean contains(double x, double y, double w, double h)
    {
        return contains(x, y) && contains(x+w, y+h) && contains(x+w, y) && contains(x,y+h);
        // todo: this will work to check the corners for entirely concave regular polygons
        // but will need to check crossings to support irregular polygons
        // (e.g. a star or cross)
    }
    
    /** does any part of shape intersect rectangle? */
    public boolean intersects(double x, double y, double w, double h)
    {
        // fast-reject: check if outside bounding box
        if (!(x + w > this.x &&
              y + h > this.y &&
              x < this.x + this.width &&
              y < this.y + this.height))
            return false;

        // fast-accept: that checks to see if any vertex is within the box
        for (int i = 0; i < sides; i++) {
            double xp = xpoints[i];
            double yp = ypoints[i];
            if (xp > x && xp < x + w &&
                yp > y && yp < y + h) {
                //System.out.println("fast accept vertex" + i + " " + this);
                return true;
            }
                
        }

	Crossings cross = getCrossings(x, y, x+w, y+h);
	return (cross == null || !cross.isEmpty());
    }
    
    private Crossings getCrossings(double xlo, double ylo,
				   double xhi, double yhi)
    {
	Crossings cross = new Crossings.EvenOdd(xlo, ylo, xhi, yhi);
	double lastx = xpoints[sides - 1];
	double lasty = ypoints[sides - 1];
	double curx, cury;

	// Walk the edges of the polygon
	for (int i = 0; i < sides; i++) {
	    curx = xpoints[i];
	    cury = ypoints[i];
	    if (cross.accumulateLine(lastx, lasty, curx, cury)) {
		return null;
	    }
	    lastx = curx;
	    lasty = cury;
	}

	return cross;
    }
    
    public void setFrame(double x, double y, double width, double height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        //System.out.println(this + " POLY setFrame " + x + "," + y + " " + width + "x" + height);
        //new Throwable().printStackTrace();
        computeVertices();
    }

    /*
    private void computeVertices()
    {
        if (sides == 3)
            computeVertices3();
        else if (sides == 4)
            computeVertices4();
        else if (sides == 5)
            computeVertices5();
        else if (sides == 6)
            computeVertices6();
        else if (sides == 8)
            computeVertices8();
    }
    */
    
    /*        
    private void computeVertices5()
        //final double horizontalOffset = 0.118033989; // verticalOffset * tan(18)
        final double horizontalOffset = verticalOffset*verticalOffset;
        // try taking into account verticalOffset before computing horizontal offset
        double dropDown = height * verticalOffset;
        double inset = width * horizontalOffset;
            
        vertices[0] = x + halfW;
        vertices[0] = y;

        vertices[1] = x + width;
        vertices[1] = y + dropDown;

        vertices[2] = x + width - inset;
        vertices[2] = y + height;

        vertices[3] = x + inset;
        vertices[3] = y + height;

        vertices[4] = x;
        vertices[4] = y + dropDown;
        }*/

    private void new_computeVertices6()
    {
        // equalateral polygons are inscribed
        // inside circles, not rectangles (all points
        // are on a containing circle) -- this
        // creates an equal hexagon inside a circle
        // of the given WIDTH -- height is ignored.
        // -- can't we just inscribe it in an ellipse?
        
        double hw = width/2;
        double hh = height/2;
        double qw = hw/2;
        double qh = Math.sqrt(hw*hw-qw*qw);
        double cx = x + hw;
        double cy = y + hh;
            
        xpoints[0] = cx + qw;
        ypoints[0] = cy - qh;

        xpoints[1] = x + width;
        ypoints[1] = y + hh;

        xpoints[2] = cx + qw;
        ypoints[2] = cy + qh;

        xpoints[3] = cx - qw;
        ypoints[3] = cy + qh;

        xpoints[4] = x;
        ypoints[4] = y + hh;
        
        xpoints[5] = cx - qw;
        ypoints[5] = cy - qh;
    }
    
    public PathIterator getPathIterator(AffineTransform affineTransform)
    {
        return new PolyIterator(affineTransform);
    }

    public Object clone()
    {
        RectangularPoly2D cloned = (RectangularPoly2D) super.clone();
        cloned.xpoints = new double[sides+1];
        cloned.ypoints = new double[sides+1];
        System.arraycopy(this.xpoints, 0, cloned.xpoints, 0, sides+1);
        System.arraycopy(this.ypoints, 0, cloned.ypoints, 0, sides+1);
        //cloned.computeVertices();
        return cloned;
    }

    public String toString()
    {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) + "[sides=" + sides + " " + x + "," + y + " " + width + "x" + height + "]";
    }
    
    class PolyIterator implements PathIterator
    {
        int index = 0;
        AffineTransform affine;

        public PolyIterator(AffineTransform affine)
        {
            this.affine = affine;
        }
    
        public int currentSegment(double[] coords) { 
            coords[0] = xpoints[index];  
            coords[1] = ypoints[index];
            if (affine != null)
                affine.transform(coords, 0, coords, 0, 1);            
            //System.out.println("i"+index + " (double)coords=" +coords[0] + "," + coords[1]);
            if (index == 0)
                return SEG_MOVETO;
            else if (index == sides)
                return SEG_CLOSE;
            else
                return SEG_LINETO;
        }
    
        public int currentSegment(float[] coords){ 
            coords[0] = (float) xpoints[index];  
            coords[1] = (float) ypoints[index];
            if (affine != null)
                affine.transform(coords, 0, coords, 0, 1);            
            //System.out.println("i"+index + " (float)coords=" +coords[0] + "," + coords[1]);
            if (index == 0)
                return SEG_MOVETO;
            else if (index == sides)
                return SEG_CLOSE;
            else
                return SEG_LINETO;
        } 
			
        public int getWindingRule() { return PathIterator.WIND_NON_ZERO; }
        public boolean isDone() { return index > sides; } 
        public void next() { index++ ; } 
    }
    
    
}

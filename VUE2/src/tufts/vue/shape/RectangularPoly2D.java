package tufts.vue.shape;

import java.awt.geom.RectangularShape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import sun.awt.geom.Crossings;

/**
 * This class implements a polygon shape fit into a specified rectanular region.
 * @author Scott Fraize
 */
public class RectangularPoly2D extends RectangularShape
{
    protected double x;
    protected double y;
    protected double width;
    protected double height;

    protected int sides;
    protected double[] xpoints;
    protected double[] ypoints;
    
    public RectangularPoly2D(int sides, double x, double y, double width, double height)
    {
        if (sides < 3 || sides > 8)
            throw new IllegalArgumentException("RectangularPoly2D: sides not >=3 && <=8: "+sides);
        this.sides = sides;
        xpoints = new double[sides+1];
        ypoints = new double[sides+1];
        xpoints[sides] = 0;
        ypoints[sides] = 0;

        setFrame(x, y, width, height);
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
    
    private void computeVertices3()
    {
        xpoints[0] = x + width/2;
        ypoints[0] = y;

        xpoints[1] = x;
        ypoints[1] = y + height;

        xpoints[2] = x + width;
        ypoints[2] = y + height;
    }

    private void computeVertices4()
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

    private void computeVertices5()
    {
        xpoints[0] = x + width/2;
        ypoints[0] = y;

        xpoints[1] = x + width;
        ypoints[1] = y + height/3;

        xpoints[2] = x + width*3/4;
        ypoints[2] = y + height;

        xpoints[3] = x + width/4;
        ypoints[3] = y + height;

        xpoints[4] = x;
        ypoints[4] = y + height/3;
    }

    private void computeVertices6()
    {
        // tan(30) = inset/halfH
        // halfH*tan(30) = inset
        double halfH = height/2;
        //double inset = halfH * Math.tan(Math.PI/6);
        double inset = 0.2257085*width;
        
        /*
        System.out.println("PENTAGON size=" + width + "x" + height);
        System.out.println("PENTAGON HALFH=" + halfH);
        System.out.println("PENTAGON INSET=" + inset);
        System.out.println("PENTAGON SEGSIZE  TOP=" + (width-(inset*2)));
        System.out.println("PENTAGON SEGSIZE LEFT=" + Math.sqrt(inset*inset+halfH*halfH));
        */
            
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
    
    private void computeVertices8()
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
        
    public PathIterator getPathIterator(AffineTransform affineTransform)
    {
        return new PolyIterator();
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
        return "RectangularPoly2D[sides=" + sides + " " + x + "," + y + " " + width + "x" + height + "]";
    }
    
    class PolyIterator implements PathIterator
    {
        int index = 0;
    
        public int currentSegment(double[] coords) { 
            coords[0] = xpoints[index];  
            coords[1] = ypoints[index];
            //System.out.println("i"+index + " dcoords=" +coords[0] + "," + coords[1]);
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
            //System.out.println("i"+index + " Fcoords=" +coords[0] + "," + coords[1]);
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

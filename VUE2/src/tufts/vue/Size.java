package tufts.vue;

import java.awt.Dimension;

public class Size {
    
    /**
     * The width dimension; negative values can be used. 
     */
    public float width;

    /**
     * The height dimension; negative values can be used. 
     */
    public float height;

    public Size() {
	this(0, 0);
    }

    public Size(Size s) {
	this(s.width, s.height);
    }

    public Size(Dimension d) {
	this(d.width, d.height);
    }

    public Size(float width, float height) {
	this.width = width;
	this.height = height;
    }

    public float getWidth() {
	return width;
    }

    public float getHeight() {
	return height;
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
	return "Size[" + width + "x" + height + "]";
    }
}

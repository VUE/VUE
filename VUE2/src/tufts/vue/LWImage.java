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

import java.awt.Image;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.AlphaComposite;
import javax.swing.ImageIcon;

public class LWImage extends LWComponent
    implements LWSelection.ControlListener
{
    protected ImageIcon imageIcon;
    protected LWIcon rollover = new LWIcon.Resource(this);
    private Point offset = new Point();
    
    public LWImage(Resource r) {
        setResource(r);
    }

    public LWComponent duplicate()
    {
        LWImage lwi = (LWImage) super.duplicate();
        lwi.setOffset(this.offset);
        return lwi;
    }
    
    public boolean isAutoSized() { return false; }

    /** does this support user resizing? */
    public boolean supportsUserResize() {
        return true;
    }

    //public void setScale(float scale) {
    //}
    
    public void setResource(Resource r) {
        super.setResource(r);
        if (r instanceof MapResource) {
            MapResource mr = (MapResource) r;
            try {
                this.imageIcon = new ImageIcon(mr.toURL());
                out("got content " + imageIcon);
                out("\tconent size " + imageIcon.getIconWidth() + "x" + imageIcon.getIconHeight());
            } catch (Exception e) {
                e.printStackTrace();
            }
            // don't set size if this is during a restore, which is the only
            // time width & height should be allowed less than 10
            if (this.width < 10 && this.height < 10) 
                setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
        }
    }


    /**
     * Keep image size to integer values as we can't clip throught the middle of a pixel.
     */
    public void setSize(float w, float h) {
        int width = (int) w;
        int height = (int) h;
        if (imageIcon.getIconWidth() < width)
            width = imageIcon.getIconWidth();
        if (imageIcon.getIconHeight() < height)
            height = imageIcon.getIconHeight();
        if (width < 10)
            width = 10;
        if (height < 10)
            height = 10;
        super.setSize(width, height);
    }

    /** for castor restore only */
    public LWImage() {}

    private final String Key_ImageOffset = "image.pan";

    public void setProperty(final Object key, Object val)
    {
        if (key == Key_ImageOffset)
            setOffset((Point) val);
        else
            super.setProperty(key, val);
    }

    public Object getPropertyValue(final Object key)
    {
        if (key == Key_ImageOffset)
            return getOffset();
        else
            return super.getPropertyValue(key);
    }
    
    public void setOffset(Point p) {
        if (p.x == offset.x && p.y == offset.y)
            return;
        Object oldValue = new Point(offset);
        this.offset.move(p.x, p.y);
        notify(Key_ImageOffset, oldValue);
    }

    public Point getOffset() {
        return new Point(this.offset);
    }
    
    static LWImage testImage() {
        LWImage i = new LWImage();
        i.imageIcon = VueResources.getImageIcon("vueIcon32x32");
        i.setSize(i.imageIcon.getIconWidth(), i.imageIcon.getIconHeight());
        return i;
    }

    public void draw(DrawContext dc)
    {
        dc.g.translate(getX(), getY());
        float scale = getScale();
        if (scale != 1f) dc.g.scale(scale, scale);

        drawImage(dc);

        if (scale != 1f) dc.g.scale(1/scale, 1/scale);
        dc.g.translate(-getX(), -getY());
    }

    public int getImageWidth() {
        return imageIcon.getIconWidth();
    }
    public int getImageHeight() {
        return imageIcon.getIconHeight();
    }

    private static final AlphaComposite MatteTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    protected void drawImage(DrawContext dc) {
        //System.out.println("\tsize " + imageIcon.getIconWidth() + "x" + imageIcon.getIconHeight());
        //setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());

        //if (isSelected() && !dc.isPrinting()) {
        if (isSelected() && !dc.isPrinting() && dc.getActiveTool() instanceof ImageTool) {
            dc.g.setComposite(MatteTransparency);
            imageIcon.paintIcon(null, dc.g, offset.x, offset.y);
            dc.g.setComposite(AlphaComposite.Src);
        }
        dc.g.clipRect(0, 0, (int)getAbsoluteWidth(), (int)getAbsoluteHeight());
        imageIcon.paintIcon(null, dc.g, offset.x, offset.y);
    }

    public void mouseOver(MapMouseEvent e)
    {
        //mIconBlock.checkAndHandleMouseOver(e);
    }


    private Point offsetStart;
    private Point2D dragStart;
    
    /** interface ControlListener handler */
    public void controlPointPressed(int index, MapMouseEvent e)
    {
        //out("control point " + index + " pressed");
        offsetStart = new Point(offset);
        dragStart = e.getMapPoint();
    }
    
    /** interface ControlListener handler */
    public void controlPointMoved(int index, MapMouseEvent e)
    {
        //out("control point " + index + " moved");
        if (index == 0) {
            // todo: isn't handling scaling and/or cume roundoff errors
            Point off = new Point();
            off.x = offsetStart.x - (int) (dragStart.getX() - e.getMapX());
            off.y = offsetStart.y - (int) (dragStart.getY() - e.getMapY());
            constrainOffset(off);
            setOffset(off);
        } else
            throw new IllegalArgumentException(this + " no such control point");

    }

    /** keep LWImage filled with image bits (never display "area" outside of the image) */
    private void constrainOffset(Point p)
    {
        if (p.x > 0)
            p.x = 0;
        if (p.y > 0)
            p.y = 0;
        if (p.x + getImageWidth() < getAbsoluteWidth())
            p.x = (int) getAbsoluteWidth() - getImageWidth();
        if (p.y + getImageHeight() < getAbsoluteHeight())
            p.y = (int) getAbsoluteHeight() - getImageHeight();
    }

    /** interface ControlListener handler */
    public void controlPointDropped(int index, MapMouseEvent e)
    {
        out("control point " + index + " dropped");
        /*
        LWComponent dropTarget = e.getViewer().getIndication();
        // TODO BUG: above doesn't work if everything is selected
        if (DEBUG.MOUSE) System.out.println("LWLink: control point " + index + " dropped on " + dropTarget);
        if (dropTarget != null) {
            if (index == 0 && ep1 == null && ep2 != dropTarget)
                setComponent1(dropTarget);
            else if (index == 1 && ep2 == null && ep1 != dropTarget)
                setComponent2(dropTarget);
            // todo: ensure paint sequence same as LinkTool.makeLink
        }
            */
    }


    private LWSelection.ControlPoint[] controlPoints = new LWSelection.ControlPoint[1];
    /** interface ControlListener */
    public LWSelection.ControlPoint[] getControlPoints()
    {
        controlPoints[0] = new LWSelection.ControlPoint(getCenterX(), getCenterY());
        controlPoints[0].setColor(null); // no fill (transparent)
        return controlPoints;
    }
}
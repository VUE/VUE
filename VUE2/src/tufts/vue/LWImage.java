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
import java.awt.Color;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.*;
import java.awt.AlphaComposite;
import javax.swing.ImageIcon;

/**
 * Handle the presentation of an image resource, allowing cropping.
 */
public class LWImage extends LWComponent
    implements LWSelection.ControlListener
{
    private final static int MinWidth = 10;
    private final static int MinHeight = 10;
    
    protected ImageIcon imageIcon;
    private Point2D.Float offset = new Point2D.Float(); // x & y always <= 0
    private double rotation = 0;
    
    //private LWIcon.Resource resourceIcon = new LWIcon.Resource(this);
    private transient LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         20, 12,
                         null,
                         LWIcon.Block.VERTICAL,
                         LWIcon.Block.COORDINATES_COMPONENT);

    public LWImage(Resource r) {
        setResource(r);
    }

    public LWComponent duplicate()
    {
        // todo: if had list of property keys in object, LWComponent
        // could handle all the duplicate code.
        LWImage i = (LWImage) super.duplicate();
        i.setOffset(this.offset);
        i.setRotation(this.rotation);
        return i;
    }
    
    public boolean isAutoSized() { return false; }

    /** does this support user resizing? */
    public boolean supportsUserResize() {
        return true;
    }

    private static final float ChildImageScale = 0.2f;
    public void setScale(float scale) {
        if (scale == 1f)
            super.setScale(1f);
        else {
            float adjustment = ChildImageScale / LWNode.ChildScale;
            super.setScale(scale * adjustment); // produce ChildImageScale at top level child
        }
    }
    
    public void setResource(Resource r) {
        super.setResource(r);
        if (r instanceof MapResource) {
            MapResource mr = (MapResource) r;
            try {
                this.imageIcon = new ImageIcon(mr.toURL());
                System.out.println("LWImage size " + imageIcon.getIconWidth() + "x" + imageIcon.getIconHeight() + " from " + mr.toURL());
                //System.out.println("LWImage got " + imageIcon + " size " + imageIcon.getIconWidth() + "x" + imageIcon.getIconHeight());
                //out("\tconent size " + imageIcon.getIconWidth() + "x" + imageIcon.getIconHeight());
            } catch (Exception e) {
                e.printStackTrace();
            }
            // don't set size if this is during a restore, which is the only
            // time width & height should be allowed less than 10
            if (this.width < 10 && this.height < 10) 
                setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
        }
        //resourceIcon.layout();
        mIconBlock.layout();
    }

    public void layout() {
        mIconBlock.layout();
    }


    /**
     * Don't let us get bigger than the size of our image, or
     * smaller than MinWidth/MinHeight.
     */
    public void userSetSize(float width, float height) {
        if (DEBUG.IMAGE) out("userSetSize0 " + width + "x" + height);
        if (imageIcon.getIconWidth() + offset.x < width)
            width = imageIcon.getIconWidth() + offset.x;
        if (imageIcon.getIconHeight() + offset.y < height)
            height = imageIcon.getIconHeight() + offset.y;
        if (width < MinWidth)
            width = MinWidth;
        if (height < MinHeight)
            height = MinHeight;
        if (DEBUG.IMAGE) out("userSetSize1 " + width + "x" + height);
        super.setSize(width, height);
    }

    /* @param r - requested LWImage frame in map coordinates */
    //private void constrainFrameToImage(Rectangle2D.Float r) {}

    /**
     * When user changes a frame on the image, if the location changes,
     * attempt to keep our content image in the same place (e.g., make
     * it look like we're just moving a the clip-region, if the LWImage
     * is smaller than the size of the underlying image).
     */
    public void userSetFrame(float x, float y, float w, float h)
    {
        if (DEBUG.IMAGE) out("userSetFrame0 " + VueUtil.out(new Rectangle2D.Float(x, y, w, h)));
        if (w < MinWidth) {
            if (x > getX()) // dragging left edge right: hold it back
                x -= MinWidth - w;
            w = MinWidth;
        }
        if (h < MinHeight) {
            if (y > getY()) // dragging top edge down: hold it back
                y -= MinHeight - h;
            h = MinHeight;
        }
        Point2D.Float off = new Point2D.Float(offset.x, offset.y);
        off.x += getX() - x;
        off.y += getY() - y;
        //if (DEBUG.IMAGE) out("tmpoff " + VueUtil.out(off));
        if (off.x > 0) {
            x += off.x;
            w -= off.x;
            off.x = 0;
        }
        if (off.y > 0) {
            y += off.y;
            h -= off.y;
            off.y = 0;
        }
        setOffset(off);
        if (DEBUG.IMAGE) out("userSetFrame1 " + VueUtil.out(new Rectangle2D.Float(x, y, w, h)));
        userSetSize(w, h);
        setLocation(x, y);
    }

    public static final Key Key_ImageOffset = new Key("image.pan") {
            public void setValue(LWComponent c, Object val) { ((LWImage)c).setOffset((Point2D)val); }
            public Object getValue(LWComponent c) { return ((LWImage)c).getOffset(); }
        };

    public void setOffset(Point2D p) {
        if (p.getX() == offset.x && p.getY() == offset.y)
            return;
        Object oldValue = new Point2D.Float(offset.x, offset.y);
        if (DEBUG.IMAGE) out("LWImage setOffset " + VueUtil.out(p));
        this.offset.setLocation(p.getX(), p.getY());
        notify(Key_ImageOffset, oldValue);
    }

    public Point2D getOffset() {
        return new Point2D.Float(offset.x, offset.y);
    }
    
    public int getImageWidth() {
        return imageIcon.getIconWidth();
    }
    public int getImageHeight() {
        return imageIcon.getIconHeight();
    }

    static LWImage testImage() {
        LWImage i = new LWImage();
        i.imageIcon = VueResources.getImageIcon("vueIcon32x32");
        i.setSize(i.imageIcon.getIconWidth(), i.imageIcon.getIconHeight());
        return i;
    }

    private static final AlphaComposite HudTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
    public void draw(DrawContext dc)
    {
        drawPathwayDecorations(dc);
        drawSelectionDecorations(dc);
        
        dc.g.translate(getX(), getY());
        float scale = getScale();

        if (scale != 1f) dc.g.scale(scale, scale);

        /*
        if (getStrokeWidth() > 0) {
            dc.g.setStroke(new BasicStroke(getStrokeWidth() * 2));
            dc.g.setColor(getStrokeColor());
            dc.g.draw(new Rectangle2D.Float(0,0, getAbsoluteWidth(), getAbsoluteHeight()));
        }
        */

        drawImage(dc);

        if (getStrokeWidth() > 0) {
            dc.g.setStroke(this.stroke);
            dc.g.setColor(getStrokeColor());
            dc.g.draw(new Rectangle2D.Float(0,0, getAbsoluteWidth(), getAbsoluteHeight()));
        }
        
        //resourceIcon.draw(dc);
        if (isSelected() && !dc.isPrinting()) {
            dc.g.setComposite(HudTransparency);
            dc.g.setColor(Color.WHITE);
            dc.g.fill(mIconBlock);
            dc.g.setComposite(AlphaComposite.Src);
            mIconBlock.draw(dc);
        }

        if (scale != 1f) dc.g.scale(1/scale, 1/scale);
        dc.g.translate(-getX(), -getY());
    }

    public static final Key KEY_Rotation = new Key("image.rotation") { // rotation in radians
            public void setValue(LWComponent c, Object val) { ((LWImage)c).setRotation(((Double)val).doubleValue()); }
            public Object getValue(LWComponent c) { return new Double(((LWImage)c).getRotation()); }
        };
    
    public void setRotation(double rad) {
        Object old = new Double(rotation);
        this.rotation = rad;
        notify(KEY_Rotation, old);
    }
    public double getRotation() {
        return rotation;
    }

    private static final AlphaComposite MatteTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    protected void drawImage(DrawContext dc)
    {
        AffineTransform transform = AffineTransform.getTranslateInstance(offset.x, offset.y);
        if (rotation != 0 && rotation != 360)
            transform.rotate(rotation, getImageWidth() / 2, getImageHeight() / 2);
        
        if (isSelected() && !dc.isPrinting() && dc.getActiveTool() instanceof ImageTool) {
            dc.g.setComposite(MatteTransparency);
            dc.g.drawImage(imageIcon.getImage(), transform, null);
            dc.g.setComposite(AlphaComposite.Src);
        }
        Shape oldClip = dc.g.getClip();
        dc.g.clip(new Rectangle2D.Float(0,0, getAbsoluteWidth(), getAbsoluteHeight()));
        //dc.g.clip(new Ellipse2D.Float(0,0, getAbsoluteWidth(), getAbsoluteHeight()));
        dc.g.drawImage(imageIcon.getImage(), transform, null);
        dc.g.setClip(oldClip);
    }

    public void mouseOver(MapMouseEvent e)
    {
        Rectangle2D tipRegion = getBounds();
        //e.getViewer().setTip(resourceIcon.getToolTipComponent(), tipRegion, tipRegion);
        //e.getViewer().setTip(tipComponent, avoidRegion, tipRegion);
        mIconBlock.checkAndHandleMouseOver(e);
    }


    private transient Point2D.Float dragStart;
    private transient Point2D.Float offsetStart;
    private transient Point2D.Float imageStart; // absolute map location of 0,0 in the image
    private transient Point2D.Float locationStart;
    
    /** interface ControlListener handler */
    public void controlPointPressed(int index, MapMouseEvent e)
    {
        //out("control point " + index + " pressed");
        offsetStart = new Point2D.Float(offset.x, offset.y);
        locationStart = new Point2D.Float(getX(), getY());
        dragStart = e.getMapPoint();
        imageStart = new Point2D.Float(getX() + offset.x, getY() + offset.y);
    }
    
    /** interface ControlListener handler */
    public void controlPointMoved(int index, MapMouseEvent e)
    {
        if (index == 0) {
            float deltaX = dragStart.x - e.getMapX();
            float deltaY = dragStart.y - e.getMapY();
            Point2D.Float off = new Point2D.Float();
            if (e.isShiftDown()) {
                // drag frame around on underlying image
                // we need to constantly adjust offset to keep
                // it fixed in absolute map coordinates.
                Point2D.Float loc = new  Point2D.Float();
                loc.x = locationStart.x - deltaX;
                loc.y = locationStart.y - deltaY;
                off.x = offsetStart.x + deltaX;
                off.y = offsetStart.y + deltaY;
                constrainLocationToImage(loc, off);
                setOffset(off);
                setLocation(loc);
            } else {
                // drag underlying image around within frame
                off.x = offsetStart.x - deltaX;
                off.y = offsetStart.y - deltaY;
                constrainOffset(off);
                setOffset(off);
            }
        } else
            throw new IllegalArgumentException(this + " no such control point");

    }

    /** Keep LWImage filled with image bits (never display "area" outside of the image) */
    private void constrainOffset(Point2D.Float off)
    {
        if (off.x > 0)
            off.x = 0;
        if (off.y > 0)
            off.y = 0;
        if (off.x + getImageWidth() < getAbsoluteWidth())
            off.x = getAbsoluteWidth() - getImageWidth();
        if (off.y + getImageHeight() < getAbsoluteHeight())
            off.y = getAbsoluteHeight() - getImageHeight();
    }
    
    /** Keep LWImage filled with image bits (never display "area" outside of the image)
     * Used for constraining the clipped region to the underlying image, which we keep
     * fixed at an absolute map location in this constraint. */
    private void constrainLocationToImage(Point2D.Float loc, Point2D.Float off)
    {
        if (off.x > 0) {
            loc.x += offset.x;
            off.x = 0;
        }
        if (off.y > 0) {
            loc.y += offset.y;
            off.y = 0;
        }
        // absolute image image location should never change from imageStart
        // Keep us from panning beyond top or left
        Point2D.Float image = new Point2D.Float(loc.x + off.x, loc.y + off.y);
        if (image.x < imageStart.x) {
            //System.out.println("home left");
            loc.x = imageStart.x;
            off.x = 0;
        }
        if (image.y < imageStart.y) {
            //System.out.println("home top");
            loc.y = imageStart.y;
            off.y = 0;
        }
        // Keep us from panning beyond right or bottom
        if (getImageWidth() + off.x < getAbsoluteWidth()) {
            //System.out.println("out right");
            loc.x = (imageStart.x + getImageWidth()) - getAbsoluteWidth();
            off.x = getAbsoluteWidth() - getImageWidth();
        }
        if (getImageHeight() + off.y < getAbsoluteHeight()) {
            //System.out.println("out bot");
            loc.y = (imageStart.y + getImageHeight()) - getAbsoluteHeight();
            off.y = getAbsoluteHeight() - getImageHeight();
        }

    }

    /** interface ControlListener handler */
    public void controlPointDropped(int index, MapMouseEvent e)
    {
        if (DEBUG.IMAGE) out("control point " + index + " dropped");
    }


    private LWSelection.ControlPoint[] controlPoints = new LWSelection.ControlPoint[1];
    /** interface ControlListener */
    public LWSelection.ControlPoint[] getControlPoints()
    {
        controlPoints[0] = new LWSelection.ControlPoint(getCenterX(), getCenterY());
        controlPoints[0].setColor(null); // no fill (transparent)
        return controlPoints;
    }


    /** @deprecated - for castor restore only */
    public LWImage() {}
}
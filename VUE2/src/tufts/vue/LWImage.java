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

import tufts.Util;

import java.awt.Image;
import java.awt.Point;
import java.awt.Color;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.*;
import java.awt.AlphaComposite;
import java.awt.image.ImageObserver;
import java.net.URL;
import javax.swing.ImageIcon;

/**
 * Handle the presentation of an image resource, allowing cropping.
 *
 * @version $Revision: 1.1 $ / $Date: 2006/01/20 18:57:33 $ / $Author: sfraize $
 */
public class LWImage extends LWComponent
    implements LWSelection.ControlListener, ImageObserver
{
    /** scale of images when a child of other nodes */
    private static final float ChildImageScale = 0.2f;
    
    private final static int MinWidth = 10;
    private final static int MinHeight = 10;
    
    private Image mImage;
    private int mImageWidth = -1;
    private int  mImageHeight = -1;
    private double rotation = 0;
    private Point2D.Float mOffset = new Point2D.Float(); // x & y always <= 0
    private Object mThreadedUndoKey;
    private boolean mImageError = false;
    
    private transient LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         20, 12,
                         null,
                         LWIcon.Block.VERTICAL,
                         LWIcon.Block.COORDINATES_COMPONENT_NO_SHRINK);

    public LWImage() {}
    
    public LWComponent duplicate(LinkPatcher linkPatcher)
    {
        // TODO: if had list of property keys in object, LWComponent
        // could handle all the duplicate code.
        LWImage i = (LWImage) super.duplicate(linkPatcher);
        i.mImage = mImage;
        i.mImageWidth = mImageWidth;
        i.mImageHeight = mImageHeight;
        i.mImageError = mImageError;
        i.setOffset(this.mOffset);
        i.setRotation(this.rotation);
        return i;
    }
    
    public boolean isAutoSized() { return false; }

    /** @return true -- image's support resize (which is currently just a crop) */
    public boolean supportsUserResize() {
        return true;
    }

    public void setScale(float scale) {
        if (scale == 1f)
            super.setScale(1f);
        else {
            float adjustment = ChildImageScale / LWNode.ChildScale;
            super.setScale(scale * adjustment); // produce ChildImageScale at top level child
        }
    }

    public void layout() {
        mIconBlock.layout();
    }
    
    public void setResource(Resource r) {
        setResourceAndLoad(r, null);
    }
    
    // todo: find a better way to do this than passing in an undo manager, which is dead ugly
    public void setResourceAndLoad(Resource r, UndoManager undoManager) {
        super.setResource(r);
        if (r instanceof MapResource) {
            // TODO: need to do this in a thread, as not enough to wait for prepareImage
            loadImage((MapResource)r, undoManager);
        }
    }

    /*
    private void loadImageAsync(MapResource r) {
        Object content = new Object();
        try {
            content = r.getContent();
            imageIcon = (ImageIcon) content;
        } catch (ClassCastException cce) {
            cce.printStackTrace();
            System.err.println("getContent didn't return ImageIcon: got "
                               + content.getClass().getName() + " from " + r.getClass() + " " + r);
            imageIcon = null;
            //if (DEBUG.CASTOR) System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("error getting " + r);
        }
        // don't set size if this is during a restore [why not?], which is the only
        // time width & height should be allowed less than 10
        // [ What?? ] -- todo: this doesn't work if we're here because the resource was changed...
        //if (this.width < 10 && this.height < 10)
        if (imageIcon != null) {
            int w = imageIcon.getIconWidth();
            int h = imageIcon.getIconHeight();
            if (w > 0 && h > 0)
                setSize(w, h);
        }
        layout();
        notify(LWKey.RepaintComponent);
    }
    */

    private void loadImage(MapResource mr, UndoManager undoManager)
    {

        // TODO: have the LWMap make a call at the end of a restore to
        // all LWComponents telling them to start loading any media
        // they need.  Pass in a media tracker that the LWMap and/or
        // MapViewer can use to track/report the status of loading,
        // and know when it's 100% complete.

        if (DEBUG.IMAGE) out("getImage " + mr.getURL());
                
        Image image = null;
        try {
            URL url = mr.toURL();
            if (url == null)
                return;

            // will need to put this in a thread: if the host isn't responding,
            // we hang here for a while.  It will apparently ALWAYS eventually
            // get an Image object, but then we just get an eventually callback to
            // imageUpdate with an error code.

            String s = mr.getSpec();
            
            if (s.startsWith("file://")) {

                // TODO: SEE Util.java: WINDOWS URL'S DON'T WORK IF START WITH FILE:// (two slashes), MUST HAVE THREE!
                // move this code to MapResource; find out if can even force a URL to have
                // an extra slash in it!  Report this as a java bug!
                
                s = s.substring(7);
                out("fetching " + s);
                image = java.awt.Toolkit.getDefaultToolkit().getImage(s);
            } else
                image = java.awt.Toolkit.getDefaultToolkit().getImage(url);
            
        } catch (Throwable t) {
            Util.printStackTrace(t);
            return;
        }

        // don't bother to set mImage here: JVM's no longer do drawing of available bits

        if (DEBUG.IMAGE) out("prepareImage on " + image);
                
        if (java.awt.Toolkit.getDefaultToolkit().prepareImage(image, -1, -1, this)) {
            if (DEBUG.IMAGE || DEBUG.THREAD) out("ALREADY LOADED");
            mImage = image;
            mImageWidth = image.getWidth(null);
            mImageHeight = image.getHeight(null);
            if (getAbsoluteWidth() < 10 && getAbsoluteHeight() < 10)
                setSize(mImageWidth, mImageHeight);
        } else {
            mDebugChar = sDebugChar;
            if (++sDebugChar > 'Z')
                sDebugChar = 'A';
            // save a key that marks the current location in the undo-queue,
            // to be applied to the subsequent thread that make calls
            // to imageUpdate, so that all further property changes eminating
            // from that thread are applied to the same location in the undo queue.
            if (undoManager == null)
                mThreadedUndoKey = UndoManager.getKeyForUpcomingMark(this);
            else
                mThreadedUndoKey = undoManager.getKeyForUpcomingMark();
        }
    }

    public void setSize(float w, float h) {
        super.setSize(w, h);
        // Even if we don't have an image yet, we need to keep these set in case user attemps to resize the frame.
        // They can still crop down if they like, but this prevents them from making it any bigger.
        if (mImageWidth < 0)
            mImageWidth = (int) getAbsoluteWidth();
        if (mImageHeight < 0)
            mImageHeight = (int) getAbsoluteHeight();
    }

    public boolean isCropped() {
        return mOffset.x < 0 || mOffset.y < 0;
    }


    private static char sDebugChar = 'A';
    private char mDebugChar;
    public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height)
    {
        if (DEBUG.IMAGE && (DEBUG.META || (flags & ImageObserver.SOMEBITS) == 0))
            out("imageUpdate " + img + " flags=" + flags + " " + width + "x" + height);
        
        if ((flags & ImageObserver.ERROR) != 0) {
            if (DEBUG.IMAGE) out("ERROR");
            mImageError = true;
            // set image dimensions so if we resize w/out image it works
            mImageWidth = (int) getAbsoluteWidth();
            mImageHeight = (int) getAbsoluteHeight();
            if (mImageWidth < 1) {
                mImageWidth = 100;
                mImageHeight = 100;
                setSize(100,100);
            }
            return false;
        }
            
        Thread thread = Thread.currentThread();

        if ((flags & ImageObserver.SOMEBITS) == 0) {
            if (DEBUG.IMAGE || DEBUG.THREAD)
                System.out.println("\n" + getResource() + " (" + flags + ") "
                                   + thread + " 0x" + Integer.toHexString(thread.hashCode())
                                   + " " + sun.awt.AppContext.getAppContext()
                                   );
        } else {
            if (DEBUG.IMAGE || DEBUG.THREAD) System.err.print(mDebugChar);
        }
        
        if ((flags & ImageObserver.WIDTH) != 0 && (flags & ImageObserver.HEIGHT) != 0) {
            mImageWidth = width;
            mImageHeight = height;
            if (DEBUG.IMAGE || DEBUG.THREAD) System.out.println(getResource() + " GOT SIZE " + width + "x" + height);

            // For the events triggered by the setSize below, make sure they go
            // to the right point in the undo queue.
            UndoManager.attachCurrentThreadToMark(mThreadedUndoKey);
            
            // If we're interrupted before this happens, and this is the drop of a new image,
            // we'll see a zombie event complaint from this setSize which is safely ignorable.
            // todo: suspend events if our thread was interrupted
            if (isCropped() == false) {
                // don't set size if we are cropped: we're probably reloading from a saved .vue
                setSize(width, height);
            }
            layout();
            notify(LWKey.RepaintAsync);
        }
        
        if ((flags & ImageObserver.ALLBITS) != 0) {
            // Be sure to set the image before detaching from the thread,
            // or when the detach issues repaint events, we won't see the image.
            mImage = img;
            if (mThreadedUndoKey == null) {
                notify(LWKey.RepaintAsync);
            } else {
                UndoManager.detachCurrentThread(mThreadedUndoKey); // in case our ImageFetcher get's re-used
                mThreadedUndoKey = null;
            }
            return false;
        }

        if (Thread.interrupted()) {
            if (DEBUG.IMAGE || DEBUG.THREAD)
                System.err.println("\n" + getResource() + " *** INTERRUPTED *** (lowering priority) " + thread);
            thread.setPriority(Thread.MIN_PRIORITY);
            return true; // let it finish anyway
            //return false;
        } else
            return true;
    }
    
    /**
     * Don't let us get bigger than the size of our image, or
     * smaller than MinWidth/MinHeight.
     */
    public void userSetSize(float width, float height) {
        if (DEBUG.IMAGE) out("userSetSize0 " + width + "x" + height);
        if (mImageWidth + mOffset.x < width)
            width = mImageWidth + mOffset.x;
        if (mImageHeight + mOffset.y < height)
            height = mImageHeight + mOffset.y;
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
        Point2D.Float off = new Point2D.Float(mOffset.x, mOffset.y);
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

    public static final Key Key_ImageOffset = new Key("image.pan") {
            public void setValue(LWComponent c, Object val) { ((LWImage)c).setOffset((Point2D)val); }
            public Object getValue(LWComponent c) { return ((LWImage)c).getOffset(); }
        };

    public void setOffset(Point2D p) {
        if (p.getX() == mOffset.x && p.getY() == mOffset.y)
            return;
        Object oldValue = new Point2D.Float(mOffset.x, mOffset.y);
        if (DEBUG.IMAGE) out("LWImage setOffset " + VueUtil.out(p));
        this.mOffset.setLocation(p.getX(), p.getY());
        notify(Key_ImageOffset, oldValue);
    }

    public Point2D getOffset() {
        return new Point2D.Float(mOffset.x, mOffset.y);
    }
    
    public int getImageWidth() {
        return mImageWidth;
    }
    public int getImageHeight() {
        return mImageHeight;
    }

    /*
    static LWImage testImage() {
        LWImage i = new LWImage();
        i.imageIcon = VueResources.getImageIcon("vueIcon32x32");
        i.setSize(i.mImageWidth, i.mImageHeight);
        return i;
    }
    */

    private static final AlphaComposite HudTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
    public void draw(DrawContext dc)
    {
        drawPathwayDecorations(dc);
        drawSelectionDecorations(dc);
        
        dc.g.translate(getX(), getY());
        float _scale = getScale();

        if (_scale != 1f) dc.g.scale(_scale, _scale);

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
        
        if (isSelected() && dc.isInteractive()) {
            dc.g.setComposite(HudTransparency);
            dc.g.setColor(Color.WHITE);
            dc.g.fill(mIconBlock);
            dc.g.setComposite(AlphaComposite.Src);
            // TODO: set a clip so won't draw outside
            // image bounds if is very small
            mIconBlock.draw(dc);
        }

        if (_scale != 1f) dc.g.scale(1/_scale, 1/_scale);
        dc.g.translate(-getX(), -getY());

    }

    private static final AlphaComposite MatteTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
    //private static final Color ErrorColor = new Color(255,128,128, 64);
    private static final Color ErrorColor = Color.red;

    protected void drawImage(DrawContext dc)
    {
        if (mImage == null) {
            float w = getAbsoluteWidth();
            float h = getAbsoluteHeight();
            if (mImageError)
                dc.g.setColor(ErrorColor);
            else
                dc.g.setColor(Color.darkGray);
            dc.g.fillRect(0, 0, (int)w, (int)h);
            dc.g.setColor(Color.lightGray);
            dc.g.drawRect(0, 0, (int)w, (int)h); // can't see this line at small scales
            return;
        }
        
        AffineTransform transform = AffineTransform.getTranslateInstance(mOffset.x, mOffset.y);
        if (rotation != 0 && rotation != 360)
            transform.rotate(rotation, getImageWidth() / 2, getImageHeight() / 2);
        
        if (isSelected() && dc.isInteractive() && dc.getActiveTool() instanceof ImageTool) {
            dc.g.setComposite(MatteTransparency);
            dc.g.drawImage(mImage, transform, null);
            dc.g.setComposite(AlphaComposite.Src);
        }
        Shape oldClip = dc.g.getClip();
        dc.g.clip(new Rectangle2D.Float(0,0, getAbsoluteWidth(), getAbsoluteHeight()));
        //dc.g.clip(new Ellipse2D.Float(0,0, getAbsoluteWidth(), getAbsoluteHeight()));
        dc.g.drawImage(mImage, transform, null);
        dc.g.setClip(oldClip);
    }

    public void mouseOver(MapMouseEvent e)
    {
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
        offsetStart = new Point2D.Float(mOffset.x, mOffset.y);
        locationStart = new Point2D.Float(getX(), getY());
        dragStart = e.getMapPoint();
        imageStart = new Point2D.Float(getX() + mOffset.x, getY() + mOffset.y);
    }
    
    /** interface ControlListener handler */
    public void controlPointMoved(int index, MapMouseEvent e)
    {
        if (index == 0) {

            if (mImageError) // don't let user play with offset if no image visible
                return;
            
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
            loc.x += mOffset.x;
            off.x = 0;
        }
        if (off.y > 0) {
            loc.y += mOffset.y;
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


}
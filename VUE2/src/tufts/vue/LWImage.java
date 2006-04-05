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
import javax.imageio.ImageIO;

/**
 * Handle the presentation of an image resource, allowing cropping.
 *
 * @version $Revision: $ / $Date: 2006/01/20 18:57:33 $ / $Author: sfraize $
 */


// TODO: get undo working again.
// TODO: on delete, null the image so it can be garbage collected, and on
//       un-delete, restore it via the resource, and hopefully it'll
//       still be in the memory cache (if not, it'll be in the disk cache)
// TODO: update bad (error) images if preview gets good data
//       Better: handle this via listening to the resource for updates
//       (the LWCopmonent can do this), and if it's a CONTENT_CHANGED
//       update (v.s., say, a META_DATA_CHANGED), then we can refetch
//       the content.  Actually, would still be nice if this happened
//       just by selecting the object, in case the resource previewer
//       didn't happen to be open.
          


public class LWImage extends LWComponent
    implements LWSelection.ControlListener, Images.Listener
{
    /** scale of images when a child of other nodes */
    static final float ChildImageScale = 0.2f;
    
    private final static int MinWidth = 10;
    private final static int MinHeight = 10;
    
    private Image mImage;
    private int mImageWidth = -1;
    private int  mImageHeight = -1;
    private double rotation = 0;
    private Point2D.Float mOffset = new Point2D.Float(); // x & y always <= 0
    private Object mUndoMarkForThread;
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

    /*
    public void setScale(float scale) {
        if (scale == 1f)
            super.setScale(1f);
        else {
            float adjustment = ChildImageScale / LWNode.ChildScale;
            super.setScale(scale * adjustment); // produce ChildImageScale at top level child
        }
    }
    */

    public void XML_addNotify(String name, Object parent) {
        super.XML_addNotify(name, parent);
        if (this.scale == 1f && parent instanceof LWNode)
            setScale(ChildImageScale);
    }
    

    public void layout() {
        mIconBlock.layout();
    }
    
    // TODO: this wants to be on LWComponent, in case this is a
    // regular node containing an LWImage, we want the image to
    // update, as it doesn't get selected.  This depends on
    // how me might redo image support in maps tho, so
    // wait on that...
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (selected && mImageError && hasResource())
            loadResourceImage(getResource(), null);
    }
    
    public void setResource(Resource r) {
        setResourceAndLoad(r, null);
    }

    // todo: find a better way to do this than passing in an undo manager, which is dead ugly
    public void setResourceAndLoad(Resource r, UndoManager undoManager) {
        super.setResource(r);
        loadResourceImage(r, undoManager);
    }


    private void loadResourceImage(final Resource r, final UndoManager um)
    {
        int width = r.getProperty("image.width", 32);
        int height = r.getProperty("image.height", 32);

        // If we know a size before loading, this will get
        // us displaying that size.  If not, we'll set
        // us to a minimum size for display until we
        // know the real size.
        setSize(width, height);
        
        // save a key that marks the current location in the undo-queue,
        // to be applied to the subsequent thread that make calls
        // to imageUpdate, so that all further property changes eminating
        // from that thread are applied to the same location in the undo queue.
        
        synchronized (this) {
            // If image is not immediately availble, need to mark current
            // place in undo key for changes that happen due to the image
            // arriving.  We sync to be certian the key is set before
            // we can get any image callbacks.
            if (!Images.getImage(r, this))
                mUndoMarkForThread = UndoManager.getKeyForNextMark(this);
            else
                mUndoMarkForThread = null;
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

    /** @see Images.Listener */
    public synchronized void gotImageSize(Object imageSrc, int width, int height)
    {
        mImageWidth = width;
        mImageHeight = height;

        if (mUndoMarkForThread == null) {
            if (DEBUG.Enabled) out("gotImageSize: no undo key");
        }
            
        // For the events triggered by the setSize below, make sure they go
        // to the right point in the undo queue.
        UndoManager.attachCurrentThreadToMark(mUndoMarkForThread);
        
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
    
    /** @see Images.Listener */
    public synchronized void gotImage(Object imageSrc, Image image, int w, int h) {
        // Be sure to set the image before detaching from the thread,
        // or when the detach issues repaint events, we won't see the image.
        mImageError = false;
        mImageWidth = w;
        mImageHeight = h;
        mImage = image;
        if (isCropped() == false)
            setSize(w, h);
        if (mUndoMarkForThread == null) {
            notify(LWKey.RepaintAsync);
        } else {
            // in case this thread get's re-used:
            UndoManager.detachCurrentThread(mUndoMarkForThread);
            mUndoMarkForThread = null;
        }

        // Any problem using the Image Fetcher thread to do this?
        //if (getResource() instanceof MapResource)
        //((MapResource)getResource()).scanForMetaData(LWImage.this, true);
    }

    /** @see Images.Listener */
    public synchronized void gotImageError(Object imageSrc, String msg) {
        // set image dimensions so if we resize w/out image it works
        mImageError = true;
        mImageWidth = (int) getAbsoluteWidth();
        mImageHeight = (int) getAbsoluteHeight();
        if (mImageWidth < 1) {
            mImageWidth = 100;
            mImageHeight = 100;
            setSize(100,100);
        }
        notify(LWKey.RepaintAsync);
        
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
            int w = (int) getAbsoluteWidth();
            int h = (int) getAbsoluteHeight();
            if (mImageError)
                dc.g.setColor(ErrorColor);
            else
                dc.g.setColor(Color.darkGray);
            dc.g.fillRect(0, 0, w, h);
            dc.g.setColor(Color.lightGray);
            dc.g.drawRect(0, 0, w, h); // can't see this line at small scales
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
        //dc.g.clip(new Ellipse2D.Float(0,0, getAbsoluteWidth(), getAbsoluteHeight())); // works nicely
        //dc.g.drawImage(mImage, 0, 0, this); // no help in drawing partial images
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

    // TODO: have the LWMap make a call at the end of a restore to all LWComponents
    // telling them to start loading any media they need.  Pass in a media tracker
    // that the LWMap and/or MapViewer can use to track/report the status of
    // loading, and know when it's 100% complete.
    
    // Note: all this code will likely be superceeded by generic content
    // loading & caching code, in which case we may not be using
    // an ImageObserver anymore, just a generic input stream, tho actually,
    // we wouldn't have the chance to get the size as soon as it comes in,
    // so probably not all will be superceeded.

    // TODO: problem: if you drop a second image before the first one
    // has finished loading, both will try and set an undo mark for their thread,
    // but the're both in the Image Fetcher thread!  So we're going to need
    // todo our own loading after all, as I see no way for the UndoManager
    // to tell between events coming in on the same thread, unless maybe
    // the mark can be associated with a particular object?  I guess that
    // COULD work: all the updates are just happening on the LWImage...
    // Well, not exactly: the parent could resize due to setting the image
    // size, tho that would be overriden by the un-drop of the image
    // and removing it as child -- oh, but the hierarchy event wouldn't get
    // tagged, so it would have be tied to any events that TOUCH that object,
    // which does not work anyway as the image could be user changed.  Well,
    // no, that would be detected by it coming from the unmarked thread.
    // So any event coming from the thread and "touching" this object could
    // be done, but that's just damn hairy...
    
    // Well, UndoManager is coalescing them for now, which seems to
    // work pretty well, but will probably break if user drops more
    // than one image and starts tweaking anyone but the first one before they load
    
    /*
    private void XloadImage(MapResource mr, UndoManager undoManager)
    {
        if (DEBUG.IMAGE || DEBUG.THREAD) out("loadImage");
        
        Image image = XgetImage(mr); // this will immediately block if host not responding
        // todo: okay, we can skip the rest of this code as getImage now uses the ImageIO
        // fetch

        if (image == null) {
            mImageError = true;
            return;
        }

        // don't bother to set mImage here: JVM's no longer do drawing of available bits

        if (DEBUG.IMAGE) out("prepareImage on " + image);
                
        if (mUndoMarkForThread != null) {
            Util.printStackTrace("already have undo key " + mUndoMarkForThread);
            mUndoMarkForThread = null;
        }

        if (java.awt.Toolkit.getDefaultToolkit().prepareImage(image, -1, -1, this)) {
            if (DEBUG.IMAGE || DEBUG.THREAD) out("ALREADY LOADED");
            mImage = image;
            setRawImageSize(image.getWidth(null), image.getHeight(null));
            // If the size hasn't already been set, set it.
            //if (getAbsoluteWidth() < 10 && getAbsoluteHeight() < 10)
                setSize(mImageWidth, mImageHeight);
            notify(LWKey.RepaintAsync);
        } else {
            if (DEBUG.IMAGE || DEBUG.THREAD) out("ImageObserver Thread kicked off");
            mDebugChar = sDebugChar;
            if (++sDebugChar > 'Z')
                sDebugChar = 'A';
            // save a key that marks the current location in the undo-queue,
            // to be applied to the subsequent thread that make calls
            // to imageUpdate, so that all further property changes eminating
            // from that thread are applied to the same location in the undo queue.

            if (undoManager == null)
                mUndoMarkForThread = UndoManager.getKeyForNextMark(this);
            else
                mUndoMarkForThread = undoManager.getKeyForNextMark();

        }
    }

    private Image XgetImage(MapResource mr)
    {
        URL url = mr.asURL();
        
        if (url == null)
            return null;

        Image image = null;
        
        try {
            // This allows reading of .tif & .bmp in addition to standard formats.
            // We'll eventually want to use this for everything, and cache
            // Resource objects themselves, but ImageIO caching doesn't
            // appear to be working right now, so we only use it if we have to.
            // .ico comes from a 3rd party library: aclibico.jar
            String s = mr.getSpec().toLowerCase();
            if (s.endsWith(".tif") || s.endsWith(".tiff") || s.endsWith(".bmp") || s.endsWith(".ico"))
                image = ImageIO.read(url);
        } catch (Throwable t) {
            if (DEBUG.Enabled) Util.printStackTrace(t);
            VUE.Log.info(url + ": " + t);
        }

        if (image != null)
            return image;

        // If the host isn't responding, Toolkit.getImage will block for a while.  It
        // will apparently ALWAYS eventually get an Image object, but if it failed, we
        // eventually get callback to imageUpdate (once prepareImage is called) with an
        // error code.  In any case, if you don't want to block, this has to be done in
        // a thread.
        
        String s = mr.getSpec();

            
        if (s.startsWith("file://")) {

            // TODO: SEE Util.java: WINDOWS URL'S DON'T WORK IF START WITH FILE://
            // (two slashes), MUST HAVE THREE!  move this code to MapResource; find
            // out if can even force a URL to have an extra slash in it!  Report
            // this as a java bug.

            // TODO: Our Cup>>Chevron unicode char example is failing
            // here on Windows (tho it works for windows openURL).
            // (The image load fails)
            // Try ensuring the URL is UTF-8 first.
            
            s = s.substring(7);
            if (DEBUG.IMAGE || DEBUG.THREAD) out("getImage " + s);
            image = java.awt.Toolkit.getDefaultToolkit().getImage(s);
        } else {
            if (DEBUG.IMAGE || DEBUG.THREAD) out("getImage");
            image = java.awt.Toolkit.getDefaultToolkit().getImage(url);
        }

        if (image == null) Util.printStackTrace("image is null");


        return image;
    }

    */
    
    /*    
    private static char sDebugChar = 'A';
    private char mDebugChar;
    public boolean XimageUpdate(Image img, int flags, int x, int y, int width, int height)
    {
        if ((DEBUG.IMAGE||DEBUG.THREAD) && (DEBUG.META || (flags & ImageObserver.SOMEBITS) == 0)) {
            if ((flags & ImageObserver.ALLBITS) != 0) System.err.println("");
            out("imageUpdate; flags=(" + flags + ") " + width + "x" + height);
        }
        
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
            notify(LWKey.RepaintAsync);
            return false;
        }
            

        if (DEBUG.IMAGE || DEBUG.THREAD) {
            
            if ((flags & ImageObserver.SOMEBITS) == 0) {
                //out("imageUpdate; flags=(" + flags + ") ");
                //+ thread + " 0x" + Integer.toHexString(thread.hashCode())
                //+ " " + sun.awt.AppContext.getAppContext()
                //Thread thread = Thread.currentThread();
                //System.out.println("\n" + getResource() + " (" + flags + ") "
                                   //+ thread + " 0x" + Integer.toHexString(thread.hashCode())
                                   //+ " " + sun.awt.AppContext.getAppContext());
            } else {
                // Print out a letter indicating the next batch of bits has come in
                System.err.print(mDebugChar);
            }
            
        }
        
        if ((flags & ImageObserver.WIDTH) != 0 && (flags & ImageObserver.HEIGHT) != 0) {
            //XsetRawImageSize(width, height);
            if (DEBUG.IMAGE || DEBUG.THREAD) out("imageUpdate; got size " + width + "x" + height);

            if (mUndoMarkForThread == null) {
                if (DEBUG.Enabled) out("imageUpdate: no undo key");
            }
            
            // For the events triggered by the setSize below, make sure they go
            // to the right point in the undo queue.
            UndoManager.attachCurrentThreadToMark(mUndoMarkForThread);
            
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
        

        if (false) {
            // the drawing of partial image results not working in current MacOSX JVM's!
            mImage = img;
            System.err.print("+");
            notify(LWKey.RepaintAsync);
        }
        
        if ((flags & ImageObserver.ALLBITS) != 0) {
            imageLoadSucceeded(img);
            return false;
        }

        // We're sill getting data: return true.
        // Unless we've been interrupted: should abort and return false.

        if (Thread.interrupted()) {
            if (DEBUG.Enabled || DEBUG.IMAGE || DEBUG.THREAD)
                System.err.println("\n" + getResource() + " *** INTERRUPTED *** " + Thread.currentThread());
            //System.err.println("\n" + getResource() + " *** INTERRUPTED *** (lowering priority) " + thread);
            // Changing priority of the Image Fetcher will prob slow down all subsequent loads
            //thread.setPriority(Thread.MIN_PRIORITY);
            
            // let it finish anyway for now, as we don't yet handle restarting this
            // operation if they Redo
            return true;

            // This is also not good enough: we're going to need to get an undo
            // key right at the start as we might get interrupted even
            // before the getImage returns..
            //return false;
            
        } else
            return true;
    }

    private void imageLoadSucceeded(Image image)
    {
        // Be sure to set the image before detaching from the thread,
        // or when the detach issues repaint events, we won't see the image.
        mImage = image;
        if (mUndoMarkForThread == null) {
            notify(LWKey.RepaintAsync);
        } else {
            UndoManager.detachCurrentThread(mUndoMarkForThread); // in case our ImageFetcher get's re-used
            // todo: oh, crap, what if this image fetch thread is attached
            // to another active image load?
            mUndoMarkForThread = null;
        }
        if (DEBUG.Enabled) {
            String[] tryProps = new String[] { "name", "title", "description", "comment" };
            for (int i = 0; i < tryProps.length; i++) {
                Object p = image.getProperty(tryProps[i], null);
                if (p != null && p != java.awt.Image.UndefinedProperty)
                    System.err.println("FOUND PROPERTY " + tryProps[i] + "=" + p);
            }
        }

        // Any problem using the Image Fetcher thread to do this?
        if (getResource() instanceof MapResource)
            ((MapResource)getResource()).scanForMetaData(LWImage.this, true);
        
    }
*/

    
    public static void main(String args[]) throws Exception {

        // GUI init required for fully loading all image codecs (tiff gets left behind otherwise)
        // Ah: the TIFF reader in Java 1.5 apparently comes from the UI library:
        // [Loaded com.sun.imageio.plugins.tiff.TIFFImageReader
        // from /System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/ui.jar]

        VUE.init(args);
        
        System.out.println(java.util.Arrays.asList(javax.imageio.ImageIO.getReaderFormatNames()));
        System.out.println(java.util.Arrays.asList(javax.imageio.ImageIO.getReaderMIMETypes()));

        String filename = args[0];
        java.io.File file = new java.io.File(filename);

        System.out.println("Reading " + file);

        System.out.println("ImageIO.read got: " + ImageIO.read(file));

        /*
          The below code requires the JAI libraries:
          // JAI (Java Advandced Imaging) libraries
          /System/Library/Java/Extensions/jai_core.jar
          /System/Library/Java/Extensions/jai_codec.jar

          Using this code below will also get us decoding .fpx images,
          tho we would need to convert it from the resulting RenderedImage / PlanarImage
          
        */

        /*
        try {
            // Use the ImageCodec APIs
            com.sun.media.jai.codec.SeekableStream stream = new com.sun.media.jai.codec.FileSeekableStream(filename);
            String[] names = com.sun.media.jai.codec.ImageCodec.getDecoderNames(stream);
            System.out.println("ImageCodec API's found decoders: " + java.util.Arrays.asList(names));
            com.sun.media.jai.codec.ImageDecoder dec =
                com.sun.media.jai.codec.ImageCodec.createImageDecoder(names[0], stream, null);
            java.awt.image.RenderedImage im = dec.decodeAsRenderedImage();
            System.out.println("ImageCodec API's got RenderedImage: " + im);
            Object image = javax.media.jai.PlanarImage.wrapRenderedImage(im);
            System.out.println("ImageCodec API's got PlanarImage: " + image);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // We're not magically getting any new codec's added to ImageIO after the above code
        // finds the .fpx codec...
        
        System.out.println(java.util.Arrays.asList(javax.imageio.ImageIO.getReaderFormatNames()));
        System.out.println(java.util.Arrays.asList(javax.imageio.ImageIO.getReaderMIMETypes()));

        */
        
    }
    

}
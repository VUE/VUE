/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

package tufts.vue;

import tufts.Util;

import java.awt.Image;
import java.awt.Point;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.*;
import java.awt.AlphaComposite;
import java.awt.image.ImageObserver;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;

import edu.tufts.vue.preferences.PreferencesManager;
import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.implementations.ImageSizePreference;

/**
 * Handle the presentation of an image resource, allowing resize.
 * Also provides special support for appear as a "node icon" -- a fixed
 * size image inside a node to represent it's resource.
 *
 * @version $Revision: 1.82 $ / $Date: 2007/11/19 06:20:27 $ / $Author: sfraize $
 */


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

public class LWImage extends
                         LWComponent
                         //LWContainer
                         //LWNode
    implements //LWSelection.ControlListener,
               Images.Listener,
               edu.tufts.vue.preferences.VuePrefListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWImage.class);

    public static final boolean SLIDE_LABELS = false;
    
    static int MaxRenderSize = PreferencesManager.getIntegerPrefValue(ImageSizePreference.getInstance());
    //private static VueIntegerPreference PrefImageSize = ImageSizePreference.getInstance(); // is failing for some reason
    //static int MaxRenderSize = PrefImageSize.getValue();
    //static int MaxRenderSize = 64;
    
    private final static int MinWidth = 32;
    private final static int MinHeight = 32;

    enum Status { UNLOADED, LOADING, LOADED, ERROR, EMPTY };

    // May want move most of this code to be done generically in URLResource, (e.g.,
    // bytes size, byte progress & status are somewhat generic for all content), and
    // either just auto-handle the special case of width/height for everything, or add
    // generic properties based on content type for the URLResource (we sort of already
    // have this with stuff that comes from MapDropTarget).  Tho we only need this for
    // content that has a needed/useful in-memory representation that's different than
    // it's on-disk content.  Currently, this only applies to images.  If we were to
    // support, say, dynamically generating icons (or even a document model) for HTML or
    // PDF content, the problem would then become a truly generic one.

    private static final double NO_ASPECT = -1;
    
    private Image mImage;
    private int mImageWidth = -1; // pixel width of raw image
    private int mImageHeight = -1; // pixel height of raw image
    private volatile Status mImageStatus = Status.UNLOADED;
    private double mImageAspect = NO_ASPECT;
    private long mDataSize = -1;
    private volatile long mDataSoFar = 0;
    private Object mUndoMarkForThread;
    
    //private double mImageScale = 1; // scale to the fixed size
    private double mRotation = 0;
    private Point2D.Float mOffset = new Point2D.Float(); // x & y always <= 0


    

    /** is this image currently serving as an icon for an LWNode? */
    private boolean isNodeIcon = false;
    
//     private transient LWIcon.Block mIconBlock =
//         new LWIcon.Block(this,
//                          20, 12,
//                          null,
//                          LWIcon.Block.VERTICAL);


    public LWImage() {
    	edu.tufts.vue.preferences.implementations.ImageSizePreference.getInstance().addVuePrefListener(this);
        setFillColor(null);
    }

    public LWImage(Resource r) {
        if (r == null || !r.isImage())
            throw new IllegalArgumentException("resource is not image content: " + r);
    	edu.tufts.vue.preferences.implementations.ImageSizePreference.getInstance().addVuePrefListener(this);
        setFillColor(null);
        setResource(r);
    }

    protected void out(String s) {
        Log.debug(s);
    }

    /** @return true -- an image is always it's own content */
    @Override
    public boolean hasContent() {
        return true;
    }
    
    
    // TODO: not so great to have every single LWImage instance be a listener...
    public void preferenceChanged(VuePrefEvent prefEvent)
    {        
        if (DEBUG.IMAGE) out("new pref value is " + ((Integer)ImageSizePreference.getInstance().getValue()).intValue());
        MaxRenderSize = ((Integer)prefEvent.getNewValue()).intValue();
        if (DEBUG.Enabled) System.out.println("New MaxRenderSize : " + MaxRenderSize + " in " + this);
        if (mImage != null && isNodeIcon)
            setMaxSizeDimension(MaxRenderSize);
    }


    @Override
    public LWImage duplicate(CopyContext cc)
    {
        // TODO: if had list of property keys in object, LWComponent
        // could handle all the duplicate code.
        LWImage i = (LWImage) super.duplicate(cc);
        i.mImage = mImage;
        i.mImageWidth = mImageWidth;
        i.mImageHeight = mImageHeight;
        i.mImageAspect = mImageAspect;
        i.isNodeIcon = isNodeIcon;
        i.mImageStatus = mImageStatus;
        i.setOffset(this.mOffset);
        i.setRotation(this.mRotation);
        return i;
    }

//     /** @return true */
//     @Override
//     public boolean isImageNode() {
//         return true;
//     }
    

    @Override
    public boolean isAutoSized() {
        if (getClass().isAssignableFrom(LWNode.class))
            return super.isAutoSized();
        else
            return false;
    }

    /** @return false: images are never transparent */
    @Override
    public boolean isTransparent() {
        if (false)// && this instanceof LWNode)
            return super.isTransparent();
        else
            return false;
    }
    
    /** @return false: images are never translucent */
    @Override
    public boolean isTranslucent() {
        if (false)// && this instanceof LWNode) {
            return super.isTranslucent();
        else {
            // Technically, if there are any transparent pixels in the image,
            // we'd want to return true.
            return false;
        }
    }

    @Override
    public int getFocalMargin() {
        return 0;
    }
    
    public boolean isNodeIcon() {
        return isNodeIcon;
    }

    /** This currently makes LWImages invisible to selection (they're locked in their parent node */
    //@Override
    protected LWComponent defaultPickImpl(PickContext pc) {
        if (getClass().isAssignableFrom(LWNode.class)) {
            return super.defaultPick(pc);
        } else {
            if (!hasFlag(Flag.SLIDE_STYLE) && isNodeIcon())
                return pc.pickDepth > 0 ? this : getParent();
            else
                return this;
        }
    }

    @Override
    public boolean supportsCopyOnDrag() {
        //return !hasFlag(Flag.SLIDE_STYLE);
        //return isNodeIcon();
        return !hasFlag(Flag.SLIDE_STYLE) && isNodeIcon();
    }
    
    
    /** @return true unless this is a node icon image */
    @Override
    public boolean supportsUserResize() {
        //return !isNodeIcon();
        return hasFlag(Flag.SLIDE_STYLE) || !isNodeIcon;
    }

    @Override
    public boolean supportsUserLabel() {
        return SLIDE_LABELS && hasFlag(Flag.SLIDE_STYLE);
    }

//     @Override
//     public Rectangle2D.Float getLayoutBounds() {
//         if (supportsUserLabel() && hasLabel()) {
//             final TextBox label = getLabelBox();
//             final Rectangle2D.Float r = super.getLocalBounds();
//             final float height = label.getBoxHeight() * 2;
//             r.y -= height;
//             r.height += height;
//             return r;
//         } else
//             return super.getLayoutBounds();
//     }
    

    /** this for backward compat with old save files to establish the image as a special "node" image */
    @Override
    public void XML_addNotify(Object context, String name, Object parent) {
        super.XML_addNotify(context, name, parent);
        if (parent instanceof LWNode)
            updateNodeIconStatus((LWNode)parent);
    }

    @Override
    protected void setParent(LWContainer parent) {
        super.setParent(parent);
        updateNodeIconStatus(parent);
    }
    
    /*
    protected void reparentNotify(LWContainer parent) {
        super.reparentNotify(parent);
        updateNodeIconStatus(parent);
    }
    */

    private void updateNodeIconStatus(LWContainer parent) {

        //tufts.Util.printStackTrace("updateNodeIconStatus, mImage=" + mImage + " parent=" + parent);
        if (DEBUG.IMAGE) out("updateNodeIconStatus, mImage=" + Util.tags(mImage) + " parent=" + parent);

        if (parent == null)
            return;

        if (parent instanceof LWNode
            && parent.getChild(0) == this
            && getResource() != null
            && getResource().equals(parent.getResource()))
        {
            // special case: if first child of a LWNode is an LWImage, treat it as an icon
            isNodeIcon = true;
            if (mImageWidth <= 0)
                return;
            if (!hasFlag(Flag.SLIDE_STYLE))
                setMaxSizeDimension(MaxRenderSize);
        } else {
            isNodeIcon = false;
            if (super.width == NEEDS_DEFAULT) {
                // use icon size also as default size for plain (non-icon) images
                setMaxSizeDimension(MaxRenderSize);
            }
        }
    }
    
    private void setMaxSizeDimension(final double max)
    {
        if (DEBUG.IMAGE) out("setMaxSizeDimension " + max);

        if (mImageWidth <= 0)
            return;

        final double width = mImageWidth;
        final double height = mImageHeight;

        if (DEBUG.IMAGE) out("setMaxSizeDimension curSize " + width + "x" + height);
        
        double newWidth, newHeight;

        if (width > height) {
            newWidth = max;
            newHeight = height * max / width;
            //newHeight = Math.round(height * max / width);
        } else {
            newHeight = max;
            newWidth = width * max / height;
            //newWidth = Math.round(width * max / height);
        }
        final float w = (float) newWidth;
        final float h = (float) newHeight;
        
        //if (DEBUG.IMAGE) out("setMaxSizeDimension newSize " + newWidth + "x" + newHeight);
        if (DEBUG.IMAGE) out("setMaxSizeDimension newSize " + w + "x" + h);

        setSize(w, h);
    }

    @Override
    protected TextBox getLabelBox()
    {
        if (super.labelBox == null) {
            initTextBoxLocation(super.getLabelBox());
            //layoutImpl("LWImage.labelBox-init");
        }
        return this.labelBox;
    }
    
    
    @Override
    public void initTextBoxLocation(TextBox textBox) {
        textBox.setBoxLocation(0, -textBox.getHeight());
    }

    @Override
    public void layoutImpl(Object triggerKey) {
        if (false&&getClass().isAssignableFrom(LWNode.class)) {
            super.layoutImpl(triggerKey);
        } else {
            //mIconBlock.layout();
//             if (super.labelBox != null) {
//                 out("layoutImpl " + triggerKey + "; SET BOX LOCATION AT Y " + getHeight() + " in " + this);
//                 super.labelBox.setBoxLocation(0, getHeight());
//             }
        }
    }
    
    // TODO: this wants to be on LWComponent, in case this is a
    // regular node containing an LWImage, we want the image to
    // update, as it doesn't get selected.  This depends on
    // how me might redo image support in maps tho, so
    // wait on that...
    
    @Override
    public void setSelected(boolean selected) {
        boolean wasSelected = this.selected;
        super.setSelected(selected);
        if (selected && !wasSelected && mImageStatus == Status.ERROR && hasResource()) {
            //Util.printStackTrace("ADD SELCTED IMAGE CLEANUP " + this);
            // don't know if this really needs to be a cleanup task,
            // or just an after-AWT task, but safer to do one of them:

            // TODO: this may be conflicting with our new image update code, and this
            // would much better be handled in the ActiveComponentHandler than via a
            // cleanup task (which should generally be a solution of last resort)

            // Note that this code does however also deal with a missing
            // network resource suddenly appearing, and then we can
            // load the image from that
            
            addCleanupTask(new Runnable() { public void run() {
                if (VUE.getSelection().only() == LWImage.this)
                    loadResourceImage(getResource(), null);
            }});
        }
    }
    
    @Override
    public void setResource(Resource r) {
        if (r == null) {
            // this will happen normally if when the creation of a new image is undone
            // (altho this is kind of pointless: may want to just deny this, tho we
            // see zombie events if we do that)
            if (DEBUG.Enabled) out("nulling resource");
            mImage = null;
            mImageWidth = -1;
            mImageHeight = -1;
            mImageStatus = Status.EMPTY;
            mImageAspect = NO_ASPECT;
            super.setResource(r);
        } else if (mXMLRestoreUnderway) {
            super.setResource(r);
        } else {
            setResourceAndLoad(r, null);
        }
    }

    // todo: find a better way to do this than passing in an undo manager, which is dead ugly
    public void setResourceAndLoad(Resource r, UndoManager undoManager) {
        super.setResource(r);
        if (r != null) {
            setLabel(MapDropTarget.makeNodeTitle(r));
            loadResourceImage(r, undoManager);
        }
    }

    public void reloadImage() {
        loadResourceImage(getResource(), null);
    }


    private void loadResourceImage(final Resource r, final UndoManager um)
    {
        int width = r.getProperty("image.width", 32);
        int height = r.getProperty("image.height", 32);

        // If we know a size before loading, this will get
        // us displaying that size.  If not, we'll set
        // us to a minimum size for display until we
        // know the real size.
        setImageSize(width, height);
        
        // save a key that marks the current location in the undo-queue,
        // to be applied to the subsequent thread that make calls
        // to imageUpdate, so that all further property changes eminating
        // from that thread are applied to the same location in the undo queue.
        
        synchronized (this) {
            // If image is not immediately availble, need to mark current
            // place in undo key for changes that happen due to the image
            // arriving.  We sync to be certian the key is set before
            // we can get any image callbacks.

            //Util.printStackTrace("GET IMAGE IN " + this);
            
            if (!Images.getImage(r, this))
                mUndoMarkForThread = UndoManager.getKeyForNextMark(this);
            else
                mUndoMarkForThread = null;
        }
    }

    

    public boolean isCropped() {
        return mOffset.x < 0 || mOffset.y < 0;
    }

    /** @see Images.Listener */
    public synchronized void gotImageSize(Object imageSrc, int width, int height, long byteSize)
    {
        if (DEBUG.IMAGE) out("gotImageSize " + width + "x" + height + " bytes=" + byteSize);
        mDataSize = byteSize;
        mImageStatus = Status.LOADING;
        setImageSize(width, height);

        if (mUndoMarkForThread == null) {
            if (DEBUG.IMAGE || DEBUG.UNDO || DEBUG.THREAD) out("gotImageSize: no undo key");
        }
            
        // For the events triggered by the setSize below, make sure they go
        // to the right point in the undo queue.
        // The mark was generated synchronously in the main model accessing thread (AWT EDT),
        // so it should point to a sane place in the undo queue to add modifications as
        // a result of callbacks.

        if (!javax.swing.SwingUtilities.isEventDispatchThread())
            UndoManager.attachCurrentThreadToMark(mUndoMarkForThread);
        
        // If we're interrupted before this happens, and this is the drop of a new image,
        // we'll see a zombie event complaint from this setSize which is safely ignorable.
        // todo: suspend events if our thread was interrupted
        // don't set size if we are cropped: we're probably reloading from a saved .vue
        //if (isRawImage && isCropped() == false) {
        //if (isCropped() == false) {
//         if (super.width == NEEDS_DEFAULT) {
//             // if this is a new image object, set it's size to the image size (natural size)
//             setSize(width, height);
//         }
        updateNodeIconStatus(getParent());
        layout();
        notify(LWKey.RepaintAsync);
    }
    
    
    private float mLastPct = 0;
    private int mLastPctEven = 0;
    private volatile String mStatusMsg;

    public void gotBytes(Object imageSrc, long bytesSoFar) {
        mDataSoFar = bytesSoFar;
        
        //out("BYTES SO FAR: " + bytesSoFar);
        // TODO: move this down to be recompute just before we actually draw
        if (mDataSize > 0 && mDataSoFar > 0) { // don't bother if we don't know the whole size yet...
            //final String statusMsg = Long.toString(mDataSoFar);
            final float pct = (float)mDataSoFar / (float)mDataSize;
            final int pctEven = Math.round(pct*100);
                //out("PCT: " + pct);
            if (pctEven > mLastPctEven) {
                // todo: if last update was more than, say 100ms ago (10fps) (statically: for ANY image),
                // also force an update
                mStatusMsg = Integer.toString(pctEven) + '%'; // todo: do in paint (no need do every time here)
                //out("notify on " + mStatusMsg);
                //mStatusMsg = String.format("%.1f%%", pct * 100);
                notify(LWKey.RepaintAsync);
            }
            mLastPct = pct;
            mLastPctEven = pctEven;
        }
        
    }
    
    /** @see Images.Listener */
    public synchronized void gotImage(Object imageSrc, Image image, int w, int h) {
        // Be sure to set the image before detaching from the thread,
        // or when the detach issues repaint events, we won't see the image.
        if (DEBUG.IMAGE) out("gotImage " + image);
        mImageStatus = Status.LOADED;
        setImageSize(w, h);
        //mImageWidth = w;
        //mImageHeight = h;
        mImage = image;

        mLastPct = mLastPctEven = 0;
        mStatusMsg = "(Load)";

        //if (isRawImage && isCropped() == false)
        //if (isCropped() == false)
        //    setSize(w, h);
        
        updateNodeIconStatus(getParent());
        
        if (mUndoMarkForThread == null) {
            //notify(LWKey.RepaintAsync);
        } else {
            // in case this thread get's re-used:
            UndoManager.detachCurrentThread(mUndoMarkForThread);
            mUndoMarkForThread = null;
        }

        notify(LWKey.RepaintAsync);
        

        // Any problem using the Image Fetcher thread to do this?
        //if (getResource() instanceof MapResource)
        //((MapResource)getResource()).scanForMetaData(LWImage.this, true);
    }

    /** @see Images.Listener */
    public synchronized void gotImageError(Object imageSrc, String msg) {
        // set image dimensions so if we resize w/out image it works
        mImageStatus = Status.ERROR;
        mImageWidth = (int) getWidth();
        mImageHeight = (int) getHeight();
        if (mImageWidth < 1) {
            mImageWidth = 128;
            mImageHeight = 128;
            setSize(128,128);
        }
        notify(LWKey.RepaintAsync);
        
    }

    public void setToNaturalSize() {
        setSize(mImageWidth, mImageHeight);
    }

//     public void X_setSize(float w, float h) {
//         super.setSize(w, h);
//         // Even if we don't have an image yet, we need to keep these set in case user attemps to resize the frame.
//         // They can still crop down if they like, but this prevents them from making it any bigger.
//         if (mImageWidth < 0)
//             mImageWidth = (int) getWidth();
//         if (mImageHeight < 0)
//             mImageHeight = (int) getHeight();
//     }

    /** record the actual pixel dimensions of the underlying raw image */
    void setImageSize(int w, int h)
    {
        mImageWidth = w;
        mImageHeight = h;
        mImageAspect = ((double)w) / ((double)h);
        // todo: may want to just always update the node status here -- covers most cases, plus better when the drop code calls this?
        if (DEBUG.IMAGE) out("setImageSize " + w + "x" + h + " aspect=" + mImageAspect);
        /*
			If below stops autoShapeToAspect from being called with default data,
			as well as cases where it'd be moot anyway.
		*/
        if (!(w == h && mImageAspect == 1.0))
        	autoShapeToAspect();
        
        //setAspect(aspect); // LWComponent too paternal for us right now
    }

    private void autoShapeToAspect() {
        if (mImageAspect > 0) {
     
                if (DEBUG.IMAGE) out("autoShapeToAspect  in: " + width + "," + height);
                
             
            	Size newSize = ConstrainToAspect(mImageAspect, width, height);
            	/*
            	 * Added this in response to VUE-948
            	 */
            	if ((DEBUG.Enabled || DEBUG.IMAGE) && newSize.width != width || newSize.height !=height)
            		System.out.println("autoShapeToAspect in:" + width + "," + height + " out newSize: " + newSize.width + "," + newSize.height);
            	//if (DEBUG.IMAGE) out("autoShapeToAspect out: " + newSize);
            	setSize(newSize.width, newSize.height);
        }
    }

    /**
     * Don't let us get bigger than the size of our image, or
     * smaller than MinWidth/MinHeight.
     */
    protected void userSetSize(float width, float height, MapMouseEvent e)
    {
        if (DEBUG.IMAGE) out("userSetSize " + Util.fmt(new Point2D.Float(width, height)) + "; e=" + e);

        if (e != null && e.isShiftDown()) {
            // Unconstrained aspect ration scaling
            super.userSetSize(width, height, e);
        } else if (mImageAspect > 0) {
            Size newSize = ConstrainToAspect(mImageAspect, width, height);
            setSize(newSize.width, newSize.height);
        } else
            setSize(width, height);


//         If (e != null && e.isShiftDown())
//             croppingSetSize(width, height);
//         else
//             scalingSetSize(width, height);
    }

    private void scalingSetSize(float width, float height)
    {
        /*
        if (DEBUG.IMAGE) out("scalingSetSize0 " + width + "x" + height);
        if (mImageWidth + mOffset.x < width)
            width = mImageWidth + mOffset.x;
        if (mImageHeight + mOffset.y < height)
            height = mImageHeight + mOffset.y;
        if (width < MinWidth)
            width = MinWidth;
        if (height < MinHeight)
            height = MinHeight;
        */

        if (DEBUG.IMAGE) out("scalingSetSize1 " + width + "x" + height);

        setSize(width, height);
    }

    /** this leaves the image exactly as it is, and just resizes the cropping region */
    private void croppingSetSize(float width, float height) {

        //if (DEBUG.IMAGE) out("croppingSetSize0 " + width + "x" + height);
        if (mImageWidth + mOffset.x < width)
            width = mImageWidth + mOffset.x;
        if (mImageHeight + mOffset.y < height)
            height = mImageHeight + mOffset.y;
        if (width < MinWidth)
            width = MinWidth;
        if (height < MinHeight)
            height = MinHeight;
        if (DEBUG.IMAGE) out("croppingSetSize1 " + width + "x" + height);

        final float oldAspect = super.mAspect;
        super.mAspect = 0; // don't pay attention to aspect when cropping
        super.setSize(width, height);
        super.mAspect = oldAspect;
    }
    
    

    /* @param r - requested LWImage frame in map coordinates */
    //private void constrainFrameToImage(Rectangle2D.Float r) {}

    /**
     * When user changes a frame on the image, if the location changes,
     * attempt to keep our content image in the same place (e.g., make
     * it look like we're just moving a the clip-region, if the LWImage
     * is smaller than the size of the underlying image).
     */
    public void X_RESIZE_CONTROL_HACK_userSetFrame(float x, float y, float w, float h, MapMouseEvent e)
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
        userSetSize(w, h, e);
        setLocation(x, y);
    }

    public static final Key KEY_Rotation = new Key("image.rotation", KeyType.STYLE) { // rotation in radians
            public void setValue(LWComponent c, Object val) { ((LWImage)c).setRotation(((Double)val).doubleValue()); }
            public Object getValue(LWComponent c) { return new Double(((LWImage)c).getRotation()); }
        };
    
    public void setRotation(double rad) {
        Object old = new Double(mRotation);
        this.mRotation = rad;
        notify(KEY_Rotation, old);
    }
    public double getRotation() {
        return mRotation;
    }

    public static final Key Key_ImageOffset = new Key("image.pan", KeyType.STYLE) {
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

    private Shape getClipShape() {
        //return super.drawnShape;
        // todo: cache & handle knowing if we need to update
        return new Rectangle2D.Float(0,0, getWidth(), getHeight());
    }

    private static final AlphaComposite HudTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
    //private static final Color IconBorderColor = new Color(255,255,255,64);
    //private static final Color IconBorderColor = new Color(0,0,0,64); // screwing up in composite drawing
    //private static final Color IconBorderColor = Color.gray;

    // FOR LWNode IMPL:
    /*
    protected void drawNode(DrawContext dc) {
        // do this for implmemented as a subclass of node
        drawImage(dc);
        super.drawNode(dc);
    }
    */

//     public void drawRaw(DrawContext dc) {
//         // (skip default composite cleanup for images)
//         drawImpl(dc);        
//     }

    // REMOVE FOR LWNode IMPL:
    protected void drawImpl(DrawContext dc)
    {
        drawWithoutShape(dc);
//         if (dc.g.getComposite() instanceof AlphaComposite) {
//             AlphaComposite a = (AlphaComposite) dc.g.getComposite();
//             System.err.println("ALPHA RULE: " + a.getRule() + " " + DrawContext.AlphaRuleNames[a.getRule()] + " " + this);
//         }

//         if (dc.focal == this)
//             super.drawChildren(dc);
    }

    
    public void drawWithoutShape(DrawContext dc)
    {
        // see comments on VUE-892 - this code does not seem to present the right behavior for search
        // please re-enable this code and reopen the bug if this code is needed- Dan H
        /*
        if (isNodeIcon()) {
            final LWComponent parent = getParent();
            if (parent != null && parent.isFiltered()) {
                // this is a hack because images are currently special cased as tied to their parent node
                return;
            }
        }*/

        final Shape shape = getClipShape();

        if (isSelected() && dc.isInteractive() && dc.focal != this) {
            dc.g.setColor(COLOR_HIGHLIGHT);
            dc.g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
            dc.g.draw(getZeroShape());
        }
        
        if (isNodeIcon && dc.focal != this) {

            drawImageBox(dc);
            
            // Forced border for node-icon's:
            if (mImage != null && !getParent().isTransparent()) {
                // this is somehow making itext PDF generation through a GC worse... (probably just a bad tickle)
                dc.g.setStroke(STROKE_TWO);
                //dc.g.setColor(IconBorderColor);
                dc.g.setColor(getParent().getRenderFillColor(dc).darker());
                dc.g.draw(shape);
            }
            
        } else {
            
            if (!super.isTransparent()) {
                final Color fill = getFillColor();
                if (fill == null || fill.getAlpha() == 0) {
                    Util.printStackTrace("isTransparent lied about fill " + fill);
                } else {
                    dc.g.setColor(fill);
                    dc.g.fill(shape);
                }
            }

            drawImageBox(dc);
            
            if (getStrokeWidth() > 0) {
                dc.g.setStroke(this.stroke);
                dc.g.setColor(getStrokeColor());
                dc.g.draw(shape);
            }

            if (supportsUserLabel() && hasLabel()) {
                initTextBoxLocation(getLabelBox());
                if (this.labelBox.getParent() == null) {
                    dc.g.translate(labelBox.getBoxX(), labelBox.getBoxY());
                    this.labelBox.draw(dc);
                }
            }
        }

        //super.drawImpl(dc); // need this for label
    }

    /** For interactive images as separate objects, which are currently disabled */
    /*
    private void drawInteractive(DrawContext dc)
    {
        drawPathwayDecorations(dc);
        drawSelectionDecorations(dc);
        
        dc.g.translate(getX(), getY());
        float _scale = getScale();

        if (_scale != 1f) dc.g.scale(_scale, _scale);
        
//         if (getStrokeWidth() > 0) {
//             dc.g.setStroke(new BasicStroke(getStrokeWidth() * 2));
//             dc.g.setColor(getStrokeColor());
//             dc.g.draw(new Rectangle2D.Float(0,0, getWidth(), getHeight()));
//         }

        drawImage(dc);

        if (getStrokeWidth() > 0) {
            dc.g.setStroke(this.stroke);
            dc.g.setColor(getStrokeColor());
            dc.g.draw(new Rectangle2D.Float(0,0, getWidth(), getHeight()));
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
*/

    private void drawImageBox(DrawContext dc)
    {
        if (mImage == null) 
            drawImageStatus(dc);
        else
            drawImage(dc);
        
        if (mImageStatus == Status.UNLOADED && getResource() != null) {

            // Doing this here (in a draw method) prevents images from loading until
            // they actually attempt to paint, which is handy when loading a map with
            // lots of large images: you can quickly see the map before the images need
            // to start loading.  Also handy if loading a large number of maps at once
            // -- images on undisplayed maps won't start to load until the first time
            // they're asked to paint.
        
            synchronized (this) {
                if (mImageStatus == Status.UNLOADED) {
                    mImageStatus = Status.LOADING;
                    if (DEBUG.IMAGE) out("invokeLater loadResourceImage " + getResource());
                    tufts.vue.gui.GUI.invokeAfterAWT(new Runnable() { public void run() {
                        loadResourceImage(getResource(), null);
                    }});
                }
            }
        }
        
    }


// This will cause images to start loading during parsing of persisted map files:
//     @Override
//     public void XML_completed() {
//         super.XML_completed();
//         if (mImageStatus == Status.UNLOADED) {
//             mImageStatus = Status.LOADING;
//             loadResourceImage(getResource(), null);
//         }
//     }
    

    

    private void drawImage(DrawContext dc)
    {
        final AffineTransform transform = new AffineTransform();
        
//    private static final AlphaComposite MatteTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
// Todo: when/if put this back in, see if we can handle it in the ImageTool so we don't need active tool in the DrawContext
//         if (isSelected() && dc.isInteractive() && dc.getActiveTool() instanceof ImageTool) {
//             dc.g.setComposite(MatteTransparency);
//             dc.g.drawImage(mImage, transform, null);
//             dc.g.setComposite(AlphaComposite.Src);
//         }

        if (false && isCropped()) {
            Shape oldClip = dc.g.getClip();
            dc.g.clip(getClipShape()); // this clip/restore clip may be screwing up our PDF export library
            dc.g.drawImage(mImage, transform, null);
            dc.g.setClip(oldClip);
        } else {
            //dc.g.clip(super.drawnShape);
            transform.scale(getWidth() / mImageWidth, getHeight() / mImageHeight);
            dc.g.drawImage(mImage, transform, null);
            //dc.g.drawImage(mImage, 0, 0, (int)super.width, (int)super.height, null);
            //dc.g.drawImage(mImage, 0, 0, mImageWidth, mImageHeight, null);
        }
   }

    private static final Color EmptyColorDark = new Color(0,0,0,128);
    private static final Color LoadedColorDark = new Color(0,0,0,160);
    private static final Color EmptyColorLight = new Color(128,128,128,64);
    private static final Color LoadedColorLight = new Color(128,128,128,128);

    private Color getEmptyColor(DrawContext dc) {
        Color fill = getFinalFillColor(dc);

        return Color.black.equals(fill) ? EmptyColorLight : EmptyColorDark;
        
//         final LWComponent parent = getParent();
//         Color pc;
//         if (parent != null && (pc=parent.getRenderFillColor(dc)) != null) {
//             return Color.black.equals(pc) ? EmptyColorLight : EmptyColorDark;
//         } else if (dc.getBackgroundFill() != null && dc.getBackgroundFill().equals(Color.black))
//             return EmptyColorLight;
//         else
//             return EmptyColorDark;
            
    }
    private Color getLoadedColor(DrawContext dc) {
        final LWComponent parent = getParent();
        Color pc;
        if (parent != null && (pc=parent.getRenderFillColor(dc)) != null) {
            return Color.black.equals(pc) ? LoadedColorLight : LoadedColorDark;
        } else
            return LoadedColorDark;
    }

    private static final int StatusHeight = 4;
    private static final Font StatusFont = new Font("Gill Sans", Font.PLAIN, 10);
    //private static final String LoadingText = "Loading...";
    //private static final float LoadingWidth = (float) tufts.vue.gui.GUI.stringWidth(StatusFont, LoadingText);

    private void drawImageStatus(DrawContext dc)
    {
        String status1 = "Loading...";
        String status2 = null;

        float pct = 0;
        
        synchronized (this) {
            if (mImageStatus == Status.ERROR) {
                status1 = "Missing";
                status2 = "Image";
            } else if (mImageStatus == Status.EMPTY) {
                status1 = "Empty Image";
                status2 = "(no resource)";
            } else if (mDataSoFar > 0 && mStatusMsg != null) {
                status1 = mStatusMsg;
                pct = mLastPct;
            }
        }
        
        //             if (mDataSoFar > 0 && mStatusMsg != null) {
        // //                 //final String statusMsg = Long.toString(mDataSoFar);
        // //                 final float pct = (float)mDataSoFar / (float)mDataSize;
        // //                 //out("PCT: " + pct);
        // //                 final String statusMsg = String.format("%.1f%%", pct * 100);
        //                 final float statusWidth = (float) tufts.vue.gui.GUI.stringWidth(StatusFont, mStatusMsg);
        //                 dc.g.drawString(mStatusMsg, (width-statusWidth)/2, (height+StatusHeight)/2);
        //                 //dc.g.drawString(""+mDataSize, 0, height-20);
        //                 //dc.g.drawString(""+mDataSoFar, 0, height);
        //             } else {
        //                 dc.g.drawString("Loading...", (width-LoadingWidth)/2, (height+StatusHeight)/2);
        //             }
        
        final int width = (int) getWidth();
        final int height = (int) getHeight();

        if (pct > 0) {
            final int split = (int) (width * pct);
            dc.g.setColor(getLoadedColor(dc));
            dc.g.fillRect(0, 0, split, height);
            dc.g.setColor(getEmptyColor(dc));
            dc.g.fillRect(split, 0, width - split, height);
        } else {
            dc.g.setColor(getEmptyColor(dc));
            dc.g.fillRect(0, 0, width, height);
        }

        dc.g.setColor(Color.lightGray);
        dc.g.setFont(StatusFont);
        
        if (status2 != null) {
            drawStatusLine(dc, status1, -5);
            drawStatusLine(dc, status2, +5);
        } else
            drawStatusLine(dc, status1, 0);
            
    }

    private void drawStatusLine(DrawContext dc, String text, int yoff) {
        final float textWidth = (float) tufts.vue.gui.GUI.stringWidth(StatusFont, text);
        dc.g.drawString(text, (getWidth()-textWidth)/2, (getHeight()+StatusHeight)/2 + yoff);
    }


    /*
    protected void drawImage(DrawContext dc)
    {    	    	
        if (mImage == null) {
            int w = (int) getWidth();
            int h = (int) getHeight();
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
        if (mRotation != 0 && mRotation != 360)
            transform.rotate(mRotation, getImageWidth() / 2, getImageHeight() / 2);
        
        if (isSelected() && dc.isInteractive() && dc.getActiveTool() instanceof ImageTool) {
            dc.g.setComposite(MatteTransparency);
            dc.g.drawImage(mImage, transform, null);
            dc.g.setComposite(AlphaComposite.Src);
        }

        if (isRawImage) {
            Shape oldClip = dc.g.getClip();
            dc.g.clip(getClipShape());
            dc.g.drawImage(mImage, transform, null);
            dc.g.setClip(oldClip);
        } else {
            dc.g.drawImage(mImage, 0, 0, mImageWidth, mImageHeight, null);
        }
   }
    */
    
//     @Override
//     public void mouseOver(MapMouseEvent e)
//     {
//         if (getClass().isAssignableFrom(LWNode.class))
//             super.mouseOver(e);
//         else
//             mIconBlock.checkAndHandleMouseOver(e);
//     }

    // Holy shit: if we somehow defined all this control-point stuff as a property editor,
    // could we then just attach the property editor to any component that
    // supported that property?  E.g. -- could help enormously with having
    // a merged LWNode and LWImage.  Not sure we REALLY want this tho.
    // Still need to figure out what to do with shape on the LWImage....

    
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

            if (mImageStatus == Status.ERROR) // don't let user play with offset if no image visible
                return;
            
            float deltaX = dragStart.x - e.getMapX();
            float deltaY = dragStart.y - e.getMapY();

            if (e.isShiftDown()) {
                dragCropImage(deltaX, deltaY);
            } else {
                dragMoveCropRegion(deltaX, deltaY);
            }
        } else
            throw new IllegalArgumentException(this + " no such control point");

    }

    private void dragCropImage(float deltaX, float deltaY)
    {
        Point2D.Float off = new Point2D.Float();
            
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
    }

    
    private void dragMoveCropRegion(float deltaX, float deltaY)
    {
        Point2D.Float off = new Point2D.Float();
        
        // drag underlying image around within frame
        off.x = offsetStart.x - deltaX;
        off.y = offsetStart.y - deltaY;
        constrainOffset(off);
        setOffset(off);
    }

    /** Keep LWImage filled with image bits (never display "area" outside of the image) */
    private void constrainOffset(Point2D.Float off)
    {
        if (off.x > 0)
            off.x = 0;
        if (off.y > 0)
            off.y = 0;
        if (off.x + getImageWidth() < getWidth())
            off.x = getWidth() - getImageWidth();
        if (off.y + getImageHeight() < getHeight())
            off.y = getHeight() - getImageHeight();
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
        if (getImageWidth() + off.x < getWidth()) {
            //System.out.println("out right");
            loc.x = (imageStart.x + getImageWidth()) - getWidth();
            off.x = getWidth() - getImageWidth();
        }
        if (getImageHeight() + off.y < getHeight()) {
            //System.out.println("out bot");
            loc.y = (imageStart.y + getImageHeight()) - getHeight();
            off.y = getHeight() - getImageHeight();
        }

    }

    /** interface ControlListener handler */
    public void controlPointDropped(int index, MapMouseEvent e)
    {
        if (DEBUG.IMAGE) out("control point " + index + " dropped");
    }


//     private LWSelection.Controller[] controlPoints = new LWSelection.Controller[1];
//     /** interface ControlListener */
//     public LWSelection.Controller[] X_getControlPoints() // DEIMPLEMENTED
//     {
//         controlPoints[0] = new LWSelection.Controller(getMapCenterX(), getMapCenterY());
//         controlPoints[0].setColor(null); // no fill (transparent)
//         return controlPoints;
//     }

    @Override
    public String paramString() {
        return super.paramString() + " " + mImageStatus + " raw=" + mImageWidth + "x" + mImageHeight + (isNodeIcon ? " <NodeIcon>" : "");
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
            //if (getWidth() < 10 && getHeight() < 10)
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
            mImageWidth = (int) getWidth();
            mImageHeight = (int) getHeight();
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
  
    /*
     * These 2 methods are used by the Preferences to set and check MaxRenderSize
     */
    public static int getMaxRenderSize()
    {
    	return MaxRenderSize;
    }
    public static void setMaxRenderSize(int size)
    {
    	//MaxRenderSize = size;
    }

}
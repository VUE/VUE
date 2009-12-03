
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

/**
 * Handle the presentation of an image resource, allowing resize.
 * Also provides special support for appear as a "node icon" -- a fixed
 * size image inside a node to represent it's resource.
 *
 * @version $Revision: 1.82 $ / $Date: 2007/11/19 06:20:27 $ / $Author: sfraize $
 */

public class LWImage extends LWComponent
    implements /*Images.Listener,*/ ImageRef.Listener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWImage.class);

    public static final boolean SLIDE_LABELS = false;
    
    public static final int DefaultMaxDimension = 128;
    
    private final static int MinWidth = 16;
    private final static int MinHeight = 16;

    // May want move most of the media-tracking code to be done generically in URLResource, (e.g.,
    // bytes size, byte progress & status are somewhat generic for all content), and either just
    // auto-handle the special case of width/height for everything, or add generic properties based
    // on content type for the URLResource (we sort of already have this with stuff that comes from
    // MapDropTarget).  Tho we only need this for content that has a needed/useful in-memory
    // representation that's different than it's on-disk content.  Currently, this only applies to
    // images.  If we were to support, say, dynamically generating icons (or even a document model)
    // for HTML or PDF content, the problem would then become a truly generic one.

    private Object mUndoMarkForInit;
    
    private final ImageRef mImageRef = new ImageRef(this);

    /** is this image currently serving as an icon for an LWNode? */
    private boolean isNodeIcon = false; // this is a messy way of supporting 2 modes of operation
    private volatile boolean isValidSize = false;
    
    private void initImage() {
        disableProperty(LWKey.FontSize); // prevent 0 font size warnings (font not used on images)
        takeFillColor(null);
    }
    
    public LWImage() {
        initImage();
    }

    public LWImage(Resource r) {
        initImage();
        if (r == null)
            throw new IllegalArgumentException("resource is not image content: " + r);
        if (!r.isImage())
            Log.warn("making LWImage: may not be image content: " + r);
        setResource(r);
    }

    /** @return true -- an image is always it's own content */
    @Override public boolean hasContent() {
        return true;
    }

    @Override public LWImage duplicate(CopyContext cc)
    {
        // note: do not duplicate the isNodeIcon bit -- leave unset
        
        // note: when the resource is copied over, the new LWImage will
        // init the new ImageRef.
        
        // note: if the ImageRef has a bad status (e.g., ERROR), that's
        // not being transferred to the new ImageRef.  The new ImageRef
        // should just try to access the image again and generate it's
        // own status.

        return super.duplicateTo(new LWImage(), cc);
    }

    /** @return false: images are never auto-sized */
    @Override public boolean isAutoSized() {
        return false;
    }
    /** @return false: images are never transparent */
    @Override public boolean isTransparent() {
        return false;
    }
    /** @return false: images are never translucent */
    @Override public boolean isTranslucent() {
        return false;
    }
    /** @return 0 */
    @Override public int getFocalMargin() {
        return 0;
    }
    /** @return true if this LWImage is being used as an icon for an LWNode */
    public boolean isNodeIcon() {
        return isNodeIcon;
    }

    /** This currently makes LWImages invisible to selection (they're locked in their parent node */
    @Override protected LWComponent defaultPickImpl(PickContext pc) {
        if (!hasFlag(Flag.SLIDE_STYLE) && isNodeIcon())
            return pc.pickDepth > 0 ? this : getParent();
        else
            return this;
    }

    @Override public boolean supportsCopyOnDrag() {
        return !hasFlag(Flag.SLIDE_STYLE) && isNodeIcon();
    }
    
    
    /** @return true unless this is a node icon image */
    @Override public boolean supportsUserResize() {
        return hasFlag(Flag.SLIDE_STYLE) || !isNodeIcon();
    }

    @Override public boolean supportsUserLabel() {
        return SLIDE_LABELS && hasFlag(Flag.SLIDE_STYLE);
    }

    /** this for backward compat with old save files to establish the image as a special "node" image */
    @Override public void XML_addNotify(Object context, String name, Object parent) {
        super.XML_addNotify(context, name, parent);
        if (parent instanceof LWNode)
            updateNodeIconStatus((LWNode)parent);
    }

    @Override protected void setParent(LWContainer parent) {
        super.setParent(parent);
        updateNodeIconStatus(parent);
    }
    
    private void updateNodeIconStatus(LWContainer parent) {

        //tufts.Util.printStackTrace("updateNodeIconStatus, mImage=" + mImage + " parent=" + parent);
        if (DEBUG.Enabled) out("updateNodeIconStatus " + mImageRef + "; parent=" + parent);

        if (parent == null)
            return;

        if (parent instanceof LWNode
            && parent.getChild(0) == this
            && getResource() != null
            && getResource().equals(parent.getResource()))
        {
            // special case: if first child of a LWNode is an LWImage, treat it as an icon
            isNodeIcon = true;
//             if (mImageWidth <= 0)
//                 return;
// //             if (!hasFlag(Flag.SLIDE_STYLE))
// //                 setMaxDimension(DefaultMaxDimension);
        } else {
            isNodeIcon = false;
            if (super.width == NEEDS_DEFAULT) {
                // use icon size also as default size for plain (non-icon) images
                setMaxDimension(DefaultMaxDimension);
            }
        }
    }

    /** used by Actions to size the image */
    public void setMaxDimension(final float max)
    {
//         if (mImageWidth <= 0) {
//             // this fixes the "gray link" which was being created when an image was
//             // dropped into a node on a slide -- it's size was never being set, leaving
//             // it infintesimally small / invisible, making it look like a link.  (see
//             // above condition on updateNodeIconStatus, where SLIDE_STYLE is checked).
//             setSize((float)max, (float)max);
//             return;
//         }

        // Todo: if the source image changes on disk, any icon needs to be re-generated

        //========================================================================================
        // This problem is what determined that we MUST save this size in the cache somehow (e.g.,
        // with the icon).  PROBLEM: if an image has an icon in cache, and we're creating a NEW
        // RESOURCE, such that resource properties image.width & image.height were never set, we
        // can't know the full pixel size.  Well HAVE to use the aspect (old image code didn't use
        // aspect here -- always used full pixel size).  The ONLY WAY around that one, w/out
        // forcing a load of a the whole image (which defeats the purpose of the image code
        // entirely) would be to store the full pixel size in the icon itself somehow.  That would
        // be a good idea anyway... how to best do it?  .PNG meta-data would be great, tho putting
        // it in the filename would be easier, tho if if the source image changed...  actually,
        // that could be one way we detect that the source image has changed, tho including the
        // modification date would be ideal -- now we REALLY need meta-data...  Oh, wait, we could
        // actually use the modification date of the icon file -- just make sure it's AFTER the
        // on-disk file.
        // ========================================================================================

        final int[] rawPixels = getFullPixelSize();

        if (rawPixels == ImageRep.ZERO_SIZE) {
            Log.warn("setMaxDimension: image rep has unset size: " + ref());
            putSize(DefaultMaxDimension, DefaultMaxDimension, false);
        } else {

            Size newSize = Images.fitInto(max, rawPixels);
            
            if (DEBUG.Enabled) out("setMaxDimension " + max + " -> " + newSize);
            
            putSize(newSize.width, newSize.height, true);
        
        }
        
        
//         final double width = rawPixels[0];
//         final double height = rawPixels[1];

//         if (DEBUG.IMAGE) out("setMaxDimension curSize " + width + "x" + height);
        
//         double newWidth, newHeight;

//         if (width > height) {
//             newWidth = max;
//             newHeight = height * max / width;
//             //newHeight = Math.round(height * max / width);
//         } else {
//             newHeight = max;
//             newWidth = width * max / height;
//             //newWidth = Math.round(width * max / height);
//         }
//         final float w = (float) newWidth;
//         final float h = (float) newHeight;
        
//         //if (DEBUG.IMAGE) out("setMaxDimension newSize " + newWidth + "x" + newHeight);
//         if (DEBUG.IMAGE)  out("setMaxDimension newSize " + w + "x" + h);

//        setSize(w, h);
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

//     public Status getStatus() {
//         return mImageStatus;
//     }

    public boolean hasImageError() {
        return ref().hasError();
    }
    
    // TODO: this wants to be on LWComponent or LWNode, in case this is a regular node containing an LWImage,
    // we want the image to update, as it doesn't get selected.  Even better is actually to handle
    // this in the global selection listener or ActiveInstance of LWComponent listener.
    
    @Override
    public void setSelected(boolean selected) {
        boolean wasSelected = isSelected();
        super.setSelected(selected);
        if (selected && !wasSelected && hasImageError() && hasResource()) {
            //Util.printStackTrace("ADD SELCTED IMAGE CLEANUP " + this);
            // don't know if this really needs to be a cleanup task,
            // or just an after-AWT task, but safer to do one of them:

            // TODO: this may be conflicting with our new image update code, and this
            // would much better be handled in the ActiveComponentHandler than via a
            // cleanup task (which should generally be a solution of last resort)
            // See if we can handle this in VUE.checkForAndHandleResourceUpdate

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
        setImageResource(r, false);
    }
    
    public void setNodeIconResource(Resource r) {
        setImageResource(r, true);
    }
    
    private void setImageResource(Resource r, boolean isNodeIconSync) {
        if (DEBUG.IMAGE) Log.debug("setImageResource " + r  + "; isNodeIconSync=" + isNodeIconSync + "; restore=" + mXMLRestoreUnderway);

//         if (r != null && !mXMLRestoreUnderway) // todo: re-init / dump ImageRef
//             ref().setImageSource(r);
// can't init this here -- could be during resource, and Resource itself may be bad -- not having finalized it's init

        if (r == null) {
            // this will happen normally if when the creation of a new image is undone
            // (altho this is kind of pointless: may want to just deny this, tho we
            // see zombie events if we do that)
            if (DEBUG.Enabled) out("nulling resource");
//             mImage = null;
//             mImageWidth = -1;
//             mImageHeight = -1;
//             mImageStatus = Status.EMPTY;
//             mImageAspect = NO_ASPECT;
            super.setResource(r);
        } else if (mXMLRestoreUnderway) {
            super.setResource(r);
        } else if (isNodeIcon() && !isNodeIconSync) {
            // we should be called back again with isNodeIconSync == true
            getParent().setResource(r);
        } else {

//             mImage = null;
//             mImageWidth = -1;
//             mImageHeight = -1;
//             mImageStatus = Status.UNLOADED;
//             mImageAspect = NO_ASPECT;
            
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
        ref().reload();
    }

    private void loadResourceImage(final Resource r, final UndoManager _ignored_undo_manager)
    {
        final int suggestWidth = r.getProperty("image.width", -1);
        final int suggestHeight = r.getProperty("image.height", -1);

        // If we know a size before loading, this will get us displaying that size.  If
        // not, we'll set us to a minimum size for display until we know the real size.

        if (suggestWidth > 0 && suggestHeight > 0 && (ref().fullPixelSize() == ImageRef.ZERO_SIZE))
            putSize(suggestWidth, suggestHeight, false);
        //setImageSize(suggestWidth, suggestHeight);
        
        // save a key that marks the current location in the undo-queue,
        // to be applied to the subsequent thread that make callbacks
        // with image data, so that all further property changes eminating
        // from that thread are applied to the same location in the undo queue.
        
        synchronized (this) {
            // If the image is not immediately availble, need to mark current
            // place in undo key for changes that happen due to the image
            // arriving.  We sync to be certian the key is set before
            // we can get any image callbacks.

            // TODO: can we get rid of this for most cases?  E.g., only for newly dropped nodes?
            ref().setImageSource(r);
            if (ref().available())
                mUndoMarkForInit = null;
            else
                mUndoMarkForInit = UndoManager.getKeyForNextMark(this);
        }
    }
    
    /** @see ImageRef.Listener */
    public /*synchronized*/ void imageRefChanged(Object cause) {
        // note: this won't be in the AWT thread, so for full thread safety any
        // size changes should happen on AWT, tho that may conflict with our undo-tracking
        // for threaded inits?  Tho as long as we make use of the mark we obtain,
        // that shouldn't matter, right?
        //
        // Oh -- that happens via UndoManager.attachCurrentThreadToMark(mUndoMarkForThread),
        // which if we do in AWT will then attach all of AWT to that undo mark?  That
        // whole mechanism probably really needs to passed all the way through to the
        // notify so the undo manager can detect that there?
        
        if (!isValidSize) {
            autoShape();
            //repaintOnResize(); // may have shrunk -- need to repaint everything
            // todo: really, repaint the intersection of old size & new size
            repaintPixels();
        } else {
            repaintPixels();
        }
    }

//     private class Repainter implements Runnable {
//             public void run() {
//                 //if (alive()) LWImage.this.notify(LWKey.RepaintAsync);
//                 //if (alive()) LWImage.this.notify(ImageRepaint);
//                 if (alive()) LWImage.this.notify(LWKey.RepaintRegion);
//             }
//         }
//     private final Runnable mRepainter = new Repainter();

    private void repaintPixels() {
        //Log.debug("ISSUING PIXEL REPAINT " + getLabel());

        //tufts.vue.gui.GUI.invokeOnEDT(mRepainter);

        // tho we're not going to be on the AWT thread, the result of a Repaint/RepaintRegion is
        // ultimately going to be to safely stick a repaint request into the AWT queue, so we
        // should be okay.  We will have to pull the bounds of this LWImage non-synchronized
        // tho.

        if (alive()) notify(LWKey.RepaintRegion);
        //if (alive()) notify(LWKey.Repaint); // for DEBUG.BOXES debug visibility in other images
    }
    private void repaintOnResize() {
        if (alive()) notify(LWKey.Repaint);
    }

//     @Override
//     protected boolean intersectsImpl(Rectangle2D mapRect) {
//         boolean i = super.intersectsImpl(mapRect);
//         Log.info("INTERSECTS " + Util.tags(i?"YES":" NO") + " " + Util.tags(mapRect) + " " + getLabel());
//         return i;
//     }
//     @Override
//     public boolean requiresPaint(DrawContext dc) {
//         boolean i = super.requiresPaint(dc);
//         Log.info("REQRSPAINT " + Util.tags(i?"YES":" NO") + getLabel());
//         return i;
//     }

    @Override public void setToNaturalSize() {
        setSize(getFullPixelSize());
    }

    private int[] getFullPixelSize() {
        int[] size = ref().fullPixelSize();
        
        if (size == ImageRep.ZERO_SIZE && hasResource()) {

            // It's possible to have only the icon loaded, and not the full representation, and
            // thus not immediately know the size of the full representation.

            // todo: note that even if one ImageRep loads it's full size, others that haven't
            // reqested it yet will still end up using this method.  As soon as one rep has a real
            // size, all should get their real size.  (also another argument for making them
            // singleton)
            
            final Resource r = getResource();
            final int w = r.getProperty(Resource.IMAGE_WIDTH, -1);
            final int h = r.getProperty(Resource.IMAGE_HEIGHT, -1);
            if (w > 0 && h > 0) {
                size = new int[2];
                size[0] = w;
                size[1] = h;
                // todo: would be better to NOT let this be used as a "valid" size -- if it later
                // disagrees with something found on disk (the image has changed on disk since this
                // resource was created), the new, real image size should take priority.
            }
        }
        return size;
    }
                                      

    private void setSize(int[] size) {
        if (size == ImageRep.ZERO_SIZE) {
            Log.warn("skipping setSize of ZERO_SIZE; " + this);
        } else {
            putSize(size[0], size[1], true);
        }
    }
    
    public void suggestSize(int w, int h) 
    {
        if (DEBUG.Enabled) out("suggestSize " + w + "x" + h);
        putSize(w,h, false);
    }

    private void putSize(float w, float h, boolean validated) {
        setSize(w, h);
        if (validated) {
            if (!isValidSize) {
                if (DEBUG.Enabled) out("set first VALID size " + w + "x" + h);
                isValidSize = true;
            }
        } else {
            if (isValidSize) {
                Log.error("DE-VALIDATING SIZE; " + this, new Throwable("HERE"));
                isValidSize = false;
            }
        }
    }

    @Override public void setSize(float w, float h) {
        if (DEBUG.Enabled) out("setSize " + w + "x" + h);
        super.setSize(w, h);
    }
    
    private float aspect() {
        return ref().aspect();
    }
    
    private void autoShape() {
        autoShapeToAspect();
    }

    @Override protected void out(String s) {
        Log.debug(String.format("%s: %s", paramString(), s));
    }
    
    @Override
    public String paramString() {
        return super.paramString() + (isNodeIcon ? " <NodeIcon> " : " ");// + mImageRef;
        //return super.paramString() + " " + mImageStatus + " raw=" + mImageWidth + "x" + mImageHeight + (isNodeIcon ? " <NodeIcon>" : "");
    }


    
    private void autoShapeToAspect() {

        final float aspect = aspect();
        
        if (aspect <= 0) {
            Log.warn("bad aspect in autoShapeToAspect: " + aspect);
            return;
        }

        if (this.width == NEEDS_DEFAULT || this.height == NEEDS_DEFAULT) {
            //Log.error("cannot auto-shape without request size: " + this, new Throwable("HERE"));
            if (DEBUG.WORK||DEBUG.IMAGE) out("autoshaping from scratch to " + DefaultMaxDimension);
            setMaxDimension(DefaultMaxDimension);
            return;
        }
     
        if (DEBUG.Enabled) out("autoShapeToAspect in: " + width + "," + height);
             
        final Size newSize = ConstrainToAspect(aspect, this.width, this.height);

        final float dw = this.width - newSize.width;
        final float dh = this.height - newSize.height;
            
        /*
         * Added this in response to VUE-948
         */
        if ((DEBUG.WORK || DEBUG.IMAGE) && (newSize.width != width || newSize.height != height))
            out(String.format("autoShapeToAspect: a=%.2f dw=%g dh=%g; %.1fx%.1f -> %s",
                              aspect,
                              dw, dh,
                              width, height,
                              newSize));
                                  
        //out("autoShapeToAspect: a=" + mImageAspect + "; dw=" + dw + ", dh=" + dh + "; " + width + "," + height + " -> adj " + newSize);
        //out("autoShapeToAspect: " + width + "," + height + " -> newSize: " + newSize.width + "," + newSize.height);
            
        if (Math.abs(dw) > 1 || Math.abs(dh) > 1) {
            // above check helps reduce needless tweaks, which make things messy during map loading
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
        } else if (aspect() > 0) {
            Size newSize = ConstrainToAspect(aspect(), width, height);
            putSize(newSize.width, newSize.height, true);
        } else
            putSize(width, height, true);

//         If (e != null && e.isShiftDown())
//             croppingSetSize(width, height);
//         else
//             scalingSetSize(width, height);
    }

    public static final Key KEY_Rotation = new Key("image.rotation", KeyType.STYLE) { // rotation in radians
            public void setValue(LWComponent c, Object val) { ((LWImage)c).setRotation(((Double)val).doubleValue()); }
            public Object getValue(LWComponent c) { return new Double(((LWImage)c).getRotation()); }
        };
    
    public void setRotation(double rad) {
//         Object old = new Double(mRotation);
//         this.mRotation = rad;
//         notify(KEY_Rotation, old);
    }
    public double getRotation() {
        //return mRotation;
        return 0;
    }

//     public static final Key Key_ImageOffset = new Key("image.pan", KeyType.STYLE) {
//             public void setValue(LWComponent c, Object val) { ((LWImage)c).setOffset((Point2D)val); }
//             public Object getValue(LWComponent c) { return ((LWImage)c).getOffset(); }
//         };

    public void setOffset(Point2D p) {
//         if (p.getX() == mOffset.x && p.getY() == mOffset.y)
//             return;
//         Object oldValue = new Point2D.Float(mOffset.x, mOffset.y);
//         if (DEBUG.IMAGE) out("LWImage setOffset " + VueUtil.out(p));
//         this.mOffset.setLocation(p.getX(), p.getY());
//         notify(Key_ImageOffset, oldValue);
    }

    public Point2D getOffset() {
        return null;
//         return new Point2D.Float(mOffset.x, mOffset.y);
    }
    
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

    private ImageRef ref() {
        return mImageRef;
    }

    @Override protected void preCacheContent() {
        ref().preLoadFullRep();
    }    
    
    private void drawImage(DrawContext dc)
    {
        if (ref().isBlank())
            ref().setImageSource(getResource());
        
        ref().drawInto(dc, getWidth(), getHeight());
    }
    

    @Override
    protected void drawImpl(DrawContext dc)
    {
        drawImage(dc);
    }
    
    private boolean isIndicatedIn(DrawContext dc) {

        if (dc.hasIndicated()) {
            final LWComponent indication = dc.getIndicated();

            return indication == this
                || (isNodeIcon() && getParent() == indication);
        } else {
            return false;
        }
    }

    
//     public void drawWithoutShape(DrawContext dc)
//     {
//         // see comments on VUE-892 - this code does not seem to present the right behavior for search
//         // please re-enable this code and reopen the bug if this code is needed- Dan H
//         /*
//         if (isNodeIcon()) {
//             final LWComponent parent = getParent();
//             if (parent != null && parent.isFiltered()) {
//                 // this is a hack because images are currently special cased as tied to their parent node
//                 return;
//             }
//         }*/

//         final Shape shape = getZeroShape();

//         if (isSelected() && dc.isInteractive() && dc.focal != this) {
//             dc.g.setColor(COLOR_HIGHLIGHT);
//             dc.g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
//             dc.g.draw(shape);
//         }

//         final boolean indicated = isIndicatedIn(dc);

//         if (indicated && dc.focal != this) {
//             Color c = getParent().getRenderFillColor(dc);
//             if (VueUtil.isTranslucent(c))
//                 c = Color.gray;
//             dc.g.setColor(c);
//             dc.g.fill(shape);
//         }
        
//         if (isNodeIcon && dc.focal != this) {

//             if (!indicated && DefaultMaxDimension > 0)
//                 drawImageBox(dc);
            
//             // Forced border for node-icon's:
//             if ((mImage != null ||*/ indicated) && !getParent().isTransparent() && DefaultMaxDimension > 0) {
//                 // this is somehow making itext PDF generation through a GC worse... (probably just a bad tickle)
//                 dc.g.setStroke(STROKE_TWO);
//                 //dc.g.setColor(IconBorderColor);
//                 dc.g.setColor(getParent().getRenderFillColor(dc).darker());
//                 dc.g.draw(shape);
//             }
            
//         } else if (!indicated) {
            
//             if (!super.isTransparent()) {
//                 final Color fill = getFillColor();
//                 if (fill == null || fill.getAlpha() == 0) {
//                     Util.printStackTrace("isTransparent lied about fill " + fill);
//                 } else {
//                     dc.g.setColor(fill);
//                     dc.g.fill(shape);
//                 }
//             }

        
//             	drawImageBox(dc);
            
//             if (getStrokeWidth() > 0) {
//                 dc.g.setStroke(this.stroke);
//                 dc.g.setColor(getStrokeColor());
//                 dc.g.draw(shape);
//             }

//             if (supportsUserLabel() && hasLabel()) {
//                 initTextBoxLocation(getLabelBox());
//                 if (this.labelBox.getParent() == null) {
//                     dc.g.translate(labelBox.getBoxX(), labelBox.getBoxY());
//                     this.labelBox.draw(dc);
//                 }
//             }
//         }

//         //super.drawImpl(dc); // need this for label
//     }

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

//     private void drawImageBox(DrawContext dc)
//     {
//         if (mImage == null && !dc.isIndicated(this)) 
//             drawImageStatus(dc);
//         else
//             drawImage(dc);
        
//         if (mImageStatus == Status.UNLOADED && getResource() != null) {

//             // Doing this here (in a draw method) prevents images from loading until
//             // they actually attempt to paint, which is handy when loading a map with
//             // lots of large images: you can quickly see the map before the images need
//             // to start loading.  Also handy if loading a large number of maps at once
//             // -- images on undisplayed maps won't start to load until the first time
//             // they're asked to paint.
        
//             synchronized (this) {
//                 if (mImageStatus == Status.UNLOADED) {
//                     mImageStatus = Status.LOADING;
                    
//                     // TODO: running this on AWT can cause problems during map loading,
//                     // as events on an image load thread we want to be ignoring in terms
//                     // of map modifications will be taken seriously when appearing on
//                     // the AWT thread.  We need a better system for ignoring image
//                     // events during map loading (events that don't want to be incrementing
//                     // the map modification count).  We should at least include new code
//                     // to ignore all events prior to the first user event, which will at
//                     // least catch everything that happens before the very first undo mark.
                    
//                     if (DEBUG.IMAGE) out("invokeLater loadResourceImage " + getResource());
//                     tufts.vue.gui.GUI.invokeAfterAWT(new Runnable() { public void run() {
//                         loadResourceImage(getResource(), null);
//                     }});
//                 }
//             }
//         }
        
//     }


    @Override
    public void XML_completed(Object context) {
        super.XML_completed(context);

        if (super.width < MinWidth || super.height < MinHeight) {
            Log.info(String.format("bad size: adjusting to minimum %dx%d: %s", MinWidth, MinHeight, this));
            super.width = MinWidth;
            super.height = MinHeight;
        }
        
//         // This will cause images to start loading during parsing of persisted map files:
//         if (mImageStatus == Status.UNLOADED) {
//             mImageStatus = Status.LOADING;
//             loadResourceImage(getResource(), null);
//         }
    }
    
//     private void OLDdrawImage(DrawContext dc)
//     {
//         final AffineTransform transform = new AffineTransform();
        
// //    private static final AlphaComposite MatteTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
// // Todo: when/if put this back in, see if we can handle it in the ImageTool so we don't need active tool in the DrawContext
// //         if (isSelected() && dc.isInteractive() && dc.getActiveTool() instanceof ImageTool) {
// //             dc.g.setComposite(MatteTransparency);
// //             dc.g.drawImage(mImage, transform, null);
// //             dc.g.setComposite(AlphaComposite.Src);
// //         }

//         if (false && isCropped()) {
//             Shape oldClip = dc.g.getClip();
//             dc.g.clip(getClipShape()); // this clip/restore clip may be screwing up our PDF export library
//             dc.g.drawImage(mImage, transform, null);
//             dc.g.setClip(oldClip);
//         } else {
//             //dc.g.clip(super.drawnShape);
//             transform.scale(getWidth() / mImageWidth, getHeight() / mImageHeight);
//             dc.g.drawImage(mImage, transform, null);
//             //dc.g.drawImage(mImage, 0, 0, (int)super.width, (int)super.height, null);
//             //dc.g.drawImage(mImage, 0, 0, mImageWidth, mImageHeight, null);
//         }
//    }

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

    // TODO[old]: update bad (error) images if preview gets good data Better: handle this via
    //       listening to the resource for updates (the LWCopmonent can do this), and if
    //       it's a CONTENT_CHANGED update (v.s., say, a META_DATA_CHANGED), then we can
    //       refetch the content.  Actually, would still be nice if this happened just by
    //       selecting the object, in case the resource previewer didn't happen to be open.

    
    

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

        System.out.println("ImageIO.read got: " + javax.imageio.ImageIO.read(file));

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
package tufts.vue;

import tufts.Util;

import java.lang.ref.*;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import tufts.vue.Images.Handle;

/**
 * A representation of an image that can allow itself to be garbage collected, and reconstituted
 * later if needed.
 */

// This would be better as part of an Images or Media package.  The package-private methods
// are intended for use by ImageRef.
public abstract class ImageRep implements /*ImageRef.Rep,*/ Images.Listener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ImageRep.class);

    static final int[] ZERO_SIZE = new int[2];

    private static interface Ref<T> {
        T get();
        boolean isLoader();
    }
    
    //===================================================================================================

    private static final Ref IMG_UNLOADED = new NullRef("UNLOADED");
    private static final Ref IMG_CACHING = new NullRef("CACHING");
    private static final Ref IMG_LOADING = new LoadingRef("LOADING");
    private static final Ref IMG_LOADING_AFTER_ERROR = new LoadingRef("LOADING-POST-ERROR");
    private static final Ref IMG_ERROR = new NullRef("ERROR");
    private static final Ref IMG_ERROR_MEMORY = new NullRef("LOW_MEMORY");

    //===================================================================================================
    
    private volatile Ref<Image> _handle = IMG_UNLOADED; // note: could make use of AtomicReference v.s. volatile

    /** pixel width and height of this representation _size[0] is width, _size[1] is height */
    private int[] _size = ZERO_SIZE;

    private final ImageRef _ref;
    
    private final ImageSource _data; // note: could pull from _ref when needed, at least for original source

    //===================================================================================================
    
    static ImageRep create(ImageRef ref, ImageSource src, boolean allowGC) {
        if (allowGC)
            return new ImageRep.Soft(ref, src);
        else
            return new ImageRep.Hard(ref, src);
    }
    static ImageRep create(ImageRef ref, ImageSource src) {
        return create(ref, src, true);
    }
    
    private static final class Hard extends ImageRep {
        Hard(ImageRef ref, ImageSource src) {
            super(ref, src);
        }
        Ref newRef(Image o) {
            if (o == null)
                Log.warn("HardRef to null " + this, new Throwable("HERE"));
            return new HardRef(o);
        }
    }
    private static final class Soft extends ImageRep {
        Soft(ImageRef ref, ImageSource src) {
            super(ref, src);
        }
        Ref newRef(Image o) { return new SoftRef(o); }
    }

    public static final ImageRep UNAVAILABLE = new ImageRep() {
            @Override public boolean available() { return false; }
            @Override protected Image image() { return null; }
            @Override protected boolean reconstitute() { error(); return false; }
            @Override protected Handle reconstitute(Object when) { error(); return null; }
            @Override protected void cacheData(Images.Handle i, Object debug) { error(); }
            @Override void renderRep(Graphics2D g, float width, float height) {
                fillRect(g, width, height, DEBUG.Enabled ? Color.orange : LoadingColor);
            }
            @Override public String toString() { return "REP.UNAVAILABLE"; }
            @Override Ref newRef(Image o) { return (Ref) error(); }
            private Object error() { throw new Error("constant-class"); }
        };
    
    //===================================================================================================

    private ImageRep(ImageRef ref, ImageSource src) {
        if (src == null)
            throw new Error("no ImageSource constructing rep for " + ref);
        _data = src;
        _ref = ref;
    }
    
//     private ImageRep(ImageRef ref, Image image, ImageSource src) {
//         if (src == null)
//             throw new Error("no ImageSource constructing rep for " + ref);
//         _data = src;
//         _ref = ref;
//         if (image != null) {
//             cacheData(image, false);
//         }
//     }
    
    private ImageRep() { // for UNAVAILABLE
        _data = null;
        _ref = null;
    }

    abstract Ref<Image> newRef(Image o);

    private synchronized void setSize(final int w, final int h) {
        if (w <= 0 || h <= 0) {
            Log.warn("bad size " + w + "x" + h);
            _size = ZERO_SIZE;
        } else {
            if (_size == ZERO_SIZE || w != _size[0] || h != _size[1]) {
                final int[] size = new int[2];
                size[0] = w;
                size[1] = h;
                takeSize(size);
            }
        }
    }
    
    synchronized void takeSize(int[] size) {
        _size = size;
    }

    // todo: could change _size to volatile (and fetch into a final in methods that make 2 refs to it)
    
    // can only guarantee width/height coherency by fetching them together in a singal sync
    synchronized int[] size() {
        return _size; // cloning would be even safer, tho this is not for general consumption
    }
    synchronized int area() {
        return _size[0] * _size[1];
    }
    synchronized float aspect() {
        return (float) _size[0] / (float) _size[1];
    }
    
    public boolean available() {
        return get(_handle) != null;
    }

    public boolean loading() {
        return _handle.isLoader();
    }
    
    public boolean hasError() {
        return _handle == IMG_ERROR || _handle == IMG_ERROR_MEMORY;
    }

    Ref handle() {
        return _handle;
    }

    // Note: old DEADLOCK if reconstitute was synchronized:
    // AWT: ImageRep locks in reconstitute, calling getImage, which is trigger a sync callback
    // through synchronized CachingRelayer (addListener) ImageProcessor: CachingRelayer gotImage
    // locks CachingRelayer, which after calling gotImage to notifyRepHasArrived attempts to pull
    // ImageRep aspect, which is locked above on AWT on reconstitute.  Doesn't appear to
    // be a problem now, but take heed.

    private static final Object LOAD_NORMAL = "painting";
    private static final Object LOAD_IMMEDIATE = "immediate";
    private static final Object LOAD_CACHE = "cache";

    /** @return true if all data was immediately available, false if we're waiting for more info from a callback */
    protected boolean reconstitute() {
        return reconstitute(LOAD_NORMAL) != IS_WAITING;
    }
    
    protected void requestCaching() {
        reconstitute(LOAD_CACHE);
    }

    private static final Handle AT_ERROR = Handle.emptyInstance();
    private static final Handle IS_WAITING = Handle.emptyInstance();
    
    protected synchronized Handle reconstitute(final Object when)
    {
        if (_handle == IMG_ERROR) {
            // if this was an OutOfMemoryError (a potentially recoverable error), we allow us to retry indefinitely
            if (DEBUG.IMAGE) debug("skipping reconstitue: last load had error: " + this);
            return AT_ERROR;
        }

        final boolean wasAtError = (_handle == IMG_ERROR_MEMORY); // why only memory?
        
        //if (DEBUG.IMAGE) Log.debug(Util.TERM_CYAN + "RECONSTITUTE " + Util.TERM_CLEAR + _data, new Throwable("HERE"));
        if (_handle.isLoader()) {
            if (DEBUG.IMAGE) debug("recon: already loading: " + _data);//, new Throwable("HERE"));
            return IS_WAITING;
        }
        
        if (DEBUG.IMAGE) debug(Util.TERM_CYAN + "RECONSTITUTE(" + when + ") " + Util.TERM_CLEAR + _data);

        final Ref oldHandle = _handle;

        final Images.Handle imageData;

        if (when == LOAD_IMMEDIATE) {

            // LOAD THE IMAGE SYNCHRONOUSLY, BLOCKING THE CURRENT THREAD IF NEEDED:
            
            // Due to the ImageRep impl using cacheData for result capture, the listener
            // can be null for this call -- we don't need the listener callbacks if we
            // know we'll immediately have a result.
            
            imageData = Images.getImageImmediately(_data, null);
            if (imageData == null)
                Log.warn("null on immediate request: " + this);
        }
        else if (when == LOAD_CACHE) {

            if (_handle == IMG_CACHING) {
                //Log.info("repeated cache request");
                return IS_WAITING;
            }

            // Note, do NOT want callbacks (we're not providing a listener), as we don't
            // want to be issuing repaint updates for images that were only requested to
            // be cached.  However, in case this IS later requested to draw, we DO
            // need the listener.  So for now we do listen and suffer the extra repaints.
            // What we really need is to track our state as to CACHING v.s. LOADING, and
            // only issue callbacks when an image arrives if we're LOADING.  What the means
            // tho is being able to upgrade our state from CACHING to LOADING if a paint
            // request comes in for us while we're already in queue for caching.
            
            imageData = Images.cacheImage(_data, this);
            
            if (imageData == null) {
                // as expected:
                setHandle(IMG_CACHING, "cacheRequest");
                return IS_WAITING;
            }
            // Result will often be null, as expected, but if it's available, we'll
            // want to QUIETLY load it w/out triggering callbacks / notifications.
        }
        else { // LOAD_NORMAL
            if (DEBUG.Enabled && when != LOAD_NORMAL) Log.error("bad recon code: " + Util.tags(when));
            imageData = Images.getImageHandle(_data, this);
        }

        
        if (imageData != null) {
            // Record the image data and update our status:
            cacheData(imageData, when);
            // Old comment: if we knew the return value of reconstitute was attented to we
            // could potentially skip the notify that's always made in cacheData.
            return imageData;
        }

        //----------------------------------------------------------------------------------------
        // There was no image data immediately available: make sure our status
        // is set to some version of "loading"
        //----------------------------------------------------------------------------------------

        if (_handle == oldHandle) {
            // As expected: _handle hasn't changed, as we had no result (only null returned)
            // (Why, EXACTLY, might _handle change here?)
            if (wasAtError) {
                setHandle(IMG_LOADING_AFTER_ERROR, "recon-kicked:" + when);
            } else {
                // Note: this may also be upgrading our status from CACHING to LOADING:
                setHandle(IMG_LOADING, "recon-kicked:" + when);
            }
        } else {
            // No data was returned, and yet, our _handle has changed anyway!
            
            // This can happen if a loader has completed and has it's image, but it hasn't left
            // the cache yet, so our getImage call returned null, but the image content was
            // still available and was delivered immediately as part of partial results.
            // That is, the result came via immediate callback, even though the return was null.
            // Images now checks for this, tho we're leaving this check in just in case.
            
            if (DEBUG.Enabled) debug("*** got immediate callback w/image, handle is now " + _handle);
        }
        return IS_WAITING; // is this really true for both above cases?
    }

    private void debug(String s) {
        debug(s, false);
    }
    private void debug(String s, boolean dumpStack) {
        String msg = String.format("%08x[%s] %s", System.identityHashCode(this), ImageRef.debugSRC(_data), s);
        if (dumpStack)
            Log.debug(msg, new Throwable("HERE"));
        else
            Log.debug(msg);
    }

    private void setHandle(Ref r, String debug) {
        if (DEBUG.Enabled) {
            String t;
            if (r instanceof NullRef && !(r instanceof ProgressRef))
                t = r.toString();
            else
                t = Util.tags(r);
            debug("setHandle " + t + "; " + debug, false);
        }
        _handle = r;
    }

    protected void cacheData(Images.Handle imageData, Object cause)
    {
        final Image image = imageData.image;
        
        if (image() == image) {
            if (DEBUG.Enabled) debug(" re-cache:"+cause);
            return;
        }

        if (_data.readable instanceof Image) {
            // In case this was a runtime generated icon, we MUST be certian to
            // null any original raw image data in the source to allow for GC.
            _data.readable = null;
        }

        synchronized (this) {

            // Recording the size here should be redundant to the gotImageSize we've already received,
            // tho there are some special cases where that call may not arrive (e.g., icon generation).
            // Note: multi-threaded coherency agaist AWT thread for the next 3 stores,
            // as well as locked against cacheData calls happening in AWT via reconstitute
            setSize(image.getWidth(null),
                    image.getHeight(null));
            // record the new width & height first before installing the handle just in case
            setHandle(newRef(image), "[cacheData/"+cause+"]");
        }

        // (at this point we could null local stack image var for potential GC help)

        // We do not include the below notify in the sync.  AWT blocks we're avoiding:
        // If the parent ImageRef uses this arrived rep to generate an icon, that could
        // take a while, and if this is an image processing thread (the common case),
        // any local class syncs in the render code (renderRep) will block until this
        // thread runs out, hanging the AWT until then.
        
        //if (true /*notify*/) {
        if (cause != LOAD_CACHE) {
            // we send the image as an argument so there's at least a temporary
            // guaranteed, hard, non-GC-able reference to it in case the ImageRef wants
            // to do something with the image data.

            // TODO: this is overkill if this was for a request during a paint, and the
            // cache already had the content!  We could do the paint immediatley.  This
            // is an mainly an issue only with multiple-maps tho -- the first paint of a
            // new tab, even if all the images are loaded, will need to update all the
            // image refs to the loaded state.  Currently, they make a default getImage
            // call, which immediately makes a callback to here with the result, and
            // then a second call to cacheData with the returned result.  All that
            // points to another reason for an Images cache that holds ImageRef's
            // directly.
            
            _ref.notifyRepHasArrived(this, imageData);
        }

    }

    //public void gotImageUpdate(Object key, Images.Progress p) {}
    
    public void gotImageSize(Object imageSrc, int w, int h, long bytes, int[] ss) {
        setSize(w, h); // could issue a special notifyRepHasProgress with a cause token
    }
    
    public void gotImage(Object imageSrc, Images.Handle imageData) {
        // note: coherency control point against AWT (this an image processing thread)
        // could there ever be a problem if this runs while an AWT thread is issuing
        // a reconstitue call at the same time?
        cacheData(imageData, "gotImage");
    }
    
    public void gotImageProgress(Object imageSrc, long bytesSoFar, float pct) {

        // Currently, only the full rep can report progress (if it's loading over the network) --
        // icon generation doesn't report progress -- that may be possible someday via our own
        // tracking ImageProducer/Consumer for the down-scaling filter, but that's alot of work for
        // something that's usually pretty quick, and for which we normally get to see the full-rep
        // drawn on-screen while it's happening.

        final Ref handle = _handle;

        if (handle.getClass() == ProgressRef.class) {
            if (((ProgressRef)handle).trackProgress(pct)) {
                _ref.notifyRepHasProgress(this, pct);
            }
        } else if (handle.isLoader() && pct > 0 /*&& pct < Float.POSITIVE_INFINITY*/) { // todo: should never see infinity
            setHandle(new ProgressRef(pct), "newProgress");
            _ref.notifyRepHasProgress(this, pct);
        } else {
            Log.warn("got image progress w/non-loading status: " + this);
        }
    }
    
    public void gotImageError(Object imageSrc, String msg) {
        // todo: distinguish between recoverable v.s. non-recoverable (e.g. OutOfMemory v.s. no image file)
        if (msg == Images.OUT_OF_MEMORY) {
            setHandle(IMG_ERROR_MEMORY, "gotMemoryError");
        } else {
            setHandle(IMG_ERROR, "gotError");
        }
        if (_ref == null)
            Log.warn("rep w/out null ref: " + this + "; error=" + msg);
        else
            _ref.notifyRepHasArrived(this, null);
        //_ref.notifyRepHasProgress(this, -1); // force a repaint (don't: can create thrashing loop us during low-memory conditions)
    }

    private Image get(final Ref<Image> handle) {
        final Image image = handle.get();
        if (image == null) {
            if (handle.getClass() == SoftRef.class) {
                if (DEBUG.Enabled) Log.debug("GC'd: " + _data.original);
                Images.setLowMemory("image-data-GC");
                setHandle(IMG_UNLOADED, "GC'd"); // don't do this if want to know of an EXPIRED state
            }
            return null;
        } else {
            return image;
        }
    }

    /** @return the image, if currently available */
    protected Image image() {
        return get(_handle);
    }

    Image getImageBlocking()
    {
        final Image image = image();

        if (image == null) {
            final Handle rawHandle = reconstitute(true);
            if (rawHandle == null || rawHandle.image == null) {
                return null;
            } else {
                return rawHandle.image;
            }
        } else
            return image;
    }
    
    

    

    /** LoadingColor chosen as what has best chance of presenting some contrast against all backgrounds */
    private static final Color LoadingColor = new Color(128,128,128,128);
    private static final Color ErrorColor = new Color(255,0,0,128);
    private static final Color LowMemoryColor = new Color(0,255,255,128);


    /**
     * Draw the representation into the given width/height with floating point scaling
     * resolution.  If the rep is not available, it will draw a transparent box, and
     * kick off reconstituting of the representation.
     */
    void renderRep(Graphics2D g, float toWidth, float toHeight)
    {
        final Ref<Image> handle = _handle; // fetch the volatile
        final Image image = get(handle);

        if (image == null) {
            
            drawUnavailable(g, toWidth, toHeight);
            
//             if (!handle.isLoader()) 
//                 reconstitute(); // could pass handle as arg to improve coherency?  Or would we just miss updates we want to see?
            
        } else {
            ImageRef.renderImage(g, image, toWidth, toHeight);
        }
    }

//     void renderImage(Graphics2D g, Image image, float toWidth, float toHeight) {
//         final int[] size = size();
//         final float pixelsWide = size[0];
//         final float pixelsTall = size[1];

//         g.drawImage(image,
//                     AffineTransform.getScaleInstance(toWidth / pixelsWide,
//                                                      toHeight / pixelsTall),
//                     null);
        
//     }

    boolean isFading() {
        return false;
        // can't test for this w/out using a ReferenceQueue -- will always be false
        //return _handle instanceof SoftReference && ((SoftReference)_handle).isEnqueued();
    }

    boolean isTrackingProgress() {
        return _handle.getClass() == ProgressRef.class;
    }
    
    protected void drawUnavailable(Graphics2D g, float width, float height) {

        final Ref handle = _handle;
        
        if (handle.getClass() == ProgressRef.class) {
            drawPartialProgress(g, ((ProgressRef)handle).progress, width, height);
        } else {
            drawStatus(g, width, height, handle);
        }
    }

    private void drawStatus(Graphics2D g, float width, float height, Object status) {
        if (DEBUG.BOXES||DEBUG.IMAGE) debug("DRAWING STATUS " + status);
        if (status == IMG_ERROR) {
            g.setColor(ErrorColor);
        }
        else if (status == IMG_ERROR_MEMORY || status == IMG_LOADING_AFTER_ERROR) {
            g.setColor(LowMemoryColor);
        }
        else if (DEBUG.Enabled) {
            if (status == IMG_LOADING)
                g.setColor(LoadingColor);
            else 
                g.setColor(Color.green); // no status yet
        }
        else {
            g.setColor(LoadingColor);
        }
        g.fillRect(0, 0, (int)width, (int)height); // okay if not a sub-pixel-perfect fill
        ImageRef.drawBetterRepAvailableIndicator(g, width, height);
    }

    private static void fillRect(Graphics2D g, float width, float height, Color c) {
        g.setColor(c);
        g.fillRect(0, 0, (int)width, (int)height);
    }

    private void drawPartialProgress(Graphics2D g, float progress, float width, float height) {
//         final int split = (int) (width * pct);
//         dc.g.setColor(getLoadedColor(dc));
//         dc.g.fillRect(0, 0, split, height);
//         dc.g.setColor(getEmptyColor(dc));
//         dc.g.fillRect(split, 0, width - split, height);
        //Log.debug("drawPartialProgress " + progress);
        final java.awt.geom.Rectangle2D.Float r = new java.awt.geom.Rectangle2D.Float();
        final float split = width * progress;
        g.setColor(Color.darkGray);
        r.setRect(0,0, split, height);
        g.fill(r);
        g.setColor(Color.gray);
        r.setRect(split,0, width - split, height);
        g.fill(r);
    }
    
    @Override public String toString() {
        //return "ImageRep[" + Util.tags(_handle.get()) + ", rs=" + _data + "]";
        // Fetch handle & contents once so don't have to worry about threading inconsistencies
        final Ref handle = _handle;
        final Object ptr = get(handle);
        return String.format("ImageRep@%08x[%s,%s %4dx%-4d %s]",
                             System.identityHashCode(this),
                             //state(handle, ptr),
                             handle == null ? "<<<BAD HANDLE>>>" : handle,
                             ptr == null ? "" : (" " + Util.tags(ptr) + ","),
                             _size[0], _size[1],
                             _data == null ? "NULL" : _data);
        //_data == null ? "NULL" : _data.original); // os=original-source
                             
    }

    // do not confuse the below as having anything to do with ImageRef's -- these are just
    // pointer handle impls entirely encapsulated in ImageRep, which is used by ImageRef.
    
    private static final class HardRef<T> implements Ref<T> {
        final T o;
        HardRef(T o) { this.o = o; }
        /*@Override*/ public T get() { return o; }
        /*@Override*/ public boolean isLoader() { return false; }
        @Override public String toString() { return "HARD"; }
    }
    // Note: garbage collection slows relative to the total number of
    // Reference objects in the runtime at any given time.  Probably
    // not by that much, but something to keep in mind.
    private static final class SoftRef<T> extends SoftReference<T> implements Ref<T> {
        SoftRef(T o) { super(o); }
        @Override public String toString() {
            if (get() == null)
                return "SOFT(GC'd)";
            else
                return "SOFT";
        }
        /*@Override*/ public boolean isLoader() { return false; }
    }
    private static class NullRef implements Ref {
        final String type;
        NullRef(String s) { type = s; }
        /*@Override*/ public Object get() { return null; }
        /*@Override*/ public boolean isLoader() { return false; }
        @Override public String toString() { return type; }
    }
    private static class LoadingRef extends NullRef {
        LoadingRef(String s) { super(s); }
        @Override public boolean isLoader() { return true; }
    }
    private static final class ProgressRef extends LoadingRef {
        private static final float MAX_REPORTS = 128; // at 100% view, 1 pixel per update on a wide-aspect 128px icon
        private int lastReport = -1;
        private volatile float progress;
        /** @return true if the change from the last update is worth reporting downstream */
        boolean trackProgress(final float percent) {
            // as downstream reports of this progress are going to result in repaint
            // requests, limit the number of reports we can maximally make.  An
            // interesting impl would be to handle this in ImageRef where we're already
            // figuring the on-screen pixel resolution, and we could issue reports
            // whenever a slice change would result in a visible progress pixel.
            progress = percent;
            final int slices = (int) (percent * MAX_REPORTS);
            //Log.debug("track progress int=" + slice + " pct " + percent);
            if (slices > lastReport) {
                lastReport = slices;
                return true;
            } else
                return false;
        }
        float progress() {
            return progress;
        }
        ProgressRef(float first) { super("LOADING%"); progress = first; }
    }
    
        
}

// final class LoaderRep implements Images.Listener { // implements Rep or extends ImageRep? if extends, can do w/out interface
//     boolean available() { return false; }
//     boolean loading() { return true; }
//     boolean isTrackingProgress() { return false; } // happens only after size arrives
//     boolean hasError() { return _hadError; }
//     int width() { return -1; }
//     int height() { return -1; }
//     void renderRep(Graphics2D g, float toW, float toH) { /*static drawUnavailable*/ }

//     final ImageRef _ref;
//     final ImageSource _src;
    
//     ImageRep _concreteRep;
//     boolean _hadError;
//     LoaderRep(ImageRef ref, ImageSource is) {
//         _ref = ref;
//         _src = is;
//     }

//     void reconstitute() {
//         final Image image = Images.getImage(_src, this);
//         // no override? need to make sure isn't called from AWT once a rep is in replacement critical section
//         // need to reproduce semantics of ImageRep.reconstitute?
//     }
    
//     public void gotImageSize(Object imageSrc, int w, int h, long byteSize) {
// //         _concreteRep = new ImageRep(imageSrc, w, h); // need hard/soft flag
// //         _ref.notifyRepIsReplaced(this, _concreteRep);
//         // note: base rep would still need size replacing code for on-disk image changes & reloads during runtime
//     }
//     public void gotImage(Object imageSrc, Image image, ImageRef ref) {
//         _concreteRep.gotImage(imageSrc, image, ref);
//     }
//     public void gotImageProgress(Object imageSrc, long bytesSoFar, float pct) {
//         _concreteRep.gotImageProgress(imageSrc, bytesSoFar, pct);
//     }
    
//     public void gotImageError(Object imageSrc, String msg) {
//         // Go to error state if had early error (e.g., file not found), otherwise relay
//         if (_concreteRep == null)
//             _hadError = true;
//         else
//             _concreteRep.gotImageError(imageSrc, msg);
//     }
    
// }

// What if the size of the underlying image actually DOES change tho?  E.g., a new
// version of it has been written to disk over the old one by the user.  We'd need to be
// ready to issue a new concrete rep in that case as well, tho that should actually be
// no big deal as long as we've got a standard method of replacing a rep.  Hell,
// actually, we could use that code as the STANDARD way of updating the rep as well!
// Then we have no ProtoRep at all -- just bootstrapping ImageReps, old-size ImageReps,
// and currently sized ImageReps.

// The reason to provide a new rep as soon as we have the size is for early size
// recording, tho we don't even do that now, except incidentally by updating the
// width/height volatiles, which will only effect any progress reports till we push some
// kind of event.

//===================================================================================================


// Note: subclassing java.awt.Image is not actually supported; in AWT Graphics drawing:
// SurfaceManager can't get surface.  see
// http://forums.sun.com/thread.jspa?threadID=5208043 -- basically, this is a 10+ year
// old bug that's never been fixed -- either java.awt.Image needs to be abstract or the
// impls need changing, and it looks like there's just no hope on this one at all.
// Subclassing BufferedImage MAY be possible tho...  this would lock us in 100% to never
// using ToolkitImage's, tho we can probably live with that as that impl gives us memory
// heartburn, at least on the Mac.  The bigger issue is we'd like to be able to rely on
// using createCompatibleImage for creating our BufferedImages, and not have to create
// our own delegating instances.


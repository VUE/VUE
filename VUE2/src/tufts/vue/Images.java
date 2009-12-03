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
import tufts.DocDump;

import static tufts.vue.Resource.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.ref.*;
import java.net.URL;
import java.net.URI;
import java.net.URLConnection;
import java.io.*;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import javax.swing.ImageIcon;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.event.*;
import javax.imageio.stream.*;

import javax.xml.xpath.*;
import org.w3c.dom.NodeList;

/**
 *
 * Handle the loading of images in background threads, making callbacks to deliver
 * results to multiple listeners that can be added at any time during the image fetch,
 * and caching (memory and disk) with a URI key, using a HashMap with SoftReference's
 * for the BufferedImage's so if we run low on memory they just drop out of the cache.
 *
 * @version $Revision: 1.71 $ / $Date: 2009-12-03 15:41:38 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class Images
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Images.class);

    private static final boolean ALLOW_HIGH_QUALITY_ICONS = true;

    private static volatile int LOW_MEMORY_COUNT = 0;

    // Setting DELAYED_ICONS to true allows maps with lots of images paint something for
    // each image much faster the first time they're loaded under plentiful memory
    // conditions, but cause horrible thrashing under low memory conditions, and it
    // would take even more complexity than we've already got to fix that, so for now
    // we're just going with immediately created icons.
    static final boolean DELAYED_ICONS = false;

    public static synchronized void setLowMemory(Object cause) {
        if (LOW_MEMORY_COUNT == 0)
            Log.info(Util.TERM_CYAN + "entering low-memory conditions, cause=" + Util.tags(cause) + Util.TERM_CLEAR);
        else
            if (DEBUG.Enabled) Log.debug("setLowMemory " + LOW_MEMORY_COUNT + ": " + Util.tags(cause));
        final boolean first = (LOW_MEMORY_COUNT == 0);
        LOW_MEMORY_COUNT++;
        ProcessingPool.shrinkIfPossible(first);
    }

    public static int lowMemoryCount() {
        return LOW_MEMORY_COUNT;
    }

    public static boolean lowMemoryConditions() {
        return LOW_MEMORY_COUNT > 0;
    }
    
    public static VueAction ClearCacheAction = new VueAction("Empty Image Cache") {
            public void act() { RawCache.clear(); }
        };
    
    
    /**
     * Calls to Images.getImage must pass in a Listener to get results.
     * The first argument to all the callbacks is the original object
     * passed in to getImage as the imageSRC.
     */
    public interface Listener {

        /** If image is already cached, this will NOT be called -- is only called from an image loading thread.
         * If sourceSize is non-null, this is an icon, and that represents the full-size */
        void gotImageSize(Object imageSrc, int w, int h, long byteSize, int[] sourceSize);
        /** If byte-tracking is enabled on the input source, this will be called periodically during loading */
        void gotImageProgress(Object imageSrc, long bytesSoFar, float percentSoFar);
        /** Will be called immediately in same thread if image cached, later in different thread if not. */
        //void gotImage(Object imageSrc, Image image, int w, int h);
        void gotImage(Object imageSrc, Handle handle);
        /** If there is an exception or problem loading the image, this will be called */
        void gotImageError(Object imageSrc, String msg);

//         /** intended to ultimately replace all the below calls */
//         void gotImageData(Object key, Progress p);
        
    }


    /**
     * Fetch the given image with meta-data.  If it's cached, listener.gotImage is called back immediately
     * in the current thread.  If not, the image is fetched asynchronously, and the
     * callbacks are made later from a special image loading thread.
     *
     * @param imageSRC - anything that might be converted to an image: a Resource, File, URL, InputStream, etc.
     *
     * @param listener - the Images.Listener to callback.  If null, the result
     * of the call would be only to ensure the given image is cached.
     *
     * @return the Image if immediately available, null if the listener will be called back.
     **/
    
    public static Handle getImageHandle(Object imageSRC, Images.Listener listener)
    {
        return getImage(imageSRC, listener, false);
    }
    
    public static Image getImage(Object imageSRC, Images.Listener listener)
    {
        //return getImage(imageSRC, listener, false).image;
        return handleImage(getImage(imageSRC, listener, false));
    }


    /**
     * If the requested content is not already loading,
     * it will be immediately loaded in the current thread.
     */
    public static Image getImageASAP(Object imageSRC, Images.Listener listener)
    {
        return getImage(imageSRC, listener, true).image;
    }
    
    /**
     * Fetch the given image.  If it's cached, listener.gotImage is called back immediately
     * in the current thread.  If not, the image is fetched asynchronously, and the
     * callbacks are made later from a special image loading thread.
     *
     * @param imageSRC - anything that might be converted to an image: a Resource, File, URL, InputStream, etc.
     *
     * @param listener - the Images.Listener to callback.  If null, the result
     * of the call would be only to ensure the given image is cached.
     *
     * @param immediate - if the requested content is not already loading,
     * it will be immediately loaded in the current thread.
     *
     * @return the Image if immediately available, null if the listener will be called back.
     **/
    private static Handle getImage(Object imageSRC, Images.Listener listener, boolean immediate)
    {
        try {
            return getCachedOrCallbackOnLoad(imageSRC, listener, immediate);
        } catch (Throwable t) {
            if (DEBUG.IMAGE) tufts.Util.printStackTrace(t);
            if (listener != null)
                listener.gotImageError(imageSRC, t.toString());
        }
        return null;
    }
    
    
//     /** SYNCHRONOUSLY retrive an image from the given data source: e.g., a Resource, File, URL, InputStream, etc */
//     public static Image getImage(Object imageData)
//     {
//         try {
//             return getCachedOrCallbackOnLoad(imageData, null);
//         } catch (Throwable t) {
//             Log.error("getImage " + imageData, t);
//             return null;
//         }
//     }

    public static void loadDiskCache()
    {
        File dir = getCacheDirectory();
        if (dir == null)
            return;

        Log.debug("listing disk cache...");
        File[] files = dir.listFiles();
        Log.debug("listing disk cache: done; entries=" + files.length);
        
        synchronized (RawCache) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String name = file.getName();
                if (name.charAt(0) == '.')
                    continue;
                //out("found cache file " + file);
                URI key = null;
                try {
                    key = cacheFileNameToKey(name);
                    if (DEBUG.IMAGE && DEBUG.META) out("made cache key: " + key);
                    if (key != null) {
                        RawCache.put(key, new CacheEntry(file));
                        //RefCache.put(key, file);
                   }
                } catch (Throwable t) {
                    Log.error("failed to load cache with [" + name + "]; key=" + key);
                }
            }
        }
    }
    
    /** @return the cache file for the given resource, or null if none exists */
    public static File findCacheFile(Resource r) {

        final ImageSource imageSRC = ImageSource.create(r);

        if (imageSRC.key != null) {
            final Object entry = RawCache.get(imageSRC.key);
            if (entry instanceof CacheEntry) {
                return ((CacheEntry)entry).file;
            } else {
                Log.warn("Cache is loading, no cache file yet for " + r);
            }
        }
        Log.warn("Failed to find cache file for " + r);
        return null;
    }
    

    // todo: really, ImageCacheEntry v.s. Loader cache entries, tho they don't
    // currently have a common super-class.
    private static class CacheEntry {
        private final Reference<Image> ref;
        private final Map<String,?> data;
        private final File file;
        // Loader loader;
        // todo: add loader here so we can always have CacheEntry's in the cache, and
        // so file is information always available, tho that could be extracted from the
        // Loader ImageSource? No -- that could be any file, not just a disk-cache entry.
        // This new complexity arising out of having icons in the cache -- we may just
        // want a separate icon cache.

        /** image should only be null for startup init with existing cache files */
        CacheEntry(Image image, File cacheFile, Map<String,?> props)
        {
            if (image == null && cacheFile == null)
                throw new IllegalArgumentException("CacheEntry: at least one of image or file must be non null");
            if (image != null)
                this.ref = new SoftReference(image);
            else
                this.ref = null;
            this.file = cacheFile;
            this.data = props;
            if (DEBUG.IMAGE) out("new " + this);
        }

        CacheEntry(Image image, File cacheFile) {
            this(image, cacheFile, Collections.EMPTY_MAP);
        }
        
        CacheEntry(Handle handle, File cacheFile) {
            this(handle.image, cacheFile, handle.data);
        }
        
        // for startup disk entries: note that props can't change (is final), so this
        // entry must be replaced once the image is loaded.
        CacheEntry(File cacheFile) {
            this(null, cacheFile, Collections.EMPTY_MAP); 
        }
        

        boolean isPreloadedDiskEntry() {
            return ref == null;
        }

        Image getCachedImage() {

            // if don't even have a ref, this was for an init-time persistent cache file
            if (ref == null)
                return null;

            Image image = ref.get();
            // will be null if was cleared
            if (image == null) {
                if (DEBUG.Enabled && file != null) out("GC'd: " + file);
                return null;
            } else
                return image;
        }

        Handle getHandle() {
            //return Handle.create(getCachedImage(), data);
            Image i = getCachedImage();
            if (i != null)
                return new Handle(i, data);
            else
                return null;
        }

        File getFile() {
            return file;
        }

        void clear() {
            if (ref != null)
                ref.clear();
        }

        public String toString() {
            return "CacheEntry[" + tag(getCachedImage()) + "; file=" + file + "]";
        }
    }

    private static final CacheMap RawCache = new CacheMap();
    //private static final NewCache RefCache = new NewCache();
    //private static final NewCache<URI,ImageRef> RefCache = new NewCache();
    
//     private static class NewCache {

//         private final Map<URI,ImageRef> map = new ConcurrentHashMap();
        
//         // can ultimately refactor access code to make all transactions explicit
//         // so don't need to be synchronized here and can rely on ConcurrentHashMap
//         // for simple ops, but leaving synchronized for now as our existing
//         // cache transactions may depend on it.
    
//         synchronized ImageRef get(URI key) {
//             return map.get(key);
//         }
        
//         synchronized boolean containsKey(URI key) {
//             return map.containsKey(key);
//         }
        
//         synchronized ImageRef put(URI key, ImageRef value) {
//             return map.put(key, value);
//         }
        
//         synchronized ImageRef put(URI key, File file) {
//             //return map.put(key, value);
//             //return map.put(key, ImageRef.create(file));
//             return null;
//         }
        
//         synchronized ImageRef remove(URI key) {
//             return map.remove(key);
//         }
        
//         synchronized void clear() {
//             throw new UnsupportedOperationException("unimplemented");
//         }
//     }
        //===================================================================================================
        // This would all be much simpler if the cache contained ImageRef's.  They could contain
        // whatever reps are available, and be the place where the full size was always recorded
        // (possibly in the size of it's full-rep, left in an UNLOADED state, but with size
        // recorded.  In that case, the _full rep may be able to be made final).  The only
        // high-level extra complexity would be that ImageRef's would then need to keep a list of
        // listeners (LWImage's), which would also want to be flushed when LWImage's leave the
        // model (and re-add if they re-join the model).  Also tho, w/out extra work, any update to
        // an ImageRef (e.g., full-rep-has-arrived), would trigger repaints to all LWImage's, even
        // if their happy displaying just an icon image.  That's probably a good trade-off to make
        // tho -- it's not often that you have a map with the same image repeated anyway (well, may
        // presentations when slide icons are turned on), and it's extra repaints that will only
        // happen once -- when the full rep fully arrives.
        //
        // Also, either all existing API's would need to change to work w/ImageRef's, or we'd need
        // to keep things backward compat.  That probably wouldn't be that much of an issue.
        // Okay, only two places use the image api: PreviewPane & ResourceIcon.  And only PreviewPane
        // makes any use of the tracking the original update source (in case we've switched to a new
        // preview after results of an old preview finally arrived).
        //
        // The big change is that the cache would now contain entries for multiple reps (full + icons).
        // Maybe this isn't an issue at all tho?  Currently, this lets full reps and icon reps
        // exist separately in the cache, and for them to be GC's separately.  If the cache contained
        // ImageRef's, nobody would actually request icon reps -- ImageRef is the only thing that does
        // that.  Normal requestors only request the original rep -- it's just sometimes they'll get
        // the icon rep instead.
        //===================================================================================================
        

    /*
     * Not all HashMap methods covered: only safe to use
     * the ones explicity implemented here.
     */
    private static class CacheMap extends HashMap {

        public synchronized Object get(Object key) {
            return super.get(key);
        }
        
        public synchronized boolean containsKey(Object key) {
            return super.containsKey(key);
        }
        
        public synchronized Object put(Object key, Object value) {
            return super.put(key, value);
        }

        public synchronized Object remove(Object key) {
            return super.remove(key);
        }
        

        // for now, only clears memory cache
        public synchronized void clear() {
            final Iterator i = values().iterator();
            while (i.hasNext()) {
                Object entry = i.next();

                // may be a Loader: todo: may want to kill thread if it is Especially:
                // if we go off line, Loaders created immediately after that (or during)
                // tend to hang forever.  Loaders created once the OS knows we're
                // offline will usually fail immediately with "no route to host", but
                // even after going back online, and other images load, the originally
                // hung Loader's won't die...  So at least an image-cache should kill
                // them.
                
                if (entry instanceof CacheEntry) {
                    CacheEntry ce = (CacheEntry) entry;
                    ce.clear();
                    if (ce.getFile() == null)
                        i.remove();
                } else {
                    // Interrupt may not be good enough: if blocked on non-async IO
                    // (non-channel IO, e.g., "regular"), this can have no
                    // effect.  Turns out using stop doesn't help even in this
                    // case.
                    if (entry instanceof LoadThread) {
                        Log.info("STOPPING THREAD ENTRY " + entry);
                        ((LoadThread)entry).stop();
                    } else if (entry instanceof LoadTask) {
                        Log.warn("LEAVING TO RUN OUT TASK ENTRY " + entry);
                        // if it was a Future, we could attempt to de-queue it
                    }
                    //((LoadThread)entry).stop();
                    //((Loader)entry).interrupt();
                }
            }

            //super.clear();
        }
        
    }

    /**
     * flush any cache BufferedImages we have for the give file: future requests
     * will force the image data to be reloaded from the file (useful if we know
     * the file has changed on disk).
     */
    public static void flushCache(File file) {
        final Object key = makeKey(file);
        final Object entry = RawCache.remove(key);
        if (entry != null) {
            Log.info(Util.TERM_RED + "flushed cache entry: " + key + "; " + entry + Util.TERM_CLEAR);
        } else {
            Log.info("failed to find cache entry for key: " + Util.tags(key));
        }
    }

//     private static URI makeKey(URL u) {
//         try {

//             if ("file".equals(u.getProtocol())) {

//                 return Resource.makeURI(u);
                
//             } else {
                
//                 return new URI(u.getProtocol(),
//                                u.getUserInfo(),
//                                u.getHost(),
//                                u.getPort(),
//                                //u.getAuthority(),
//                                u.getPath(),
//                                u.getQuery(),
//                                u.getRef()).normalize();
                
//             }
            
//         } catch (Throwable t) {
//             Util.printStackTrace(t, "can't make URI cache key from URL " + u);
//         }
//         return null;
//     }

    static URI makeKey(File file) {
        try {
            return file.toURI().normalize();
        } catch (Throwable t) {
            Util.printStackTrace(t, "can't make URI cache key from file " + file);
        }
        return null;
    }

    static String keyToCacheFileName(URI key)
    //throws java.io.UnsupportedEncodingException
    {
        try {
            //return key.toASCIIString();
            return java.net.URLEncoder.encode(key.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.error("transforming key to cache-file name: " + key, e);
        }
        return null;
    }
    
    private static URI cacheFileNameToKey(String name) 
    {
        try {
            return new URI(java.net.URLDecoder.decode(name, "UTF-8"));
            //return new URL(java.net.URLDecoder.decode(name, "UTF-8"));
        } catch (Throwable t) {
            if (DEBUG.Enabled) tufts.Util.printStackTrace(t);
            return null;
        }
    }

    /**
     * Using a relay system, as opposed to say a list of listeners maintained by the
     * Loader, allows the image loading code to not care if there is a single listener
     * or multiple listeners, which is handy in the case where the result is cached and
     * we don't even create a loader: we just callback the listener immediately in the
     * same thread.  But when a Loader is created, it can create ListenerRelay's to
     * relay results down the chain, starting with it's special LoaderRelayer to relay
     * partial results to listeners added in the middle of an image fetch, and again,
     * the image loading code doesn't need to know about this: it just has a single
     * listener.
     *
     * Performance-wise, there is rarely ever more than a single relayer object
     * created (covering two listeners for the same image load).
     *
     * This is also a handy place for diagnostics.
     */
    private static class ListenerRelay implements Listener {
        protected final Listener head;
        protected Listener tail;
        
        ListenerRelay(Listener l0, Listener l1) {
            // if head is null, nobody is listening at the start, but listeners may be added later to tail
            this.head = l0;
            this.tail = l1;
        }
        ListenerRelay(Listener l0) {
            this(l0, null);
        }

//         public void gotImageUpdate(Object key, Progress p) {
//             if (head != null) relayUpdate(head, key, p);
//             if (tail != null) relayUpdate(tail, key, p);
//         }
//         protected final void relayUpdate(Listener l, Object key, Progress p) {
//             try {
//                 if (DEBUG.IMAGE && l instanceof ListenerRelay == false)
//                     out("relay UPDATE of " + key + " to " + tag(l));
//                 l.gotImageUpdate(key, p);
//             } catch (Throwable t) {
//                 Log.error("relaying UPDATE " + key + " to " + Util.tags(l), t);
//             }
//         }
        
        public void gotImageSize(Object src, int w, int h, long bytes, int[] sourceSize) {
            relaySize(head, src, w, h, bytes, sourceSize);
            relaySize(tail, src, w, h, bytes, sourceSize);
        }
        public void gotImageProgress(Object src, long bytes, float pct) {
            relayProgress(head, src, bytes, pct);
            relayProgress(tail, src, bytes, pct);
        }
        public void gotImage(Object src, Handle handle) {
            relayImage(head, src, handle);
            relayImage(tail, src, handle);
        }
        public void gotImageError(Object src, String msg) {
            relayError(head, src, msg);
            relayError(tail, src, msg);
        }

        protected final void relaySize(Listener l, Object src, int w, int h, long bytes, int[] sourceSize) {
            if (l != null) {
                try {
                    if (DEBUG.IMAGE && l instanceof ListenerRelay == false)
                        out("relay SIZE to " + tag(l));
                    l.gotImageSize(src, w, h, bytes, sourceSize);
                } catch (Throwable t) {
                    Log.error("relaying size to " + Util.tags(l), t);
                }
            }
        }
        protected final void relayProgress(Listener l, Object src, long bytes, float pct) {
            if (l != null) {
                try {
                    if (DEBUG.IMAGE && DEBUG.META && l instanceof ListenerRelay == false)
                        out(String.format("relay PROGRESS %.2f %5d to %s", pct, bytes, tag(l)));
                    l.gotImageProgress(src, bytes, pct);
                } catch (Throwable t) {
                    Log.error("relaying progress to " + Util.tags(l), t);
                }
            }
        }
        protected final void relayImage(Listener l, Object src, Handle handle) {
            if (l != null) {
                try {
                    if (DEBUG.IMAGE && l instanceof ListenerRelay == false)
                        out(Util.TERM_CYAN + "relay IMAGE to " + tag(l) + Util.TERM_CLEAR);
                    l.gotImage(src, handle);
                } catch (Throwable t) {
                    Log.error("relaying image to " + Util.tags(l), t);
                }
            }
        }
        protected final void relayError(Listener l, Object src, String msg) {
            if (l != null) {
                try {
                    if (DEBUG.IMAGE && l instanceof ListenerRelay == false)
                        out("relay ERROR to " + tag(head) + "; is=" + src);
                    l.gotImageError(src, msg);
                } catch (Throwable t) {
                    Log.error("relaying error to " + Util.tags(l), t);
                }
            }
        }

        boolean hasListener(Listener l) {
            if (head == l || tail == l)
                return true;
            else if (tail instanceof ListenerRelay)
                return ((ListenerRelay)tail).hasListener(l);
            else
                return false;
        }

        protected boolean addListener(Listener newListener) {
            
            synchronized (this) {
                if (hasListener(newListener)) {
                    if (DEBUG.Enabled) out("CachingRelayer: ALREADY A LISTENER FOR THIS IMAGE: " + tag(newListener));
                    return false;
                }
                if (tail == null)  {
                    this.tail = newListener;
                } else {
                    this.tail = new ListenerRelay(tail, newListener);
                }
                return true;
            }

            // Note: WE CAN DEADLOCK if deliverPartialResults is in the sync.  E.g. --
            // trying moving around raw images as they're loading / icon generating.
            
            // TODO: this really should be in the sync, as well as all the above
            // Listener API methods -- normally we'll be in the AWT thread, and if a
            // loader modifies the partial results while this call is being made, they
            // may be incoherent, and not all partial data may delivered.  The lock is
            // inherently dangerous tho, in that anything may generally happen during
            // client code callbacks, including calls back into this API.
            
            //deliverPartialResults(newListener);
        }
    }

    /**
     * Track what's been delivered, to send to listeners that are added when
     * partial results have already been delivered.
     */
    
    // note: would be simpler to have a single ImageResult/ImageProgress object that
    // slowly accumulates it's results, and is reported to a single gotImageData call,
    // with an added event type argument (size/progress/image/icon/error) An ImageRep
    // might be tempting to serve this purpose, but that doesn't really make sense: the
    // ImageRep doesn't need to know things like byteSize or bytesSoFar.  This Progress
    // class is an an attempt to that, tho we'd need to change all the Listener API
    // impls to make use of it -- cleanup for another day.

    /*public*/ static class Progress {

//         public static final String SIZE = "IMG_SIZE";
//         public static final String SOURCE_SIZE = "IMG_SOURCE_SIZE";
//         public static final String PROGRESS = "IMG_BYTE_PROGRESS";
//         public static final String IMAGE = "IMG_RENDERABLE";
//         public static final String ERROR = "IMG_ERROR";

        protected final ImageSource imageSRC;
        
        protected Handle handle;
        protected int width = -1;
        protected int height = -1;
        protected int[] sourceSize;
        protected long byteSize;
        protected long bytesSoFar;
        protected String errorMsg;

        private Progress(ImageSource is) {
            imageSRC = is;
        }
        
        public ImageSource source() { return imageSRC; }
        public Image image() { return handle == null ? null : handle.image; }
        public int width() { return width; }
        public int height() { return height; }
        public long byteSize() { return byteSize; }
        public long bytesSoFar() { return bytesSoFar; }
        public String errorMessage() { return errorMsg; }
    }
    
    private static final class CachingRelayer extends Progress implements Listener
    {
        final ListenerRelay chain;
        
        CachingRelayer(ImageSource is, Listener firstListener) {
            //super(firstListener, null);
            super(is);
            chain = new ListenerRelay(firstListener);
        }

//         public void gotImageData(Object key, Progress p) {
//             chain.gotImageUpdate(key, p);
//         }
        
        public synchronized void gotImageSize(Object imageSrc, int w, int h, long byteSize, int[] sourceSize) {
            this.width = w;
            this.height = h;
            this.byteSize = byteSize;
            if (sourceSize != null)
                this.sourceSize = sourceSize;
            chain.gotImageSize(imageSrc, w, h, byteSize, sourceSize);
        }

        public synchronized void gotImageProgress(Object imageSrc, long bytesSoFar, float _p) {
            this.bytesSoFar = bytesSoFar;
            // incoming percent is empty -- we fill it here
            // Note that the underlying raw stream may send us byte progress reports before
            // the image size is known.  This is a bug -- the byteSize is known
            // before this and should be recored here.  The # of bytes read before
            // the image size is known is usually negligable tho.  For now we just don't report
            // the progress until we have a byteSize.
            if (byteSize > 0)
                chain.gotImageProgress(imageSrc, bytesSoFar, percentProgress());
        }
        
        public synchronized void gotImage(Object imageSrc, Handle handle) {
            //this.image = image;
            this.handle = handle;
            chain.gotImage(imageSrc, handle);

        }
        
        public synchronized void gotImageError(Object imageSrc, String msg) {
            this.errorMsg = msg;
            chain.gotImageError(imageSrc, msg);

        }

        private float percentProgress() {
//             // note: bytes can arrive from the raw stream before the im
//             final float x = (float) bytesSoFar / (float) byteSize;
//             Log.debug("bytesSoFar " + bytesSoFar + " / bytes " + byteSize + " = " + x);
            if (byteSize > 0)
                return (float) bytesSoFar / (float) byteSize;
            else
                return 0;
        }

        void addListener(Listener newListener)
        {
            if (chain.addListener(newListener))
                deliverPartialResults(newListener);
        }

        void flushForGC() {
            //chain.imageSRC = null; // is now final
            //image = null; // this is the most important
            // chain = null; // is final
            handle = null;
        }
        
        /**
         * Deliver any results we've already got.  It's possible for this to happen
         * even after we have all our results, if the image completed between the
         * time we found the Loader in the cache, and the time the requestor was
         * added as a listener.
         */
        private void deliverPartialResults(Listener l)
        {
            if (DEBUG.IMAGE) out("DELIVERING PARTIAL RESULTS TO: " + tag(l));
            
            if (width > 0)
                chain.relaySize(l, imageSRC.original, width, height, byteSize, sourceSize);

            if (bytesSoFar > 0)
                chain.relayProgress(l, imageSRC.original, bytesSoFar, percentProgress());

            if (handle != null)
                chain.relayImage(l, imageSRC.original, handle);

            if (errorMsg != null) {
                if (handle != null)
                    Log.warn("had both image and error: " + errorMsg + "; for " + l);
                chain.relayError(l, imageSRC.original, errorMsg);
            }


            if (DEBUG.IMAGE) out(" DELIVERED PARTIAL RESULTS TO: " + tag(l));
            
        }

    }
    
//     private static Image getCachedOrCallbackOnLoad(Object imageSource, Images.Listener listener)
//         throws java.io.IOException, java.lang.InterruptedException
//     {
//         return getCachedOrCallbackOnLoad(imageSource, listener, false);
//     }

    /**
     * @return Image if cached or listener is null, otherwise makes callbacks to the listener from
     * a new thread.
     */
    private static Handle getCachedOrCallbackOnLoad
        (final Object imageSource,
         final Images.Listener listener,
         final boolean immediate)
        throws java.io.IOException, java.lang.InterruptedException
    {
        if (imageSource instanceof Image) {
            if (DEBUG.Enabled) Log.info("image source was an instance of Image", new Throwable(Util.tags(imageSource)));
            final Image image = (Image) imageSource;
            final int w, h;
            if (image instanceof BufferedImage) {
                BufferedImage bi = (BufferedImage) imageSource;
                w = bi.getWidth();
                h = bi.getHeight();
            } else {
                w = image.getWidth(null);
                h = image.getHeight(null);
            }

            final Handle handle = new Handle(image);
            
            if (listener != null)
                listener.gotImage(imageSource, // same as image
                                  handle);

            return handle;
        }
        
        final ImageSource imageSRC = ImageSource.create(imageSource);

        //if (DEBUG.IMAGE) System.out.println("-------------------------------------------------------");

        //Log.debug("fetching image source " + imageSRC + " for " + tag(listener));
        //if (DEBUG.Enabled) Log.debug("fetching " + imageSRC + " for listener " + Util.tag(listener));
        if (DEBUG.IMAGE) Log.debug("fetching for listener " + Util.tag(listener) + " " + imageSRC);

        final Object cacheEntry = getCachedOrKickLoad(imageSRC, listener, immediate);

        // TODO: if nothing was found in the cache and an ICON for the
        // given source exists in the cache, return that.  Either that
        // or change all Images callers to request an ImageRef, which
        // will handle that for us.
        
        if (cacheEntry == IMAGE_LOADER_STARTED) {
            // if this is the START of a new load, the caller has already been attached
            // as a listener, and the results will be coming via callback.
            return null;
        }
            
        Handle handle = null;
        
        if (cacheEntry instanceof Loader) {

            // Another request has already put a Loader into the cache -- have
            // the listener observe the existing loader.
            
            final Loader loader = (Loader) cacheEntry;

            final Image loaderImage = loader.getLoaderImage();

            if (loaderImage != null) {
                // This can happen if a loader has completed, but hasn't left the cache yet.  We
                // can just return the image immediately w/out delivering partial-result callbacks
                // (or in this case, it would be full-result callbacks).  We're ignoring any sync
                // issues on the call to loader.getImage(), because even if we see null when it's
                // really there, the results will still be delivered by our fully synced partial
                // results delivery, and we don't need any more sync issues to test.
                return new Handle(loaderImage);
            }

            if (listener != null) {
                //if (DEBUG.IMAGE) out("Adding us as listener to existing Loader");
                loader.addListener(listener);
                return null;
                
            } else {
                
                // We had no listener, so run synchronous & wait on existing loader thread to die:
                // We can't have a cache-lock when we do this.
                
                // NOTE: this is dangerous -- multiple non-listener requests for the
                // same loading content will quickly have all image processing threads
                // hung waiting on the same load -- do we still need this?  Generally
                // speaking, image requests w/out listeners are not safe.  For now,
                // we'll only allow this if this was an immediate requrest.

                if (!immediate) {
                    // No listener, no cache entry, no result.  Caller simply
                    // gets null and doesn't know why, even tho the image
                    // may now be loading.
                    Log.warn("no listener for non-cached content: caller in the dark as to the presence of image content: " + imageSRC);
                    return null;
                }

                Log.info("Joining " + tag(loader) + "...");
                loader.join();
                Log.info("Join of " + tag(loader) + " completed, cache has filled.");
                
                // Note we get here only in one rare case: there was an entry in the
                // cache that was already loading on another thread, and somebody new
                // requested the image that did NOT have a listener, so we joined the
                // existing thread and waited for it to finish (with no listener, we
                // have to run synchronous).
                
                // So now that we've waited, we should be guaranteed to have a full
                // Image result in the cache at this point.
                
                // Note: theoretically, the GC could have cleared our SoftReference
                // betwen loading the cache and now.  We can't lock the cache while
                // we're waiting on the join tho.
                
//                 cachedImage = ((CacheEntry)RawCache.get(imageSRC.key)).getCachedImage();
//                 if (cachedImage == null)
//                     Log.warn("Zealous GC: image tossed immediately " + imageSRC);
                
                handle = ((CacheEntry)RawCache.get(imageSRC.key)).getHandle();
                if (handle == null)
                    Log.warn("Zealous GC: image tossed immediately " + imageSRC);
            }
        }
        else if (cacheEntry instanceof Handle) {
            handle = (Handle) cacheEntry;
        }
        else if (cacheEntry instanceof Image) {
            Log.warn("DEPRECATED USE OF CACHE ENTRY -- USE HANDLE; " + imageSRC, new Throwable("HERE"));
            //cachedImage = (Image) cacheEntry;
            handle = new Handle((Image) cacheEntry);
        } else {
            //if (listener != null)
                Log.warn("Unhandled cache entry: " + Util.tags(cacheEntry), new Throwable("HERE"));
            // if we have no listener, it's reasonable for the getCachedOrKickLoad to return null,
            // tho wouldn't it be better just to return one anyway, and allow quiet async caching?
            // later requests could still hook up to the loader
        }

        if (handle != null && listener != null) {
            // THE IMAGE WAS IN THE CACHE: immediately callback the listener with the result
//             if (handle.data.size() > 0)
//                 listener.gotImageUpdate(imageSRC.original, handle.data);
            listener.gotImage(imageSRC.original, handle);
        }
//         if (cachedImage != null) {
//             if (listener != null) {
//                 // THE IMAGE WAS IN THE CACHE: immediately callback the listener with the result
//                 listener.gotImage(imageSRC.original,
//                                   cachedImage,
//                                   null);

//             }
//             //return cachedImage;
//         }

        return handle;
        //return cachedImage;
        
// [no longer needed: request can be immediate]        
//         // We had no image, and no Loader was started: this should
//         // only happen if there was no listener for the Loader, tho we
//         // allow the sync load to go ahead just in case.  (Could get
//         // here due to an over-zealous GC).

//         if (listener != null)
//             Log.warn("had a listener, but no Loader created: backup synchronous loading for " + imageSRC);

//         if (DEBUG.IMAGE) out("synchronous load of " + imageSRC);
        
//         // load the image and don't return until we have it
//         return loadImageAndCache(imageSRC, listener);
    }
    

    private static final Object IMAGE_LOADER_STARTED = "<image-loader-created>";
    
    
    /**
     * Although ThreadPoolExecutors have an API to allow resizing, the resize does not
     * appear to take effect until all tasks are completed and the pool has has run to
     * idle.  This class wraps an ExecutorService (a ThreadPoolExecutor), and allows it
     * to be immediately shut down and all of it's tasks transferred to a new, smaller
     * pool at any time.  This needs to happen ASAP if we start getting
     * OutOfMemoryError's.
     */
    private static class ImmediatelyReducablePool implements Runnable {
        
        private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ImmediatelyReducablePool.class);

        private Thread _shutdownThread;
        private ThreadPoolExecutor _pool;
        private ExecutorService _poolInShutdown;
        private List<Runnable> _deferredTasks;
        private int _nextSmallerPoolSize;
            
        ImmediatelyReducablePool(int startSize) {
            _pool = createThreadPool(startSize);
            if (startSize > 1) {
                //_nextSmallerPoolSize = startSize / 2;
                // for now, any EOM will ramp us straight down to a single processing thread
                _nextSmallerPoolSize = 1;
            } else {
                _nextSmallerPoolSize = 0;
            }
        }
        
        public void run() {
            Log.info("reducer started");
            ExecutorService oldPool = null;
            for (;;) {
                synchronized (this) {
                    if (oldPool == _poolInShutdown)
                        Log.error("should never happen: re-waiting on the same pool " + oldPool);
                    oldPool = _poolInShutdown;
                }
                //---------------------------------
                waitForTermination(oldPool);
                //---------------------------------
                synchronized (this) {
                    _poolInShutdown = null;
                    createAndLoadNewPool(_nextSmallerPoolSize);
                    Log.info(Util.TERM_YELLOW + "pool resized to " + _nextSmallerPoolSize + Util.TERM_CLEAR);
                    if (_nextSmallerPoolSize <= 1) {
                        _nextSmallerPoolSize = 0;
                        _shutdownThread = null; // allow GC
                        _deferredTasks = null; // allow GC
                        break; // no more resizes possible
                    } else {
                        reduceNextPoolSize();
                        Log.info("next pool size: " + _nextSmallerPoolSize);
                        try {
                            Log.info("resizer sleeping...");
                            wait();
                            Log.info("resizer woke");
                        } catch (InterruptedException e) {
                            Log.error("interrupted " + this, e);
                        }
                    }
                }
            }
            Log.info("resizer terminating, pool at size = 1, no further shrinkages possible");
        }

        private void reduceNextPoolSize() {
            if (_nextSmallerPoolSize > 1)
                _nextSmallerPoolSize /= 2;
            else
                _nextSmallerPoolSize = 1;
        }

        private synchronized void forkAndWaitForTermination(ExecutorService pool) {
            _poolInShutdown = pool;
            if (_shutdownThread == null) {
                // if we never run out of memory, we'll never need to start this thread
                _shutdownThread = new Thread(this, "PoolReducer");
                _shutdownThread.start();
            } else {
                notify();
            }
        }
        
        private void waitForTermination(final ExecutorService poolInShutdown) {
            Log.info("awaitTermination...");
            try {
                if (poolInShutdown.awaitTermination(60L, TimeUnit.SECONDS)) {
                    Log.info("pool terminated gracefully: " + poolInShutdown);
                } else {
                    Log.warn("pool shutdown timed out: " + poolInShutdown);
                }
            } catch (InterruptedException e) {
                Log.error("awaiting termination", e);
            }
        }
        
        private synchronized void createAndLoadNewPool(int size) {
            if (size < 1) {
                Log.error("createAndLoadNewPool, bad size " + size);
                size = 1;
            }
            _pool = createThreadPool(size);

            loadTasks(_deferredTasks);

            _deferredTasks = null;
        }

        private synchronized void loadTasks(Collection<Runnable> tasks) {

            Log.info("RESUBMIT: " + Util.tags(tasks));
            //Util.dump(_deferredTasks);
            for (Runnable r : tasks) {
                //_pool.submit(r); // java 1.6 impl
                _pool.execute(r); // java 1.5 impl
            }
        }

        public synchronized void addTask(Runnable r) {
            if (_pool == null) {
                Log.info("deferred " + r);
                _deferredTasks.add(r);
            } else {
                // _pool.submit(r); // java 1.6 impl
                _pool.execute(r); // java 1.5 impl
            }
        }

        /** re-load the queue as the priority mechanism has changed -- any IconTasks need to be sorted to the front */
        private synchronized void resortQueue() {
            final BlockingQueue q = _pool.getQueue();

            if (q.size() > 1) {
                Log.info("resorting queue " + Util.tags(q));
                final Collection<Runnable> qlist = new ArrayList(q.size());
                q.drainTo(qlist);
                loadTasks(qlist);
            } else {
                Log.info("queue doesn't need re-sorting: " + Util.tags(q));
            }
        }
        
        public synchronized void shrinkIfPossible(boolean firstLowMemory)
        {
            if (_nextSmallerPoolSize < 1) {
                if (DELAYED_ICONS && firstLowMemory)
                    resortQueue();
                return;
            }

            if (_pool != null) {
                // if EOM's stack up, we may try and shrink while
                // already waiting for a shrink, and _pool will be null
        
                final List<Runnable> dequeued;
                if (false) {
                    dequeued = new ArrayList();
                    TaskQueue.drainTo(dequeued);
                    Log.debug("shutdown...");
                    _pool.shutdown();
                } else {
                    Log.debug("shutdownNow...");
                    dequeued = _pool.shutdownNow();
                }
                Log.info("back from shutdown, drained=" + Util.tags(dequeued));
                if (DEBUG.Enabled) Util.dump(dequeued);
                if (_deferredTasks == null) {
                    _deferredTasks = new ArrayList(dequeued);
                } else {
                    _deferredTasks.addAll(dequeued);
                }

                final ExecutorService oldPool = _pool;
                _pool = null;
                forkAndWaitForTermination(oldPool);

                // note: will not need to re-sort queue: is automatically resorted when
                // _deferredTasks is re-loaded
                
            } else {

                reduceNextPoolSize();

                if (DELAYED_ICONS && firstLowMemory)
                    resortQueue();
                
                Log.warn("stacked OutOfMemoryError conditions: next pool size reduced to " + _nextSmallerPoolSize);
            }
        }
    }

    private static final ImmediatelyReducablePool ProcessingPool;

    private static BlockingQueue<Runnable> TaskQueue;
    
    private static final int ImageThreadPriority;

    private static final ThreadFactory ImageThreadFactory = new ThreadFactory() {
            private int count = 0;
            public Thread newThread(Runnable r) {
                final Thread it = new Thread(r);
                try {
                    it.setPriority(ImageThreadPriority);
                } catch (Throwable t) {
                    Log.warn("failed to set image thread priority to " + ImageThreadPriority, t);
                }
                count++;
                if (DEBUG.Enabled)
                    it.setName("IPX-" + count);
                else
                    it.setName("imageProcessor-" + count);
                if (DEBUG.Enabled) Log.debug("created thread: " + it);
                return it;
            }
        };

    static {
        final int cores = Runtime.getRuntime().availableProcessors();
        final int useCores = DEBUG.SINGLE_THREAD ? 1 : cores;
        // rough test: on a 2-core laptop, our use-case came in at 1min v.s. 1:30min w/all cores in use
        // (all icons being generated)

        int priority = 1; // should be lowest

        try {
            priority = Thread.MIN_PRIORITY + Thread.NORM_PRIORITY / 2; 
            // in case NORM_PRIORITY access fails -- there was some case of this happening in Applets?
        } catch (Throwable t) {}

        ImageThreadPriority = priority; // for thread factory
        
        ProcessingPool = new ImmediatelyReducablePool(useCores);
    }

    private static ThreadPoolExecutor createThreadPool(int nThreads) {

        // Note: LinkedBlockingQueue is a FIFO queue.  An argument could be made that
        // LIFO would be better for drawing -- e.g., older requests may no longer be
        // needed on screen.  Example: a map is loading triggering tons of image loads.
        // A presentation is immediately started, requesting the the content at the
        // start of the presentation (which is already queued up with a LoadTask from
        // the original map paint) -- this content at the head of the presentation
        // should now get priority over other content waiting to load.  This could
        // include re-ordering the queue to move older items at the the head of a LIFO
        // queue to the higher-priority tail if a second request comes in for the same
        // content.  This might generally be supported through using a PriorityQueue /
        // PriorityBlockingQueue.

        // Tho note that with an updating-on-request PriorityQueue, there could be lots of queue
        // shuffling during repaints of maps with lots unloaded images -- the entire queue could be
        // rotated front to back to on each repaint as every unloaded image is re-requested.

        // A wrapped LinkedBlockingDequeue forced to LIFO may do the trick

        // An implementaiton that maximally adressed user concerns would record the
        // canvas object drawn to (e.g., MapViewer object: the full-screen v.s. standard
        // map instances, etc) for each desired representation, and an API call for the
        // application to report the current priority canvas (e.g., when a MapViewer
        // gets application focus).  Within the priority canvas items, the most recently
        // desired reps would take priority (e.g., the most recently requested content
        // to be drawn to the full-screen viewer when in presentation mode has
        // priority). As ImageRep's won't re-poll the cache (call getImage) if their
        // alreadly loading (and that would be messy to enforce) this would require
        // coordination with the ImageRef's desired reps each time they're drawn.  This
        // would probably be most simply done by changing ImageRef's to singleton
        // instances that are stored in the cache.

        final ThreadPoolExecutor pool;

        if (true) {
            pool = new PriorityThreadPool(nThreads);
        } else {
            pool = new ThreadPoolExecutor(nThreads, nThreads,
                                          0L, TimeUnit.MILLISECONDS,
                                          /*TaskQueue =*/ new LinkedBlockingQueue<Runnable>(),
                                          ImageThreadFactory);
        }

        Log.info("created thread pool: " + Util.tags(pool) + "; maxSize=" + pool.getMaximumPoolSize());
        
        return pool;
    }

    private static class PriorityThreadPool extends ThreadPoolExecutor {

        private int lowMemoryRepaints;
        
        PriorityThreadPool(int nThreads) {
            super(nThreads, nThreads,
                  0L, TimeUnit.MILLISECONDS,
                  new PriorityBlockingQueue<Runnable>(),
                  ImageThreadFactory);
        }

//         /** This method was added to AbstractExecutorService in Java 1.6 -- is not available in java 1.5 */
//         @Override protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
//             if (runnable instanceof RunnableFuture) {
//                 if (DEBUG.Enabled) Log.debug("resubmit " + runnable);
//                 return (RunnableFuture) runnable; // for re-submits
//             } else {
//                 if (DEBUG.IMAGE) Log.debug("newTaskFor runnable " + Util.tags(runnable));
//                 return new PriorityTask((Loader)runnable);
//             }
//         }

//         /** This method was added to AbstractExecutorService in Java 1.6 -- is not available in java 1.5 */
//         @Override protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
//             throw new UnsupportedOperationException("newTaskFor Callable; " + callable);
//         }

        @Override public Future<?> submit(Runnable task) {
            throw new Error("use execute; " + task);
        }
        @Override public <T> Future<T> submit(Runnable task, T result) {
            throw new Error("use execute; " + task);
        }
        @Override public <T> Future<T> submit(Callable<T> task) {
            throw new Error("use execute; " + task);
        }
        
        @Override public void execute(Runnable runnable) {
            if (runnable instanceof Loader) {
                if (DEBUG.Enabled) Log.debug("new-task " + runnable);
                super.execute(new PriorityTask((Loader)runnable));
            } else {
                if (DEBUG.Enabled) Log.debug("resubmit " + runnable);
                super.execute(runnable);
            }
        }
        
        @Override protected void afterExecute(Runnable r, Throwable t) {
            if (DEBUG.IMAGE) Log.debug("AFTER-EXECUTE " + Util.tags(r) + "; ex=" + t); // don't toString the task -- members flushed for GC
            // Only allow a few of these, otherwise we can get continuous looping failures if
            // memory becomes full enough.  This is mainly needed for recovery from the first
            // low-memory failure.
            //if (DEBUG.Enabled) Util.dump(Cache);
            if (lowMemoryRepaints < 3 && lowMemoryConditions() && !isShutdown() && getQueue().isEmpty()) {
                java.awt.Component v = VUE.getActiveViewer();
                if (v != null) {
                    Log.info("LOW-MEMORY-REPAINT " + lowMemoryRepaints);
                    v.repaint();
                    lowMemoryRepaints++;
                }
            }
            if (r instanceof Loader)
                ((Loader)r).flushForGC();
            
        }
    }


    /** A task that can have a priority, that defaults to FIFO if priorities are equal */
    private static final class PriorityTask extends FutureTask
        implements Comparable<PriorityTask>
    {
        final static AtomicLong seq = new AtomicLong();
        final long seqNum;
        final Loader loader; // is Loader just for getPriority -- could be a Comparable
        public PriorityTask(Loader loader) {
            super(loader, null);
            this.loader = loader;
            this.seqNum = seq.getAndIncrement();
        }
        public int compareTo(PriorityTask other) {
            if (!(other instanceof PriorityTask)) {
                Log.error("can't compare to " + Util.tags(other));
                return 0;
            }
            final int diff = other.priority() - priority();
            final int priority;
            if (diff == 0) {
                //priority = seqNum > other.seqNum ? -1 : 1; // follow LIFO sequence
                priority = seqNum > other.seqNum ? 1 : -1; // follow FIFO sequence
                // FIFO is better for VUE right now, as it gives us better
                // control over pre-caching when kicking off a presentation.
            } else {
                priority = diff; // follow task-type priority
            }
            return priority;
        }

        private int priority() {
            return loader.getPriority();
        }
        
        @Override public String toString() {
            return "PriorityTask[#" + seqNum + " " + loader + "]";
        }

    }

    // what was the problem with making the Loader a FutureTask itself?
    static abstract class Loader implements Runnable
    {
        CachingRelayer relay;
        ImageSource imageSRC; 

        Loader(ImageSource _imageSRC, Listener firstRelay) 
        {
            imageSRC = _imageSRC;
            relay = new CachingRelayer(imageSRC, firstRelay);
            if (firstRelay == null)
                Log.info(this + "; nobody currently listening: image may be quietly cached: " + imageSRC);
        }

        Image getLoaderImage() {
            return relay.image();
        }

        public void join() throws InterruptedException {
            if (DEBUG.Enabled) Log.debug(this + " has been joined...");
            Thread.currentThread().join();
        }

        public final void run() {
            try {
                runToResult();
            } catch (OutOfMemoryError eom) {
                setLowMemory(eom);
                Log.error("Uncaught EOM running " + this, eom);
            } catch (Throwable t) {
                Log.error("Uncaught Exception running " + this, t);
            }
        }

        private void flushForGC() {
            // assist GC -- may help it run slightly faster:
            imageSRC = null;
            relay.flushForGC();
//             relay.imageSRC = null;
//             relay.image = null; // this is the most important
//             //relay.ref = null;
            relay = null;
        }
        

        /** @return an Image that's already been put into the cache */
        private final Handle runToResult() {
            if (DEBUG.IMAGE) debugMark(">>>>>");
            final Handle h = produceResult();
            if (DEBUG.IMAGE) debugMark("<<<<<");
            return h;
        }

        private void debugMark(String s) {
            Log.debug(Util.TERM_YELLOW
                      + s + "---"
                      + getClass().getSimpleName()
                      + "---"
                      + imageSRC.debugName()
                      + "-----------------------------------------------------------------------------"
                      + Util.TERM_CLEAR
                      );
        }

        /** @return an Image that's already been put into the cache */
        Handle produceResult() {
            if (DEBUG.IMAGE || DEBUG.THREAD) out("loadAndCache kick: " + imageSRC);
            Handle h = loadImageAndCache(imageSRC, relay);
            if (DEBUG.IMAGE || DEBUG.THREAD) out("loadAndCache return: " + h);
            return h;
        }
        
        public void addListener(Listener newListener) {
            if (DEBUG.IMAGE) out(this + " addListener " + tag(newListener));
            relay.addListener(newListener);
        }

        public int getPriority() {
            return 5;
        }
        

        @Override public String toString() {
            return String.format("%s[%s ->%s]", getClass().getSimpleName(), imageSRC.debugName(), relay.chain.head);
        }

    }

    /** a marker class to differentiate from LoadThread */
    private static final class LoadTask extends Loader {
        LoadTask(ImageSource is, Listener relay) {
            super(is, relay);
        }
    }

    /** a task to generate an icon */
    private static final class IconTask extends Loader {
        IconTask(ImageSource is, Listener relay) {
            super(is, relay);
        }
        @Override Handle produceResult() {
            //if (DEBUG.IMAGE || DEBUG.THREAD) out("ICON-TASK for " + imageSRC + " kicked off");

            // At this point, there's a cache entry containing this IconTask, so no
            // other icon requestor in another thread will attempt to create an icon --
            // it will simply be chained up to the results of this Loader waiting for
            // callback.
            return createAndCacheIcon(relay, imageSRC);
        }
        public int getPriority() {
            // Note: if we change from normal to low-mem conditions with DELAYED_ICONS
            // enabled, the priority of IconTasks jumps from lowest to highest, so to
            // ensure that the next task pulled is an IconTask if there are any in the
            // queue, the queue will need to be re-sorted when memory conditions change.
            if (DELAYED_ICONS) {
                // A fancier impl that allows us to generate icons later, which provides faster initial map
                // painting and a better user experience under "normal" conditions (plenty of RAM), but
                // switches to the conservative method once any OutOfMemoryError is seen.  This is still
                // only a tradeoff for the the 1st time a map with images loads tho -- once icons are
                // generated everything starts up very fast -- faster than any previous version of VUE.
                // This impl would still need work: recovering from the "wall" that's hit when memory
                // runs low is much more complicated.
                return lowMemoryConditions() ? 10 : 1;
            } else {
                // In this impl we just give IconTask's the highest priority is so that
                // we can create the icon ASAP while original image is still in memory
                // (normall forced there via a hard-ref in an ImageSource).  We can't
                // normally toss the original full image until the icon is generated,
                // which puts a big strain on memory.
                return 10;
            }
        }
    }

    
    /** a marker class */
    private static final class ImgThread extends Thread {
        ImgThread(Runnable r, String name) {
            super(r, name);
        }
    }

    
    /**
     * A thread for loading a single image.  Images.Listener results are delievered
     * from this thread (unless the image was already cached).
     */
    private static final class LoadThread extends Loader {
        private static int LoaderCount = 0;
        private final Thread thread;

        /**
         * @param src must be any valid src *except* a Resource
         * @param resource - if this is tied to a resource to update with meta-data after loading
         */
        LoadThread(ImageSource imageSRC, Listener firstRelay) {
            super(imageSRC, firstRelay);
            thread = new ImgThread(this, String.format("ImgLoader-%02d", LoaderCount++));
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
        }

        public void join() throws InterruptedException {
            thread.join();
        }
        public void start() {
            //if (DEBUG.IMAGE) Log.debug("=======================================================");
            thread.start();
        }        
        public void stop() {
            thread.stop();
        }        
    }
    

    /**
     * This method has side effects to the cache.  At the end of this call, we know
     * there is *something* in the cache for the given imageSRC.key -- either we found
     * the image already there, or we found a Loader thread already started there, or we
     * put a new Loader into the cache and kicked it off, unless it was an immediate request,
     * (an anusual but supported case) in which case first the Loader will have been put in the
     * cache, and then the final result before we return.  
     *
     * This method also deals with cache cleanup: if an entry is found to be
     * empty (it has no disk file, and it's image has been GC'd), the entry
     * is removed.
     *
     * @return If an Image, we had the image in the cache immediately availble.  If a
     * Loader thread object, the image is loading, and we should become and additional
     * listener (if there is a listener given), or wait for the Loader to die to get
     * it's results.  If the special value IMAGE_LOADER_STARTED is returned, there is
     * nothing to do be done for now -- just wait for the callbacks to the listener if
     * one was provided.
     */
    private static Object getCachedOrKickLoad(ImageSource imageSRC, Images.Listener listener, boolean immediate)
    {
        Loader loader = null;

        synchronized (RawCache) {

            final Object entry = getCacheContentsWithAutoFlush(imageSRC);

            // if anything in the Cache, immediately return it.  Could
            // be the desired image, or an existing loader.

            if (entry != null)
                return entry;
        
            // Nothing was in the cache.  If we have a listener, handle
            // the image loading in another thread.  If we don't, return
            // null -- caller must handle that condition.
        
            if (imageSRC.key == null) Util.printStackTrace("attempting to load cache w/null key: " + imageSRC);
        
            // It's okay if listener is null: a CachingRelayer will still be created, in
            // the Loader, and listeners can be added later.

            // note: if imageSRC is a file, and it doesn't exist, we could fail-fast
            // (perhaps if immediate is requested?), rather than wait for the error to
            // be reported via Listener.gotImageError, tho that's the standard API for
            // now.
        
            if (imageSRC.mayBlockIndefinitely()) {
                loader = new LoadThread(imageSRC, listener);
            } else if (imageSRC.isImageSourceForIcon()) {
                loader = new IconTask(imageSRC, listener);
            } else {
                loader = new LoadTask(imageSRC, listener);
            }
            
            // Note that even if we've been requested to run synchronously, we still want
            // a Loader created that can have listeners added later, and have it marked in the
            // cache, in case subsequent asynchronous requests come in for this image, or
            // even future immediate requests, which then won't be honored: they'll get a
            // callback when the previous immediate load finishes.
            
            markCacheAsLoading(imageSRC.key, loader);

        }
        //-------------------------------------------------------
        // CACHE LOCK IS RELEASED
        //-------------------------------------------------------

        if (listener == null || immediate) {
            // It's crucial that this NOT be run in a Cache-lock, or
            // every other image thread will soon hang until this is
            // done, including the AWT thread if anything requests
            // image data.

            // runToResult should always return an image that's been loaded into the cache
            return loader.runToResult();
            
        } else {
            kickTask(loader);
            return IMAGE_LOADER_STARTED;
        }
    }

    private static void markCacheAsLoading(URI key, Loader loader) {
        final Object old = RawCache.put(key, loader); // todo: under what conditiions is this normal?
        if (old != null && old instanceof CacheEntry && !((CacheEntry)old).isPreloadedDiskEntry()) {
            Log.warn("blew away existing cache content:\n\told: " + Util.tags(old) + "\n\tfor: " + Util.tags(loader));
        }
    }

    private static void kickTask(Loader loader)
    {
        if (loader instanceof LoadThread) {
            // could submit to a special pool or some future fancy non-blocking NIO multi-stream handler
            ((LoadThread)loader).start();
        } else {
            ProcessingPool.addTask(loader);
        }
    }

    /**
     * Get the cache contents for the given source (either an Image or
     * a Loader), and update the cache entry if needed. This should
     * only be called from within a Cache lock.  Will return null if
     * no cache entry was found, or one was found but it's contents
     * had been garbage collected.  In the latter case, the empty
     * entry will be flushed from the cache.
     */
    private static Object getCacheContentsWithAutoFlush(ImageSource imageSRC)
    {
        if (imageSRC.key == null)
            return null;
        
        final Object entry = RawCache.get(imageSRC.key);

        if (entry == null)
            return null;
        
        if (DEBUG.IMAGE) out("found cache entry for key " + tag(imageSRC.key) + ": " + entry);
                
        if (entry instanceof Loader) {
            if (DEBUG.IMAGE) out("Image is loading into the cache via already existing Loader...");
            return entry;
        }

        // Entry is not a Loader, so it must be a regular CacheEntry We still may not
        // have an image tho: it may have be been garbage collected, or the entry may
        // actually be for a file on disk.

        final CacheEntry ce = (CacheEntry) entry;
        final Image cachedImage = ce.getCachedImage();
                     	
        // if we have the image, we're done (it was loaded this runtime, and not GC'd)
        // if not, either it was GC'd, or it's a cache file entry from the persistent
        // cache -- in either case, there is a file on disk -- mark it in the imageSRC,
        // and the loader will notice it and use it.
                    
        boolean emptyEntry = true;

        if (cachedImage != null) {
            emptyEntry = false;
        } else if (ce.getFile() != null) {
            if (ce.file.canRead()) {
                imageSRC.setCacheFile(ce.file); // Note: imageSRC side effect
                emptyEntry = false;
            } else
                Log.warn("cache file no longer available: " + ce.file);
        }

        if (emptyEntry) {
            // there is a cache entry with no image OR file: this could only
            // happen if the disk cache is not operating, and the memory
            // image was garbage collected: we need to remove this entry
            // from the cache completely and start from scratch:
            if (DEBUG.IMAGE) out("REMOVING FROM CACHE: " + imageSRC);
            RawCache.remove(imageSRC.key);
        }

        if (cachedImage == null)
            return null;
        else
            return new Handle(cachedImage, ce.data);
        
        //return cachedImage;

        // If cachedImage is null at this point, there was an entry in the cache, but it was of no use:
            
        // That happens in the following cases:
        // (1) We had the image, but is was GC'd -- we're going back to the cache file
        // (2) We had the image, but is was GC'd -- original was on disk: go back to that
        // (3) We had the image, but is was GC'd, and disk cache not working: reload original from network
        // (4) We never had the image, but it is in disk cache: go get it
        // (5) unlikely case case of zealous GC: reload original

        // Note that original image sources that were on disk are NOT moved
        // to the disk cache, and CacheEntry.file should always be null for those.
    }

    private static final String NO_READABLE_FOUND = "No Readable";


    /** @return true if there's a cache entry for the given key.  The entry may be in any state:
     * an unloaded pre-registered disk cache entry, a loaded cache entry, or a loader in progress.
     */
    public static boolean hasCacheEntry(URI cacheKey)
    {
        return cacheKey != null && RawCache.get(cacheKey) != null;
    }
    
    private static Handle createAndCacheIcon(Listener listener, ImageSource iconSource)
    {
        final Image hardImage;
        boolean badReadable = false;

        if (iconSource.readable instanceof Image) {
            hardImage = (Image) iconSource.readable;
            // lose hard reference to the original image so it can be GC'd,
            // and be sure do this before we're start creating the icon, so if we
            // EOM or error it's already been cleared.  THIS IS CRUCIAL --
            // if we don't do this, recovering from OutOfMemoryError's when
            // generating icons is virtually impossible.
            iconSource.readable = null;

            //========================================================================================
            // TODO: OUTSTANDING PROBLEM:
            // If we hit EOM creating this icon, the icon-source readable is cleared, and
            // thus the icon ImageRep goes permanently bad -- it can't reconsititute.
            // Yet the icon ImageSource (can) has a ref to the full ImageRep, which it
            // could reconstitute, and then generate the image from, but then
            // the Icon ImageRep would need to listen first for the full callbacks,
            // then the the icon callbacks.  We could just try NOT clearing it here,
            // and let the ImageRep clear it only if it gets the image, but then we
            // can run lower on memory by leaving this hard-ref around...  Need to run tests.
            //
            // The best fix for this probably includes going all the way to keeping singleton
            // ImageRef's in the cache, perhaps only allow icons to be obtained through their
            // ImageRef.
            //========================================================================================
            
        } else if (iconSource.readable instanceof ImageRep) {
            hardImage = ((ImageRep) iconSource.readable).image();
        } else {
            badReadable = true;
            hardImage = null;
        }


        if (badReadable)
            throw new Error("iconSource had no image content in readable: " + iconSource);

        if (hardImage == null) {
            Log.warn("hard image GC'd before icon creation, forcing low-memory conditions (" + iconSource + ")");

            setLowMemory("GC-wanted-data");

            // REMOVE THE FAILED LOADER FROM THE CACHE
            RawCache.remove(iconSource.key); // todo: generally handle caching in our caller based on return value?
            
            // todo: nobody to catch this error and deliver gotImageError!
            //throw new OutOfMemoryError("full-rep was GC'd: forcing low memory conditions");
            if (listener != null) {
                listener.gotImageError(iconSource, OUT_OF_MEMORY);
                return null;
            }
        }
        

        final Dimension originalSize = new Dimension(hardImage.getWidth(null), hardImage.getHeight(null));
        
        final Handle iconImage =
            createIcon(hardImage, iconSource.iconSize);

        if (listener != null) {
            // note that we could init the handle with data containing he size of the source image,
            // but that info is only useful when we do NOT already have the full rep loaded,
            // and we only get here if we just created an icon from the full rep, so it's okay to leave
            // the image data blank in the delivered handle.  That data shouldn't even need to go into
            // the cache this runtime?  It should probably go there anyway just to be safe.
            listener.gotImage(iconSource, iconImage);
        }

        File cacheFile = null;
        try {
            cacheFile = makePermanentCacheFile(iconSource.key);
        } catch (Throwable t) {
            Log.error("creating cache file for " + iconSource, t);
        }
        // if for any reason the disk cache has failed, we can still create the CacheEntry with a null file
        RawCache.put(iconSource.key, new CacheEntry(iconImage, cacheFile));
        if (cacheFile != null && iconImage.image != null)
            cacheIconToDisk(iconSource.key, (RenderedImage) iconImage.image, cacheFile, originalSize);
        return iconImage;
    }

    private static boolean cacheIconToDisk(URI iconKey, RenderedImage image, File cacheFile, Dimension originalSize)
    {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");

        ImageWriter writer = null;

        int count = 1;
        while (writers.hasNext()) {
            ImageWriter i = writers.next();
            if (writer == null)
                writer = i;
            if (count > 1 || (DEBUG.IMAGE && DEBUG.IO)) {
                Log.debug("FOUND WRITER #" + count + "for PNG: " + Util.tags(i));
            }
            count++;
        }
        
        try {
            //File cacheFile = makePermanentCacheFile(iconKey);
            if (DEBUG.IMAGE||DEBUG.IO) Log.debug("writing " + cacheFile);
            if (writer != null) {
                writeWithComments(writer, image, cacheFile, originalSize);
            } else {
                Log.warn("unable to find writer for PNG; " + cacheFile);
                ImageIO.write(image, "png", cacheFile);
            }
            if (DEBUG.IMAGE||DEBUG.IO) Log.debug("  wrote " + cacheFile);
            return true;
        } catch (Throwable t) {
            Log.error("writing icon cache file " + iconKey, t);
            return false;
        }
    }

    /** see com.sun.imageio.plugins.png.PNGMetadata */
    private static final String PNG_META_DATA = "javax_imageio_png_1.0";
    private static final String GENERIC_META_DATA = IIOMetadataFormatImpl.standardMetadataFormatName;
    
    //private static final String VUE_META_DATA = GENERIC_META_DATA;
    
    private static final String VUE_IMAGE_META_KEY = "VueData";
    private static final String VUE_IMAGE_META_VALUE_PREFIX = "@(#)VUE-DATA/";
    

    private static void writeWithComments(ImageWriter writer, RenderedImage image, File file, Dimension originalSize)
        throws IOException
    {
        if (DEBUG.Enabled||DEBUG.IMAGE||DEBUG.IO) Log.debug("WRITER WRITE " + writer + "; " + file);
        
        //ImageWriteParam param = writer.getDefaultWriteParam();
        //Log.debug("DEFAULT WRITE PARAM: " + Util.tags(param));

        final ImageTypeSpecifier image_type = new ImageTypeSpecifier(image);
        final IIOMetadata meta_data = writer.getDefaultImageMetadata(image_type, null);

//         Log.debug("GOT META-DATA: " + Util.tags(meta_data));
        if (DEBUG.IMAGE && DEBUG.IO) {
            Log.debug(" FORMAT-NAMES: " + Arrays.asList(meta_data.getMetadataFormatNames()));
            Log.debug("NATIVE-FORMAT: " + meta_data.getNativeMetadataFormatName());
        }

        final org.w3c.dom.Node metaTree = meta_data.getAsTree(GENERIC_META_DATA);

        addData(metaTree, "cacheFile", file.getName());
        addData(metaTree, "sourceSize", String.format("%d,%d", originalSize.width, originalSize.height));

        meta_data.mergeTree(GENERIC_META_DATA, metaTree);

        if (DEBUG.Enabled) {
            //Log.debug("TREE: " + Util.tags(tree));
            //Log.debug("ATTRIBS: " + Util.tags(tree.getAttributes()));
            //Log.debug("CHILDREN: " + Util.tags(tree.getChildNodes()));
            //DocDump.dump(meta_data.getAsTree(GENERIC_META_DATA));
            DocDump.dump(meta_data.getAsTree(PNG_META_DATA));
        }
        
        final IIOImage io_image = new IIOImage(image, Collections.EMPTY_LIST, meta_data);

        final ImageOutputStream output = ImageIO.createImageOutputStream(file);

        writer.setOutput(output);
        writer.write(io_image);
        output.close();
    }

    private static void addData
        (final org.w3c.dom.Node tree,
         final String key,
         final String value)
        throws IOException
    {
        // note: we add a trailing newline for /usr/bin/what
        final String comment = String.format("%s%s: %s\n", VUE_IMAGE_META_VALUE_PREFIX, key, value); 
        final String keyword = VUE_IMAGE_META_KEY + "/" + key;

        // note: the keyword data is currently redundant (ignored by us when loading icons we've written out),
        // but its presence is required by the ImageIO meta-data API.
        
        final IIOMetadataNode text;
        final IIOMetadataNode textRoot;

        if (DEBUG.IO||DEBUG.IMAGE) Log.debug("addData; " + keyword + "=" + comment);

        if (false) {
            // use PNG-specific meta-data -- simpler, but can only be used with PNG's
            text = new IIOMetadataNode("tEXtEntry"); // PNG name
            text.setAttribute("keyword", keyword);
            text.setAttribute("value", comment);
            
            textRoot = new IIOMetadataNode("tEXt"); // PNG name
        } else {
            
            //-----------------------------------------------------------------------------
            // The most generic method -- we should be able to use this with image
            // formats beyond PNG, tho that's untested.  The meta-data format in the tree
            // (root node) should be IOMetadataFormatImpl.standardMetadataFormatName
            // ("javax_imageio_1.0").
            // -----------------------------------------------------------------------------
            
            text = new IIOMetadataNode("TextEntry"); // generic name
            text.setAttribute("keyword", keyword);
            text.setAttribute("value", comment);
            // required, but ignored in PNGMetadata:
            text.setAttribute("encoding", "ISO-8859-1");
            // required, but ignored in PNGMetadata as long as 1st 255 chars all ascii:
            text.setAttribute("language", ""); // apparently safest: empty means undefined
            text.setAttribute("compression", "none");
            
            textRoot = new IIOMetadataNode("Text"); // generic name
        }
        
        textRoot.appendChild(text);

        tree.appendChild(textRoot);
    }

    private static Map<String,Object> extractVueImageMetaData(IIOMetadata metaData) {
        //final org.w3c.dom.Node metaTree
        final IIOMetadataNode metaTree = (IIOMetadataNode) metaData.getAsTree(GENERIC_META_DATA);
        
        if (DEBUG.IMAGE && DEBUG.IO) {
            Log.debug(" FORMAT-NAMES: " + Arrays.asList(metaData.getMetadataFormatNames()));
            Log.debug("GOT META-DATA: " + Util.tags(metaData) + " for format " + GENERIC_META_DATA);
            DocDump.dump(metaTree);
            //DocDump.dump(data.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName));
            //DocDump.dump(data.getAsTree(PNG_FORMAT_NAME));
        }

        return extractVueImageMetaData(metaTree);
    }

    private static Map<String,Object> extractVueImageMetaData(IIOMetadataNode metaTree)
    {
        Map result = Collections.EMPTY_MAP;
        for (org.w3c.dom.Node n : Util.iterable(metaTree.getElementsByTagName("TextEntry"))) {
            //Log.debug("FOUND TEXT NODE " + Util.tags(n) + " " + Util.tags(n.getAttributes()));
            if (DEBUG.IO && DEBUG.DATA) Log.debug("FOUND TEXT NODE " + Util.tags(n));
            //Log.debug("FOUND TEXT " + n.getTextContent());
            for (org.w3c.dom.Node a : Util.iterable(n.getAttributes())) {
                if (DEBUG.IO && DEBUG.DATA) Log.debug("FOUND TEXT ATTRIB " + Util.tags(a));
                String text = a.getNodeValue();
                // we don't bother to check for the keyword=value attribute -- it's all in the value anyway
                if (text != null && text.startsWith(VUE_IMAGE_META_VALUE_PREFIX)) {
                    final String s, key, value;
                    s = text.substring(VUE_IMAGE_META_VALUE_PREFIX.length());
                    key = s.substring(0, s.indexOf(':'));
                    value = s.substring(key.length()+2, s.length()-1);
                    if (DEBUG.IO || DEBUG.DATA) Log.debug(String.format("VUE-DATA: key=%s value=%s", key, Util.tags(value)));

                    if ("cacheFile".equals(key)) {
                        // skip this one -- is not currently needed, and clutters up debug bigtime
                        continue;
                    }
                    
                    if (result == Collections.EMPTY_MAP)
                        result = new HashMap(4);
                    result.put(key, value);
                }
            }

        }
        return result;
    }
    
// Our test w/NODESET works in XML-INGEST w/NY-TIMES RSS feed, but not here...
//                     XPath xpath = XPathFactory.newInstance().newXPath();
//                     //String expression = "javax_imageio_1.0" + "/Text/TextEntry/VueMetaData";
//                     //String expression = "/javax_imageio_1.0" + "/Text";
//                     //String expression = "/javax_imageio_1.0" + "/Text/TextEntry";
//                     String expression = "/Text";
//                     //String expression = "/Text/TextEntry";

//                     Log.debug("Extracting " + Util.tags(expression) + " from " + Util.tags(metaTree));
                    
//                     // First, obtain the element as a node.
                    
//                     NodeList nodeSet = (NodeList) xpath.evaluate(expression,
//                     //Object nodeSet = xpath.evaluate(expression,
//                                                     metaTree,
//                                                     XPathConstants.NODESET); // NODE will only do 1 node
//                         //Log.debug("   Node: " + nodeValue);
                    
//                     Log.debug("NodeList: " + Util.tags(nodeSet) + "; size=" + nodeSet.getLength());
//                     //Log.debug("NodeResult: " + Util.tags(nodeSet));
                    
                    
                    
    
    public static Object getCacheLock() {
        return RawCache;
    }

    static class ImageException extends Exception { ImageException(String s) { super(s); }}
    static class DataException extends ImageException { DataException(String s) { super(s); }}

    public static final String OUT_OF_MEMORY = "Out of memory";

    
    /** An wrapper for readAndCreateImage that deals with exceptions, and puts successful results in the cache */
    // note: we don't actually need to know the second arg is a ListenerRelay -- it could just be a
    // Listener, tho it never is -- this is for clarity -- remember that the callbacks may be
    // happening to a chain of pending listeners, not just one.
    private static Handle loadImageAndCache(final ImageSource imageSRC, final Listener relay)
    {
        //Image image = null;
        Handle imageData = null;

        if (imageSRC.resource != null)
            imageSRC.resource.getProperties().holdChanges();

        try {
            imageData = readImageInAvailableMemory(imageSRC, relay);
        } catch (Throwable t) {
            
            if (DEBUG.IMAGE) Util.printStackTrace(t);

            RawCache.remove(imageSRC.key);
            
            if (relay != null) {
                String msg;
                boolean dumpTrace = false;
                if (t instanceof java.io.FileNotFoundException) {
                    msg = "Not Found: " + t.getMessage();
                }
                else if (t instanceof java.net.UnknownHostException) {
                    msg = "Unknown Host: " + t.getMessage();
                }
                else if (t instanceof java.lang.IllegalArgumentException && t.getMessage().startsWith("LUT has improper length")) {
                    // known java bug: many small PNG images fail to read (effects thumbshots)
                    msg = null; // don't bother to report an error
                }
                else if (t instanceof OutOfMemoryError) {
                    setLowMemory(t);
                    msg = OUT_OF_MEMORY;
                }
                else if (t instanceof ImageException) {
                    msg = (t.getMessage() == NO_READABLE_FOUND ? null : t.getMessage());
                }
                else if (t instanceof ThreadDeath) {
                    msg = "interrupted";
                }
                else if (t.getMessage() != null && t.getMessage().length() > 0) {
                    msg = t.getMessage();
                    dumpTrace = true;
                }
                else {
                    msg = t.toString();
                    dumpTrace = true;
                }

                // this is the one place we deliver caught exceptions
                // during image loading:
                relay.gotImageError(imageSRC.original, msg);

                if (dumpTrace)
                    Log.warn(imageSRC + ":", t);
                else
                    Log.warn(imageSRC + ": " + t);
            }

            if (imageSRC.resource != null)
                imageSRC.resource.getProperties().releaseChanges();
        }

        if (relay != null && imageData != null) {
            relay.gotImage(imageSRC.original, imageData);
        }

        //-----------------------------------------------------------------------------
        // If we were to auto-generate icons in Images, this would be the place to do it.
        //-----------------------------------------------------------------------------
        
        // TODO opt: if this item was loaded from the disk cache, we're needlessly
        // replacing the existing CacheEntry with a new one, instead of
        // updating the old with the new in-memory image buffer.

        if (imageSRC.useCacheFile()) {
            if (imageData != null) {
                File permanentCacheFile = null;
                if (imageSRC.hasCacheFile())
                    permanentCacheFile = ensurePermanentCacheFile(imageSRC.getCacheFile());
                
                if (DEBUG.IMAGE) out("getting cache lock for storing result; " + imageSRC);
                RawCache.put(imageSRC.key, new CacheEntry(imageData, permanentCacheFile));
                
                // If the cache file has moved from tmp to permanent, we'd need to do this to keep imageSRC
                // current, tho at the moment this is a bit overkill as we should no longer need imageSRC
                // at this point, but just in case it wants to update something such as a Resource with
                // a reference to the right cache file, we do this here.
                if (permanentCacheFile != null)
                    imageSRC.setCacheFile(permanentCacheFile);
                
//                 if (imageSRC.resource != null && permanentCacheFile != null)
//                     imageSRC.resource.setCacheFile(permanentCacheFile);
            }
        } else {
            if (imageData.image != null)
                RawCache.put(imageSRC.key, new CacheEntry(imageData, null));
            //RawCache.put(imageSRC.key, new CacheEntry(image, null));
        }

        return imageData;
        
    }

    private static Handle readImageInAvailableMemory(ImageSource imageSRC, Listener listener)
        throws ImageException
    {
        //Image image = null;
        Handle imageData = null;
        
        do {
            try {
                imageData = readAndCreateImage(imageSRC, listener);
            } catch (OutOfMemoryError eom) {
                setLowMemory(eom);
                Log.warn(Util.TERM_YELLOW + "out of memory reading " + imageSRC.readable + ": " + eom + Util.TERM_CLEAR);
                if (Thread.currentThread() instanceof ImgThread) {
                    Log.info("reader sleeping & retrying...");
                    try {
                        // Todo: sleep until all other image load threads have finished, or,
                        // ideally, until there aren't any running (those still running are
                        // blocked).  May actually just be easiest to to abort and re-submit this
                        // to entirely new execution queue, or could just stick it on the end of
                        // the existing queue, tho if this is a network IO thread, we still want to
                        // run it later in it's own thread, so it could just be a task the
                        // re-creates a new LoadThread
                        Thread.currentThread().sleep(1000 * 45);
                    } catch (InterruptedException e) {
                        Log.warn("reader interrupted");
                    }
                    Log.info(Util.TERM_GREEN + "reader re-awakened, retrying... " + imageSRC.readable + Util.TERM_CLEAR);
                } else { // if instanceof POOLTHREAD

                    // we must be running in the thread-pool accessing local disk

                    if (DEBUG.Enabled) Log.info("EOM; consider re-kicking a LoadTask for " + imageSRC);
                    //e.g., somehing like: kickLoad(imageSRC, listener);
                    
                    throw eom;
               }
            } catch (Throwable t) {
                throw new ImageException("reader failed: " + t.toString());
            }
        } while (imageData == null);

        if (DEBUG.IMAGE) out("readAndCreateImage got " + Util.tags(imageData));

        return imageData;
    }
    

    // todo: this probably wants to move to a resource impl class
    private static void setDateValue(Resource r, String name, long value) {
        if (value > 0)
            r.setProperty(name, new java.util.Date(value).toString());
        // todo: set raw value for compares, but allow prop displayer to convert it?
        // or put a raw Date object in there?
    }

    // todo: this probably wants to move to a resource impl class
    private static void setResourceMetaData(Resource r, java.net.URLConnection uc) {

//         r.getProperties().holdChanges();
//         try {
        
            long len = uc.getContentLength();
            //r.setProperty("url.contentLength", len);
            r.setProperty(CONTENT_SIZE, len); 
            r.setByteSize(len); // todo: update later from cache file size for correctness
            String ct = uc.getContentType();
            //r.setProperty("url.contentType", ct);
            r.setProperty(CONTENT_TYPE, ct);
            if (DEBUG.Enabled && ct != null && !ct.toLowerCase().startsWith("image")) {
                Log.warn("NON-IMAGE CONTENT TYPE [" + ct + "]; " + r);
            }
            setDateValue(r, "URL.expires", uc.getExpiration());
            //setDateValue(r, "url.date", uc.getDate());
            setDateValue(r, CONTENT_ASOF, uc.getDate()); // should probably ignore this an generate ourselves
            //setDateValue(r, "url.lastModified", uc.getLastModified());
            setDateValue(r, CONTENT_MODIFIED, uc.getLastModified());
            
//         } catch (Throwable t) {
//             Util.printStackTrace(t);
//         } finally {
//             r.getProperties().releaseChanges();
//         }
                      
    }
    
    // todo: this probably wants to move to a resource impl class
    private static void setResourceMetaData(Resource r, File f) {
//         r.getProperties().holdChanges();
//         try {
        
            //r.setProperty("file.size", f.length());
            r.setProperty(CONTENT_SIZE, f.length());
            //setDateValue(r, "file.lastModified", f.lastModified());
            setDateValue(r, CONTENT_MODIFIED, f.lastModified());

            //r.setProperty(CONTENT_TYPE, java.net.URLConnection.guessContentTypeFromName(f.getName()));
            // todo: also URLConnection.guessContentTypeFromStream (could use in FileBackedImageInputStream)
                
//         } finally {
//             r.getProperties().releaseChanges();
//         }
            
    }

    private static File makePermanentCacheFile(URI key)
        throws java.io.UnsupportedEncodingException
    {
        return makeCacheFile(key, false);
    }
        
    private static File makeTmpCacheFile(URI key)
        throws java.io.UnsupportedEncodingException
    {
        return makeCacheFile(key, true);
    }
    
    /** @param temporary -- for temporary cache files that have yet to complete (e.g., not all data has arrived)  */
    private static File makeCacheFile(URI key, boolean temporary)
        throws java.io.UnsupportedEncodingException
    {
        final String cacheName;
        if (temporary)
            cacheName = "." + keyToCacheFileName(key);
        else
            cacheName = keyToCacheFileName(key);
        
        File cacheDir = getCacheDirectory();
        File file = null;
        if (cacheDir != null) {
            file = new File(getCacheDirectory(), cacheName);
            try {
                if (!file.createNewFile()) {
                    if (DEBUG.IO) Log.debug("cache file already exists: " + file);
                }
            } catch (java.io.IOException e) {
                String msg = "can't create cache file: " + file;
                if (DEBUG.Enabled)
                    Util.printStackTrace(e, msg);
                else
                    Log.warn(msg, e);
                return null;
            }
            if (!file.canWrite()) {
                Log.warn("can't write cache file: " + file);
                return null;
            }
            if (DEBUG.IMAGE) out("got cache file " + file);
        }
        return file;
    }

    private static File ensurePermanentCacheFile(File file)
    {
        try {
            // chop off the initial "." to make permanent
            // If doesn't start with a dot, this is one of our existing cache
            // files: nothing to do.
            String tmpName = file.getName();
            String permanentName;
            if (tmpName.charAt(0) == '.') {
                permanentName = tmpName.substring(1);
            } else {
                // it's already permanent: we must have loaded this from our cache originally
                return file;
            }
            File permanentFile = new File(file.getParentFile(), permanentName);
            if (file.renameTo(permanentFile)) {
                Log.debug("new perm cache file: " + permanentFile);
                return permanentFile;
            }
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, "Unable to create permanent cache file from tmp " + file);
        }
        return null;
    }
    

    private static File CacheDir;
    private static File getCacheDirectory()
    {
        if (CacheDir == null) {
            File dir = VueUtil.getDefaultUserFolder();
            CacheDir = new File(dir, "cache");
            if (!CacheDir.exists()) {
                Log.debug("creating cache directory: " + CacheDir);
                if (!CacheDir.mkdir())
                    Log.warn("couldn't create cache directory " + CacheDir);
            } else if (!CacheDir.isDirectory()) {
                Log.warn("couldn't create cache directory (is a file) " + CacheDir);
                return CacheDir = null;
            }
            Log.debug("Got cache directory: " + CacheDir);
        }
        return CacheDir;
    }
//     private static final class ImageProps extends HashMap<String,?> {
//         ImageProps() {
//             super(6);
//         }
//     }

    public static final class Handle {
        public final Image image;
        final Map<String,?> data; // note: shouldn't expose this globally as modifiable
        Handle(Image i, Map<String,?> data) {
            if (i == null)
                throw new NullPointerException("null image");
            if (data == null)
                throw new NullPointerException("null data");
            this.image = i;
            this.data = data;
        }
        Handle(Image i) {
            this(i, Collections.EMPTY_MAP);
        }

//         private Handle() {
//             image = null;
//             data = Collections.EMPTY_MAP;
//         }

        //Handle create(image i
        
        @Override public String toString() {
            return Util.tags(image) + "; " + data;
        }
    }

    private static Image handleImage(Handle h) {
        return h == null ? null : h.image;
    }

    //private static Handle NULL_HANDLE = new Handle();

    /**
     * @param imageSRC - see ImageSource ("anything" that we can get an image data stream from)
     * @param listener - an Images.Listener: if non-null, will be issued a callback for when the size is first obtained
     * @return the loaded image, or null if none found
     */
    private static boolean FirstFailure = true;
    private static Handle readAndCreateImage(ImageSource imageSRC, Images.Listener listener)
        throws java.io.IOException, ImageException
    {
        if (DEBUG.IMAGE) out("trying: " + imageSRC);

        InputStream urlStream = null; // if we create one, we need to keep this ref to close it later
        File tmpCacheFile = null; // if we create a tmp cache file, it will be put here

        int dataSize = -1;
        
        if (DEBUG.IMAGE && imageSRC.resource != null) {
            imageSRC.resource.setDebugProperty("image.read", imageSRC.readable);
        }

        if (imageSRC.hasCacheFile()) {
            // just point us at the cache file: ImageIO will create the input stream
            imageSRC.readable = imageSRC.getCacheFile();
            if (DEBUG.IMAGE && imageSRC.resource != null) {
                imageSRC.resource.setDebugProperty("image.cache", imageSRC.getCacheFile());
                out("reading cache file: " + imageSRC.getCacheFile());
            }

            // note: can get away with this because imageSRC.resource will
            // be null if this is for a preview icon, so don't need to worry
            // about getting wrong size todo: a hack anyway -- include
            // in clean-up of meta-data setting

            if (imageSRC.resource != null) {
                imageSRC.resource.setProperty(CONTENT_SIZE, imageSRC.getCacheFile().length());
                // java has no creation date for Files!  Well, last modified good enough...
                setDateValue(imageSRC.resource, CONTENT_ASOF, imageSRC.getCacheFile().lastModified());
            }
        
        } else if (imageSRC.readable instanceof java.net.URL) {

            final URL url = (URL) imageSRC.readable;

            int tries = 0;
            boolean success = false;

            final boolean debug = DEBUG.IMAGE || DEBUG.IO;

            do {

                final URLConnection conn = UrlAuthentication.getAuthenticatedConnection(url);
                urlStream = conn.getInputStream();

                if (imageSRC.resource != null) {
                    dataSize = conn.getContentLength();
                    try {
                        setResourceMetaData(imageSRC.resource, conn);
                    } catch (Throwable t) {
                        // Don't fail if a problem with meta data: still give
                        // a chance for the content to work...
                        Util.printStackTrace(t, "URLConnection Meta Data Failure");
                        //imageSRC.resource.setProperty("MetaDataFailure", t.toString());
                    }
                }
                
                
                if (!imageSRC.useCacheFile()) {
                    
                    imageSRC.readable = urlStream;
                    success = true;
                    
                } else {

                    tmpCacheFile = makeTmpCacheFile(imageSRC.key);
                    imageSRC.setCacheFile(tmpCacheFile);  // will be made permanent if no errors
                    
                    if (imageSRC.hasCacheFile()) {
                        try {
                            imageSRC.readable = new FileBackedImageInputStream(urlStream, tmpCacheFile, listener);
                            success = true;
                        } catch (Images.DataException e) {
                            Log.warn(imageSRC + ": " + e);
                            if (++tries > 1) {
                                final String msg = "Try #" + tries + ": " + e;
//                                 if (DEBUG.Enabled)
//                                     Util.printStackTrace(msg);
//                                 else
                                Log.warn(msg);
                                throw e;
                            } else {
                                Log.info("second try for " + imageSRC);
                                urlStream.close();
                            }
                            // try the reconnect one more time
                        }
                    } else {
                        // unable to create cache file: read directly from the stream
                        Log.warn("Failed to create cache file " + tmpCacheFile);
                        imageSRC.readable = urlStream;
                        success = true;
                    }
                }
                
            } while (!success && tries < 2);

        } else if (imageSRC.readable instanceof java.io.File) {
            if (DEBUG.IMAGE) Log.debug("Loading local file " + imageSRC.readable);
            if (imageSRC.resource != null)
                setResourceMetaData(imageSRC.resource, (File) imageSRC.readable);
        }

        if (imageSRC.resource != null) { // in case any held changes
            //if (DEBUG.DR) imageSRC.resource.setDebugProperty("readsrc", Util.tags(imageSRC.readable));
            imageSRC.resource.getProperties().releaseChanges();
        }

        final ImageInputStream inputStream;

        if (imageSRC.readable instanceof ImageInputStream) {
            inputStream = (ImageInputStream) imageSRC.readable;
        } else if (imageSRC.readable != null) {
            //if (DEBUG.IMAGE) out("ImageIO converting " + tag(imageSRC.readable) + " to InputStream...");
            inputStream = ImageIO.createImageInputStream(imageSRC.readable);
        } else {
            throw new ImageException(NO_READABLE_FOUND);
            //Log.warn("not readable: " + imageSRC);
        }

        if (DEBUG.IMAGE) out("Got ImageInputStream " + inputStream);

        if (inputStream == null)
            throw new ImageException("Can't Access [" + imageSRC.readable + "]"); // e,g., local file permission denied

        ImageReader reader = getDecoder(inputStream, imageSRC);

        if (reader == null) {
            badStream: {
                if (FirstFailure) {
                    // This FirstFailure code was an attempt to deal with what is now handled
                    // via DataException, but it's not a bad idea to keep it around.
                    FirstFailure = false;
                    Log.warn("No reader found: first failure, rescanning for codecs: " + imageSRC);
                    // TODO: okay, problem appears to be with the URLConnection / stream? Is only
                    // getting us tiny amount of bytes the first time...
                    if (DEBUG.Enabled) tufts.Util.printStackTrace("first failure: " + imageSRC);
                    ImageIO.scanForPlugins();
                    reader = getDecoder(inputStream, imageSRC);
                    if (reader != null)
                        break badStream;
                }
                if (DEBUG.IMAGE) out("NO IMAGE READER FOUND FOR " + imageSRC);
                throw new ImageException("Unreadable Image Stream");
            }
        }

        if (DEBUG.IMAGE) out("Chosen ImageReader for stream: " + reader + "; format=" + reader.getFormatName());
        
        //reader.addIIOReadProgressListener(new ReadListener());
        //out("added progress listener");

        final boolean allowSeekBack = false;
        final boolean ignoreMetadata;

        if (imageSRC.isDiskCacheEntry()) {
            ignoreMetadata = false;
        } else {
            // Note that basic meta-data is still present on the resulting image when we
            // say to ignore it, tho extra meta-data we've added won't be there.  This
            // may just be because it's creating it from scratch for the newly built
            // image instead of reading it from the on-disk image -- not sure.
            ignoreMetadata = true;
        }
        
        reader.setInput(inputStream, allowSeekBack, ignoreMetadata);
        if (DEBUG.IMAGE) out("Input for reader set to " + inputStream);
        if (DEBUG.IMAGE) out("Getting size...");
        int w = reader.getWidth(0);
        int h = reader.getHeight(0);
        if (DEBUG.IMAGE) out("ImageReader got size " + w + "x" + h);

        if (w == 0 || h == 0)
            throw new ImageException("invalid size: width=" + w + "; height=" + h);

        if (imageSRC.resource != null) {
            if (DEBUG.IMAGE || DEBUG.THREAD || DEBUG.RESOURCE)
                out("setting resource image.* meta-data for " + imageSRC.resource);
            
            imageSRC.resource.getProperties().holdChanges();
            imageSRC.resource.setProperty(Resource.IMAGE_WIDTH,  Integer.toString(w));
            imageSRC.resource.setProperty(Resource.IMAGE_HEIGHT, Integer.toString(h));
            imageSRC.resource.setProperty(Resource.IMAGE_FORMAT, reader.getFormatName());
            imageSRC.resource.setCached(true);

            // Note: If MetaDataPane is not carefully coded, this call
            // can lead to a DEADLOCK against the AWT thread (e.g.,
            // entering PropertyMap.removeListener)
            imageSRC.resource.getProperties().releaseChanges();
        }

        if (listener != null) {
            if (DEBUG.IMAGE) out("Sending size to " + tag(listener));
            listener.gotImageSize(imageSRC.original, w, h, dataSize, null);
        }

        // FYI, if fetch meta-data, will need to trap exceptions here, as if there are
        // any problems or inconsistencies with it, we'll get an exception, even if the
        // image is totally readable.
        //out("meta-data: " + reader.getImageMetadata(0));

        //-----------------------------------------------------------------------------
        // Now read the image, creating the BufferedImage (or otherwise)
        // 
        // Todo performance: using Toolkit.getImage on MacOSX gets us OSXImages, instead
        // of the BufferedImages which we get from ImageIO, which are presumably
        // non-writeable, and may perform better / be cached at the OS level.  This of
        // course would only work for the original java image types: GIF, JPG, and PNG.
        //-----------------------------------------------------------------------------

        if (DEBUG.Enabled) out("reading " + imageSRC + "; " + reader + "...");
        
        Image image = null;
        Map<String,Object> imageData = Collections.EMPTY_MAP;
        Throwable exception = null;

        try {
            image = reader.read(0);
            if (DEBUG.Enabled) out("    got " + imageSRC + ".");
            //testImageInspect(reader, image, imageSRC);

            //========================================================================================
            //========================================================================================
            //========================================================================================
            // IF LISTENER IS NON-NULL, then fetch full-size, only to make a callback
            // analogue to gotImageSize.  Tho would be better to stuff this info into
            // the cache, AS, OH, CRAP, WE NEED TO STUFF THIS INFO INTO THE CACHE....
            // Subsequent callers, after load, are still going to need the full pixel
            // size.
            //========================================================================================
            //========================================================================================
            //========================================================================================

            if (listener != null && !ignoreMetadata) {
                // look for meta-data we may have written with the image:
                
                // Note that arbitrary image meta-data created by god knows what app may
                // at any time be considered "bad" according to java, and cause an
                // exception to be thrown here.  E.g., reading a random JPG:
                // "javax.imageio.IIOException: ICC APP2 encountered without prior
                // JFIF!"  thrown from com.sun.imageio.plugins.jpeg.JPEGMetadata.

                // If we successfully wrote the meta-data ourselves, we should be pretty
                // darn safe tho.

                try {
                    // note: the meta-data should have been cached by the reader -- it's
                    // normally stored & read first
                    final IIOMetadata data = reader.getImageMetadata(0);

                    imageData = extractVueImageMetaData(data);

                    String sourceSize = (String) imageData.get("sourceSize");

                    if (sourceSize != null) {
                        if (DEBUG.IO) Log.debug("FOUND SOURCE SIZE[" + sourceSize + "]");
                        String[] dim = sourceSize.split(",");
                        int sw = Integer.parseInt(dim[0]);
                        int sh = Integer.parseInt(dim[1]);
                        if (DEBUG.IO || DEBUG.IMAGE) Log.debug(String.format("parsed meta-data source size: %dx%d", sw, sh));
                        //imageData.put("sourceWidth", w);
                        //imageData.put("sourceHeight", h);
                        //listener.gotImageUpdate(Progress.SOURCE_SIZE, null); // NEED TO PASS IMAGE WIDTH/HEIGHT
                        int[] srcSize = new int[2];
                        srcSize[0] = sw;
                        srcSize[1] = sh;
                        imageData.put("sourcePixels", srcSize);
                        // we could deliver this, but we'll eventually get it with the image
                        //listener.gotImageSize(imageSRC.original, w, h, dataSize, srcSize);
                    } else {
                        Log.warn("failed to find sourceSize in meta-data: " + imageData + " for " + imageSRC);
                    }

                } catch (Throwable t) {
                    Log.error("loading meta-data for " + imageSRC, t);
                }
            }
            
        } catch (Throwable t) {
            exception = t;
        } finally {
            reader.reset();
            inputStream.close();
            if (urlStream != null)
                urlStream.close();
        }


        if (exception instanceof OutOfMemoryError) {
            setLowMemory(exception);
            throw (Error) exception;
        } else if (exception != null) {
            throw new ImageException("reader.read(0) failure: " + exception);
        }

        return new Handle(image, imageData);
    }


    private void testImageInspect(ImageReader reader, Image image, ImageSource imageSRC) {
        try {
            int thumbs = reader.getNumThumbnails(0);
            if (thumbs > 0)
                Log.info("thumbs: " + thumbs + "; " + imageSRC);
        } catch (Throwable t) {
            Log.debug("getNumThumbnails", t);
        }
        if (DEBUG.WORK && image != null) {
            String[] tryProps = new String[] { "name", "title", "description", "comment" };
            for (int i = 0; i < tryProps.length; i++) {
                Object p = image.getProperty(tryProps[i], null);
                if (p != null && p != java.awt.Image.UndefinedProperty)
                    System.err.println("FOUND PROPERTY " + tryProps[i] + "=" + p);
            }
        }
        
    }

    static Handle createIcon(Image source, int maxSide) {

        final Handle icon;

//         if (USE_SCALED_INSTANCE) {
//             return producePlatformIcon(source, maxSide);
//         } else {
//             return produceDrawnIcon(source, maxSide);
//         }
        if (DEBUG.Enabled) Log.debug("image pre-scaled: " + Util.tags(source) + " -> toMaxSide " + maxSide);
        icon = produceDrawnIcon(source, maxSide);
        if (DEBUG.Enabled) Log.debug("icon post-scaled: " + icon);

        return icon;
    }
    
    public static java.awt.Dimension fitInto(int maxSide, int srcW, int srcH) {
        int width, height;

        if (srcW > srcH) {
            width = maxSide;
            height = srcH * maxSide / srcW;
            // todo: ensure precision in above, and round to nearest EVEN value (for memory alignment)
        } else {
            height = maxSide;
            width = srcW * maxSide / srcH;
            // todo: ensure precision in above, and round to nearest EVEN value (for memory alignment)
        }

        //Log.debug(String.format("%dx%d -> (%d) %dx%d", srcW, srcH, maxSide, width, height));

        return new java.awt.Dimension(width, height);
    }

    public static tufts.vue.Size fitInto(float maxSide, int[] srcSize) {
        float srcW = srcSize[0];
        float srcH = srcSize[1];

        final float width, height;

        if (srcW < 1 || srcH < 1) {
            Log.error("fitInto " + maxSide + ": bad source size " + srcW + "x" + srcH);
            // make sure we never generate NaN results
            if (srcW < 1)
                srcW = 16;
            if (srcH < 1)
                srcH = 16;
        }

        if (srcW > srcH) {
            width = maxSide;
            height = srcH * maxSide / srcW;
        } else {
            height = maxSide;
            width = srcW * maxSide / srcH;
        }

        return new tufts.vue.Size(width, height);
    }
    

    private static Handle produceDrawnIcon(Image source, int maxSide)
    {
        final java.awt.Dimension size = fitInto(maxSide, source.getWidth(null), source.getHeight(null));

        final Image iconSource;
        int transparency;

        if (source instanceof BufferedImage)
            transparency = ((BufferedImage)source).getTransparency();
        else
            transparency = Transparency.OPAQUE;

        if (ALLOW_HIGH_QUALITY_ICONS /*&& DrawContext.isImageQualityRequested()*/) {
            // this is clever: we can still make use of the high-quality image
            // smoothing (tho it's still quite slow) by copying out the quality
            // image data then immediately flusing the created image.  And with our
            // old 1GB busting use-case we're still down at 82MB after GC cool-down.
            // Amazing.
            iconSource = source.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);

            if (iconSource instanceof sun.awt.image.ToolkitImage) { // will probably always be a ToolkitImage
                // generally neeeded in case the the image has alpha, so transparency will be Transparency.TRANSLUCENT
                // This will normally have already been pulled from the BufferedImage source, but just in
                // case the source wasn't a BufferedImage:
                transparency = ((sun.awt.image.ToolkitImage)iconSource).getColorModel().getTransparency();
            }
                
            // note: there are supposed to be faster methods available for generating
            // similar quality that involve multi-pass down-scaling that have been
            // documented, tho the benchmarks don't likely include a comparison to
            // the most recent Mac OS X Java 1.6 implementation, that may use
            // CoreImage underneath. See:
            // http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
                
        } else {
            iconSource = source;
        }

            
        final Image icon = tufts.vue.gui.GUI.getDeviceConfigForWindow(null)
            .createCompatibleImage(size.width, size.height, transparency);

        final Graphics2D g = (Graphics2D) icon.getGraphics();
            
        if (iconSource == source) {
            // The source is the original raw image -- we'll be down-scaling during the
            // drawImage, so quality is going to be low.  Note: setImageQuality is not much
            // help (if any?) as to quality in Java 1.6 (used to make a big difference in java
            // 1.5), but we try anyway.
            DrawContext.setImageQuality(g, true);
        }

        g.drawImage(iconSource, 0, 0, size.width, size.height, null);

        if (iconSource != source)
            iconSource.flush(); // crucial to release the memory consumed
        
        return new Handle(icon); // note: could load with source size info, but not needed in this case
    }

    private static Image producePlatformIcon(Image source, int maxSide)
    {
        final Dimension size = fitInto(maxSide, source.getWidth(null), source.getHeight(null));

        final Image icon;

        // Note that on Mac OS X this returns an apple.awt.OSXImage's, and does so
        // IMMEDIATELY, which means they will later hang the paint thread the first time
        // they're painted (and your entire application) as the image production
        // (scaling/smoothing) takes place in lazy fashion at the last possible moment.

        // Note that whatever's returned will also probably always be an instance of
        // sun.awt.image.ToolkitImage (as OSXImage is), but how the various scaling
        // hints are handled, when/if they're lazy evaluated, etc is going to vary by
        // platform.

        // SCALE_SMOOTH produces dramatically higher quality than SCALE_FAST on Mac,
        // but is significantly slower
        icon = source.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
            
        // On Mac OSX, immediately drawing the image to a scratch buffer will force
        // the loading (and the actual scaling) of the apple.awt.OSXImage

//         if (false) {
//             // (would we have wanted to render icon, not _image?)                
//             ScratchGraphics.drawImage(source, 0, 0, size.width, size.height, null); 
//         }
            
        // This doesn't appear to be having a big impact on memory -- OSXImage is
        // apparently holding a reference to the underlying content.  Even if all
        // images are read & scaled sequentially, we run out of memory (bumping
        // against 1GB using our test case).

        return icon;
    }
//     private static final Image ScratchImage;
//     private static final Graphics2D ScratchGraphics;

//     static {
//         if (USE_SCALED_INSTANCE) {
//             ScratchImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
//             ScratchGraphics = (Graphics2D) ScratchImage.getGraphics();
//         } else {
//             ScratchImage = null;
//             ScratchGraphics = null;
//         }
//     }

    
    public static void dumpImage(Image image, String debug) {

        Object g;
        try {
            g = image.getGraphics();
        } catch (Throwable t) {
            g = t;
        }
        Object s;
        try {
            s = image.getSource();
        } catch (Throwable t) {
            s = t;
        }
        
        Log.debug("Image " + debug + ": " + Util.tags(image)
                  + "\n\t    size: " + image.getWidth(null) + "x" + image.getHeight(null)
                  + "\n\t  source: " + Util.tags(s)
//                  + "\n\tgraphics: " + Util.tags(g)
//                   + "\n\taccelPri: " + image.getAccelerationPriority()
//                   + "\n\t    caps: " + image.getCapabilities(null)
                  );
        
        //Util.dump(image.getClass().getMethods());
    }

    private static ImageReader getDecoder(ImageInputStream istream, ImageSource imageSRC)
    {
        java.util.Iterator iri = ImageIO.getImageReaders(istream);

        ImageReader reader = null;
        int idx = 0;
        while (iri.hasNext()) {
            final ImageReader ir = (ImageReader) iri.next();

            String formatName;
            try {
                formatName = ir.getFormatName();
            } catch (Throwable t) {
                formatName = "[" + t + "]";
            }
            
            if (reader == null) {
                reader = ir;
            }
//             else if ("ico".equalsIgnoreCase(formatName)) {
//                 try {
//                     if (imageSRC.key.toString().toLowerCase().endsWith(".ico")) {
//                         if (DEBUG.IMAGE) out("CHOOSING ICO READER FOR .ICO");
//                         reader = ir;
//                     }
//                 } catch (Throwable t) {
//                     Log.error("getDecoder: " + imageSRC + " " + t);
//                 }
//             }
            

            if (DEBUG.IMAGE) {
                out("\t found ImageReader #" + idx + ": " + Util.tags(ir)
                    + "; format=" + formatName
                    + "; provider=" + Util.tags(ir.getOriginatingProvider()));
            }
            idx++;
        }

        return reader;
    }
    
    // nl.ikarus.nxt.priv.imageio.icoreader.lib.ICOReaderSpi.registerIcoReader(); // not needed: is self registering or SPI providing
    // nl.ikarus.nxt.priv.imageio.icoreader.lib.ICOReader [this reader, from ICOReader-1.04.jar, appears to mostly work]

    /* Apparently, not all decoders actually report to the listeners, (e.g., TIFF), so we're not using this for now */
    private static class ReadListener implements IIOReadProgressListener {
        public void sequenceStarted(ImageReader source, int minIndex) {
            out("sequenceStarted; minIndex="+minIndex);
        }
        public void sequenceComplete(ImageReader source) {
            out("sequenceComplete");
        }
        public void imageStarted(ImageReader source, int imageIndex) {
            out("imageStarted; imageIndex="+imageIndex);
        }
        public void imageProgress(ImageReader source, float pct) {
            out("imageProgress; "+(int)(pct + 0.5f) + "%");
        }
        public void imageComplete(ImageReader source) {
            out("imageComplete");
        }
        public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex){}
        public void thumbnailProgress(ImageReader source, float percentageDone) {}
        public void thumbnailComplete(ImageReader source) {}
        public void readAborted(ImageReader source) {
            out("readAborted");
        }
    }

    private static String tag(Object o) {
        if (o instanceof java.awt.Component)
            return tufts.vue.gui.GUI.name(o);
        else if (o instanceof LWComponent)
            return o.toString();
        //return ((LWComponent)o).getDiagnosticLabel();
        else
            return Util.tags(o);
        
//         String s = Util.tag(o);
//         s += "[";
//         if (o instanceof Thread) {
//             s += ((Thread)o).getName();
//         } else if (o instanceof BufferedImage) {
//             BufferedImage bi = (BufferedImage) o;
//             s += bi.getWidth() + "x" + bi.getHeight();
//         } else if (o != null)
//             s += o.toString();
//         return s + "]";
    }
    
    private static void out(Object o) {
        Log.debug(o);
        //Log.debug((o==null?"null":o.toString()));

        /*
        String s = "Images " + (""+System.currentTimeMillis()).substring(8);
        s += " [" + Thread.currentThread().getName() + "]";
        System.err.println(s + " " + (o==null?"null":o.toString()));
        */
    }

    
    /*
    private static void copyStreamToFile(InputStream in, File file)
        throws java.io.IOException
    {
        ByteBuffer buf = ByteBuffer.allocate(2048);
        FileOutputStream fout = new FileOutputStream(file);
        FileChannel fcout = fout.getChannel();
        ReadableByteChannel chin = Channels.newChannel(in);

        while (true) { 
            buf.clear(); 
            int r = chin.read(buf);
            //out("read " + r + " bytes");
            if (DEBUG.IMAGE) System.err.print(r + "; ");
            if (r == -1)
                break; 
            buf.flip(); 
            fcout.write(buf);
        }

        fcout.close();
        chin.close();
        if (DEBUG.IMAGE) out("\nFILLED " + file);
    }

    
    private static File cacheURLContent(URL url, InputStream in)
        throws java.io.IOException
    {
        File file = getCacheFile(url);
        copyStreamToFile(in, file);
        return file;
    }
    */

    /*
    static {
         // ImageIO file caching is a runtime-only scheme for allowing
         // file streams to seek backwards: nothing to with a presistant store.
         // It's also on by default.
         javax.imageio.ImageIO.setUseCache(true);
         javax.imageio.ImageIO.setCacheDirectory(new java.io.File("/tmp"));
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
        java.io.File file = null;
        java.net.URL url = null;
        Object imageSRC;
        if (args[0].startsWith("http:") || args[0].startsWith("file:"))
            imageSRC = url = new java.net.URL(filename);
        else
            imageSRC = file = new java.io.File(filename);

        DEBUG.IMAGE=true;

////getImage(imageSRC, new LWImage()); new Impl no longer an Images.Listener
        //loadImage(imageSRC, null);


        /*
          
        ImageInputStream iis = ImageIO.createImageInputStream(imageSRC);

        out("Got ImageInputStream " + iis);
        
        java.util.Iterator i = ImageIO.getImageReaders(iis);

        ImageReader IR = null;
        int idx = 0;
        while (i.hasNext()) {
            ImageReader ir = (ImageReader) i.next();
            if (IR == null)
                IR = ir;
            out("\tfound ImageReader #" + idx + " " + ir);
            idx++;
        }

        if (IR == null) {
            out("NO IMAGE READER FOUND FOR " + imageSRC);
            if (file == null)
                System.err.println("ImageIO.read got: " + ImageIO.read(url));
            else
                System.err.println("ImageIO.read got: " + ImageIO.read(file));
            //System.out.println("Reading " + file);
            System.exit(0);
        }

        out("Chosen ImageReader for stream " + IR + " formatName=" + IR.getFormatName());
        

        IR.addIIOReadProgressListener(new ReadListener());
        out("added progress listener");

        out("Reading " + IR);
        IR.setInput(iis);
        out("Input for reader set to " + iis);
        //out("meta-data: " + IR.getImageMetadata(0));
        out("Getting size...");
        int w = IR.getWidth(0);
        int h = IR.getHeight(0);
        out("ImageReader got size " + w + "x" + h);
        BufferedImage bi = IR.read(0);
        out("ImageReader.read(0) got " + bi);

        */




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




/**
 * An implementation of <code>ImageInputStream</code> that gets its
 * input from a regular <code>InputStream</code>.  As the data
 * is read, is it backed by a File for seeking backward.
 *
 */
class FileBackedImageInputStream extends ImageInputStreamImpl
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(FileBackedImageInputStream.class);

    private static final int BUFFER_LENGTH = 2048;

    private final RandomAccessFile cache;
    private final byte[] streamBuf = new byte[BUFFER_LENGTH];
    private final InputStream stream;
    
    private long length = 0L;
    private boolean foundEOF = false;

    private final Images.Listener listener;

    private final File file;

    // todo opt: pass in content size up front to init RAF to full size at start
    /**
     * @param stream - image data stream -- will be closed when reading is done
     * @param file - file to write incoming data to to use as cache
     */
    public FileBackedImageInputStream(InputStream stream, File file, Images.Listener listener)
        throws IOException, Images.ImageException
    {
        if (stream == null || file == null)
            throw new IllegalArgumentException("FileBackedImageInputStream: stream or file is null");

        this.stream = stream;
        this.cache = new RandomAccessFile(file, "rw");
        this.file = file;
        this.listener = listener;

        this.cache.setLength(0); // in case already there

        if (true) {

            final byte[] testBuf = new byte[64];
            final int got = read(testBuf);
            super.seek(0); // put as back at the start
            
            final String contentHead = new String(testBuf, 0, got, "US-ASCII");
            
            if (DEBUG.IMAGE) {
                Log.debug(String.format("ContentHead; got=%d; streamPos=%d; length=%d [%s]",
                                        got,
                                        streamPos,
                                        length,
                                        contentHead.trim()));

                // For inspecting largeer content-head's in log debug stream:
                // compress strings of newlines/whitespaces into single newlines:
                //contentHead = contentHead.replaceAll("(\\n\\s*)(\\n\\s*)+", "\n");
                // or just compress all whitespace down to single spaces:
                //contentHead = contentHead.replaceAll("\\s+", " ");
                //Log.debug("CONTENT-HEAD:\n" + contentHead + "\n-------");
            }

        
            final String trimmed = contentHead.trim();
            final String matcher = trimmed.substring(0, Math.min(16, trimmed.length())).toUpperCase();

            if (matcher.startsWith("<HTML>") || matcher.startsWith("<!DOCTYPE")) {
                Log.warn("Stream " + stream + " contains HTML, not image data: [" + contentHead.trim() + "]");

                Log.info("see cache file for HTML sample:\n\t" + file);
                
                // DEBUG: we force this readUntil to get more info on the streams that are starting
                // with <HTML> every once in a while: we can be sure to have a cache file with a bit 
                // of data in it we can inspect afterwords.
                readUntil(BUFFER_LENGTH);

                if (DEBUG.IMAGE) {
                    byte[] buf = new byte[BUFFER_LENGTH];
                    int n = read(buf);
                    super.seek(0);
                    Log.debug("Cache contents:\n");
                    System.out.println(new String(buf, 0, n, "US-ASCII"));
                }
                
                close();
                throw new Images.DataException("Content is HTML, not image data");
                
            } else {
                readUntil(BUFFER_LENGTH);  // debug: same reason as above
            }

            // setting this at the end will prevent any gotImageProgress callbacks to any listener
            // until we at know it's not an HTML data stream
            //this.listener = listener;
        }
    }

    /*
    public void seek(long pos) throws IOException {
        System.err.println("SEEK " + pos);
        super.seek(pos);
    }
    */

    /**
     * Ensures that at least <code>pos</code> bytes are cached,
     * or the end of the source is reached.  The return value
     * is equal to the smaller of <code>pos</code> and the
     * length of the source file.
     */
    private long readUntil(long pos) throws IOException {

        //System.err.println("<=" + pos + "; ");
        
        // We've already got enough data cached
        if (pos < length)
            return pos;

        // pos >= length but length isn't getting any bigger, so return it
        if (foundEOF)
            return length;

        long len = pos - length;
        cache.seek(length);
        while (len > 0) {
            // Copy a buffer's worth of data from the source to the cache
            // BUFFER_LENGTH will always fit into an int so this is safe
            final int nbytes = stream.read(streamBuf, 0, (int)Math.min(len, (long)BUFFER_LENGTH));
            if (nbytes == -1) {
                if (DEBUG.IMAGE && DEBUG.IO) System.err.println("<EOF @ " + length + ">");
                foundEOF = true;
                return length;
            }

            //if (DEBUG.IMAGE && DEBUG.IO) System.err.format("+%4d; ", nbytes);
            if (DEBUG.IO) Log.debug(String.format("+%4d bytes; %7d total", nbytes, length+nbytes));
            cache.write(streamBuf, 0, nbytes);
            len -= nbytes;
            length += nbytes;
            //System.out.println("READ TO " + length);
            if (listener != null)
                listener.gotImageProgress(stream, length, -1);
        }

        return pos;
    }

    public int read() throws IOException {
        bitOffset = 0;
        long next = streamPos + 1;
        long pos = readUntil(next);
        if (pos >= next) {
            if (DEBUG.IMAGE && DEBUG.IO) Log.debug("SEEK " + (streamPos+1));
            cache.seek(streamPos++);
            return cache.read();
        } else {
            return -1;
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null)
            throw new NullPointerException();

        if (off < 0 || len < 0 || off + len > b.length || off + len < 0)
            throw new IndexOutOfBoundsException();

        if (len == 0)
            return 0;


        checkClosed();

        bitOffset = 0;

        long pos = readUntil(streamPos + len);

        // len will always fit into an int so this is safe
        len = (int)Math.min((long)len, pos - streamPos);
        if (len > 0) {
            if (DEBUG.IMAGE && DEBUG.IO) Log.debug("SEEK " + streamPos);
            cache.seek(streamPos);
            cache.readFully(b, off, len);
            streamPos += len;
            return len;
        } else {
            return -1;
        }
    }

    public void close() throws IOException {
        super.close();
        cache.close();
        stream.close();
    }

    public String toString() {
        return getClass().getName() + "[" + file.toString() + "]";
    }

 }
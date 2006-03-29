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

import java.util.*;
import java.lang.ref.*;
import java.net.URL;
import java.io.File;
import java.io.InputStream;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.imageio.*;
import javax.imageio.event.*;
import javax.imageio.stream.*;

/**
 *
 * Handle the loading of images in background threads, making callbacks to deliver
 * results to multiple listeners that can be added at any time during the image fetch,
 * and memory caching based on a URL key, using a HashMap with SoftReference's
 * so if we run low on memory they just drop out of the cache.
 *
 * @version $Revision: 1.4 $ / $Date: 2006-03-29 19:40:55 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class Images
{
    public static VueAction ClearCacheAction = new VueAction("Empty Image Cache") {
            public void act() { Cache.clear(); }
        };
    
    private static Map Cache = new SoftMap();

    
    /**
     * Calls to Images.getImage must pass in a Listener to get results.
     * The first argument to all the callbacks is the original object
     * passed in to getImage as the imageSRC.
     */
    public interface Listener {
        /** If image is already cached, this will NOT be called -- is only called from an image loading thread. */
        void gotImageSize(Object imageSrc, int w, int h);
        /** Will be called immediately in same thread if image cached, later in different thread if not. */
        void gotImage(Object imageSrc, Image image, int w, int h);
        /** If there is an exception or problem loading the image, this will be called */
        void gotImageError(Object imageSrc, String msg);
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
     * @return true if the result is immediately available: the image was cached, or if there was an immediate error
     **/
    
    public static boolean getImage(Object imageSRC, Images.Listener listener)
    {
        if (DEBUG.IMAGE) {
            System.out.println("\n");
            out("FETCHING IMAGE SOURCE " + imageSRC + " for " + tag(listener));
        }
        try {
            if (getCachedOrLoad(imageSRC, listener) == null)
                return false;
        } catch (Throwable t) {
            if (DEBUG.IMAGE) tufts.Util.printStackTrace(t);
            if (listener != null)
                listener.gotImageError(imageSRC, t.toString());
        }
        return true;
    }
    
    
    /** synchronously retrive an image from the given data source: e.g., a Resource, File, URL, InputStream, etc */
    public static BufferedImage getImage(Object imageSRC)
    {
        try {
            return getCachedOrLoad(imageSRC, null);
        } catch (Throwable t) {
            if (DEBUG.IMAGE) tufts.Util.printStackTrace(t);
            VUE.Log.error("getImage " + imageSRC + ": " + t);
            return null;
        }
    }


    /**
     * A soft-reference cache-map for the images: if we
     * run low on memory, they'll be garbage collected
     * and will seem to have disspeared from the cache.
     * 
     * Not all HashMap methods covered: only safe to use
     * the onces explicity implemented here.
     */
    private static class SoftMap extends HashMap {

        public synchronized Object get(Object key) {
            //if (DEBUG.IMAGE) out("SoftMap; get: " + key);
            Object val = super.get(key);
            if (val == null)
                return null;
            Reference ref = (Reference) val;
            val = ref.get();
            if (val == null) {
                if (DEBUG.IMAGE) out("SoftMap; image was garbage collected: " + key);
                super.remove(key);
                return null;
            } else
                return val;
        }
        
        public synchronized Object put(Object key, Object value) {
            //if (DEBUG.IMAGE) out("SoftMap; put: " + key);
            return super.put(key, new SoftReference(value));
        }

        public synchronized boolean containsKey(Object key) {
            return get(key) != null;
        }

        public synchronized void clear() {
            Iterator i = values().iterator();
            while (i.hasNext())
                ((Reference)i.next()).clear();
            super.clear();
        }
        
    }

    private static class ImageSource {
        final Object original;  // anything plausably covertable image source (e.g. a Resource, URL, File, stream)
        final Resource resource;// if original was a resource, it goes here.
        final URL key;          // Unique key for caching (currently always a URL)
        Object readable;        // the readable image source (not a Resource, and URL's converted to stream before ImageIO)

        ImageSource(Object original) {
            this.original = original;

            if (original instanceof Resource) {
                Resource r = (Resource) original;
                if (r.getSpec().startsWith("/"))  {
                    File file = new java.io.File(r.getSpec());
                    this.readable = file;
                } else {
                    this.readable = r.asURL();
                }
                this.resource = r;
            } else if (original instanceof java.net.URL) {
                this.readable = (java.net.URL) original;
                this.resource = null;
            } else
                this.resource = null;

            
            if (readable instanceof java.net.URL) {
                this.key = (URL) readable;
            } else if (readable instanceof java.io.File) {
                URL k = null;
                try {
                    k = ((File) readable).toURL();
                } catch (java.net.MalformedURLException e) {
                    tufts.Util.printStackTrace(e);
                }
                this.key = k;
            } else
                this.key = null; // will not be cacheable
            
        }

        public String toString() {
            String s = tag(original);
            if (readable != original)
                s += "; readable=[" + tag(readable) + "]";
            return s;
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
            this.head = l0;
            this.tail = l1;
        }
        ListenerRelay(Listener l0) {
            this(l0, null);
        }

        public void gotImageSize(Object imageSrc, int w, int h) {
            if (DEBUG.IMAGE) out("relay SIZE to head " + tag(head) + " " + imageSrc);
            head.gotImageSize(imageSrc, w, h);
            if (tail != null) {
                if (DEBUG.IMAGE) out("relay SIZE to tail " + tag(tail) + " " + imageSrc);
                tail.gotImageSize(imageSrc, w, h);
            }
        }
        public void gotImage(Object imageSrc, Image image, int w, int h) {
            if (DEBUG.IMAGE) out("relay IMAGE to head " + tag(head) + " " + imageSrc);
            head.gotImage(imageSrc, image, w, h);
            if (tail != null) {
                if (DEBUG.IMAGE) out("relay IMAGE to tail " + tag(tail) + " " + imageSrc);
                tail.gotImage(imageSrc, image, w, h);
            }
        }
        public void gotImageError(Object imageSrc, String msg) {
            if (DEBUG.IMAGE) out("relay ERROR to head " + tag(head) + " " + imageSrc);
            head.gotImageError(imageSrc, msg);
            if (tail != null) {
                if (DEBUG.IMAGE) out("relay ERROR to tail " + tag(tail) + " " + imageSrc);
                tail.gotImageError(imageSrc, msg);
            }
        }

        boolean hasListener(Listener l) {
            if (head == l || tail == l)
                return true;
            if (tail instanceof ListenerRelay)
                return ((ListenerRelay)tail).hasListener(l);
            else
                return false;
        }

    }

    /**
     * Track what's been delivered, to send to listeners that are added when
     * partial results have already been delivered.
     */
    private static class LoaderRelayer extends ListenerRelay
    {
        private final ImageSource imageSRC;
        
        private Image image = null;
        private int width = -1;
        private int height = -1;
        private String errorMsg = null;
        
        
        LoaderRelayer(ImageSource is, Listener firstListener) {
            super(firstListener, null);
            imageSRC = is;
        }

        public synchronized void gotImageSize(Object imageSrc, int w, int h) {
            this.width = w;
            this.height = h;
            super.gotImageSize(imageSrc, w, h);

        }
        public synchronized void gotImage(Object imageSrc, Image image, int w, int h) {
            this.image = image;
            super.gotImage(imageSrc, image, w, h);

        }
        public synchronized void gotImageError(Object imageSrc, String msg) {
            //this.errorSrc = imageSrc;
            this.errorMsg = msg;
            super.gotImageError(imageSrc, msg);

        }

        synchronized void addListener(Listener newListener)
        {
            if (hasListener(newListener)) {
                if (DEBUG.IMAGE) out("Loader; ALREADY A LISTENER FOR THIS IMAGE: " + tag(newListener));
                return; 
            }
            
            // First deliver any results we've already got:
            deliverPartialResults(newListener);
            
            if (tail == null)  {
                tail = newListener;
            } else {
                tail = new ListenerRelay(tail, newListener);
            }

        }
        
        private void deliverPartialResults(Listener l)
        {
            if (DEBUG.IMAGE) out("DELIVERING PARTIAL RESULTS TO: " + tag(l));
            
            if (width > 0)
                l.gotImageSize(imageSRC.original, width, height);
            if (image != null)
                l.gotImage(imageSRC.original, image, width, height);

            if (errorMsg != null) {
                out("DELIVERING PARTIAL RESULT ERROR FOR " + imageSRC + " [" + errorMsg + "] to: " + tag(l));
                l.gotImageError(imageSRC.original, errorMsg);
            }

            if (DEBUG.IMAGE) out("DONE DELIVERING PARTIAL RESULTS TO: " + tag(l));
            
        }

    }
    
    

    /**
     * A thread for loading a single image.  Images.Listener results are delievered
     * from this thread (unless the image was already cached).
     */
    static class Loader extends Thread {
        private static int LoaderCount = 0;
        private final ImageSource imageSRC; 
        private final LoaderRelayer relay;


        /**
         * @param src must be any valid src *except* a Resource
         * @param resource - if this is tied to a resource to update with meta-data after loading
         */
        Loader(ImageSource imageSRC, Listener l) {
            super("VUE-ImageLoader" + LoaderCount++);
            if (l == null)
                throw new IllegalArgumentException("Images.Loader: listener is null; results would be invisible");
            this.imageSRC = imageSRC;
            this.relay = new LoaderRelayer(imageSRC, l);
        }

        public void run() {
            if (DEBUG.IMAGE || DEBUG.THREAD) out("Loader: load " + imageSRC + " kicked off");
            BufferedImage bi = loadImage(imageSRC, relay);
            if (DEBUG.IMAGE || DEBUG.THREAD) out("Loader: load returned, result=" + tag(bi));
        }

        void addListener(Listener newListener) {
            relay.addListener(newListener);
        }
    }


    
    /** @return Image if cached or listener is null, otherwise makes callbacks to the listener from
     a new thread. */
    private static BufferedImage getCachedOrLoad(Object _imageSRC, Images.Listener listener)
        throws java.io.IOException, java.lang.InterruptedException
    {
        final ImageSource imageSRC = new ImageSource(_imageSRC);

        synchronized (Cache) {

            Object entry;

            if (imageSRC.key != null && (entry = Cache.get(imageSRC.key)) != null) {
                if (DEBUG.IMAGE) out("found cache entry for key " + tag(imageSRC.key) + ": " + entry);
                
                if (entry instanceof Loader) {
                    if (DEBUG.IMAGE) out("Image is loading into the cache via already existing Loader...");
                    Loader loader = (Loader) entry;
                    if (listener != null) {
                        if (DEBUG.IMAGE) out("Adding us as listener to existing Loader");
                        loader.addListener(listener);
                        return null;
                    } else {
                        // We've got no listener, so run synchronous & wait on existing loader thread to die:
                        out("Joining " + tag(loader) + "...");
                        loader.join();
                        out("Join of " + tag(loader) + " completed, cache has filled.");
                    }
                }

                // We should be guarnateed to have a full Image result in the cache at this point
                BufferedImage image = (BufferedImage) Cache.get(imageSRC.key);
                if (listener != null)
                    listener.gotImage(imageSRC.original, image, image.getWidth(), image.getHeight());

                return image;
            }

            // Image wasn't in the cache: we must go get it.
            // If we have a listener, create a thread to do this.
            // If we don't, do now and don't return until we have the result.

            if (listener != null) {
                Loader newLoader = new Loader(imageSRC, listener);
                newLoader.start();
                Cache.put(imageSRC.key, newLoader);
            }
            
        }

        if (listener == null) {
            // load the image and don't return until we have it
            return loadImage(imageSRC, listener);
        } else {
            // the image is in the process of loading
            return null;
        }
            
    }

    private static class ImageException extends Exception {
        ImageException(String s) {
            super(s);
        }
    }

    /** An wrapper for readAndCreateImage that deals with exceptions, and puts successful results in the cache */
    private static BufferedImage loadImage(ImageSource imageSRC, Images.Listener listener)
    {
        BufferedImage image = null;
        
        try {
            image = readAndCreateImage(imageSRC, listener);
        } catch (Throwable t) {
            if (DEBUG.IMAGE) tufts.Util.printStackTrace(t);
            if (listener != null) {
                String msg;
                if (t instanceof javax.imageio.IIOException || t instanceof ImageException)
                    msg = t.getMessage();
                else if (t instanceof java.io.FileNotFoundException)
                    msg = "Not Found: " + t.getMessage();
                else
                    msg = t.toString();

                // this is the one place we deliver caught exceptions
                // during image loading:
                listener.gotImageError(imageSRC.original, msg);

                VUE.Log.warn("Image source: " + imageSRC + ": " + t);
            }
            
            synchronized (Cache) {
                Cache.remove(imageSRC.key);
            }
            
        }

        if (image != null) {
            synchronized (Cache) {
                Cache.put(imageSRC.key, image);
            }
        }

        return image;
        
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
        long len = uc.getContentLength();
        r.setProperty("url.contentLength", len);
        r.setProperty("url.contentType", uc.getContentType());
        setDateValue(r, "url.expires", uc.getExpiration());
        setDateValue(r, "url.date", uc.getDate());
        setDateValue(r, "url.lastModified", uc.getLastModified());
    }
    
    // todo: this probably wants to move to a resource impl class
    private static void setResourceMetaData(Resource r, File f) {
        r.setProperty("file.size", f.length());
        setDateValue(r, "file.lastModified", f.lastModified());
    }

    /**
     * @param imageSRC - see ImageSource ("anything" that we can get an image data stream from)
     * @param listener - an Images.Listener: if non-null, will be issued callbacks for size & completion
     * @return the loaded image, or null if none found
     */
    private static BufferedImage readAndCreateImage(ImageSource imageSRC, Images.Listener listener)
        throws java.io.IOException, ImageException
    {
        if (DEBUG.IMAGE) out("creating input stream for source " + tag(imageSRC.readable));

        InputStream urlStream = null;
        if (imageSRC.readable instanceof java.net.URL) {
            URL url = (URL) imageSRC.readable;
            if (DEBUG.IMAGE) out("opening URL connection...");
            java.net.URLConnection uc = url.openConnection();
            if (imageSRC.resource != null)
                setResourceMetaData(imageSRC.resource, uc);
            if (DEBUG.IMAGE) out("opening URL stream...");
            urlStream = uc.getInputStream();
            imageSRC.readable = urlStream;
            if (DEBUG.IMAGE) out("got URL stream");
        } else if (imageSRC.readable instanceof java.io.File) {
            if (imageSRC.resource != null)
                setResourceMetaData(imageSRC.resource, (File) imageSRC.readable);
        }
        
        ImageInputStream inputStream = ImageIO.createImageInputStream(imageSRC.readable);

        if (DEBUG.IMAGE) out("Got ImageInputStream " + inputStream);
        
        java.util.Iterator iri = ImageIO.getImageReaders(inputStream);

        ImageReader reader = null;
        int idx = 0;
        while (iri.hasNext()) {
            ImageReader ir = (ImageReader) iri.next();
            if (reader == null)
                reader = ir;
            if (DEBUG.IMAGE) out("\tfound ImageReader #" + idx + " " + ir);
            idx++;
        }

        if (reader == null) {
            if (DEBUG.IMAGE) out("NO IMAGE READER FOUND FOR " + imageSRC);
            throw new ImageException("Unreadable Image Stream");
        }

        if (DEBUG.IMAGE) out("Chosen ImageReader for stream " + reader + " formatName=" + reader.getFormatName());
        
        //reader.addIIOReadProgressListener(new ReadListener());
        //out("added progress listener");

        reader.setInput(inputStream, false, true); // allow seek back, can ignore meta-data (can generate exceptions)
        if (DEBUG.IMAGE) out("Input for reader set to " + inputStream);
        if (DEBUG.IMAGE) out("Getting size...");
        int w = reader.getWidth(0);
        int h = reader.getHeight(0);
        if (DEBUG.IMAGE) out("ImageReader got size " + w + "x" + h);


        if (imageSRC.resource != null) {
            if (DEBUG.IMAGE || DEBUG.THREAD || DEBUG.RESOURCE)
                out("setting resource meta-data for " + imageSRC.resource);
            imageSRC.resource.setProperty("image.width",  Integer.toString(w));
            imageSRC.resource.setProperty("image.height", Integer.toString(h));
            imageSRC.resource.setProperty("image.format", reader.getFormatName());
            imageSRC.resource.setCached(true);
        }

        
        if (listener != null)
            listener.gotImageSize(imageSRC.original, w, h);

        // FYI, if fetch meta-data, will need to trap exceptions here, as if there are
        // any problems or inconsistencies with it, we'll get an exception, even if the
        // image is totally readable.
        //out("meta-data: " + reader.getImageMetadata(0));

        //-----------------------------------------------------------------------------
        // Now read the image, creating the BufferedImage
        //-----------------------------------------------------------------------------

        if (DEBUG.IMAGE) out("Reading " + reader);
        BufferedImage image = reader.read(0);
        if (DEBUG.IMAGE) out("ImageReader.read(0) got " + image);

        if (listener != null)
            listener.gotImage(imageSRC.original, image, w, h);

        inputStream.close();
        
         if (urlStream != null)
             urlStream.close();

        if (DEBUG.Enabled) {
            String[] tryProps = new String[] { "name", "title", "description", "comment" };
            for (int i = 0; i < tryProps.length; i++) {
                Object p = image.getProperty(tryProps[i], null);
                if (p != null && p != java.awt.Image.UndefinedProperty)
                    System.err.println("FOUND PROPERTY " + tryProps[i] + "=" + p);
            }
        }


        return image;
    }
    


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
        if (o instanceof LWComponent)
            return ((LWComponent)o).getDiagnosticLabel();
        
        String s = Util.tag(o);
        s += "[";
        if (o instanceof Thread) {
            s += ((Thread)o).getName();
        } else if (o instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) o;
            s += bi.getWidth() + "x" + bi.getHeight();
        } else if (o != null)
            s += o.toString();
        return s + "]";
    }
    
    private static void out(Object o) {
        String s = "Images " + (""+System.currentTimeMillis()).substring(9);
        s += " [" + Thread.currentThread().getName() + "]";
        if (false)
            VUE.Log.debug(s + " " + (o==null?"null":o.toString()));
        else
            System.out.println(s + " " + (o==null?"null":o.toString()));
    }

    
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

        getImage(imageSRC, new LWImage());
        //loadImage(imageSRC, null);


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
package tufts.vue;

import tufts.Util;

import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.Dimension;

import tufts.vue.MapDropTarget.DropHandler;

import edu.tufts.vue.ontology.ui.TypeList;

/**
 * implements java.awt.datatransfer.Transferable for LWComponent(s)
 *
 * @version $Revision: 1.3 $ / $Date: 2009-09-02 16:40:12 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class LWTransfer implements Transferable {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWTransfer.class);    
    
    private static boolean isLocalDrop;

    // hack for working around java drop-handler bug that requests all byte representations no matter what
    public static void markAsLocal(boolean t) {
        isLocalDrop = t;
    }
    
    private static final DataFlavor LWFlavors[] = {
        LWComponent.DataFlavor,
        DataFlavor.stringFlavor,
        DataFlavor.imageFlavor,
        DropHandler.DataFlavor
        //MapResource.DataFlavor,
        // TypeList.DataFlavor // commented out 2009-06-24 SMF
        //URLFlavor, // try text/uri-list
    };


    private final LWComponent LWC;
    private final boolean isSelection;
    

    public LWTransfer(LWComponent c, boolean selection) {
        this.LWC = c;
        this.isSelection = selection;
    }
    
    public LWTransfer(LWComponent c) {
        this(c, false);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return LWFlavors;
    }
            
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        if (DEBUG.DND) Log.debug("isDataFlavorSupported, flavor=" + flavor);
                
        if (flavor == null)
            return false;

        if (flavor == LWComponent.DataFlavor && LWC instanceof LWMap) {
            // prevent the dropping of the entire map onto itself
            return false;
        }

        if (isLocalDrop && flavor == DataFlavor.imageFlavor) {
            // we never need raw image data when dropping onto a local source
            return false;
        }

        if (flavor == DropHandler.DataFlavor && LWC.hasClientData(DropHandler.class)) {
            // note this is a bit convoluted: the producer is currently only passed within an
            // LWComponent.  This needn't be true, but our current usage depends on it.
            // (We want an LWComponent available as the drag image).  We could easily
            // extend LWTransfer to support a producer directly and allowing for a null LWC.
            return true;
        }
                
        //                 if (flavor == LWComponent.Producer.DataFlavor && LWC.hasClientData(LWComponent.Producer.class)) {
        //                     // note this is a bit convoluted: the producer is currently only passed within an
        //                     // LWComponent.  This needn't be true, but our current usage depends on it.
        //                     // (We want an LWComponent available as the drag image).  We could easily
        //                     // extend LWTransfer to support a producer directly and allowing for a null LWC.
        //                     return true;
        //                 }
                
        if (flavor == Resource.DataFlavor && LWC.getResource() == null)
            return false;

        // TODO BUG: support for TypeList is being incorrectly reported as true here
        // even if there is no old-style meta-data!
                
        for (int i = 0; i < LWFlavors.length; i++)
            if (flavor.equals(LWFlavors[i]))
                return true;

        return false;
    }

    public synchronized Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, java.io.IOException
    {
        //tufts.Util.printStackTrace("GTD " + flavor.getHumanPresentableName());
        // Note: the sun transfer handler (for drops to java) always requests
        // all data for all types during a drop, which is a horrible waste in
        // that we need to create a whole image every time, even it it's just
        // the resource being dropped.  If want to optimize this out, would need
        // to create our own Image class that delays creation of the actual
        // image until something is requested of it.  Drops to the OS, at
        // least in the case of MacOSX are smart and only request data for
        // what is ultimately dropped.

        // @see sun.awt.datatransfer.DataTransferer

        //Log.info("getTransferData " + Util.tags(flavor), new Throwable("HERE"));
        final String type = isLocalDrop ? "LOCAL " : "REMOTE ";

        Log.info("getTransferData " + type + flavor);
                
        if (DEBUG.DND && DEBUG.META) System.err.print("<LWTransfer.getTransferData("
                                                      + flavor.getHumanPresentableName() + ")>");
        //if (DEBUG.DND && DEBUG.META)
        //Log.debug("getTransferData(" + flavor.getHumanPresentableName() + ")", new Throwable("HERE"));
        
        Object data = null;
        
                
        if (TypeList.DataFlavor.equals(flavor)) {
            try {
                data = LWC.getMetadataList().getMetadata().get(0).getObject();
            } catch (Throwable t) {
                Log.warn("getTransferData: " + flavor + "; " + t);
            }
        }
        else if (DataFlavor.stringFlavor.equals(flavor)) {

            if (LWC != null && LWC.getClientData(DataFlavor.stringFlavor) != null) {
                return LWC.getClientData(DataFlavor.stringFlavor);
            }

                    
            String s = null;
            if (LWC instanceof LWMap && ((LWMap)LWC).getFile() != null)
                s = ((LWMap)LWC).getFile().getAbsolutePath();
            if (LWC.hasResource())
                s = LWC.getResource().getSpec();
            if (s == null && LWC.hasLabel())
                s = LWC.getLabel();
            if (s == null && LWC.hasNotes())
                s = LWC.getNotes();
            if (s == null)
                s = LWC.getDisplayLabel();
            //if (s != null) s += "\n";
            data = s;
                    
        }
        else if (DataFlavor.imageFlavor.equals(flavor)) {

            //data = new LazyImage(new LazyImage.Producer() {
            //       public Image getImage() { return LWC.getAsImage(); }
            //});

            // The LazyImage method attempted above is no good for avoiding the creation of large
            // images unless they're needed.  (1) sun.awt.image.SurfaceManager.getManager assumes
            // all images are BufferedImage's with a cast (!)  And (2): The Java VM impl data
            // transfer code appears to request the byte representations of ALL THE SUPPORT FLAVORS
            // whenever a drop happens -- not just the flavor requested.  (Which leads us to the
            // above cast failure, and this whole problem in the first place).  E.g., this image
            // data should ONLY be requested if, say, we drop the image on the desktop or into
            // another app that will import the raw image data.  But for some reason the impls
            // (including 1.5 & 1.6, at least Apple -- need to check Windows) request all possible
            // data, which can be a big issue if you happen to be dragging a very large image, but
            // are not interested in actually ever obtain all it's bytes.  (E.g., very slow to copy
            // all those bytes, oftening hanging or even crashing the internal Apple code).
                    
            if (isLocalDrop) {
                return null;
                //throw new java.io.IOException("raw image data never produced for local drops");
            } else {
                return LWC.getAsImage();
            }
                    
        }
        else if (DropHandler.DataFlavor.equals(flavor)) {
                    
            data = LWC.getClientData(DropHandler.class);
                    
        //} else if (LWComponent.Producer.DataFlavor.equals(flavor)) {

            //data = LWC.getClientData(LWComponent.Producer.class);
                    
        }
        else if (LWComponent.DataFlavor.equals(flavor)) {

                    
            final java.util.Collection duplicates;
            if (isSelection) {
                duplicates = tufts.vue.Actions.duplicatePreservingLinks(LWC.getChildren());
            }
            else if (LWC instanceof LWMap) {
                // don't send the actual map just yet...
                duplicates = tufts.vue.Actions.duplicatePreservingLinks(LWC.getChildren());
            }
            
//             else if (LWC.hasClientData(LWComponent.Producer.class)) {
//                 Util.printStackTrace("deprecated use of node producer");
//                 //duplicates = LWC.getClientData(LWComponent.ListFactory.class).produceNodes(null);
//                 duplicates = LWC.getClientData(LWComponent.Producer.class).produceNodes(null);
//             }
                    
            else if (LWC.hasFlag(LWComponent.Flag.INTERNAL) /*&& LWC.getClientProperty(Field.class) != null*/) {
                // an INTERNAL flagged LWComponent during drag is merely a bucket for other
                // information, and we can return it directly.
                return LWC;
            }
            else {
                duplicates = java.util.Collections.singletonList(LWC.duplicate());
            }
                        
            data = duplicates;
                    
        } else if (Resource.DataFlavor.equals(flavor)) {

            data = LWC.getResource();

            //                 } else if (URLFlavor.equals(flavor) && LWC.getResource() instanceof URLResource) {

            //                     data = ((URLResource)LWC.getResource()).asURL();

        } else {
            throw new UnsupportedFlavorException(flavor);
        }
        
        //if (DEBUG.DND) out("LWTransfer: returning " + Util.tag(data));

        return data;
    }
}
    

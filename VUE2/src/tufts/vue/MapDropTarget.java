package tufts.vue;

import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DnDConstants;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.awt.geom.Point2D;

class MapDropTarget
    implements java.awt.dnd.DropTargetListener
{
    static final String MIME_TYPE_MAC_URLN = "application/x-mac-ostype-75726c6e";
    // 75726c6e="URLN" -- mac uses this type for a flavor containing the title of a web document
    // this existed in 1.3, but apparently went away in 1.4.
    static final String MIME_TYPE_TEXT_PLAIN = "text/plain";

    private final int ACCEPTABLE_DROP_TYPES = DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK;

    ConceptMap conceptMap;
    java.awt.Container mapContainer;
    java.awt.Point lastPoint;

    private final boolean debug = false;
    
    public MapDropTarget(java.awt.Container mapContainer, ConceptMap conceptMap)
    {
       this.mapContainer = mapContainer;
       this.conceptMap = conceptMap;
    }

    public void dragEnter(DropTargetDragEvent e)
    {
        e.acceptDrag(ACCEPTABLE_DROP_TYPES);
        if (debug) System.err.println("MapDropTarget: dragEnter " + e);
    }

    public void dragOver(DropTargetDragEvent e)
    {
        e.acceptDrag(ACCEPTABLE_DROP_TYPES);
        lastPoint = e.getLocation();
        //System.err.println("MapDropTarget:  dragOver");
    }

    public void dragExit(DropTargetEvent e)
    {
        if (debug) System.err.println("MapDropTarget: dragExit " + e);
    }

    public void dropActionChanged(DropTargetDragEvent e)
    {
        if (debug) System.err.println("MapDropTarget: dropActionChanged " + e);
    }
    
    public void drop(DropTargetDropEvent e)
    {
        if (debug) System.err.println("MapDropTarget: drop " + e);
        if (debug) System.err.println("MapDropTarget: lastPoint=" + lastPoint);

        if ((e.getSourceActions() & DnDConstants.ACTION_COPY) != 0) {
            e.acceptDrop(DnDConstants.ACTION_COPY);
        } else {
            e.rejectDrop();
            return;
        }

        // Scan thru the data-flavors, looking for a useful mime-type

        boolean success = false;
        Transferable transfer = e.getTransferable();
        DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();

        String resourceName = null;
        java.awt.Image droppedImage = null;
        java.util.List fileList = null;
        
        if (debug) System.err.println("drop: found " + dataFlavors.length + " dataFlavors");
        for (int i = 0; i < dataFlavors.length; i++) {
            DataFlavor flavor = dataFlavors[i];
            Object data = null;
            
            if (debug) System.err.print("flavor" + i + " " + flavor.getMimeType());
            try {
                data = transfer.getTransferData(flavor);
            } catch (Exception ex) {
                System.err.println("getTransferData: " + ex);
            }
            if (debug) System.err.println(" transferData=" + data);

            try {
                if (data instanceof java.awt.Image) {
                    droppedImage = (java.awt.Image) data;
                    break;
                } else if (flavor.isFlavorJavaFileListType()) {
                    fileList = (java.util.List) transfer.getTransferData(flavor);
                    break;
                } else if (flavor.getMimeType().startsWith(MIME_TYPE_TEXT_PLAIN))
                    // && flavor.isFlavorTextType() -- java 1.4 only
                {
                    // checking isFlavorTextType() above should be
                    // enough, but some Windows apps (e.g.,
                    // Netscape-6) are leading the flavor list with
                    // 20-30 mime-types of "text/uri-list", but the
                    // reader only ever spits out the first character.

                    resourceName = readTextFlavor(flavor, transfer);
                    if (resourceName != null)
                        break;
                    
                } else {
                    //System.err.println("Unhandled flavor: " + flavor);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                //System.err.println(ex);
                continue;
            }
        }

        //MapDropEvent mapDropEvent = null;

        if (droppedImage != null) {
            createNewNode(droppedImage, lastPoint);
            //mapDropEvent = new MapDropEvent(droppedImage, lastPoint);
            success = true;
        } else if (fileList != null) {
            if (debug) System.err.println("\tLIST, size= " + fileList.size());
            java.util.Iterator iter = fileList.iterator();
            int x = lastPoint.x;
            int y = lastPoint.y;
            while (iter.hasNext()) {
                java.io.File file = (java.io.File) iter.next();
                if (debug) System.err.println("\t" + file.getClass().getName() + " " + file);
                createNewNode(file.toString(), file.getName(), new java.awt.Point(x, y));
                //mapDropEvent = new MapDropEvent(file.toString(), file.getName(), new java.awt.Point(x, y));
                // todo: need to raise here too... or rather, should
                // raise a MapDropCollectionEvent, or rather the MapDropEvent contains a collection
                x += 15;
                y += 15;
                success = true;
            }
        } else if (resourceName != null) {
            createNewNode(resourceName, null, lastPoint);
            //mapDropEvent = new MapDropEvent(resourceName, null, lastPoint);
            success = true;
        }

        e.dropComplete(success);
        
        /*if (mapDropEvent != null) {
            e.dropComplete(true);            
        } else {
            e.dropComplete(false);
            }*/

            
    }

    String readTextFlavor(DataFlavor flavor, Transferable transfer)
    {
        java.io.Reader reader = null;
        String value = null;
        try {
            reader = flavor.getReaderForText(transfer);
            if (debug) System.err.println("\treader=" + reader);
            char buf[] = new char[512];
            int got = reader.read(buf);
            value = new String(buf, 0, got);
            if (debug) System.err.println("\t[" + value + "]");
            if (reader.read() != -1)
                System.err.println("there was more data in the reader");
        } catch (Exception e) {
            System.err.println("readTextFlavor: " + e);
        }
        return value;
    }

    // fixme todo: change this to a resource-dropped event (w/location)
    // then we don't even have to know about the map, or the MapViewer
    // which for instance can do the zoom conversion of the drop location
    // for us.
    void createNewNode(String resourceName, String resourceTitle, java.awt.Point p)
    {
        Point2D location = dropToMapLocation(p);

        if (resourceTitle == null)
            resourceTitle = createResourceTitle(resourceName);
            
        Node n = new Node(resourceTitle, new Resource(resourceName), location);
        conceptMap.addNode(n);
        //((MapViewer)mapContainer).setSelection(((MapViewer)mapContainer).findLWNode(n));
        //todo:fixme
    }

    void createNewNode(java.awt.Image image, java.awt.Point p)
    {
        Point2D location = dropToMapLocation(p);
        Node n = new Node("Image Label", location);
        conceptMap.addNode(n);
        LWNode lwNode = ((MapViewer)mapContainer).findLWNode(n);
        lwNode.setImage(image);
        //((MapViewer)mapContainer).setSelection(lwNode);
        //todo:fixme
    }

    private Point2D dropToMapLocation(java.awt.Point p)
    {
        return dropToMapLocation(p.x, p.y);
    }

    private Point2D dropToMapLocation(int x, int y)
    {
        /*
        java.awt.Insets mapInsets = mapContainer.getInsets();
        java.awt.Point mapLocation = mapContainer.getLocation();
        System.err.println("mapContainer insets=" + mapInsets);
        System.err.println("mapContainer location=" + mapLocation);
        x -= mapLocation.x;
        y -= mapLocation.y;
        x -= mapInsets.left;
        y -= mapInsets.top;
        */

        // todo: hack till we raise an application drop event
        return ((MapViewer)mapContainer).screenToMapPoint(x, y);
    }


    String createResourceTitle(String resourceName)
    {
        int i = resourceName.lastIndexOf('/');
        if (i == resourceName.length() - 1)
            return resourceName;
        else
            return i > 0 ? resourceName.substring(i+1) : resourceName;
        // todo: go search the HTML for <title>
    }
    
}

package tufts.vue;

import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DnDConstants;

import java.awt.datatransfer.*;

import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Image;
import java.awt.image.BufferedImage;

import java.util.List;
import java.util.regex.*;

import java.io.File;
import java.io.FileInputStream;

import java.net.*;

import osid.dr.*;


class MapDropTarget
    implements java.awt.dnd.DropTargetListener
{
    static final String MIME_TYPE_MAC_URLN = "application/x-mac-ostype-75726c6e";
    // 75726c6e="URLN" -- mac uses this type for a flavor containing the title of a web document
    // this existed in 1.3, but apparently went away in 1.4.
    static final String MIME_TYPE_TEXT_PLAIN = "text/plain";

    private final int ACCEPTABLE_DROP_TYPES =
        DnDConstants.ACTION_COPY |
        DnDConstants.ACTION_LINK |
        0xFFFFFF;
        //DnDConstants.ACTION_MOVE;
        // Do NOT include MOVE, or dragging a URL from the IE address bar becomes
        // a denied drag option!  Even dragged text from IE becomes disabled.
        // FYI, also, "move" doesn't appear to actually ever mean delete original source.
        // Putting this in makes certial special windows files "available"
        // for drag (e.g., a desktop "hard" reference link), yet there are never
        // any data-flavors available to process it, so we might as well indicate
        // that we can't accept it.

    private final boolean debug = true;
    
    private MapViewer viewer;

    public MapDropTarget(MapViewer viewer)
    {
       this.viewer = viewer;
    }

    public void dragEnter(DropTargetDragEvent e)
    {
        //e.acceptDrag(ACCEPTABLE_DROP_TYPES);
        if (debug) System.out.println("MapDropTarget: dragEnter " + e);
    }

    public void dragOver(DropTargetDragEvent e)
    {
        LWComponent over = viewer.getMap().findChildAt(dropToMapLocation(e.getLocation()));
        if (over instanceof LWNode || over instanceof LWLink)
            // todo: if over resource icon and we can set THAT indicated, do
            // so and also use that to indicate we'd like to set the resource
            // instead of adding a new child
            viewer.setIndicated(over);
        else
            viewer.clearIndicated();
        //e.acceptDrag(ACCEPTABLE_DROP_TYPES);
    }

    public void dragExit(DropTargetEvent e)
    {
        if (debug) System.out.println("MapDropTarget: dragExit " + e);
    }

    public void dropActionChanged(DropTargetDragEvent e)
    {
        if (debug) System.out.println("MapDropTarget: dropActionChanged " + e + " dropAction=" + e.getDropAction());

    }
    
    public void drop(DropTargetDropEvent e)
    {
        if (DEBUG.Enabled) {
            try {
                System.out.println("caps state="+VUE.getActiveViewer().getToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK));
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }
        if (debug) System.out.println("MapDropTarget: DROP " + e
                                      + "\n\tsourceActions=" + e.getSourceActions()
                                      + "\n\tdropAction=" + e.getDropAction()
                                      + "\n\tlocation=" + e.getLocation()
                                      );

        //if ((e.getSourceActions() & DnDConstants.ACTION_COPY) != 0) {
        if (false&&(e.getSourceActions() & DnDConstants.ACTION_LINK) != 0) {
            e.acceptDrop(DnDConstants.ACTION_LINK);
        } else if (false&&(e.getSourceActions() & DnDConstants.ACTION_COPY) != 0) {
            e.acceptDrop(DnDConstants.ACTION_COPY);
            //} else if ((e.getSourceActions() & ACCEPTABLE_DROP_TYPES) != 0) {
        } else {
            e.acceptDrop(e.getDropAction());
            //e.acceptDrop(DnDConstants.ACTION_COPY);
            //e.acceptDrop(DnDConstants.ACTION_LINK);
        }
        /*
        else {
            if (debug) System.out.println("MapDropTarget: rejecting drop");
            e.rejectDrop();
            return;
        }
        */

        // Scan thru the data-flavors, looking for a useful mime-type

        //boolean success = processTransferable(e.getTransferable(), e.getLocation());
        boolean success = processTransferable(e.getTransferable(), e);

        e.dropComplete(success);
        viewer.clearIndicated();        
    }

    // debug
    private void dumpFlavors(Transferable transfer)
    {
        DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();

        System.out.println("TRANSFERABLE: " + transfer + " has " + dataFlavors.length + " dataFlavors:");
        for (int i = 0; i < dataFlavors.length; i++) {
            DataFlavor flavor = dataFlavors[i];
            System.out.print("flavor " + (i<10?" ":"") + i
                             + " \"" + flavor.getHumanPresentableName() + "\""
                             + " " + flavor.getMimeType()
                             );
            try {
                Object data = transfer.getTransferData(flavor);
                System.out.println(" [" + data + "]");
                //if (flavor.getHumanPresentableName().equals("text/uri-list")) readTextFlavor(flavor, transfer);
            } catch (Exception ex) {
                System.out.println("\tEXCEPTION: getTransferData: " + ex);
            }
        }
    }

    //    public boolean processTransferable(Transferable transfer, java.awt.Point dropLocation)
    /**
     * Process any transferrable: @param e can be null if don't have a drop event
     * (e.g., could use to process clipboard contents as well as drop events)
     */
    public boolean processTransferable(Transferable transfer, DropTargetDropEvent e)
    {
        Point dropLocation = null;
        int dropAction = DnDConstants.ACTION_MOVE; // default action
        final boolean modifierKeyWasDown = false;
        
        if (e != null) {
            dropLocation = e.getLocation();
            dropAction = e.getDropAction();

            // ACTION_MOVE action is default action on PC, COPY on the
            // Mac, so if not that, assume a modifier key was being
            // held down to change the from the default OS drag
            // action.  We have no way of knowing anything about
            // actual keyboard state that initiated the drag.

            //if (VueUtil.isMacPlatform())
            //    modifierKeyWasDown = (dropAction != DnDConstants.ACTION_COPY);
            //else
            //    modifierKeyWasDown = (dropAction != DnDConstants.ACTION_MOVE);

            // Okay, can't reliably determine if modifier key was down
            // on all platforms and drag operations: e.g.: on Windows XP,
            // default action is different in drag from windows file explorer
            // than on drag from Mozilla (or maybe web browsers in general)

            // FYI, Mac OS X 10.2.8/JVM 1.4.1_01 is not telling us about
            // changes to dropAction that happen when the drag was
            // initiated (e.g., ctrl was down) -- this is a BUG, however,
            // if you press ctrl down AFTER the drop starts, sourceAction
            // & dropAction will be set to some rediculous number -- so at
            // least we can detect that by making modifer down the default
            // if drop action anything other than the default.  ALSO:
            // somtimes we get ACTION_NONE, which is 0, which will always
            // fail our acceptable drop types test.
        }

        LWComponent hitComponent = null;

        if (dropLocation != null) {
            hitComponent = viewer.getMap().findChildAt(dropToMapLocation(dropLocation));
            System.out.println("\thitComponent=" + hitComponent);
        }
        
        boolean createAsChildren = !modifierKeyWasDown && hitComponent instanceof LWNode;
        boolean overwriteResource = modifierKeyWasDown;

        // if no drop location (e.g., we did a "Paste") then assume where
        // they last clicked.
        if (dropLocation == null)
            dropLocation = viewer.getLastMousePressPoint();
        //dropLocation = new java.awt.Point(viewer.getWidth()/2, viewer.getHeight()/2);
        
        List resourceList = null;
        List fileList = null;        
        String droppedText = null;
        DataFlavor foundFlavor = null;
        Object foundData = null;

        DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
        if (debug) System.out.println("TRANSFER: found " + dataFlavors.length + " dataFlavors");
        if (DEBUG.DND) dumpFlavors(transfer);

        try {
            if (transfer.isDataFlavorSupported(VueDragTreeNodeSelection.resourceFlavor)) {
                
                foundFlavor = VueDragTreeNodeSelection.resourceFlavor;
                foundData = transfer.getTransferData(foundFlavor);
                resourceList = (java.util.List) foundData;
            
            } else if (transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {

                foundFlavor = DataFlavor.javaFileListFlavor;
                foundData = transfer.getTransferData(foundFlavor);
                fileList = (java.util.List) foundData;

            } else if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            
                foundFlavor = DataFlavor.stringFlavor;
                foundData = transfer.getTransferData(DataFlavor.stringFlavor);
                droppedText = (String) foundData;
            
            } else {
                System.out.println("TRANSFER: found no supported dataFlavors");
                dumpFlavors(transfer);
                return false;
            }
        } catch (UnsupportedFlavorException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: Transfer lied about supporting " + foundFlavor);
            return false;
        } catch (ClassCastException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: Transfer data did not match declared type! flavor="
                               + foundFlavor + " data=" + foundData.getClass());
            return false;
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: data no longer available");
            return false;
        }

        System.out.println("TRANSFER: Found supported flavor \"" + foundFlavor.getHumanPresentableName() + "\""
                           + "\n\tflavor=" + foundFlavor
                           + "\n\t  data=[" + foundData + "]");

        boolean success = false;

        if (fileList != null) {
            if (debug) System.out.println("\tLIST, size= " + fileList.size());
            java.util.Iterator iter = fileList.iterator();
            int x = dropLocation.x;
            int y = dropLocation.y;

            List addedNodes = new java.util.ArrayList();
            while (iter.hasNext()) {
                File file = (File) iter.next();

                //String resourceSpec = file.getPath();
                String resourceSpec = "file://" + file.getPath();
                String resourceName = file.getName();
                
                if (file.getPath().toLowerCase().endsWith(".url")) {
                    // Search a windows .url file (an internet shortcut)
                    // for the actual web reference.
                    String url = convertWindowsURLShortCutToURL(file);
                    if (url != null) {
                       //resourceSpec = url;
                          resourceSpec = "file://" + url;
                        if (file.getName().length() > 4)
                            resourceName = file.getName().substring(0, file.getName().length() - 4);
                        else
                            resourceName = file.getName();
                    }
                }
                
                //if (debug) System.out.println("\t" + file.getClass().getName() + " " + file);
                //if (hitComponent != null && fileList.size() == 1) {
                
                if (hitComponent != null) {
                    if (createAsChildren) {
                        ((LWNode)hitComponent).addChild(createNewNode(resourceSpec, resourceName, null));
                    } else {
                        hitComponent.setResource(resourceSpec);
                    }
                } else {
                    addedNodes.add(createNewNode(resourceSpec, resourceName, new java.awt.Point(x, y)));
                    x += 15;
                    y += 15;
                }
                success = true;
            }
            if (addedNodes.size() > 0)
                VUE.getSelection().setTo(addedNodes.iterator());
         
        } else if (resourceList != null) {
            if (resourceList.size() == 1 && hitComponent != null && overwriteResource) {
                // modifier key was down: force resetting of the resource
                hitComponent.setResource((Resource)resourceList.get(0));
            } else {
                java.util.Iterator iter = resourceList.iterator();
                int x = dropLocation.x;
                int y = dropLocation.y;
                while (iter.hasNext()) {
                    Resource resource = (Resource) iter.next();
                    if (createAsChildren) {
                        ((LWNode)hitComponent).addChild(createNewNode(resource, null));
                    } else {
                        createNewNode(resource, new java.awt.Point(x,y));
                        x += 15;
                        y += 15;
                    }
                    success = true;
                }
            }
        } else if (droppedText != null) {
            // Attempt to make a URL of any string dropped -- if fails,
            // just treat as regular pasted text.  todo: if newlines
            // in middle of string, don't do this, or possibly attempt
            // to split into list of multiple URL's (tho only if *every*
            // line succeeds as a URL -- prob too hairy to bother)

            String[] rows = droppedText.split("\n");
            URL url = null;
            String resourceTitle = null;

            if (rows.length < 3) {
                try {
                    url = new URL(rows[0]);
                } catch (MalformedURLException ex) {}
                if (rows.length > 1) {
                    // Current version of Mozilla (at least on Windows XP, as of 2004-02-22)
                    // includes the HTML <title> as second row of text.
                    resourceTitle = rows[1];
                }
            }

            if (url != null) {
                if (hitComponent != null) {
                    if (createAsChildren)
                        ((LWNode)hitComponent).addChild(createNewNode(url.toString(), resourceTitle, dropLocation));
                    else
                        hitComponent.setResource(url.toString());
                } else {
                    createNewNode(url.toString(), resourceTitle, dropLocation);
                }
            } else {
                createNewTextNode(droppedText, dropLocation);
            }
            success = true;
        }

        if (success)
            VUE.getUndoManager().mark("Drop");

        return success;
    }

    private static final Pattern URL_Line = Pattern.compile(".*^URL=([^\r\n]+).*", Pattern.MULTILINE|Pattern.DOTALL);
    private String convertWindowsURLShortCutToURL(File file)
    {
        String url = null;
        try {
            if (debug) System.out.println("*** Searching for URL in: " + file);
            FileInputStream is = new FileInputStream(file);
            byte[] buf = new byte[2048]; // if not in first 2048, don't bother
            int len = is.read(buf);
            is.close();
            String str = new String(buf, 0, len);
            if (debug) System.out.println("*** size="+str.length() +"["+str+"]");
            Matcher m = URL_Line.matcher(str);
            if (m.lookingAt()) {
                url = m.group(1);
                if (url != null)
                    url = url.trim();
                if (debug) System.out.println("*** FOUND URL ["+url+"]");
                int i = url.indexOf("|/");
                if (i > -1) {
                    // odd: have found "file:///D|/dir/file.html" example
                    // where '|' is where ':' should be -- still works
                    // for Windows 2000 as a shortcut, but NOT using
                    // Windows 2000 url DLL, so VUE can't open it.
                    url = url.substring(0,i) + ":" + url.substring(i+1);
                    System.out.println("**PATCHED URL ["+url+"]");
                }
                // if this is a file:/// url to a local html page,
                // AND we can determine that we're on another computer
                // accessing this file via the network (can we?)
                // then we should not covert this shortcut.
                // Okay, this is good enough for now, tho it also
                // won't end up converting a bad shortcut, and
                // ideally that wouldn't be our decision.
                // [this is not worth it]
                /*
                URL u = new URL(url);
                if (u.getProtocol().equals("file")) {
                    File f = new File(u.getFile());
                    if (!f.exists()) {
                        url = null;
                        System.out.println("***  BAD FILE ["+f+"]");
                    }
                }
                */
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return url;
    }

        
    

    private String readTextFlavor(DataFlavor flavor, Transferable transfer)
    {
        java.io.Reader reader = null;
        String value = null;
        try {
            reader = flavor.getReaderForText(transfer);
            if (debug) System.out.println("\treader=" + reader);
            char buf[] = new char[512];
            int got = reader.read(buf);
            value = new String(buf, 0, got);
            if (debug) System.out.println("\t[" + value + "]");
            if (reader.read() != -1)
                System.out.println("there was more data in the reader");
        } catch (Exception e) {
            System.err.println("readTextFlavor: " + e);
        }
        return value;
    }

    // todo?: change this to a resource-dropped event (w/location)
    // then we don't even have to know about the map, or the MapViewer
    // which for instance can do the zoom conversion of the drop location
    // for us.
    private LWNode createNewNode(String resourceName, String resourceTitle, java.awt.Point p)
    {
        Resource resource = new MapResource(resourceName);

        if (resourceTitle == null) {
            ((MapResource)resource).setTitleFromContent();
            if (resource.getTitle() != null)
                resourceTitle = resource.getTitle();
            else
                resourceTitle = createResourceTitle(resourceName);
        }

        LWNode node = NodeTool.createNode(resourceTitle);
        node.setResource(resource);
        if (p != null) {
            node.setCenterAt(dropToMapLocation(p));
            viewer.getMap().addNode(node);            //set selection to node?
        } // else: special case: no node location, so we're creating a child node -- don't add to map
        return node;
    }

    private LWNode createNewNode(Resource resource, java.awt.Point p)
    {
        String title;
        if (resource.getTitle() != null)
            title = resource.getTitle();
        else
            title = resource.getSpec();
        LWNode node = NodeTool.createNode(title);
        node.setResource(resource);
        if (p != null) {
            node.setCenterAt(dropToMapLocation(p));
            viewer.getMap().addNode(node);            //set selection to node?
        } // else: special case: no node location, so we're creating a child node -- don't add to map
        return node;
        
    }
    /*
    private LWNode createNewNode(File file, Point p)
    {
        // TODO BUG: adding the file:/// here produces inconsistent results --
        // that needs to be done in the Resource object!
        return createNewNode("file://"+file.toString(), file.getName(), p);
    }
    */
        

    private void createNewTextNode(String text, java.awt.Point p)
    {
        LWNode node = NodeTool.createTextNode(text);
        node.setCenterAt(dropToMapLocation(p));
        viewer.getMap().addNode(node);
    }

    private void createNewNode(java.awt.Image image, java.awt.Point p)
    {
        // todo: query the NodeTool for current node shape, etc.
        LWNode node = NodeTool.createNode();
        node.setCenterAt(dropToMapLocation(p));
        node.setImage(image);
        node.setNotes(image.toString());
        viewer.getMap().addNode(node);
        //set selection to node?
        
        /*
        String label = "[image]";
        if (image instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) image;
            label = "[image "
                + bi.getWidth() + "x"
                + bi.getHeight()
                + " type " + bi.getType()
                + "]";
            //System.out.println("BufferedImage: " + bi.getColorModel());
            // is null System.out.println("BufferedImage props: " + java.util.Arrays.asList(bi.getPropertyNames()));
            }*/
    }
  /**  
    private void createNewNode(Asset asset, java.awt.Point p) {
        String resourceTitle = "Fedora Node";
        Resource resource =new Resource(resourceTitle);
        try {
            resourceTitle = asset.getDisplayName();
             resource.setAsset(asset);
        } catch(Exception e) { System.out.println("MapDropTarget.createNewNode " +e ) ; }
      
       
        LWNode node = NodeTool.createNode(resourceTitle);
        node.setCenterAt(dropToMapLocation(p));
        node.setResource(resource);
        viewer.getMap().addNode(node);
    }
   */
    private Point2D dropToMapLocation(java.awt.Point p)
    {
        return dropToMapLocation(p.x, p.y);
    }

    private Point2D dropToMapLocation(int x, int y)
    {
        return viewer.screenToMapPoint(x, y);
        /*
        java.awt.Insets mapInsets = viewer.getInsets();
        java.awt.Point mapLocation = viewer.getLocation();
        System.out.println("viewer insets=" + mapInsets);
        System.out.println("viewer location=" + mapLocation);
        x -= mapLocation.x;
        y -= mapLocation.y;
        x -= mapInsets.left;
        y -= mapInsets.top;
        */
    }


    private String createResourceTitle(String resourceName)
    {
        int i = resourceName.lastIndexOf('/');//TODO: fileSeparator? test on PC
        if (i == resourceName.length() - 1)
            return resourceName;
        else
            return i > 0 ? resourceName.substring(i+1) : resourceName;
        // todo: go search the HTML for <title>
    }

    /*
    public boolean OLD_processTransferable(Transferable transfer, DropTargetDropEvent e)
    {
        Point dropLocation = null;
        int dropAction = DnDConstants.ACTION_MOVE; // default action

        if (e != null) {
            dropLocation = e.getLocation();
            dropAction = e.getDropAction();
        }

        boolean modifierKeyWasDown = (dropAction == DnDConstants.ACTION_COPY);

        LWComponent hitComponent = null;

        if (dropLocation != null) {
            hitComponent = viewer.getMap().findChildAt(dropToMapLocation(dropLocation));
            System.out.println("\thitComponent=" + hitComponent);
        }
        
        // if no drop location (e.g., we did a "Paste") then assume where
        // they last clicked.
        if (dropLocation == null)
            dropLocation = viewer.getLastMousePressPoint();
        //dropLocation = new java.awt.Point(viewer.getWidth()/2, viewer.getHeight()/2);
        
        boolean success = false;


        String resourceName = null;
        String droppedText = null;
        java.awt.Image droppedImage = null;
        java.util.List fileList = null;
        java.util.List assetList = null;
        DataFlavor foundFlavor = null;
        Object foundData = null;

        DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
        if (debug) System.out.println("TRANSFER: found " + dataFlavors.length + " dataFlavors");
        dumpFlavors(transfer);

        try {
            if (VueDragTreeNodeSelection.assetFlavor == null)
                System.err.println("ASSET FLAVOR IS NULL");
                
            if (transfer.isDataFlavorSupported(VueDragTreeNodeSelection.assetFlavor)) {
                
                foundFlavor = VueDragTreeNodeSelection.assetFlavor;
                foundData = transfer.getTransferData(foundFlavor);
                assetList = (java.util.List) foundData;
            
            } else if (transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {

                foundFlavor = DataFlavor.javaFileListFlavor;
                foundData = transfer.getTransferData(foundFlavor);
                fileList = (java.util.List) foundData;

            } else if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            
                foundFlavor = DataFlavor.stringFlavor;
                foundData = transfer.getTransferData(DataFlavor.stringFlavor);
                droppedText = (String) foundData;
            
            } else {
                System.out.println("TRANSFER: found no supported dataFlavors");
                dumpFlavors(transfer);
                return false;
            }
        } catch (UnsupportedFlavorException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: Transfer lied about supporting " + foundFlavor);
            return false;
        } catch (ClassCastException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: Transfer data did not match declared type! flavor="
                               + foundFlavor + " data=" + foundData.getClass());
            return false;
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: data no longer available");
            return false;
        }

        System.out.println("TRANSFER: Found supported flavor " + foundFlavor
                           + "\n\tdata=[" + foundData + "]");

        
        for (int i = 0; i < dataFlavors.length; i++) {
            DataFlavor flavor = dataFlavors[i];
            Object data = null;
            //System.out.println("DATA FLAVOR "+flavor+"  Mime type" +flavor.getHumanPresentableName());
            
            //if (debug) System.out.print("FLAVOR " + (i<10?" ":"") + i + " " + flavor.getMimeType());
            try {
                data = transfer.getTransferData(flavor);
            } catch (Exception ex) {
                System.out.println("\tEXCEPTION: getTransferData: " + ex);
            }
            //if (debug) System.out.println("\ttransferData=" + data);

            try {
                if (data instanceof java.awt.Image) {
                    droppedImage = (java.awt.Image) data;
                    if (debug) System.out.println("TRANSFER: found image " + droppedImage);
                    break;
                } else if (flavor.isFlavorJavaFileListType()) {
                    fileList = (java.util.List) transfer.getTransferData(flavor);
                    break;
                } else if (flavor.getHumanPresentableName().equals("asset")) {
                    System.out.println("ASSET FOUND");
                    assetList = (java.util.List) transfer.getTransferData(flavor);
                    break;
                } else if (false&&flavor.equals(DataFlavor.stringFlavor)) {
                    
                } else if (flavor.getMimeType().startsWith(MIME_TYPE_TEXT_PLAIN))
                    //} else if (flavor.isFlavorTextType() || flavor.getMimeType().startsWith(MIME_TYPE_TEXT_PLAIN))
                    // flavor.isFlavorTextType() is picking up text/html, etc, which we don't want here.
                {
                    // checking isFlavorTextType() above should be
                    // enough, but some Windows apps (e.g.,
                    // Netscape-6) are leading the flavor list with
                    // 20-30 mime-types of "text/uri-list", but the
                    // reader only ever spits out the first character.

                    // todo: handle RTF via text.trf.RTFReader(StyledDocument output)
                    //  -- will need to figure out how to persist a styled document tho...
                    // see text.rtf.RTFGenerator -- all of those accessed
                    // thru the RTFEditorKit...
                    droppedText = readTextFlavor(flavor, transfer);
                    if (droppedText != null)
                        break;
                    
                } else {
                    //System.out.println("Unhandled flavor: " + flavor);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                //System.out.println(ex);
                continue;
            }
        }

        if (droppedImage != null) {
            createNewNode(droppedImage, dropLocation);
            success = true;
        } else if (fileList != null) {
            if (debug) System.out.println("\tLIST, size= " + fileList.size());
            java.util.Iterator iter = fileList.iterator();
            int x = dropLocation.x;
            int y = dropLocation.y;

            while (iter.hasNext()) {
                File file = (File) iter.next();
                if (debug) System.out.println("\t" + file.getClass().getName() + " " + file);
                if (hitComponent != null && fileList.size() == 1) {
                    if (!modifierKeyWasDown && hitComponent instanceof LWNode) {
                        ((LWNode)hitComponent).addChild(createNewNode(file, null));
                    } else {
                        hitComponent.setResource(file.toString());
                    }
                } else {
                    //createNewNode("file:///"+file.toString(), file.getName(), new java.awt.Point(x, y));
                    createNewNode(file, new java.awt.Point(x, y));
                    x += 15;
                    y += 15;
                }
                success = true;
            }
            
        } else if (assetList != null) {
            java.util.Iterator iter = assetList.iterator();
            int x = dropLocation.x;
            int y = dropLocation.y;
            while(iter.hasNext()) {
                Asset asset = (Asset) iter.next();
                createNewNode(asset, new java.awt.Point(x,y));
                x += 15;
                y+= 15;
                success = true;
            }
        } else if (resourceName != null) { // Christ! - what happened to this code?? resourceName is never set anywhere!
            if (hitComponent != null)
                hitComponent.setResource(resourceName);
            else
                createNewNode(resourceName, null, dropLocation);
            success = true;
        } else if (droppedText != null) {
            createNewTextNode(droppedText, dropLocation);
            success = true;
        }

        return success;
    }
    */    
    
}

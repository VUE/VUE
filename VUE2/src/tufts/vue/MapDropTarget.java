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


/**
 * Handle the dropping of drags mediated by host operating system onto the map.
 * Right now all we handle are the dropping of File's & URL's.
 */

class MapDropTarget
    implements java.awt.dnd.DropTargetListener
{
    static final String MIME_TYPE_MAC_URLN = "application/x-mac-ostype-75726c6e";
    // 75726c6e="URLN" -- mac uses this type for a flavor containing the title of a web document
    // this existed in 1.3, but apparently went away in 1.4.
    static final String MIME_TYPE_TEXT_PLAIN = "text/plain";

    private static final int ACCEPTABLE_DROP_TYPES =
        DnDConstants.ACTION_COPY | // 1
        DnDConstants.ACTION_LINK | // 0x40000000
        0xFFFFFF;
        //DnDConstants.ACTION_MOVE; // 2
        // Do NOT include MOVE, or dragging a URL from the IE address bar becomes
        // a denied drag option!  Even dragged text from IE becomes disabled.
        // FYI, also, "move" doesn't appear to actually ever mean delete original source.
        // Putting this in makes certial special windows files "available"
        // for drag (e.g., a desktop "hard" reference link), yet there are never
        // any data-flavors available to process it, so we might as well indicate
        // that we can't accept it.

    private final static boolean debug = true;
    private final static boolean CenterNodesOnDrop = true;
    
    private MapViewer viewer;

    public MapDropTarget(MapViewer viewer)
    {
       this.viewer = viewer;
    }

    public void dragEnter(DropTargetDragEvent e)
    {
        //e.acceptDrag(ACCEPTABLE_DROP_TYPES);
        //if (VueUtil.isMacPlatform()) e.acceptDrag(DnDConstants.ACTION_COPY);
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

        // doing this will turn on green "+" icon as the default, which is appropriate,
        // yet doesn't allow for changing the indicator to the shortcut icon if CTRL
        // is pressed, and the drop code for that is the huge bogus # on the mac,
        // so we're skipping this for now.
        //if (VueUtil.isMacPlatform())
        //  e.acceptDrag(DnDConstants.ACTION_COPY);

        //e.acceptDrag(ACCEPTABLE_DROP_TYPES);
    }

    public static String dropName(int dropAction) {
        String name = "";
        if ((dropAction & DnDConstants.ACTION_COPY) != 0)
            name += "COPY";
        if ((dropAction & DnDConstants.ACTION_MOVE) != 0)
            name += "MOVE";
        if ((dropAction & DnDConstants.ACTION_LINK) != 0)
            name += "LINK";
        if (name.length() < 1)
            name = "NONE";
        name += " (0x" + Integer.toHexString(dropAction) + ")";
        return name;
    }
    
    public void dragExit(DropTargetEvent e)
    {
        if (debug) System.out.println("MapDropTarget: dragExit " + e);
    }

    public void dropActionChanged(DropTargetDragEvent e)
    {
        if (debug) System.out.println("MapDropTarget: dropActionChanged to " + dropName(e.getDropAction()));

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
                                      + "\n\tdropAction is " + dropName(e.getDropAction())
                                      + "\n\tsourceActions are " + dropName(e.getSourceActions())
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

    /** attempt to make a URL from a string: return null if malformed */
    private static URL makeURL(String s) {
        try {
            return new URL(s);
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    

    //    public boolean processTransferable(Transferable transfer, java.awt.Point dropLocation)
    /**
     * Process any transferrable: @param e can be null if don't have a drop event
     * (e.g., could use to process clipboard contents as well as drop events)
     */
    public boolean processTransferable(Transferable transfer, DropTargetDropEvent e)
    {
        // Make us the active viewer.  Otherwise we'll need special code for
        // repaint updates and setting the selection.
        // Okay, that's no good: they can still switch to another viewer after the drop begins.
        //VUE.setActiveViewer(viewer);
        
        Point dropLocation = null;
        int dropAction = DnDConstants.ACTION_COPY; // default action, in case no DropTargetDropEvent
        //boolean modifierKeyWasDown = false;

        // On current JVM's on Mac and PC, default action for dragging
        // a desktop item is MOVE, and holding CTRL down (both
        // platforms) changes action to COPY.  However, when dragging
        // a link from Internet Explorer on Win2k, or Safari on OS X
        // 10.4.2, CTRL doesn't change drop action at all, SHIFT
        // changes drop action to NONE, and only CTRL-SHIFT changes
        // action to LINK, which fortunately is at least the same on
        // the mac: CTRL-SHIFT gets you LINK.
        //
        // Note: In both Safari & IE6/W2K, dragging an image from
        // within a web page will NOT allow ACTION_LINK, so we can't
        // change a resource that way on the mac.  Also note that
        // Safari will give you the real URL of the image, where as at
        // least as of IE 6 on Win2k, it will only give you the image
        // file from your cache.  (IE does also give you HTML snippets
        // as data transfer options, with the IMG tag, but it gives
        // you no base URL to add to the relative locations usually
        // named in IMG tags!)
        
        if (e != null) {
            dropLocation = e.getLocation();
            dropAction = e.getDropAction();

            if (DEBUG.DND) System.out.println("MapDropTarget: processTransferable: dropAction is " + dropName(e.getDropAction()));

            // BELOW HAS CHANGED AS OF AT LEAST OS X 10.4.2 / JVM 1.4.2:
            // Mac now ACTION_MOVE as the default action, just like the PC
            // 
            // [OLD] ACTION_MOVE action is default action on PC, COPY on the
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

            // [ The below has been fixed as of at least OS X 10.4.2 JVM 1.4.2 ]
            // 
            // [OLD] FYI, Mac OS X 10.2.8/JVM 1.4.1_01 is not telling us about
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
            if (hitComponent instanceof LWImage) { // todo: does LWComponent accept drop events...
                System.out.println("\thitComponent=" + hitComponent + " (ignored)");
                hitComponent = null;
            } else 
                System.out.println("\thitComponent=" + hitComponent);
        }
        
        /*
        // Now: when either CTRl or ALT is down on the mac, drop action changes from
        // default of 2 (MOVE) to 1 (COPY)
        if (VueUtil.isMacPlatform()) {
            if (dropAction > 2) // hack: drop action == 1073741842 with Ctrl down on mac [OLD OS X / JVM].
                modifierKeyWasDown = true;
        } else {
            // not safe: see above re: unpredictable due to drags from browsers and/or WinXP
            //if (dropAction == 1)
            //modifierKeyWasDown = true;
        }
        boolean createAsChildren = !modifierKeyWasDown && hitComponent instanceof LWNode;
        boolean overwriteResource = modifierKeyWasDown;
        */
            
        boolean overwriteResource = (dropAction == DnDConstants.ACTION_LINK);
        boolean createAsChildren = !overwriteResource && hitComponent instanceof LWNode;

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
                           + "\n\t  data=[" + foundData + "]"
                           //+ "\n\tdropptedText=[" + droppedText + "]"
                           );

        boolean success = false;

        // TODO: we can handle Mac .fileloc's if we check multiple data-flavors: the initial LIST
        // flavor gives us the .fileloc, which we could even pull a name from if we want, and in
        // any case, a later STRING data-flavor actually gives us the source of the link!
        // SAME APPLIES TO .webloc files... AND .textClipping files
        // Of course, if they drop multple ones of these, we're screwed, as only the last
        // one gets translated for us in the later string data-flavor.  Oh well -- at least
        // we can handle the single case if we want.

        if (fileList != null) {
            if (DEBUG.DND) System.out.println("\tHANDLING LIST, size= " + fileList.size());
            java.util.Iterator iter = fileList.iterator();
            int x = dropLocation.x;
            int y = dropLocation.y;

            List addedNodes = new java.util.ArrayList();
            while (iter.hasNext()) {
                File file = (File) iter.next();

                //String resourceSpec = "file://" + file.getPath(); // why was this done? it breaks URL shortcuts
                String resourceSpec = file.getPath();
                String resourceName = file.getName();
                String path = file.getPath();
                
                if (path.toLowerCase().endsWith(".url")) {
                    // Search a windows .url file (an internet shortcut)
                    // for the actual web reference.
                    String url = convertWindowsURLShortCutToURL(file);
                    if (url != null) {
                       resourceSpec = url;
                       // why was this done?  It breaks URL shortcuts...
                       //   resourceSpec = "file://" + url;
                        if (file.getName().length() > 4)
                            resourceName = file.getName().substring(0, file.getName().length() - 4);
                        else
                            resourceName = file.getName();
                    }
                } else if (path.endsWith(".textClipping")  || path.endsWith(".webloc") || path.endsWith(".fileloc")) {

                    if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {

                        // Unforunately, this only works if there's ONE ITEM IN THE LIST,
                        // or more accurately, the last item in the list, or perhaps even
                        // more accurately, the item that also shows up as the application/x-java-url.
                        // Which is why ultimately we'll want to put all this type of processing
                        // in a full-and-fancy filing OSID to end all filing OSID's, that'll
                        // handle windows .url shortcuts, the above mac cases plus mac aliases, etc, etc.

                        String unicodeString;
                        try {
                            unicodeString = (String) transfer.getTransferData(DataFlavor.stringFlavor);
                            System.out.println("*** GOT MAC REDIRECT DATA [" + unicodeString + "]");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        // for textClipping, the string data is raw dropped text, for webloc,
                        // it's URL be we already have text->URL in dropped text code below,
                        // and same for .fileloc...  REFACTOR THIS MESS.

                        //resourceSpec = unicodeString; 
                        //resourceName = 
                        // NOW WE NEED TO JUMP DOWN TO HANDLING DROPPED TEXT...
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
            if (DEBUG.DND) System.out.println("\tHANDLING RESOURCE LIST, size= " + resourceList.size());
            //if (resourceList.size() == 1 && hitComponent != null && overwriteResource) {
            if (resourceList.size() == 1 && hitComponent != null && !createAsChildren) {
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
            if (DEBUG.DND) System.out.println("\tHANDLING DROPPED TEXT");
            // Attempt to make a URL of any string dropped -- if fails,
            // just treat as regular pasted text.  todo: if newlines
            // in middle of string, don't do this, or possibly attempt
            // to split into list of multiple URL's (tho only if *every*
            // line succeeds as a URL -- prob too hairy to bother)

            String[] rows = droppedText.split("\n");
            URL url = null;
            String resourceTitle = null;

            if (rows.length < 3) {
                url = makeURL(rows[0]);
                if (rows.length > 1) {
                    // Current version of Mozilla (at least on Windows XP, as of 2004-02-22)
                    // includes the HTML <title> as second row of text.
                    resourceTitle = rows[1];
                }
            }

            if (url != null && url.getQuery() != null) {
                final String query = url.getQuery();
                // special case for google image search:
                System.out.println("host " + url.getHost() + " query " + url.getQuery());

                if (DEBUG.DND) {
                    String[] pairs = query.split("&");
                    for (int i = 0; i < pairs.length; i++) {
                        System.out.println("query pair " + pairs[i]);
                    }
                }
                
                if ("images.google.com".equals(url.getHost()) && query.startsWith("imgurl=")) {
                    int endURLindex = query.indexOf('&');
                    String imageURL = query.substring(7, endURLindex);

                    //-------------------------------------------------------
                    // WIERD -- what is google doing here? or is %2520 some obscure
                    // whitespace code not understood on mac?
                    imageURL = imageURL.replaceAll("%2520", "%20");
                    //-------------------------------------------------------
                    
                    System.out.println("redirect to google image search url " + imageURL);
                    url = makeURL(imageURL);
                }
            }

                
            if (url != null) {
                if (hitComponent != null) {
                    if (createAsChildren) {
                        ((LWNode)hitComponent).addChild(createNewNode(url.toString(), resourceTitle, dropLocation));
                    } else {
                        hitComponent.setResource(url.toString());
                        // HACK: clean this up:  resource should load meta-data on CREATION.
                        ((MapResource)hitComponent.getResource()).setTitleFromContentAsync(hitComponent);
                    }
                } else {
                    // if action is LINK and this is an image, create a node, not the image,
                    // otherwise LINK when not over a node does nothing special.
                    createNewNode(url.toString(), resourceTitle, dropLocation);
                }
            } else {
                createNewTextNode(droppedText, dropLocation);
            }
            success = true;
        }

        if (success)
            viewer.getMap().getUndoManager().mark("Drop");

        return success;
    }

    private static final Pattern URL_Line = Pattern.compile(".*^URL=([^\r\n]+).*", Pattern.MULTILINE|Pattern.DOTALL);
    private static String convertWindowsURLShortCutToURL(File file)
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
    /**
     * @param resourceSpec The simplest, raw string reference handed to us from the operating system as a name for the object
     * @param resourceName A name for the new node that will contain the resource
     */
    // TODO: handle case of a dropped reference to a local HTML file, where we want to use
    // the file name as the generated node name, and yet ALSO want to set the resourceTitle
    // via an HTML scan of the file (if one can be found).

    // HANDLES DROP OF A URL
    
    private LWComponent createNewNode(String resourceSpec, String resourceName, java.awt.Point p)
    {
        MapResource resource = new MapResource(resourceSpec);

        if (resourceName == null) {
            resource.setTitleFromContent();
            if (resource.getTitle() != null)
                resourceName = resource.getTitle();
            else
                resourceName = createResourceTitle(resourceSpec);
        } else if (resourceSpec.endsWith(".html") || resourceSpec.endsWith(".htm")) {
            // attempt to scan a local file
            resource.setTitleFromContent();
        }

        final LWComponent node;

        if (resource.isImage()) {
            System.out.println("IMAGE DROP ON " + viewer);
            node = new LWImage(resource, viewer.getMap().getUndoManager());
        } else {
            node = NodeTool.createNode(resourceName);
            node.setResource(resource);
        }

        if (p != null) {
            if (CenterNodesOnDrop)
                node.setCenterAt(dropToMapLocation(p));
            else
                node.setLocation(dropToMapLocation(p));
        }
        
        viewer.getMap().addLWC(node);

        // maybe just set us to be the active map?
        // In any case, do NOT set the selection to anything that is NOT from the active map.
        if (VUE.getActiveViewer() == this.viewer)
            VUE.getSelection().setTo(node);

        return node;
    }

    // HANDLES DROP OF EXISTING RESOURCE (from our own DR browser)
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
            if (CenterNodesOnDrop)
                node.setCenterAt(dropToMapLocation(p));
            else
                node.setLocation(dropToMapLocation(p));
            viewer.getMap().addNode(node);            //set selection to node?
        } // else: special case: no node location, so we're creating a child node -- don't add to map

// test code
if (MapResource.isImage(resource))
    node.addChild(new LWImage(resource));
        
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
        if (CenterNodesOnDrop)
            node.setCenterAt(dropToMapLocation(p));
        else
            node.setLocation(dropToMapLocation(p));
        viewer.getMap().addNode(node);
    }

    private void createNewNode(java.awt.Image image, java.awt.Point p)
    {
        // todo: query the NodeTool for current node shape, etc.
        LWNode node = NodeTool.createNode();
        if (CenterNodesOnDrop)
            node.setCenterAt(dropToMapLocation(p));
        else
            node.setLocation(dropToMapLocation(p));
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
        Point2D p = viewer.screenToMapPoint(x, y);
        //if (DEBUG.DND) System.out.println("drop location " + x + "," + y + " mapLoc=" + p);
        return p;
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
    
}

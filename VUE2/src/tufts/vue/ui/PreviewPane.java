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

package tufts.vue.ui;

import tufts.Util;
import tufts.vue.*;
import tufts.vue.gui.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;

import java.awt.datatransfer.*;


/**
 * Display a preview of the selected resource.  E.g., and image or an icon.
 *
 * @version $Revision: 1.30 $ / $Date: 2008-05-23 15:10:40 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class PreviewPane extends JPanel
    implements Images.Listener, Runnable, MouseWheelListener

{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(PreviewPane.class);
    
    private final Image NoImage = VueResources.getImage("/icon_noimage64.gif");
    
    private static boolean FirstPreview = true;

    private Resource mResource;
    private Object mPreviewData;
    private Image mImage;
    private int mImageWidth;
    private int mImageHeight;
    private boolean isLoading = false;

    private final JLabel StatusLabel = new JLabel("(status)", JLabel.CENTER);

    private final int MinHeight = 128;
    private final int MaxHeight = 1024;
        
    PreviewPane() {
        super(new BorderLayout());
        setOpaque(false);

        //StatusLabel.setLineWrap(true);
        //StatusLabel.setAlignmentX(0.5f);
        //StatusLabel.setAlignmentY(0.5f);
        //StatusLabel.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        //StatusLabel.setBorder(new LineBorder(Color.red));
        StatusLabel.setForeground(Color.darkGray);
        StatusLabel.setVisible(false);
        add(StatusLabel);
        setHeight(MinHeight);

        addMouseListener(new java.awt.event.MouseAdapter() {    
                public void mouseClicked(MouseEvent me){
                    if (mResource != null && me.getClickCount() == 2)
                        mResource.displayContent();
                }
            });
        
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {    
                public void mouseDragged(MouseEvent me){
                    if (mResource != null) {
                        GUI.startSystemDrag(PreviewPane.this, me, mImage, new GUI.ResourceTransfer(mResource));
                        // start caching the non-preview versiomn of the image if it's not already there
                        // todo: make this a low-priority disk-cache only, then if it's actually
                        // dropped, we can create the in-memory image
                        //Images.getImage(mResource, PreviewPane.this);
                        // need to set preview data so that when this comes back, we don't ignore it.
                        // Tho then we need to not clear existing thumbnail, but can put "loading" over it...
                    }
                }
            });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        GUI.MouseWheelRelay.addListenerOrIntercept(this, this);
    }

    protected void setHeight(int h) {
        Dimension d = new Dimension(200,h);
        setMinimumSize(d);
        setPreferredSize(d);
    }
        
    public void mouseWheelMoved(MouseWheelEvent e) {

        if (GUI.noModifierKeysDown(e))
            return;

        e.consume();

        //int height = getPreferredSize().height;
        int height = getHeight();

        int rotation = e.getWheelRotation();
        if (rotation < 0)
            height += MinHeight;
        else if (rotation > 0)
            height -= MinHeight;

        if (height < MinHeight)
            height = MinHeight;
        else if (height > MaxHeight)
            height = MaxHeight;

        setHeight(height);
    }
    
    private void showLoadingStatus(Object data) {
        if (DEBUG.Enabled)
            StatusLabel.setText(""+data);
        else
            StatusLabel.setText("Loading...");
        StatusLabel.setVisible(true);
    }
    private void status(String msg) {
        StatusLabel.setText("<HTML>"+msg);
        StatusLabel.setVisible(true);
    }
    private void clearStatus() {
        StatusLabel.setVisible(false);
    }

    private static final boolean LazyLoadOnPaint = false;
    

    synchronized void loadResource(Resource r) {

        if (DEBUG.RESOURCE || DEBUG.IMAGE) out("loadResource: " + r);
            
        // CURRENTLY: if a URLResource has already cached image, it will
        // return SELF to be used again, otherwise, it returns
        // the URL (messy...)

        mResource = r;
        if (r != null)
            mPreviewData = r.getPreview();
        else
            mPreviewData = null;
        mImage = null;

//        if (mPreviewData == null && mResource.isImage())
//            mPreviewData = mResource;

        if (DEBUG.RESOURCE) out(" got preview: " + mPreviewData);

        // TODO: somehow, Component.isShowing starts lying to us(!!) if
        // we request too many image loads in quick succession
        // (try arrowing down a pathway list of items that all
        // have thumbshots)  This on Leopard Java 1.5 as of 2008-05-21.
        if (!LazyLoadOnPaint || isShowing()) { 

            if (DEBUG.IMAGE && !isShowing()) Log.debug("claims not visible on screen");
            loadPreview(mPreviewData);
            FirstPreview = false;

        } else {

            if (FirstPreview /*&& mPreviewData != null*/) {
                FirstPreview = false;
                //Widget.setExpanded(PreviewPane.this, true);
                // This now handled in Widget.java:
                //GUI.makeVisibleOnScreen(PreviewPane.this);
            } else {
                if (DEBUG.RESOURCE || DEBUG.IMAGE) out("not showing: no action");
            }
        }

    }

    private void loadPreview(Object previewData)
    {
        // todo: handle if preview is a Component, 
        // todo: handle a String as preview data.

        if (false /*&& r.getIcon() != null*/) { // these not currently valid from Osid2AssetResource (size=-1x-1)
            //displayIcon(r.getIcon());
        } else if (previewData == null) {
            displayImage(NoImage);
        } else if (previewData instanceof java.awt.Component) {
            Log.info("TODO: handle Component preview " + previewData);
            displayImage(NoImage);
        } else if (previewData != null) { // todo: check an Images.isImageableSource
            if (previewData instanceof Image)
                displayImage((Image)previewData);
            else
                loadImage(previewData);
        } else {
            displayImage(NoImage);
        }
    }

    // TODO: if this triggered from an LWImage selection, and LWImage had
    // an image error, also notify the LWImage of good data if it comes
    // in as a result of selection.

    // TODO: make the preview handling / image loading code in ResourceIcon generic
    // enough that PreviewPane can use a ResourceIcon instance or subclass, as the code
    // here is almost identical to that in ResourceIcon.

    private synchronized void loadImage(Object imageData) {
        if (DEBUG.IMAGE) out("loadImage " + Util.tags(imageData));

        // test of synchronous loading:
        //out("***GOT IMAGE " + Images.getImage(imageData));
        
        if (!Images.getImage(imageData, this)) {
            // will make callback to gotImage when we have it
            isLoading = true;
            showLoadingStatus(imageData);
        } else {
            // gotImage has already been called
            isLoading = false;
        }
    }


    /** @see Images.Listener */
    public void gotImageSize(Object imageSrc, int width, int height, long byteSize) {

        if (imageSrc != mPreviewData)
            return;
            
        mImageWidth = width;
        mImageHeight = height;
    }

    /** @see Images.Listener */
    public void gotBytes(Object imageSrc, long bytesSoFar) {}
    
    
    /** @see Images.Listener */
    public synchronized void gotImage(Object imageSrc, Image image, int w, int h) {

        if (imageSrc != mPreviewData && !imageSrc.equals(mPreviewData)) {
            if (DEBUG.Enabled) Log.info("skipping: image source != mPreviewData;\n\t"
                                        + Util.tags(imageSrc)
                                        + "\n\t" + Util.tags(mPreviewData));
        } else {
            displayImage(image);
            isLoading = false;
        }
    }
    
    /** @see Images.Listener */
    public synchronized void gotImageError(Object imageSrc, String msg) {

        if (imageSrc != mPreviewData) {
            return;
        } else {
            displayImage(NoImage);
            if (msg != null)
                status("Error: " + msg);
            isLoading = false;
        }
    }

    /*
      private void displayIcon(ImageIcon icon) {
      displayImage(icon.getImage());
      }
    */

    private void displayImage(Image image) {
        if (DEBUG.RESOURCE || DEBUG.IMAGE) out("displayImage " + Util.tags(image));

        mImage = image;
        if (mImage != null) {
            mImageWidth = mImage.getWidth(null);
            mImageHeight = mImage.getHeight(null);
            if (DEBUG.IMAGE) out("displayImage " + Util.tags(image) + "; " + mImageWidth + "x" + mImageHeight);
        }

        clearStatus();
        repaint();
    }

    public void run() {
        loadPreview(mPreviewData);
    }

    private void out(String s) {
        Log.debug(s);
    }
        

    /** draw the image into the current avilable space, scaling it down if needed (never scale up tho) */
    @Override
    public void paintComponent(Graphics g)
    {
        if (DEBUG.IMAGE) out("paint");

        if (mImage == null) {
            
            if (LazyLoadOnPaint) {
                if (!isLoading && mPreviewData != null) {
                    // TODO: fix this double-checked locking (not a safe idiom)
                    // don't even try LazyLoadOnPaint until this is fixed
                    synchronized (this) {
                        if (!isLoading && mPreviewData != null)
                            VUE.invokeAfterAWT(PreviewPane.this); // load the preview
                    }
                }
            }
            
            return;
        }
            
        //g.setColor(Color.black);
        //g.fillRect(0,0, w,h);

        double zoomFit;
        double netZoom;
        if (mImage == NoImage) {
            zoomFit = 1;
            netZoom = 1;
        } else {
            java.awt.geom.Rectangle2D imageBounds
                = new java.awt.geom.Rectangle2D.Float(0, 0, mImageWidth, mImageHeight);
            zoomFit = ZoomTool.computeZoomFit(getSize(),
                                              0,
                                              imageBounds,
                                              null,
                                              false);

            final double MaxZoom = getHeight() > MinHeight ? 2 : 1;
            
            if (zoomFit > 1 && zoomFit < 2)
                netZoom = 1;
//             if (zoomFit > 1 && zoomFit <= 1.25)
//                 netZoom = 1;
//             else if (zoomFit > 2 && zoomFit < 3)
//                 netZoom = 2;
            else if (zoomFit > MaxZoom)
                netZoom = MaxZoom;
            else
                netZoom = zoomFit;
        }
            

        final int drawW = (int) (mImageWidth * netZoom);
        final int drawH = (int) (mImageHeight * netZoom);
                                                     
        final int w = getWidth();
        final int h = getHeight();
            
        // center if drawable area is bigger than image
        int xoff = 0;
        int yoff = 0;
        if (drawW != w)
            xoff = (w - drawW) / 2;
        if (drawH != h)
            yoff = (h - drawH) / 2;
            
        //if (DEBUG.IMAGE) out("painting " + Util.tag(mImage) + "; into " + drawW + "x" + drawH);
        if (DEBUG.IMAGE) out(String.format("painting %s into %dx%d; zoomFit=%.1f%%; netZoom=%.1f%%;",
                                           Util.tags(mImage), drawW, drawH, zoomFit*100, netZoom*100));
        g.drawImage(mImage, xoff, yoff, drawW, drawH, null);
    }
}

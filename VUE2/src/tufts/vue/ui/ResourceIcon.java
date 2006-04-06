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

package tufts.vue.ui;

import tufts.Util;
import tufts.vue.*;
import tufts.vue.gui.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * An icon for the preview of a given resource.
 *
 * TODO: merge common code with PreviewPane, and perhaps put in a 3rd class
 * so can have multiple icons referencing the same underlying image.
 *
 * @version $Revision: 1.1 $ / $Date: 2006-04-06 01:35:01 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class ResourceIcon
    implements javax.swing.Icon, Images.Listener, Runnable
{
    private final Image NoImage = VueResources.getImage("NoImage");
    
    private Resource mResource;
    private Object mPreviewData;
    private Image mImage;
    private int mImageWidth;
    private int mImageHeight;
    private boolean isLoading = false;
    private Component mParent;

    private int mWidth = -1;
    private int mHeight = -1;

    //private final JLabel StatusLabel = new JLabel("(status)", JLabel.CENTER);


    /** Act like a regular Icon */
    public ResourceIcon(Resource r, int width, int height) {
        setSize(width, height);
        //StatusLabel.setVisible(false);
        //add(StatusLabel);
        //loadResource(r);
        mResource = r;
        out("Constructed " + width + "x" + height + " " + r);
    }
        
    /** Expand's to fit the size of the component it's painted into */
    public ResourceIcon() {}

    public void setSize(int w, int h) {
        if (w != mWidth || h != mHeight) {
            mWidth = w;
            mHeight = h;
            repaint();
        }
    }

    private void repaint() {
        if (mParent != null) {
            //if (DEBUG.IMAGE)
                out("repaint in " + GUI.name(mParent));
            mParent.repaint();
        }
        // FYI, this will not repaint if the parent is a DefaultTreeCellRenderer,
        // and the parent of the parent (a JLabel), is null, so we can't get that.
    }

    public int getIconWidth() { return mWidth; }
    public int getIconHeight() { return mHeight; }
    

    private void showLoadingStatus() {
        //StatusLabel.setText("Loading...");
        //StatusLabel.setVisible(true);
    }
    private void status(String msg) {
        //StatusLabel.setText("<HTML>"+msg);
        //StatusLabel.setVisible(true);
    }
    private void clearStatus() {
        //StatusLabel.setVisible(false);
    }
    

    synchronized void loadResource(Resource r) {

        if (DEBUG.RESOURCE || DEBUG.IMAGE) out("loadResource: " + Util.tag(r) + " " + r);
            
        mResource = r;
        if (r != null)
            mPreviewData = r.getPreview();
        else
            mPreviewData = null;
        mImage = null;

        if (mPreviewData == null && mResource.isImage())
            mPreviewData = mResource;

        loadPreview(mPreviewData);
    }

    private void loadPreview(Object previewData)
    {
        // todo: handle if preview is a Component, 
        // todo: handle a String as preview data.

        if (false /*&& r.getIcon() != null*/) { // these not currently valid from Osid2AssetResource (size=-1x-1)
            //displayIcon(r.getIcon());
        } else if (previewData instanceof java.awt.Component) {
            out("TODO: handle Component preview " + previewData);
            displayImage(NoImage);
        } else if (previewData != null) { // todo: check an Images.isImageableSource
            loadImage(previewData);
        } else {
            displayImage(NoImage);
        }
    }

    // TODO: if this triggered from an LWImage selection, and LWImage had
    // an image error, also notify the LWImage of good data if it comes
    // in as a result of selection.

    private synchronized void loadImage(Object imageData) {
        if (DEBUG.IMAGE) out("loadImage " + imageData);

        // test of synchronous loading:
        //out("***GOT IMAGE " + Images.getImage(imageData));
        
        if (!Images.getImage(imageData, this)) {
            // will make callback to gotImage when we have it
            isLoading = true;
            showLoadingStatus();
        } else {
            // gotImage has already been called
            isLoading = false;
        }
    }


    /** @see Images.Listener */
    public synchronized void gotImageSize(Object imageSrc, int width, int height) {

        if (imageSrc != mPreviewData)
            return;
            
        mImageWidth = width;
        mImageHeight = height;
    }
    
    /** @see Images.Listener */
    public synchronized void gotImage(Object imageSrc, Image image, int w, int h) {

        if (imageSrc != mPreviewData)
            return;
            
        displayImage(image);
        isLoading = false;
    }
    /** @see Images.Listener */
    public synchronized void gotImageError(Object imageSrc, String msg) {

        if (imageSrc != mPreviewData)
            return;
            
        displayImage(NoImage);
        status("Error: " + msg);
        isLoading = false;
    }

    private void displayImage(Image image) {
        if (DEBUG.RESOURCE || DEBUG.IMAGE) out("displayImage " + Util.tag(image));

        mImage = image;
        if (mImage != null) {
            mImageWidth = mImage.getWidth(null);
            mImageHeight = mImage.getHeight(null);
            if (DEBUG.IMAGE) out("displayImage " + mImageWidth + "x" + mImageHeight);
        }

        clearStatus();
        repaint();
    }

    public synchronized void run() {
        //loadPreview(mPreviewData);
        if (!isLoading)
            loadResource(mResource);
    }

    private static final double MaxZoom = 2.0;
    
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        final boolean fillComponent = (mWidth < 1);
        
        mParent = c;
        
        if (DEBUG.IMAGE) out("paint, parent=" + GUI.name(c));

        if (!fillComponent) {
            g.setColor(Color.gray);
            g.drawRect(x, y, mWidth-1, mHeight-1);
        }
            
        if (mImage == null) {

            if (!isLoading /*&& mPreviewData != null*/) {
                synchronized (this) {
                    if (!isLoading /*&& mPreviewData != null*/)
                        VUE.invokeAfterAWT(ResourceIcon.this); // load the preview
                }
            }
            return;
        }

        int fitWidth, fitHeight;
        final Dimension maxImageSize;

        if (fillComponent) {
            // fill the given component
            fitWidth = c.getWidth();
            fitHeight = c.getHeight();
            maxImageSize = c.getSize();
        } else {
            // paint at our fixed size
            fitWidth = mWidth;
            fitHeight = mHeight;

            // Leave room for border drawn above and
            // 1 pix of whitespace around it
            maxImageSize = new Dimension(fitWidth - 4,
                                         fitHeight - 4);
            if (DEBUG.IMAGE) out("painting into " + GUI.name(maxImageSize));
        }


        double zoomFit;
        if (mImage == NoImage && fillComponent) {
            zoomFit = 1;
        } else {
            java.awt.geom.Rectangle2D imageBounds
                = new java.awt.geom.Rectangle2D.Float(0, 0, mImageWidth, mImageHeight);
            zoomFit = ZoomTool.computeZoomFit(maxImageSize,
                                              0,
                                              imageBounds,
                                              null,
                                              false);
            if (zoomFit > MaxZoom)
                zoomFit = MaxZoom;
        }
            

        final int drawW = (int) (mImageWidth * zoomFit);
        final int drawH = (int) (mImageHeight * zoomFit);
                                                     
        int xoff = x;
        int yoff = y;

        // center if drawable area is bigger than image
        if (drawW != fitWidth)
            xoff += (fitWidth - drawW) / 2;
        if (drawH != fitHeight)
            yoff += (fitHeight - drawH) / 2;
            
        if (DEBUG.IMAGE) out("painting " + Util.tag(mImage));
        g.drawImage(mImage, xoff, yoff, drawW, drawH, null);
    }

    private void out(String s) {
        System.out.println("ResourceIcon: " + s);
    }
        
    
}

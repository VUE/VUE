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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;

import tufts.Util;
import tufts.vue.gui.*;
import tufts.vue.NotePanel;
import tufts.vue.filter.NodeFilterEditor;

/**
 * Display information about the selected resource, including "spec" (e.g., URL),
 * meta-data, and if available: title and a preview (e.g., an image preview or icon).
 *
 * @version $Revision: 1.18 $ / $Date: 2006-03-29 20:27:14 $ / $Author: sfraize $
 */

//public class ResourcePanel extends WidgetStack
// TODO: if we keep this with all the node components included (not just Resource), need
// to rename this InspectorPanel or something.
public class ResourcePanel extends JPanel
    implements VueConstants, LWSelection.Listener, ResourceSelection.Listener
{

    // fields for the Summary Pane
    private final JTextComponent mTitleField = new JTextArea();
    private final JTextComponent mWhereField = new JTextPane();
    private final JLabel mSizeField = new JLabel();

    private final Image NoImage = VueResources.getImage("NoImage");

    private final boolean isMacAqua = GUI.isMacAqua();

    /* the displayed resource */
    //private Resource mResource;
    

    // Resource panes
    private final JPanel mSummary;
    private final PropertiesEditor mMetaData;
    private final PreviewPane mPreview;
    
    // Node panes
    private final NotePanel mNotePanel = new NotePanel();
    private final UserMetaData mUserMetaData = new UserMetaData();
    private final NodeTree mNodeTree = new NodeTree();
    
    public ResourcePanel()
    {
        super(new BorderLayout());
        
        mSummary = new SummaryPane();
        mMetaData = new PropertiesEditor(false);
        mPreview = new PreviewPane();

        WidgetStack stack = new WidgetStack();
        
        //add(mSummary, BorderLayout.NORTH);

        stack.addPane("Resource Summary",      mSummary, 0f);
        stack.addPane("Resource Meta Data",    mMetaData, 1f);
        stack.addPane("Resource Preview",      mPreview, 1f);

        stack.addPane(mNotePanel, 1f);
        stack.addPane(mUserMetaData, 1f);
        stack.addPane(mNodeTree, 1f);

        add(stack, BorderLayout.CENTER);

        VUE.getSelection().addListener(this);
        VUE.getResourceSelection().addListener(this);
    }

    public void resourceSelectionChanged(ResourceSelection selection)
    {
        if (DEBUG.RESOURCE) out("resource selected: " + selection.get());
        showNodePanes(false);
        loadResource(selection.get());
    }

    public void selectionChanged(LWSelection selection) {
        showNodePanes(true);
        if (selection.isEmpty() || selection.size() > 1) {
            loadResource(null);
        } else {
            LWComponent c = selection.first();
            if (c.hasResource()) {
                loadResource(c.getResource());
                showResourcePanes(true);
            } else {
                showResourcePanes(false);
            }
            mUserMetaData.load(c);
            mNodeTree.load(c);
        }
    }
    
    private void loadResource(final Resource r) {
        
        if (DEBUG.RESOURCE) out("loadResource: " + r);
        
        if (r != null) {
            setAllEnabled(true);
            loadText(mWhereField, r.getSpec());
            loadText(mTitleField, r.getTitle());
        } else {
            // leave current display, but grayed out
            setAllEnabled(false);
            return;
        }
        

        long size = r.getSize();
        String ss = "";
        if (size >= 0)
            ss = VueUtil.abbrevBytes(size);
        mSizeField.setText(ss);
        
        //loading the metadata if it exists
        if (r != null) {
            java.util.Properties properties = r.getProperties();
            if (properties != null) {
                if (r.getType() == Resource.ASSET_FEDORA)
                    mMetaData.setProperties(properties, false);
                else
                    mMetaData.setProperties(properties, true);
            }
        } else {
            mMetaData.clear();
        }

        mMetaData.getPropertiesTableModel().setEditable(false);
        //mResource = rs;

        mPreview.loadResource(r);
    }
    
    private void showNodePanes(boolean visible) {
        Widget.setHidden(mNotePanel, !visible);
        Widget.setHidden(mUserMetaData, !visible);
        Widget.setHidden(mNodeTree, !visible);
    }
    private void showResourcePanes(boolean visible) {
        Widget.setHidden(mSummary, !visible);
        Widget.setHidden(mMetaData, !visible);
        Widget.setHidden(mPreview, !visible);
    }


    public static class NodeTree extends JPanel
    {
        private final OutlineViewTree tree;
        
        public NodeTree()
        {
            super(new BorderLayout());
            setName("Node Tree");
            tree = new OutlineViewTree();
            
            JScrollPane mTreeScrollPane = new JScrollPane(tree);
            mTreeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(mTreeScrollPane);
        }

        public void load(LWComponent c)
        {
            // if the tree is not intiliazed, hidden, or doesn't contain the given node,
            // then it switches the model of the tree using the given node
            
            if (!tree.isInitialized() || !isVisible() || !tree.contains(c)) {
                //panelLabel.setText("Node: " + pNode.getLabel());
                if (c instanceof LWContainer)
                    tree.switchContainer((LWContainer)c);
                else if (c instanceof LWLink)
                    tree.switchContainer(null);
            }
        }
    }
    
    public static class UserMetaData extends JPanel
    {
        private NodeFilterEditor userMetaDataEditor = null;
        
        public UserMetaData()
        {
            super(new BorderLayout());
            setName("Custom Meta Data");
            //setBorder( BorderFactory.createEmptyBorder(10,10,10,6));

            // todo in VUE to create map before adding panels or have a model that
            // has selection loaded when map is added.
            // userMetaDataEditor = new NodeFilterEditor(mNode.getNodeFilter(),true);
            // add(userMetaDataEditor);
        }

        void load(LWComponent c) {
            if (DEBUG.SELECTION) System.out.println("NodeFilterPanel.updatePanel: " + c);
            if (userMetaDataEditor != null) {
                //System.out.println("USER META SET: " + c.getNodeFilter());
                userMetaDataEditor.setNodeFilter(c.getNodeFilter());
            } else {
                if (VUE.getActiveMap() != null && c.getNodeFilter() != null) {
                    // NodeFilter bombs entirely if no active map, so don't let
                    // it mess us up if there isn't one.
                    userMetaDataEditor = new NodeFilterEditor(c.getNodeFilter(), true);
                    add(userMetaDataEditor, BorderLayout.CENTER);
                    //System.out.println("USER META DATA ADDED: " + userMetaDataEditor);
                }
            }
        }
    }
    

    // summary fields
    private final Object[] labelTextPairs = {
        "-Title",   mTitleField,
        "-Where",   mWhereField,
        "-Size",    mSizeField,
    };

    public class SummaryPane extends JPanel {
    
        SummaryPane() {
            super(new BorderLayout());
            //super(new GridBagLayout());

            final String fontName;
            final int fontSize;

            if (isMacAqua) {
                fontName = "Lucida Grande";
                fontSize = 10;
            } else {
                fontName = "SansSerif";
                fontSize = 11;
            }
            
            /*
            GridBagConstraints c = new GridBagConstraints();
            mTitleField.setFont(new Font(fontName, Font.BOLD, 13));
            mTitleField.setOpaque(false);
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER; 
            c.anchor = GridBagConstraints.NORTHWEST;
            add(mTitleField, c);
            */

            Font fieldFace = new Font(fontName, Font.PLAIN, fontSize);
            Font labelFace = new GUI.Face(fontName, Font.BOLD, fontSize, Color.gray);

            JPanel gridBag = new JPanel(new GridBagLayout());
            //JPanel gridBag = this;
            addLabelTextRows(0, labelTextPairs, gridBag, labelFace, fieldFace);

            Font f = mTitleField.getFont();
            mTitleField.setFont(f.deriveFont(Font.BOLD));
            //mTitleField.setFont(f.deriveFont(Font.BOLD, (float) (fontSize+2)));

            add(gridBag, BorderLayout.NORTH);

            // allow fixed amount of veritcal space so stack isn't always resizing
            // if the location line-wraps and makes itself taller
            // (Note that the Summary pane must be a JPanel, *containing* the
            // gridbag, for this to work: we can't just be the gridBag directly)
            setPreferredSize(new Dimension(Short.MAX_VALUE,63));
            setMinimumSize(new Dimension(200,63));
            setMaximumSize(new Dimension(Short.MAX_VALUE,63));
        }

        public void paint(Graphics g) {
            if (!isMacAqua) {
                // this is on by default on the mac
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            super.paint(g);
        }
        
    }
    
    private static boolean FirstPreview = true;

    class PreviewPane extends JPanel
        implements Images.Listener, Runnable
    {
        private Resource mResource;
        private Object mPreviewData;
        private Image mImage;
        private int mImageWidth;
        private int mImageHeight;
        private boolean isLoading = false;

        private final JLabel StatusLabel = new JLabel("(status)", JLabel.CENTER);
        //private final JTextArea StatusLabel = new JTextArea("(status)");
        //private final JTextPane StatusLabel = new JTextPane();
        // how in holy hell to get a multi-line text object centered w/out using a styled document?

        //private Image LoadingImage = null;
        
        PreviewPane() {
            super(new BorderLayout());
            setMinimumSize(new Dimension(32,32));
            setPreferredSize(new Dimension(200,200));
            setOpaque(false);

            //StatusLabel.setLineWrap(true);
            //StatusLabel.setAlignmentX(0.5f);
            //StatusLabel.setAlignmentY(0.5f);
            //StatusLabel.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
            //StatusLabel.setBorder(new LineBorder(Color.red));
            StatusLabel.setVisible(false);
            add(StatusLabel);

            /*
            addComponentListener(new ComponentAdapter() {
                    public void componentShown(ComponentEvent e) { handleShown(); }
                    public void componentHidden(ComponentEvent e) { handleHidden(); }
                });
            */
            //private void handleShown() {out("PREVIEW SHOWN");}
            //private void handleHidden() {out("PREVIEW HIDDEN");}
            
            
            
        }
        /*
        public void XXXsetVisible(boolean visible) {
            //mImage = LoadingImage;
            // Null image in case had an old image: don't repaint with it -- we want
            // to load the current image in case it wasn't already loaded.
            mImage = null;
            super.setVisible(visible);
            //tufts.Util.printStackTrace("SET-VISIBLE");
            if (isShowing()) {
                if (DEBUG.RESOURCE) out("PreviewPane; setVisible, now showing: loading resource");
                VUE.invokeAfterAWT(new Runnable() { public void run() { loadResource(mResource); }});
            }
        }
        */

        private void status(String msg) {
            StatusLabel.setText(msg);
            StatusLabel.setVisible(true);
        }
        private void clearStatus() {
            StatusLabel.setVisible(false);
        }
    

        // Won't need to sync this if Images handles single notifier?
        synchronized void loadResource(Resource r) {

            if (DEBUG.RESOURCE || DEBUG.IMAGE) out("loadResource: " + r);
            
            mResource = r;
            if (r != null)
                mPreviewData = r.getPreview();
            else
                mPreviewData = null;
            mImage = null;

            if (mPreviewData == null && mResource.isImage())
                mPreviewData = mResource;

            if (isShowing()) {

                loadPreview(mPreviewData);
                FirstPreview = false;

            } else {

                if (FirstPreview && mPreviewData != null) {
                    FirstPreview = false;
                    Widget.setExpanded(PreviewPane.this, true);
                    // Exposing the panel will cause repaint, which
                    // will trigger a preview load.
                    return;
                }
                
                if (DEBUG.RESOURCE || DEBUG.IMAGE) out("not showing: no action");
            }

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

        private synchronized void loadImage(Object imageData) {
            if (DEBUG.IMAGE) out("loadImage " + imageData);
            if (!Images.getImage(imageData, this)) {
                // will make callback to gotImage when we have it
                isLoading = true;
                status("Loading...");
            } else
                isLoading = false;
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
            status("Image Error:\n" + msg);
            isLoading = false;
        }

        /*
        private void displayIcon(ImageIcon icon) {
            displayImage(icon.getImage());
        }
        */

        private void displayImage(Image image) {
            if (DEBUG.RESOURCE || DEBUG.IMAGE) out("displayImage " + image);

            mImage = image;
            if (mImage != null) {
                mImageWidth = mImage.getWidth(null);
                mImageHeight = mImage.getHeight(null);
                if (DEBUG.IMAGE) out("displayImage " + mImageWidth + "x" + mImageHeight);
            }

            /*
            if (mImage != null && mImage != NoImage && FirstPreview) {
                FirstPreview = false;
                //System.out.println("FIRST PREVIEW");
                //VUE.invokeAfterAWT(new Runnable() { public void run() {
                    Widget.setExpanded(PreviewPane.this, true);
                    //}});
            }
            */

            clearStatus();
            repaint();
        }

        public void run() {
            loadPreview(mPreviewData);
        }

        private void out(String s) {
            System.out.println("PreviewPane: " + s);
        }
        

        /** draw the image into the current avilable space, scaling it down if needed (never scale up tho) */
        public void paintComponent(Graphics g)
        {
            if (DEBUG.IMAGE) out("paint");

            if (mImage == null) {
                if (!isLoading && mPreviewData != null) {
                    synchronized (this) {
                        if (!isLoading && mPreviewData != null)
                            VUE.invokeAfterAWT(PreviewPane.this); // load the preview
                    }
                }
                return;
            }
            
            //g.setColor(Color.black);
            //g.fillRect(0,0, w,h);

            java.awt.geom.Rectangle2D imageBounds
                = new java.awt.geom.Rectangle2D.Float(0, 0, mImageWidth, mImageHeight);
            double zoomFit = ZoomTool.computeZoomFit(getSize(),
                                                     0,
                                                     imageBounds,
                                                     null,
                                                     false);

            if (zoomFit > 1)
                zoomFit = 1;

            final int drawW = (int) (mImageWidth * zoomFit);
            final int drawH = (int) (mImageHeight * zoomFit);
                                                     
            final int w = getWidth();
            final int h = getHeight();
            
            // center if drawable area is bigger than image
            int xoff = 0;
            int yoff = 0;
            if (drawW != w)
                xoff = (w - drawW) / 2;
            if (drawH != h)
                yoff = (h - drawH) / 2;
            
            if (DEBUG.IMAGE) out("painting " + Util.tag(mImage));
            g.drawImage(mImage, xoff, yoff, drawW, drawH, null);
        }
    }


    private void setAllEnabled(boolean enabled) {
        int pairs = labelTextPairs.length;
        for (int i = 0; i < pairs; i += 2) {
            JComponent field = (JComponent) labelTextPairs[i+1];
            field.setEnabled(enabled);
        }
        mMetaData.setEnabled(enabled);
    }
    
    
    //----------------------------------------------------------------------------------------
    // Utility methods
    //----------------------------------------------------------------------------------------
    
    private void loadText(JTextComponent c, String text) {
        String hasText = c.getText();
        // This prevents flashing where fields of
        // length greater the the visible area do
        // a flash-scroll when setting the text, even
        // if it's the same as what's there.
        if (hasText != text && !hasText.equals(text))
            c.setText(text);
    }
    private void loadText(JLabel c, String text) {
        String hasText = c.getText();
        // This prevents flashing where fields of
        // length greater the the visible area do
        // a flash-scroll when setting the text, even
        // if it's the same as what's there.
        if (hasText != text && !hasText.equals(text))
            c.setText(text);
    }
    
    private static void addLabelTextRows(int starty, Object[] labelTextPairs, Container gridBag, Font labelFace, Font fieldFace)
    {
        // Note that the resulting alignment ends up being somehow FONT dependent!
        // E.g., works great with Lucida Grand (MacOSX), but with system default,
        // if the field value is a wrapping JTextPane (thus gets taller as window
        // gets narrower), the first line of text rises slightly and is no longer
        // in line with it's label.
        
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int num = labelTextPairs.length;

        final int vpad = 2;
        final Insets labelInsets = new Insets(vpad, 3, vpad, 5);
        final Insets fieldInsets = new Insets(vpad, 0, vpad, 1);
        
        for (int i = 0; i < num; i += 2) {
            
            String txt = (String) labelTextPairs[i];
            boolean readOnly = false;
            if (txt.charAt(0) == '-') {
                txt = txt.substring(1);
                readOnly = true;
            }

            //-------------------------------------------------------
            // Add the label field
            //-------------------------------------------------------

            c.gridx = 0;
            c.gridy = starty++;
            c.insets = labelInsets;
            c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last in row
            c.fill = GridBagConstraints.NONE; // the label never grows
            c.anchor = GridBagConstraints.NORTHEAST;
            c.weightx = 0.0;                  // do not expand

            JLabel label = new JLabel(txt);
            if (labelFace != null) {
                label.setFont(labelFace);
                if (labelFace instanceof GUI.Face)
                    label.setForeground(((GUI.Face)labelFace).color);
            }
            label.setToolTipText(txt);
                
            gridBag.add(label, c);


            //-------------------------------------------------------
            // Add the text value field
            //-------------------------------------------------------
            
            c.gridx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;     // last in row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.insets = fieldInsets;
            c.weightx = 1.0; // field value expands horizontally to use all space
            
            JComponent field = (JComponent) labelTextPairs[i+1];
            if (fieldFace != null) {
                field.setFont(fieldFace);
                if (fieldFace instanceof GUI.Face)
                    field.setForeground(((GUI.Face)fieldFace).color);
            }
            
            gridBag.add(field, c);
            
            if (readOnly) {

                field.setOpaque(false);
                field.setBorder(null);
                
                /*
                
                Border b = field.getBorder();
                //System.out.println("LWCInfoPanel: got border " + b);
                //lastBorder = b;
                if (b != null) {
                    final Insets borderInsets = b.getBorderInsets(field);
                    //System.out.println("ResourcePanel: got border insets " + borderInsets + " for " + field);
                    field.putClientProperty(VueTextField.ActiveBorderKey, b);
                    Border emptyBorder = new EmptyBorder(borderInsets);
                    field.putClientProperty(VueTextField.InactiveBorderKey, emptyBorder);
                    field.setBorder(emptyBorder);
                }
                //field.setBorder(new EmptyBorder(1,1,1,1));
                if (field instanceof JTextComponent) {
                    JTextComponent tc = (JTextComponent) field;
                    tc.setEditable(false);
                    tc.setFocusable(false);
                }
                if (VueUtil.isMacPlatform()) { // looks crappy on PC aso
                    //field.setBackground(SystemColor.control);
                    field.setOpaque(false);
                    }
                */
            }
        }
    }
    
    private void out(Object o) {
        System.out.println("ResourcePanel: " + (o==null?"null":o.toString()));
    }
    
    public static void main(String args[]) {

        VUE.init(args);

        // Must have at least ONE active frame for our focus manager to work
        //new Frame("An Active Frame").setVisible(true);

        String rsrc = null;
        if (args.length > 0 && args[0].charAt(0) != '-')
            rsrc = args[0];
        displayTestResourcePanel(rsrc);
    }

    static void displayTestResourcePanel(String rsrc)
    {
        //MapResource r = new MapResource("file:///System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Home");
        if (rsrc == null)
            rsrc = "file:///VUE/src/tufts/vue/images/splash_graphic_1.0.gif";

        ResourcePanel p = new ResourcePanel();
        LWComponent node = new LWNode("Test Node");
        node.setNotes("I am a note.");
        System.out.println("Loading resource[" + rsrc + "]");
        MapResource r = new MapResource(rsrc);
        System.out.println("Got resource " + r);
        r.setTitle("A Very Long Long Resource Title Ya Say");
        node.setResource(r);
        for (int i = 1; i < 6; i++)
            r.setProperty("field_" + i, "value_" + i);

        DockWindow w = null;
        if (false) {
            //ToolWindow w = VUE.createToolWindow("LWCInfoPanel", p);
            JScrollPane sp = new JScrollPane(p,
                                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                             //JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                                             );
            w = GUI.createDockWindow("Inspector", sp);
        } else {
            w = GUI.createDockWindow("Inspector", p);
            //w = GUI.createDockWindow("Resource Inspector", p.mSummary);
            //tufts.Util.displayComponent(p);
        }

        if (w != null) {
            w.setUpperRightCorner(GUI.GScreenWidth, GUI.GInsets.top);
            w.setVisible(true);
        }
        
        VUE.getSelection().setTo(node); // setLWComponent does diddly -- need this

    }
    
    
    
}

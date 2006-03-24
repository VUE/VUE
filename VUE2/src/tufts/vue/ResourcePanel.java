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

import tufts.vue.gui.*;
import tufts.vue.NotePanel;
import tufts.vue.filter.NodeFilterEditor;

/**
 * Display information about the selected resource, including "spec" (e.g., URL),
 * meta-data, and if available: title and a preview (e.g., an image preview or icon).
 *
 * @version $Revision: 1.14 $ / $Date: 2006-03-24 21:05:54 $ / $Author: sfraize $
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
    
    private void loadResource(final Resource rs) {
        
        if (rs != null) {
            setAllEnabled(true);
            loadText(mWhereField, rs.getSpec());
            loadText(mTitleField, rs.getTitle());
        } else {
            // leave current display, but grayed out
            setAllEnabled(false);
            return;
        }
        

        String ss = "";
        if (rs instanceof MapResource) // todo: REALLY got to clean up the Resource interface & add an abstract class...
            ss = VueUtil.abbrevBytes(((MapResource)rs).getSize());
        mSizeField.setText(ss);
        
        //loading the metadata if it exists
        if (rs != null) {
            java.util.Properties properties = rs.getProperties();
            if (properties != null) {
                if (rs.getType() == Resource.ASSET_FEDORA)
                    mMetaData.setProperties(properties, false);
                else
                    mMetaData.setProperties(properties, true);
            }
        } else {
            mMetaData.clear();
        }

        mMetaData.getPropertiesTableModel().setEditable(false);
        //mResource = rs;

        mPreview.loadResource(rs);
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

    class PreviewPane extends JPanel {
        private Resource mResource;
        private Image mImage;
        private int mImageWidth;
        private int mImageHeight;

        //private Image LoadingImage = null;
        
        PreviewPane() {
            super(new BorderLayout());
            //loadImage(VueResources.getImage("splashScreen")); // test
            //setBorder(new LineBorder(Color.red));
            setMinimumSize(new Dimension(32,32));
            setPreferredSize(new Dimension(200,200));
            setOpaque(false);

            /*
            try {
                LoadingImage = VueResources.getImageIconResource("/tufts/vue/images/zoomin_cursor_32.gif").getImage();
            } catch (Throwable t) { t.printStackTrace(); }
            */
            
        }

        public void setVisible(boolean visible) {
            //mImage = LoadingImage;
            mImage = null;
            super.setVisible(visible);
            //System.err.println("setVisible " + visible);
            VUE.invokeAfterAWT(new Runnable() { public void run() { loadResource(mResource); }});
        }

        void loadResource(Resource r) {

            mResource = r;
            mImage = null;

            boolean didFirstPreview = false;

            if (FirstPreview) {
                FirstPreview = false;
                //System.out.println("FIRST PREVIEW");
                //VUE.invokeAfterAWT(new Runnable() { public void run() {
                Widget.setExpanded(PreviewPane.this, true);
                //}});
                didFirstPreview = true;
            }
            
            if (!isVisible() && !didFirstPreview)
                return;

            Image image = null;
            boolean imageLoading = false;

            if (r instanceof MapResource) {
                image = NoImage;
                MapResource mr = (MapResource) r;
                if (mr.isImage()) {
                    final java.net.URL url = mr.asURL();
                    if (url != null) {
                        imageLoading = true;
                        mImage = null;
                        repaint();
                        GUI.activateWaitCursor();
                        VUE.invokeAfterAWT(new Runnable() { public void run() {
                            loadImage(new ImageIcon(url).getImage());
                            GUI.clearWaitCursor();
                        }});
                    }
                }
            }

            if (!imageLoading)
                loadImage(image);
        }

        private void loadImage(Image image) {
            mImage = image;
            if (mImage != null) {
                mImageWidth = mImage.getWidth(null);
                mImageHeight = mImage.getHeight(null);
                //setPreferredSize(new Dimension(mImageWidth, mImageHeight));
                //setMaximumSize(new Dimension(mImageWidth, mImageHeight));
            }
            /*
            if (mImage != null && FirstPreview) {
                FirstPreview = false;
                System.out.println("FIRST PREVIEW");
                VUE.invokeAfterAWT(new Runnable() { public void run() {
                    Widget.setExpanded(PreviewPane.this, true);
                }});
            }
            */
            repaint();
                
        }

        /** draw the image into the current avilable space, scaling it down if needed (never scale up tho) */
        public void paintComponent(Graphics g) {

            if (mImage == null)
                return;
            
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

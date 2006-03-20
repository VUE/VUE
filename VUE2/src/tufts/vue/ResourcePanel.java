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

/**
 * Display information about the selected resource, including "spec" (e.g., URL),
 * meta-data, and if available: title and a preview (e.g., an image preview or icon).
 *
 * @version $Revision: 1.5 $ / $Date: 2006-03-20 18:13:59 $ / $Author: sfraize $
 */

public class ResourcePanel extends WidgetStack
    implements VueConstants, LWSelection.Listener
{
    // the collapsable pane's
    private final JPanel mSummary;
    private final PropertiesEditor mMetaData;
    private final PreviewPane mPreview;

    // fields for the Summary Pane
    private final JTextComponent mTitleField = new JTextArea();
    private final JTextComponent mWhereField = new JTextPane();
    private final JLabel mSizeField = new JLabel();

    private final Image NoImage = VueResources.getImage("NoImage");

    private final boolean isMacAqua = GUI.isMacAqua();

    /** the displayed resource */
    private Resource mResource;
    

    public ResourcePanel()
    {
        addPane("Info",         mSummary = new SummaryPane(), 0f);
        addPane("Meta-Data",    mMetaData = new PropertiesEditor(false), 1f);
        addPane("Preview",      mPreview = new PreviewPane(), 1f);

        VUE.ModelSelection.addListener(this);
    }

    

    // summary fields
    private final Object[] labelTextPairs = {
        "-Title",   mTitleField,
        "-Where",   mWhereField,
        "-Size",    mSizeField,
    };

    class SummaryPane extends JPanel {
    
        SummaryPane() {
            super(new BorderLayout());

            final String fontName;
            final int fontSize;

            if (isMacAqua) {
                fontName = "Lucida Grande";
                fontSize = 10;
            } else {
                fontName = "SansSerif";
                fontSize = 11;
            }
            
            Font fieldFace = new Font(fontName, Font.PLAIN, fontSize);
            Font labelFace = new GUI.Face(fontName, Font.BOLD, fontSize, Color.gray);

            JPanel gridBag = new JPanel(new GridBagLayout());
            addLabelTextRows(labelTextPairs, gridBag, labelFace, fieldFace);

            Font f = mTitleField.getFont();
            mTitleField.setFont(f.deriveFont(Font.BOLD));
            //mTitleField.setFont(f.deriveFont(Font.BOLD, (float) (fontSize+2)));

            add(gridBag, BorderLayout.NORTH);

            // allow fixed mount of veritcal space so stack isn't always resizing
            // if the location line-wraps and makes itself taller
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
    
    class PreviewPane extends JPanel {
        private Image mImage;
        private int mImageWidth;
        private int mImageHeight;
        
        PreviewPane() {
            super(new BorderLayout());
            //loadImage(VueResources.getImage("splashScreen")); // test
            //setBorder(new LineBorder(Color.red));
            setMinimumSize(new Dimension(32,32));
            setPreferredSize(new Dimension(200,200));
        }

        void loadResource(Resource r) {
            Image image = null;

            if (r instanceof MapResource) {
                image = NoImage;
                MapResource mr = (MapResource) r;
                if (mr.isImage()) {
                    java.net.URL url = mr.asURL();
                    if (url != null)
                        image = new ImageIcon(url).getImage();
                }
            }

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
            repaint();
        }

        /** draw the image into the current avilable space, scaling it down if needed (never scale up tho) */
        public void paintComponent(Graphics g) {

            if (mImage == null)
                return;
            
            final int w = getWidth();
            final int h = getHeight();
            //g.setColor(Color.black);
            //g.fillRect(0,0, w,h);
            int drawW = w;
            int drawH = h;
            if (drawW > mImageWidth)
                drawW = mImageWidth;
            if (drawH > mImageHeight)
                drawH = mImageHeight;

            // center if drawable area is bigger than image
            int xoff = 0;
            int yoff = 0;
            if (drawW != w)
                xoff = (w - drawW) / 2;
            if (drawH != h)
                yoff = (h - drawH) / 2;
            
            //System.out.println("paint; preview size: " + getSize() + "\n\tpaint size: " + drawWidth + "x" + drawHeight);
            g.drawImage(mImage, xoff, yoff, drawW, drawH, null);
        }
    }


    public void selectionChanged(LWSelection selection) {
        if (selection.isEmpty() || selection.size() > 1) {
            loadResource(null);
        } else {
            loadResource(selection.first().getResource());
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
        mResource = rs;

        mPreview.loadResource(rs);
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
    
    private void addLabelTextRows(Object[] labelTextPairs, Container gridBag, Font labelFace, Font fieldFace)
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
            c.gridy = i;
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
            w = GUI.createDockWindow("Resource Inspector", sp);
        } else {
            w = GUI.createDockWindow("Resource Inspector", p);
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

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
 * @version $Revision: 1.1 $ / $Date: 2006-03-15 18:14:33 $ / $Author: sfraize $
 */

public class ResourcePanel extends WidgetStack
    implements VueConstants, LWSelection.Listener
{
    private final JTextComponent mTitleField = new JTextArea();
    private final JTextComponent mLocationField = new JTextPane();
    private final JLabel mSizeField = new JLabel();
    private final PropertiesEditor mMetaData;
    private final PreviewPane mPreview;
    

    private Resource mResource;
    
    private final Object[] labelTextPairs = {
        "-Title",    mTitleField,
        "-Location", mLocationField,
        "-Size",    mSizeField,
    };

    class SummaryPane extends JPanel {
        SummaryPane() {
            super(new BorderLayout());
            GridBagLayout gridBagLayout = new GridBagLayout();
            JPanel gridBag = new JPanel(gridBagLayout);
            //setLayout(gridBag);
            addLabelTextRows(labelTextPairs, gridBagLayout, gridBag);
            add(gridBag, BorderLayout.CENTER);
        }
    }
    
    class PreviewPane extends JPanel {
        private Image mImage;
        private int mImageWidth;
        private int mImageHeight;
        
        PreviewPane() {
            super(new BorderLayout());
            mImage = VueResources.getImage("splashScreen");
            //add(new JLabel(VueResources.getImageIcon("splashScreen")), BorderLayout.CENTER);
            mImageWidth = mImage.getWidth(null);
            mImageHeight = mImage.getHeight(null);
            setPreferredSize(new Dimension(mImageWidth, mImageHeight));
            //setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
            setMaximumSize(new Dimension(mImageWidth, mImageHeight));
            setMinimumSize(new Dimension(16,16));
        }

        void loadResource(Resource r) {
            mImage = null;
            if (r instanceof MapResource) {
                MapResource mr = (MapResource) r;
                if (mr.isImage()) {
                    java.net.URL url = mr.asURL();
                    if (url != null)
                        mImage = new ImageIcon(url).getImage();
                }
            }
            repaint();
        }

        public void paintComponent(Graphics g) {

            if (mImage == null) {
                System.out.println("no image");
                return;
            }
            
            final int w = getWidth();
            final int h = getHeight();
            g.setColor(Color.black);
            g.fillRect(0,0, w,h);
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


    public ResourcePanel()
    {
        mLocationField.setEditable(false);
        mLocationField.setOpaque(false);
        mLocationField.setBorder(null);
        
        mMetaData = new PropertiesEditor(false);

        addPane("Summary", new SummaryPane());
        addPane("Meta-Data", mMetaData, 50);
        // for now put preview last, otherwise meta-data stays at bottom even it preview is "closed"
        addPane("Preview", mPreview = new PreviewPane(), 50);

        VUE.ModelSelection.addListener(this);
    }

    
    public void selectionChanged(LWSelection selection) {
        if (selection.isEmpty() || selection.size() > 1) {
            loadResource(null);
        } else {
            loadResource(selection.first().getResource());
        }
    }
    
    
    private void setAllEnabled(boolean tv) {
        int pairs = labelTextPairs.length;
        for (int i = 0; i < pairs; i += 2) {
            JComponent field = (JComponent) labelTextPairs[i+1];
            field.setEnabled(tv);
        }
        //resourceMetadataPanel.setEnabled(tv);
        //metadataPane.setEnabled(tv);
        mMetaData.setEnabled(tv);
    }
    
    private void loadResource(final Resource rs) {
        
        if (rs != null) {
            loadText(mLocationField, rs.getSpec());
            setAllEnabled(true);
        } else {
            setAllEnabled(false);
            loadText(mLocationField, "");
        }
        
        loadText(mTitleField, rs.getTitle());

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
    
    private void addLabelTextRows(Object[] labelTextPairs, GridBagLayout gridbag, Container container)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int num = labelTextPairs.length;
        boolean lastWasLabelAbove = false;

        Border lastBorder = null;
        
        for (int i = 0; i < num; i += 2) {
            
            String txt = (String) labelTextPairs[i];
            boolean readOnly = false;
            boolean labelAbove = false;
            if (txt.charAt(0) == '-') {
                txt = txt.substring(1);
                readOnly = true;
            } else if (txt.charAt(0) == '+') {
                labelAbove = true;
                txt = txt.substring(1);
            }
            txt += ": ";
            
            //-------------------------------------------------------
            // Add the label field
            //-------------------------------------------------------

            int topPad = lastWasLabelAbove ? 3 : 1;
            
            if (labelAbove) {
                c.insets = new Insets(topPad, 0, 0, 0);
                c.gridwidth = GridBagConstraints.REMAINDER; // last in row
                c.anchor = GridBagConstraints.WEST;
            } else {
                c.insets = new Insets(topPad, 0, 1, 0);
                c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last in row
                c.anchor = GridBagConstraints.EAST;
                // this makes labels stay at top left of multi-line fields, tho it throws off
                // baseline alignment for normal cases.  What we REALLY want is a baseline
                // alignment against the first line of text in the field.
                // c.anchor = GridBagConstraints.NORTHEAST;
            }
            c.fill = GridBagConstraints.NONE; // the label never grows
            c.weightx = 0.0;                  // reset
            
            JLabel label = new JLabel(txt);
            gridbag.setConstraints(label, c);
            container.add(label);
            label.setFont(FONT_NARROW);

            //-------------------------------------------------------
            // Add the text value field
            //-------------------------------------------------------
            
            c.gridwidth = GridBagConstraints.REMAINDER;     // last in row
            c.fill = GridBagConstraints.HORIZONTAL;
            if (labelAbove)
                c.insets = new Insets(0, 0, 0, 0);
            else
                c.insets = new Insets(0, 0, 1, 0);
            c.weightx = 1.0;
            
            JComponent field = (JComponent) labelTextPairs[i+1];
            //field.setFont(VueConstants.SmallFont);
            //field.setFont(FONT_NARROW);
            gridbag.setConstraints(field, c);
            container.add(field);

            if (lastBorder != null && field instanceof JTextPane) {
                field.setBorder(lastBorder);
            }
            
            
            if (readOnly) {
                Border b = field.getBorder();
                //System.out.println("LWCInfoPanel: got border " + b);
                //lastBorder = b;
                if (b != null) {
                    final Insets borderInsets = b.getBorderInsets(field);
                    System.out.println("ResourcePanel: got border insets " + borderInsets + " for " + field);
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
                if (VueUtil.isMacPlatform()) {
                    //field.setBackground(SystemColor.control);
                    field.setOpaque(false);
                }
            }

            lastWasLabelAbove = labelAbove;
        }
        /**
         * JLabel field  = new JLabel("Metadata");
         * c.gridwidth = GridBagConstraints.REMAINDER;     //end row
         * c.fill = GridBagConstraints.HORIZONTAL;
         * c.anchor = GridBagConstraints.WEST;
         * gridbag.setConstraints(field, c);
         * container.add(field);
         */
    }
    
    public static void main(String args[]) {

        VUE.init(args);

        // Must have at least ONE active frame for our focus manager to work
        new Frame("An Active Frame").setVisible(true);
        
        ResourcePanel p = new ResourcePanel();
        LWComponent node = new LWNode("Test Node");
        node.setNotes("I am a note.");
        node.setResource("file:///System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Home");
        Resource r = node.getResource();
        for (int i = 1; i < 6; i++)
            r.setProperty("field_" + i, "value_" + i);

        DockWindow w;
        if (args.length > 0) {
            //ToolWindow w = VUE.createToolWindow("LWCInfoPanel", p);
            JScrollPane sp = new JScrollPane(p,
                                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                             //JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                                             );
            w = GUI.createDockWindow("Resource Inspector", sp);
        } else {
            w = GUI.createDockWindow("Resource Inspector", p);
            //tufts.Util.displayComponent(p);
        }

        w.setVisible(true);
        
        VUE.getSelection().setTo(node); // setLWComponent does diddly -- need this

        
        /*
        p.setAllEnabled(true);
        p.labelField.setEditable(true);
        p.labelField.setEnabled(true);
        p.mLocationField.setEditable(true);
        p.mLocationField.setEnabled(true);
        */
    }
    
    
    
}

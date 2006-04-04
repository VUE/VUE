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
import tufts.vue.NotePanel;
import tufts.vue.filter.NodeFilterEditor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;


/**
 * Display information about the selected Resource, or LWComponent and it's Resource.
 *
 * @version $Revision: 1.2 $ / $Date: 2006-04-04 04:57:09 $ / $Author: sfraize $
 */

public class InspectorPane extends JPanel
    implements VueConstants, LWSelection.Listener, ResourceSelection.Listener
{

    // fields for the Summary Pane
    //private final JTextComponent mTitleField = new JTextArea();
    private final JTextComponent mWhereField = new JTextPane();
    private final JLabel mSizeField = new JLabel();

    private final Image NoImage = VueResources.getImage("NoImage");

    private final boolean isMacAqua = GUI.isMacAqua();

    // Resource panes
    //private final JPanel mSummary;
    private final PropertiesEditor mResourceMetaData;
    private final ResourcePreview mPreview;
    
    // Node panes
    private final NotePanel mNotePanel = new NotePanel();
    private final UserMetaData mUserMetaData = new UserMetaData();
    private final NodeTree mNodeTree = new NodeTree();
    
    private final Font textFont;
    private final Font textFontBold;
    
    public InspectorPane()
    {
        super(new BorderLayout());
        setName("Selection Inspector");

        String fontName;
        int fontSize;

        if (isMacAqua) {
            fontName = "Lucida Grande";
            fontSize = 10;
        } else {
            fontName = "SansSerif";
            fontSize = 11;
        }

        textFont = new Font(fontName, Font.PLAIN, fontSize);
        textFontBold = new Font(fontName, Font.BOLD, fontSize);
        

        //mSummary = new SummaryPane();
        mResourceMetaData = new PropertiesEditor(false);
        mPreview = new ResourcePreview();

        WidgetStack stack = new WidgetStack();

        mNotePanel.setName("Node Notes");

        stack.addPane(mUserMetaData, 1f);
        stack.addPane("Resource Preview",      mPreview, 0.3f);
        //stack.addPane("Resource Summary",      mSummary, 0f);
        stack.addPane("Resource Meta Data",    mResourceMetaData, 1f);
        stack.addPane(mNotePanel, 1f);
        stack.addPane(mNodeTree, 1f);

        Widget.setExpanded(mUserMetaData, false);
        Widget.setExpanded(mResourceMetaData, false);
        Widget.setExpanded(mNodeTree, false);
        
        add(stack, BorderLayout.CENTER);

        VUE.getSelection().addListener(this);
        VUE.getResourceSelection().addListener(this);
    }

    public void resourceSelectionChanged(ResourceSelection selection)
    {
        if (DEBUG.RESOURCE) out("resource selected: " + selection.get());
        showNodePanes(false);
        showResourcePanes(true);
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
            //setAllEnabled(true);
            //loadText(mWhereField, r.getSpec());
            //loadText(mTitleField, r.getTitle());
        } else {
            // leave current display, but grayed out
            ///setAllEnabled(false);
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
                    mResourceMetaData.setProperties(properties, false);
                else
                    mResourceMetaData.setProperties(properties, true);
            }
        } else {
            mResourceMetaData.clear();
        }

        mResourceMetaData.getPropertiesTableModel().setEditable(false);
        //mResource = rs;

        mPreview.loadResource(r);
    }
    
    private void showNodePanes(boolean visible) {
        Widget.setHidden(mNotePanel, !visible);
        Widget.setHidden(mUserMetaData, !visible);
        Widget.setHidden(mNodeTree, !visible);
    }
    private void showResourcePanes(boolean visible) {
        //Widget.setHidden(mSummary, !visible);
        Widget.setHidden(mResourceMetaData, !visible);
        Widget.setHidden(mPreview, !visible);
    }


    private class ResourcePreview extends JPanel {
    
        private final JLabel mTitleField = new JLabel("", JLabel.CENTER);
        private final PreviewPane mPreviewPane = new PreviewPane();
        
        ResourcePreview() {
            super(new BorderLayout());

            mTitleField.setOpaque(false);
            mTitleField.setFont(textFontBold);
            mTitleField.setBorder(new EmptyBorder(0,2,5,2));

            add(mPreviewPane, BorderLayout.CENTER);
            add(mTitleField, BorderLayout.SOUTH);
        }

        void loadResource(Resource r) {
            String title = r.getTitle();
            if (title == null || title.length() < 1) {
                mTitleField.setVisible(false);
            } else {
                mTitleField.setText(r.getTitle());
                mTitleField.setVisible(true);
            }
            mPreviewPane.loadResource(r);
        }
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
            setName("Node custom keywords");
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
    
    /*
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
            **

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
    
    private void setAllEnabled(boolean enabled) {
        int pairs = labelTextPairs.length;
        for (int i = 0; i < pairs; i += 2) {
            JComponent field = (JComponent) labelTextPairs[i+1];
            field.setEnabled(enabled);
        }
        mResourceMetaData.setEnabled(enabled);
    }

    */
    
    
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
        System.out.println("Inspector: " + (o==null?"null":o.toString()));
    }
    
    public static void displayTestPane(String rsrc)
    {
        //MapResource r = new MapResource("file:///System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Home");
        if (rsrc == null)
            rsrc = "file:///VUE/src/tufts/vue/images/splash_graphic_1.0.gif";

        InspectorPane p = new InspectorPane();
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
            w = GUI.createDockWindow(p);
            //w = GUI.createDockWindow("Resource Inspector", p.mSummary);
            //tufts.Util.displayComponent(p);
        }

        if (w != null) {
            w.setUpperRightCorner(GUI.GScreenWidth, GUI.GInsets.top);
            w.setVisible(true);
        }
        
        VUE.getSelection().setTo(node); // setLWComponent does diddly -- need this

    }

    
    public static void main(String args[]) {

        VUE.init(args);

        // Must have at least ONE active frame for our focus manager to work
        //new Frame("An Active Frame").setVisible(true);

        String rsrc = null;
        if (args.length > 0 && args[0].charAt(0) != '-')
            rsrc = args[0];
        displayTestPane(rsrc);
    }

    
    
}

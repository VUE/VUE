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

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.border.*;

/**
 * Display information about the selected Resource, or LWComponent and it's Resource.
 *
 * @version $Revision: 1.12 $ / $Date: 2006-04-13 05:22:06 $ / $Author: sfraize $
 */

public class InspectorPane extends JPanel
    implements VueConstants, LWSelection.Listener, ResourceSelection.Listener
{
    // fields for the Summary Pane
    //private final JTextComponent mTitleField = new JTextArea();
    //private final JTextComponent mWhereField = new JTextPane();
    //private final JLabel mSizeField = new JLabel();

    private final Image NoImage = VueResources.getImage("NoImage");

    private final boolean isMacAqua = GUI.isMacAqua();

    // Resource panes
    private final SummaryPane mSummaryPane;
    private final MetaDataPane mResourceMetaData;
    private final ResourcePreview mPreview;
    
    // Node panes
    private final NotePanel mNotePanel = new NotePanel();
    private final UserMetaData mUserMetaData = new UserMetaData();
    //private final NodeTree mNodeTree = new NodeTree();
    
    private final Font mLabelFont;
    private final Font mTitleFont;
    private final Font mValueFont;

    private final Color mLabelColor = new Color(61,61,61);

    private Resource mResource; // the current resource

    public InspectorPane()
    {
        super(new BorderLayout());
        setName("Properties");

        String fontName;
        int fontSize;

        if (isMacAqua) {
            fontName = "Lucida Grande";
            fontSize = 11;
        } else {
            fontName = "SansSerif";
            fontSize = 11;
        }

        mLabelFont = new GUI.Face(fontName, Font.PLAIN, fontSize, mLabelColor);
        mValueFont = new GUI.Face(fontName, Font.PLAIN, fontSize, Color.black);
        mTitleFont = new GUI.Face(fontName, Font.BOLD, fontSize, mLabelColor);


        mSummaryPane = new SummaryPane();
        //mResourceMetaData = new PropertiesEditor(false);
        mResourceMetaData = new MetaDataPane();
        mPreview = new ResourcePreview();

        WidgetStack stack = new WidgetStack();

        stack.addPane("Information",            mSummaryPane,           0f);
        stack.addPane("Content Preview",        mPreview,               0.3f);
        stack.addPane("Content Description",    mResourceMetaData,      1f);
        stack.addPane("Notes",                  mNotePanel,             1f);
        stack.addPane("Keywords",               mUserMetaData,          1f);
        //stack.addPane("Nested Nodes",           mNodeTree,              1f);

        Widget.setExpanded(mUserMetaData, false);
        Widget.setExpanded(mResourceMetaData, false);
        //Widget.setExpanded(mNodeTree, false);
        
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
            mSummaryPane.load(c);
            mUserMetaData.load(c);
            //mNodeTree.load(c);

            //setTypeName(mNotePanel, c, "Notes");
        }
    }

    private static void setTypeName(JComponent component, LWComponent c, String suffix) {
        component.setName(c.getComponentTypeLabel() + " " + suffix);
    }

    private void loadResource(final Resource r) {
        
        if (DEBUG.RESOURCE) out("loadResource: " + r);
        
        if (r == null)
            return;

        mResource = r;
        mResourceMetaData.loadResource(r);
        mPreview.loadResource(r);
        
        /*
        long size = r.getSize();
        String ss = "";
        if (size >= 0)
            ss = VueUtil.abbrevBytes(size);
        mSizeField.setText(ss);
        */
    }
    
    private void showNodePanes(boolean visible) {
        Widget.setHidden(mSummaryPane, !visible);
        Widget.setHidden(mNotePanel, !visible);
        Widget.setHidden(mUserMetaData, !visible);
        //Widget.setHidden(mNodeTree, !visible);
    }
    private void showResourcePanes(boolean visible) {
        Widget.setHidden(mResourceMetaData, !visible);
        Widget.setHidden(mPreview, !visible);
    }


    private class ResourcePreview extends tufts.vue.ui.PreviewPane
    {
        private final JLabel mTitleField;
        //private final JTextPane mTitleField;
        //private final JTextArea mTitleField;
        //private final PreviewPane mPreviewPane = new PreviewPane();
        
        ResourcePreview() {
            //super(new BorderLayout());

            // JTextArea -- no good (no HTML)
            //mTitleField = new JTextArea();
            //mTitleField.setEditable(false);

            // JTextPane -- no good, too fuckin hairy and slow (who needs an HTML editor?)
            //mTitleField = new JTextPane();
            //StyledDocument doc = new javax.swing.text.html.HTMLDocument();
            //mTitleField.setStyledDocument(doc);
            //mTitleField.setEditable(false);

            mTitleField = new JLabel("", JLabel.CENTER);

            //-------------------------------------------------------

            GUI.apply(mTitleFont, mTitleField);
            mTitleField.setAlignmentX(0.5f);
            //mTitleField.setBorder(new LineBorder(Color.red));
                
            //mTitleField.setOpaque(false);
            //mTitleField.setBorder(new EmptyBorder(0,2,5,2));
            //mTitleField.setSize(200,50);
            //mTitleField.setPreferredSize(new Dimension(200,30));
            //mTitleField.setMaximumSize(new Dimension(Short.MAX_VALUE,Short.MAX_VALUE));
            //mTitleField.setMinimumSize(new Dimension(100, 30));


            //add(mPreviewPane, BorderLayout.CENTER);
            add(mTitleField, BorderLayout.SOUTH);
        }

        void loadResource(Resource r) {
            super.loadResource(r);
            String title = r.getTitle();

            //mPreviewPane.setVisible(false);
            
            if (title == null || title.length() < 1) {
                mTitleField.setVisible(false);
                return;
            }
            
            // Always use HTML, which creates auto line-wrapping for JLabels
            title = "<HTML><center>" + title;

            
            mTitleField.setVisible(true);

            if (true) {
                mTitleField.setText(title);
            } else { 
                //remove(mTitleField);
                out("OLD            size=" + mTitleField.getSize());
                out("OLD   preferredSize=" + mTitleField.getPreferredSize());
                mTitleField.setText(title);
                out("NOLAY          size=" + mTitleField.getSize());
                out("NOLAY preferredSize=" + mTitleField.getPreferredSize());
                //mTitleField.setSize(298, mTitleField.getHeight());
                //mTitleField.setSize(298, mTitleField.getHeight());
                //mTitleField.setPreferredSize(new Dimension(298, mTitleField.getHeight()));
                //mTitleField.setSize(mTitleField.getPreferredSize());
                //mTitleField.setSize(mTitleField.getPreferredSize());
                out("SETSZ          size=" + mTitleField.getSize());
                out("SETSZ preferredSize=" + mTitleField.getPreferredSize());
                //out("SETSZ preferredSize=" + mTitleField.getPreferredSize());
                mTitleField.setPreferredSize(null);
                //add(mTitleField, BorderLayout.SOUTH);
                //mTitleField.revalidate();
                //repaint();
                //mTitleField.setVisible(true);
            }

            //mPreviewPane.loadResource(mResource);
            //mPreviewPane.setVisible(true);
            //VUE.invokeAfterAWT(this);
        }

        /*
        public void run() {
            //mTitleField.revalidate();
            //mTitleField.setVisible(true);
            VUE.invokeAfterAWT(new Runnable() { public void run() {
                mPreviewPane.loadResource(mResource);
            }});
        }
        */
    }


    /*
    public static class NodeTree extends JPanel
    {
        private final OutlineViewTree tree;
        
        public NodeTree()
        {
            super(new BorderLayout());
            setName("Nested Nodes");
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
    */
    
    public static class UserMetaData extends JPanel
    {
        private NodeFilterEditor userMetaDataEditor = null;
        
        public UserMetaData()
        {
            super(new BorderLayout());
            //setBorder( BorderFactory.createEmptyBorder(10,10,10,6));

            // todo in VUE to create map before adding panels or have a model that
            // has selection loaded when map is added.
            // userMetaDataEditor = new NodeFilterEditor(mNode.getNodeFilter(),true);
            // add(userMetaDataEditor);
        }

        void load(LWComponent c) {
            //setTypeName(this, c, "Keywords");
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

    private JLabel makeLabel(String s) {
        JLabel label = new JLabel(s);
        label.setFont(mLabelFont);
        return label;
    }

    public class SummaryPane extends tufts.Util.JPanelAA
    {
        final VueTextPane labelField = new VueTextPane();
        
        SummaryPane()
        {
            add(makeLabel("Label: "));
            add(labelField);

            //setPreferredSize(new Dimension(Short.MAX_VALUE,63));
            //setMinimumSize(new Dimension(200,63));
            //setMaximumSize(new Dimension(Short.MAX_VALUE,63));
        }

        void load(LWComponent c) {
            setTypeName(this, c, "Information");
            labelField.attachProperty(c, LWKey.Label);
        }
    }
    
    
    /*
    // summary fields
    private final Object[] labelTextPairs = {
        "-Title",   mTitleField,
        "-Where",   mWhereField,
        "-Size",    mSizeField,
    };
    */

    private static class ScrollGrid extends JPanel implements Scrollable {
        private JComponent delegate;
        private int vertScrollUnit;
        ScrollGrid(JComponent delegate, int vertScrollUnit) {
            //super(new BorderLayout());
            super(new GridBagLayout());
            this.delegate = delegate;
            this.vertScrollUnit = vertScrollUnit;
        }
        public Dimension getPreferredScrollableViewportSize() {
            Dimension d = delegate.getSize();
            d.height = getHeight();
            return d;
        }

        /** clicking on the up/down arrows of the scroll bar use this */
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return vertScrollUnit;
            // TODO: can make this the full height of the component given the direction vertically
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return vertScrollUnit*2; // what triggers this?
        }
    
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
        
    }
    

    /**
     *
     * This works somewhat analogous to a JTable, except that the renderer's are persistent.
     * We fill a GridBagLayout with all the labels and value fields we might ever need, set their
     * layout constraints just right, then set the text values as properties come in, and setting
     * all the unused label's and fields invisible.  There is a maximum number of rows that can
     * be displayed (initally 20), but this number is doubled when exceeded.
     *
     */
    public class MetaDataPane extends JPanel
        implements PropertyMap.Listener, Runnable
    {
        private JLabel[] mLabels;
        //private JLabel[] mValues;
        private JTextComponent[] mValues;
        private final JPanel mGridBag;
        private final JScrollPane mScrollPane = new JScrollPane();
    
        MetaDataPane() {
            super(new BorderLayout());
            
            expandSlots();

            //mGridBag = new JPanel(new GridBagLayout());
            mLabels[0].setText("X");
            mGridBag = new ScrollGrid(this, mLabels[0].getPreferredSize().height + 4);
            Insets insets = (Insets) GUI.WidgetInsets.clone();
            insets.top = insets.bottom = 0;
            insets.right = 1;
            mGridBag.setBorder(new EmptyBorder(insets));
            //mGridBag.setBorder(new LineBorder(Color.red));
            
            addLabelTextRows(0, mLabels, mValues, mGridBag, null, null);

            if (true) {
                mScrollPane.setViewportView(mGridBag);
                mScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                mScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                mScrollPane.setOpaque(false);
                mScrollPane.getViewport().setOpaque(false);
                //scrollPane.setBorder(null); // no focus border
                //scrollPane.getViewport().setBorder(null); // no focus border
                add(mScrollPane);
            } else {
                add(mGridBag);
            }
        }


        // double the number if available slots
        private void expandSlots() {
            int maxSlots;
            if (mLabels == null)
                maxSlots = 20; // initial maximum
            else
                maxSlots = mLabels.length * 2;

            mLabels = new JLabel[maxSlots];
            //mValues = new JLabel[maxSlots];
            mValues = new JTextComponent[maxSlots];

            for (int i = 0; i < mLabels.length; i++) {
                mLabels[i] = new JLabel();
                //mValues[i] = new JLabel();
                mValues[i] = new JTextArea();
                mValues[i].setEditable(false);
                ((JTextArea)mValues[i]).setLineWrap(true);
                GUI.apply(mLabelFont, mLabels[i]);
                GUI.apply(mValueFont, mValues[i]);
                mLabels[i].setOpaque(false);
                mValues[i].setOpaque(false);
                mLabels[i].setVisible(false);
                mValues[i].setVisible(false);
            }
            
        }

        private void loadRow(int row, String label, String value) {
            if (DEBUG.RESOURCE) out("adding row " + row + " " + label + "=" + value);
            mLabels[row].setText(label + ":");
            mLabels[row].setVisible(true);
            mValues[row].setText(value);
            mValues[row].setVisible(true);
        }

        public void loadResource(Resource r) {
            loadProperties(r.getProperties());
        }
        
        private PropertyMap mRsrcProps;
        public synchronized void propertyMapChanged(PropertyMap source) {
            if (mRsrcProps == source)
                loadProperties(source);
        }

        public synchronized void loadProperties(PropertyMap rsrcProps)
        {
            // TODO: loops if we don't do this first: not safe!
            // we should be loading directly from the props themselves,
            // and by synchronized on them...  tho is nice that
            // only a single sorted list exists for each resource,
            // tho of course, then we have tons of  cached
            // sorted lists laying about.
            
            TableModel model = rsrcProps.getTableModel();

            if (mRsrcProps != rsrcProps) {
                if (mRsrcProps != null)
                    mRsrcProps.removeListener(this);
                mRsrcProps = rsrcProps;
                mRsrcProps.addListener(this);
            }
            
            int rows = model.getRowCount();

            if (rows > mLabels.length) {
                expandSlots();
                mGridBag.removeAll();
                addLabelTextRows(0, mLabels, mValues, mGridBag, null, null);
            }

            loadAllRows(model);
            
            // also doesn't seem to help
            //GUI.invokeAfterAWT(this);
                
            /*
            // none of these sync's seem to making much difference
            // Oh, hey -- try on the AWT thread & wait to complete?
            synchronized (mScrollPane.getTreeLock()) {
            synchronized (mScrollPane.getViewport().getTreeLock()) {
            synchronized (getTreeLock()) {
                
            }}}
            */
            
        }

        public synchronized void run() {
            loadAllRows(mRsrcProps.getTableModel());
        }

        private void loadAllRows(TableModel model) {
            int rows = model.getRowCount();
            int row;
            for (row = 0; row < rows; row++) {
                String label = model.getValueAt(row, 0).toString();
                String value = model.getValueAt(row, 1).toString();
                
                // loadRow(row++, label, value); // debug non-HTML display
                
                // FYI, some kind of HTML bug for text strings with leading slashes
                // -- they show up empty.  Right now, we're disable HTML for
                // all synthetic keys, which covers URL.path, which was the problem.
                //if (label.indexOf(".") < 0) 
                //value = "<html>"+value;
                
                loadRow(row, label, value);
                
                
            }
            for (; row < mLabels.length; row++) {
                //out(" clear row " + row);
                mLabels[row].setVisible(false);
                mValues[row].setVisible(false);
            }
            //mScrollPane.getViewport().setViewPosition(new Point(0,0));
        }
        
    }

    //----------------------------------------------------------------------------------------
    // Utility methods
    //----------------------------------------------------------------------------------------
    
    private final int topPad = 2;
    private final int botPad = 2;
    private final Insets labelInsets = new Insets(topPad, 0, botPad, GUI.LabelGapRight);
    private final Insets fieldInsets = new Insets(topPad, 0, botPad, 0);
    
    /** labels & values must be of same length */
    private void addLabelTextRows(int starty,
                                  JLabel[] labels,
                                  JComponent[] values,
                                  Container gridBag,
                                  Font labelFace,
                                  Font fieldFace)
    {
        // Note that the resulting alignment ends up being somehow FONT dependent!
        // E.g., works great with Lucida Grand (MacOSX), but with system default,
        // if the field value is a wrapping JTextPane (thus gets taller as window
        // gets narrower), the first line of text rises slightly and is no longer
        // in line with it's label.
        
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        c.weighty = 0;

        
        for (int i = 0; i < labels.length; i++) {
            
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

            final JLabel label = labels[i];
            if (labelFace != null)
                GUI.apply(labelFace, label);
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
            
            final JComponent field = values[i];
            if (fieldFace != null)
                GUI.apply(fieldFace, field);
            gridBag.add(field, c);

        }

        // add a default vertical expander to take up extra space
        // (so the above stack isn't vertically centered if it
        // doesn't fill the space).

        c.weighty = 1;
        c.weightx = 1;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JComponent defaultExpander = new JPanel();
        defaultExpander.setPreferredSize(new Dimension(Short.MAX_VALUE, 1));
        defaultExpander.setOpaque(true);
        if (DEBUG.BOXES) defaultExpander.setBackground(Color.red);
        gridBag.add(defaultExpander, c);
    }
    
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
            //w = GUI.createDockWindow("Resource Inspector", p.mSummaryPane);
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

        //displayTestPane(rsrc);
        InspectorPane ip = new InspectorPane();

        VUE.getResourceSelection().setTo(new URLResource("file:///VUE/src/tufts/vue/images/splash_graphic_1.0.gif"));
        
        Widget.setExpanded(ip.mResourceMetaData, true);
        GUI.createDockWindow("Test Properties", ip).setVisible(true);

        
    }

    
    
}

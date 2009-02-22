/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.StringTokenizer;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.TableBag;
import tufts.vue.Resource;
import tufts.vue.VUE;
import tufts.vue.MetaMap;
import tufts.vue.gui.GUI;

import javax.swing.JButton;
/**
*
* This works somewhat analogous to a JTable, except that the renderer's are persistent.
* We fill a GridBagLayout with all the labels and value fields we might ever need, set their
* layout constraints just right, then set the text values as properties come in, and setting
* all the unused label's and fields invisible.  There is a maximum number of rows that can
* be displayed (initally 20), but this number is doubled when exceeded.
*
*/
public class MetaDataPane extends tufts.vue.gui.Widget
   implements TableBag.Listener, Runnable
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MetaDataPane.class);
    
    private JLabel[] mLabels;
    private JTextArea[] mValues;
    private final ScrollableGrid mGridBag;
    private final boolean inOwnedScroll;
    private JScrollPane mScrollPane;
    private boolean inScroll;
    
    /** The current MetaMap we're displaying and listening to updates from */
    private TableBag mProperties;

    private final String mLabel;
 	  
//     /** If the displayed properties were from a Resource, this will be set to it, otherwise null */
//     private Resource mResource;
    
    public MetaDataPane(String label, boolean scroll) {
        super("contentInfo");
        mLabel = label;
        inOwnedScroll = scroll;
        ensureSlots(20);
       
        mLabels[0].setText("X"); // make sure label will know it's max height
        final int scrollUnit = mLabels[0].getPreferredSize().height + 4;
        mGridBag = new ScrollableGrid(this, scrollUnit);

        Insets insets = (Insets) GUI.WidgetInsets.clone();
        insets.top = insets.bottom = 0;
        insets.right = 1;
       
        mGridBag.setBorder(GUI.makeSpace(insets));
        //mGridBag.setBorder(new LineBorder(Color.red));
       
        addLabelTextRows(0, mLabels, mValues, mGridBag, null, null);

        if (inOwnedScroll) {
            mScrollPane = new JScrollPane();
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

//         if (DEBUG.Enabled)
//             mScrollPane.getVerticalScrollBar().getModel().addChangeListener(new ChangeListener() {
//                     public void stateChanged(ChangeEvent e) {
//                         if (DEBUG.SCROLL) VUE.Log.debug("vertScrollChange " + e.getSource());
//                     }
//                 });

    }

    @Override
    public void addNotify() {
        super.addNotify();

        if (mScrollPane == null)
            mScrollPane = (JScrollPane) javax.swing.SwingUtilities.getAncestorOfClass(JScrollPane.class, this);

        inScroll = (mScrollPane != null);
    }

    private JLabel createLabel() {
        final JLabel label;
        
//         if (DEBUG.Enabled) {
//             label = new tufts.Util.JLabelAA();
//             //label.setBackground(Color.red);
//             //label.setOpaque(true);
//         } else
            label = new JLabel();
        
        return label;
    }

    private boolean wasDebug = DEBUG.Enabled;

    private static final Font LabelFace;
    private static final Font ValueFace;

    private static final boolean EasyReading1 = false;
    private static final boolean EasyReading2 = false;

    static {

        GUI.Face nameFace = null;
        GUI.Face dataFace = null;

        if (true) {

            String labelFont;
            String dataFont;
            int fontSize;
    
            if (GUI.isMacAqua()) {
                labelFont = "Lucida Grande";
                dataFont = labelFont;
                fontSize = 11;

                if (DEBUG.DR) dataFont = "Lucida Sans Typewriter";
            } else {
                labelFont = "SansSerif";
                fontSize = 12;
                
                // On XP, Lucida Sans Unicode looks better for values not so much for
                // bold labels tho) Note: this is a smaller font than SansSerif, and our
                // layout code is primarily tuned to the pixel for SanSerif, so
                // adjustments are need to use this, or we need to write the code to
                // compute the layout using actual font metrics.

                dataFont = "Lucida Sans Unicode";
                
                if (DEBUG.DR) {
                    dataFont = "Lucida Console";
                    fontSize = 11;
                }
            }

            //nameFace = new GUI.Face(labelFont, Font.BOLD, fontSize, Color.gray);
            //dataFace = new GUI.Face(dataFont, Font.PLAIN, fontSize, Color.black);
            nameFace = new GUI.Face(labelFont, Font.BOLD, fontSize, null);
            dataFace = new GUI.Face(dataFont, Font.PLAIN, fontSize, null);
        }

        if (EasyReading1) {
            LabelFace = nameFace;
            ValueFace = dataFace;
        } else {
            LabelFace = GUI.LabelFace;
            if (DEBUG.DR)
                ValueFace = dataFace;
            else
                ValueFace = GUI.ValueFace;
        }
    }

   /**
    * Make sure at least this minimum number of slots is available.
    * @return true if # of slots is expanded
    **/
   private boolean ensureSlots(int minSlots) {
       int curSlots;

       if (mLabels == null || (wasDebug != DEBUG.Enabled)) {
           curSlots = 0;
           wasDebug = DEBUG.Enabled;
       } else
           curSlots = mLabels.length;

       if (minSlots <= curSlots)
           return false;

       int maxSlots;
       if (curSlots == 0)
           maxSlots = minSlots;
       else
           maxSlots = minSlots + 4;

       if (DEBUG.RESOURCE) out("expanding slots to " + maxSlots);

       mLabels = new JLabel[maxSlots];
       mValues = new JTextArea[maxSlots];

       final Color alternatingColor;

       if (Util.isMacPlatform())
           alternatingColor = Color.white;
       else
           alternatingColor = new Color(250,250,250);

       final Border fillBorder = GUI.makeSpace(TopPad,4,BotPad,0);
       final Border macAdjustBorder = GUI.makeSpace(0,4,0,0);
       final Border winAdjustBorder = GUI.makeSpace(0,4,0,0);
       final Border winAdjustBorder1 = GUI.makeSpace(1,0,0,0);

//        if (EasyReading2)
//            winAdjustBorder = GUI.makeSpace(0,4,0,0);
//        else
//            winAdjustBorder = GUI.makeSpace(0,4,1,0);
       
       final Border WindowsPlatformAdjustBorder = GUI.makeSpace(0,0,2,0);
       
       for (int i = 0; i < mLabels.length; i++) {
           mLabels[i] = createLabel();
           mValues[i] = new JTextArea();
           mValues[i].setEditable(false);
           mValues[i].setLineWrap(true);
           GUI.apply(LabelFace, mLabels[i]);
           GUI.apply(ValueFace, mValues[i]);
           mLabels[i].setOpaque(false);
           mValues[i].setOpaque(false);
           mLabels[i].setVisible(false);
           mValues[i].setVisible(false);

           if (Util.isWindowsPlatform())
               mValues[i].setBorder(WindowsPlatformAdjustBorder);

           if (EasyReading2) {
               if (i % 2 != 0) {
                   //mLabels[i].setBackground(alternatingColor);
                   mValues[i].setBackground(alternatingColor);
                   //mLabels[i].setOpaque(true);
                   mValues[i].setOpaque(true);
                   mLabels[i].setBorder(fillBorder);
                   mValues[i].setBorder(fillBorder);
               } else {
                   if (Util.isMacPlatform())
                       mValues[i].setBorder(macAdjustBorder);
                   else
                       mValues[i].setBorder(winAdjustBorder);
               }
           } else if (EasyReading1) {
               // todo: compute all this based on font metrics
               if (Util.isWindowsPlatform()) {
                   mLabels[i].setBorder(winAdjustBorder1);
               }
           } else if (DEBUG.BOXES) {
               if (i % 2 == 0) {
                   //mLabels[i].setBackground(new Color(255,255,255,128));
                   //mValues[i].setBackground(new Color(255,255,255,128));
                   mLabels[i].setBackground(Color.white);
                   mValues[i].setBackground(Color.white);
                   mLabels[i].setOpaque(true);
                   mValues[i].setOpaque(true);
               }
                   
           }
           
           mValues[i].addMouseListener(CommonURLListener);
           
       }
       
       return true;
       
   }

    private static final String CAN_OPEN = "vue.openable";

    private class URLMouseListener extends tufts.vue.MouseAdapter {
        public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getClickCount() != 2)
                return;
            try {
                final JTextArea value = (JTextArea) e.getSource();
                if (value.getClientProperty(CAN_OPEN) == Boolean.TRUE) {
                    e.consume();
                    final Color c = value.getForeground();
                    value.setForeground(Color.red);
                    value.paintImmediately(0,0, value.getWidth(), value.getHeight());
                    tufts.vue.VueUtil.openURL(value.getText());
                    GUI.invokeAfterAWT(new Runnable() {
                            public void run() {
                                value.select(0,0);
                                GUI.invokeAfterAWT(new Runnable() { public void run() { value.setForeground(c); }});
                            }
                        });
                }
            } catch (Throwable t) {
                Log.error(t);
            }
        }
    }


//     public void loadResource(Resource r) {
//         if (DEBUG.RESOURCE) out("loadResource:  " + r);
//         loadProperties(r.getProperties());
//     }

   
    /** MetaMap.Listener event delivery: do not synchronize this method,
     * as the call is usually coming from an ImgLoader thread, and
     * we must not synchronize or we risk deadlock */
    
    public void tableBagChanged(final TableBag source) {
        
        // Note: we can deadlock here obtaining lock held by AWT (if we synchronize this method)
        // This is normally called NOT from an AWT thread (e.g., from an ImageLoader)

        // If we synchronize this method, this can happen:

        // Thread-AWT is trying to load new properties (a ResourceSelection change has
        // lead to MetaDataPane.loadProperties), and locks the singleton MetaDataPane to
        // do so.  It has not yet attempted to obtain any MetaMap locks.
        
        // Thread-ImageLoader updated MetaMap-X (e.g., called releaseChanges()), and
        // has locked MetaMap-X to notify all it's listeners of the change (in this
        // case, call MetaDataPane.propertyMapChanged).

        // The call in Thread-ImageLoader to propertyMapChanged cannot complete until
        // the AWT lock on MetaDataPane is released.

        // The call in Thread-AWT, already locking AWT on MetaDataPane, now attempting to
        // lock MetaMap-X to remove us as a listener, cannot complete until Thread-ImageLoader
        // releases the lock on MetaMap-X, which is attempting to notify all it's listeners.

        // Thus, we hava a classic deadlock.

        // To handle this, we force this event to ultimately be delivered on the AWT thread.
        
        // if (DEBUG.IMAGE) Log.info("propertyMapChanged entry " + Util.tag(source));

        // This update will most often be coming from an ImageLoader thread.
        // updateDisplay, once it's holding the lock (it's synchronized), will check the
        // current value of mProperties to see if it matches the current update source,
        // and only perform the update if mProperties hasn't changed since we got this
        // notification.

        GUI.invokeAfterAWT(new Runnable() {
                public void run() {
                    try {
                        updateDisplayAWT(source);
                    } catch (Throwable t) {
                        Log.error("udpateDisplayAWT: " + Util.tags(source) + ";", t);
                    }
                }
            });
        
        // if (DEBUG.IMAGE) Log.info("propertyMapChanged exit  " + Util.tag(source));
    }

    public void loadTable(final TableBag propertyMap) {

        if (DEBUG.THREAD) Log.debug("loadProperties: " + Util.tag(propertyMap));
                                    
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
               
            // If called from AWT, (e.g., normally as the result of a ResourceSelection change),
            // we can proceed immediately.  This is the common case.
            
            loadPropertiesAWT(propertyMap);
            
        } else {

            // If NOT called from AWT, force the event to happen on AWT or
            // we risk a dead-lock.  This is not the normal case, but
            // we handle it to ensure we can never dead-lock.

            GUI.invokeAfterAWT(new Runnable() {
                    public void run() {
                        loadPropertiesAWT(propertyMap);
                    }
                });
        }
    }
   
    // as we guarantee that we only ever run this in the AWT thread, this method
    // doesn't need to be synchronized
    private void loadPropertiesAWT(TableBag propertyMap)
    {
        if (DEBUG.THREAD || DEBUG.RESOURCE || DEBUG.IMAGE) out("loadPropertiesAWT: " + propertyMap.size() + " key/value pairs");
       
        try {

            // Note: if another thread has a lock on mProperties (e.g., MetaMap is
            // in the middle of delivering an update to us about the same mProperties),
            // we could dead-lock in propertyMapChanged above if it was a synchronized
            // method (which is why it is not).
           
            if (mProperties != propertyMap) {
                if (mProperties != null)
                    mProperties.removeListener(this); 
                mProperties = propertyMap;
                mProperties.addListener(this);
                updateDisplayAWT(mProperties);
            }
           
            
        } catch (Throwable t) {
            Log.error("loadPropertiesAWT: " + propertyMap, t);
        }
    }

    // Note: should only be called on AWT Event Dispatch Thread.
    // If it were synchronized, and this method is was from a non-AWT thread, we could risk deadlock.
    private void updateDisplayAWT(final TableBag properties)
    {
        if (mProperties != properties) {
            if (DEBUG.Enabled) Log.debug("too late for update to " + properties);
            return;
        }
        
        if (DEBUG.THREAD || DEBUG.RESOURCE) out("updateDisplay: " + properties.size() + " key/value pairs");
       
        if (DEBUG.SCROLL)
            Log.debug("scroll model listeners: "
                      + Arrays.asList(((DefaultBoundedRangeModel)
                                       mScrollPane.getVerticalScrollBar().getModel())
                                      .getListeners(ChangeListener.class)));

	if (inScroll)
            mScrollPane.getVerticalScrollBar().setValueIsAdjusting(true);

        mGridBag.setPaintDisabled(true);

        try {

            if (DEBUG.RESOURCE || DEBUG.THREAD) out("updateDisplay: getTableModel() on " + tufts.Util.tag(properties));
           
            // Note: MetaMap.getTableModel() is synchronized, as is MetaMap.addListener/removeListener,
            // so if this (or they) are called while we already hold a lock on the MetaMap
            // from another thread, we'll deadlock.
            final TableModel model = properties.getTableModel();

            // TODO: get as raw collection instead: old PropertyMap impl can
            // provide a collection view of a table-model if need be for backward compat.

            if (DEBUG.RESOURCE) out("updateDisplay: model=" + model
                                    + " modelSlots=" + model.getRowCount()
                                    + " slotsAvail=" + mLabels.length
                                    );
           
            if (ensureSlots(model.getRowCount())) {
                mGridBag.removeAll();
                addLabelTextRows(0, mLabels, mValues, mGridBag, null, null);
            }
           
            loadAllRows(model, properties instanceof MetaMap ? (MetaMap) properties : null);

            if (DEBUG.Enabled)
                setTitle(mLabel + " (" + properties.size() + ")");
            
            GUI.invokeAfterAWT(this);

        } catch (Throwable t) {
            mGridBag.setPaintDisabled(false);
            Log.error("updateDisplayAWT: " + Util.tags(properties) + ";", t);
        }
    }
    

    public synchronized void run() {

        // TODO: move this code up to a generic Widget capability,
        // merging it with similar code in InspectorPane Widget.
        
        // And does this still need to be synchronized?  This always
        // runs in AWT, and now that updateDisplay always runs in AWT,
        // our calls to JScrollBar.setValueIsAdjusting and
        // mGridBag.setPaintDisabled (which are not threadsafe calls
        // in of themseleves) should always be running synchronously.
        
        try {
           
            // Always put the scroll-bar back at the top, as it defaults to moving to the
            // bottom.  E.g., when selecting through search results that all have tons of
            // meta-data, we're left looking at just the meta-data, not the preview.
            // Ideally, we could check the scroller position first to see if we were
            // already at the top at the start and only do this if that was the case.
           
//             VUE.getInfoDock().scrollToTop();
//             if (inOwnedScroll)
//                 mScrollPane.getVerticalScrollBar().setValue(0);
//             // Now release all scroll-bar updates.
//             if (inScroll) 
//                 mScrollPane.getVerticalScrollBar().setValueIsAdjusting(false);

            if (inScroll) {
                //out("SCROLL-TO-TOP");
                mScrollPane.getVerticalScrollBar().setValue(0);
                mScrollPane.getVerticalScrollBar().setValueIsAdjusting(false);
            }
           
            // Now allow the grid to repaint.
        } finally {
            mGridBag.setPaintDisabled(false);
            mGridBag.repaint();
        }
    }

    private String valueToText(final Object value) {
        if (value == null)
            return "(empty)";
        else if (value instanceof java.awt.Component)
            return GUI.name(value);
        else if (value instanceof java.lang.ref.Reference)
            return "[" + value.getClass().getSimpleName() + "] "
                + valueToText(((java.lang.ref.Reference)value).get()); // note recursion
        else
            return value.toString();
    }

    private void loadAllRows(TableModel model, MetaMap dataMap) {
        final int rows = model.getRowCount();
        final int maxRow;

        if (Util.getJavaVersion() > 1.5) {
            maxRow = rows;
        } else {
            // prior to java 1.6, GridBag has serious bug in that it completely fails
            // if more than 512 items are loaded into it.
            if (rows > 512)
                maxRow = 512;
            else
                maxRow = rows;
        }

        int rowIdx = 0;

        mLastLabel = null;
        
        for (int row = 0; row < maxRow; row++) {
            final Object label =  model.getValueAt(row, 0);
            final String labelText = "" + label;
            final Object value = model.getValueAt(row, 1);
            final String valueText = valueToText(value);

            // loadRow(row++, label, value); // debug non-HTML display
            // FYI, some kind of HTML bug for text strings with leading slashes
            // -- they show up empty.  Right now, we're disable HTML for
            // all synthetic keys, which covers URL.path, which was the problem.
            //if (label.indexOf(".") < 0) 
            //value = "<html>"+value;

            if (Resource.isHiddenPropertyKey(labelText)) {

                if (DEBUG.DR || DEBUG.DATA) {
                    // Allow hidden properties to be seen
                } else {
                    // default: hide the hidden property
                    //mLabels[row].setVisible(false);
                    //mValues[row].setVisible(false);
                    // we skip loading the row completely -- keep alternating colors in order
                    continue;
                }
            }

            boolean loaded = false;

            try {
                loaded = loadRow(rowIdx, labelText, value, valueText);
            } catch (Throwable t) {
                Log.error("Failed to load row " + row + "; label= " + Util.tags(label) + "; value=" + Util.tags(value), new Throwable());
            }
            if (loaded)
                rowIdx++;
        }

        if (DEBUG.Enabled && dataMap != null && dataMap.getSchema() != null) {
            loadRow(rowIdx++, "SCHEMA", null, ""+dataMap.getSchema());
        }
          

        for (; rowIdx < mLabels.length; rowIdx++) {
            //out(" clear row " + row);
            mLabels[rowIdx].setVisible(false);
            mValues[rowIdx].setVisible(false);
        }

        if (rows > maxRow) {
            // prior to java 1.6, GridBag has serious bug in that it completely fails
            // if more than 512 items are loaded into it.  This merges all rows
            // > 512 into a single field.
            final StringBuilder mergeRow = new StringBuilder();
            for (int r = maxRow - 1; r < rows; r++) {
                final Object label = model.getValueAt(r, 0);
                final Object value = model.getValueAt(r, 1);
                
                mergeRow.append(label);
                mergeRow.append(": ");
                mergeRow.append(""+value);
                mergeRow.append('\n');
            }
            loadRow(511, "(Remaining)", "Overflow Rows > 512", mergeRow.toString());
            mLabels[511].setVisible(true);
            mValues[511].setVisible(true);
        }
        
        
        //mScrollPane.getViewport().setViewPosition(new Point(0,0));
    }


    private MouseListener CommonURLListener = new URLMouseListener();

    private final Color ObjectColor = new Color(128,0,0);

    private String mLastLabel = null;
    private JTextArea mLastValue = null;

    private boolean loadRow(int row, final String labelText, final Object value, final String valueText) {
        if (DEBUG.RESOURCE && DEBUG.META) out("adding row " + row + " " + labelText + "=[" + valueText + "]");

        //Log.debug(String.format("lastLabel[%s], thisLabel[%s]", mLastLabel, labelText));
        
        if (mLastLabel != null && mLastValue != null && labelText.startsWith(mLastLabel + "@")) {
            
            // This hack removes Foo@attribute-name repeats in list -- is just a decoration
            // tweak for now -- ultimately, this should be represtented in the MetaMap as an
            // association between keys (will need sub-keys), which will be required to
            // accurately represent XML data, which will currently cause a repeated
            // key-value pair to be ignored: e.g., if a jira item had multiple comments,
            // each with an "author=<username>" key-value with it, only the first unique
            // instance will appear in the map: e.g., only the first "author=melanie" would
            // appear -- subsequent comments by user melanie would have no author associated
            // with them, as our data-map, although it allows multple values per key, is still
            // flat and only allows one instance of key-value pair.
            
            String attributeLabel = labelText.substring(mLastLabel.length() + 1);
            mLastValue.setText(String.format("%s [%s=%s]", mLastValue.getText(), attributeLabel, value));
            if (!DEBUG.DR) return false;
        }

        final JLabel label = mLabels[row];
        final JTextArea field = mValues[row];

        if (EasyReading2) {
            label.setText(labelText);
        } else {
            final String txt;

            //-----------------------------------------------------------------------------
            // hack to trim some long XML names in RSS feeds until UI can wrap keys as
            // well as labels (e.g. NY Times XML news items sometimes have an associated image)
            final String trim1 = "media:group.media:content.media:";
            final String trim2 = "media:group.media:";
            final String lowText = labelText.toLowerCase();
            if (lowText.startsWith(trim1)) {
                txt = "Media" + labelText.substring(trim1.length()-1);
                //txt = "MGMCM" + txt.substring(trim1.length()-1);
            }
            else if (lowText.startsWith(trim2)) {
                txt = "Media" + labelText.substring(trim2.length()-1);
                //txt = "MGM" + txt.substring(trim2.length()-1);
            }
            else if (lowText.equals("comments.comment")) {
                // hack for jira comments
                txt = "Comment";
            }
//             else if (lowText.indexOf("@") > 0) {
//                 txt = "<html>" + lowText + "<br>newline";
//             }
            else if (labelText.length() > 20) {
                final int len = labelText.length();
                final int half = len / 2 + 1;
                txt = "<html>"
                    + labelText.substring(0,half) + "<br>"
                    + labelText.substring(half,len);
            }
            else
                txt = Util.upperCaseWords(labelText);
                
            //-----------------------------------------------------------------------------
            
            final StringBuilder buf = new StringBuilder(txt.length() + 1);
            buf.append(txt);
            buf.append(':');
            label.setText(buf.toString());
        }

        mLastLabel = labelText;
        mLastValue = field;

        if (Resource.looksLikeURLorFile(valueText)) {
            //field.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            field.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            field.putClientProperty(CAN_OPEN, Boolean.TRUE);
            if (DEBUG.Enabled || EasyReading1) field.setForeground(Color.blue);
        } else {
            field.putClientProperty(CAN_OPEN, Boolean.FALSE);
            if (DEBUG.Enabled || EasyReading1) field.setForeground(Color.black);
            //label.removeMouseListener(CommonURLListener);
            field.setCursor(Cursor.getDefaultCursor());
            //GUI.apply(GUI.ValueFace, mValues[i]);
        }

// JTextArea doesn't support HTML
//         if (valueText.indexOf("</") > 0)
//             field.setText("<html>" + valueText);
//         else
//             field.setText(valueText);
        
        field.setText(valueText);
        
        if (DEBUG.Enabled) {

            if (Resource.canDump(value)) {
                String txt = Resource.getDump(value);
                txt = "<html><code>" + txt.replaceAll("\n", "&nbsp;<br>&nbsp;&nbsp;") + "</code>";
                field.setToolTipText(txt);
            } else {
                //if (value instanceof String == false)
                field.setToolTipText(Util.tags(value));
            }
            
            if (value instanceof String == false)
                field.setForeground(ObjectColor);
        }
       
        // if field has at least one space, use word wrap
        if (valueText.indexOf(' ') >= 0)
            field.setWrapStyleWord(true);
        else
            field.setWrapStyleWord(false);
       
        label.setVisible(true);
        field.setVisible(true);

        return true;
       
    }
    

   private void out(Object o) {
        Log.debug((o==null?"null":o.toString()));
   }
   
   //----------------------------------------------------------------------------------------
   // Utility methods
   //----------------------------------------------------------------------------------------
   
//    private void addLabelTextPairs(Object[] labelTextPairs, Container gridBag) {
//        JLabel[] labels = new JLabel[labelTextPairs.length / 2];
//        JComponent[] values = new JComponent[labels.length];
//        for (int i = 0, x = 0; x < labels.length; i += 2, x++) {
//            //out("ALTP[" + x + "] label=" + labelTextPairs[i] + " value=" + GUI.name(labelTextPairs[i+1]));
//            String labelText = (String) labelTextPairs[i];
//            labels[x] = new JLabel(labelText + ":");
//            values[x] = (JComponent) labelTextPairs[i+1];
//        }
//        addLabelTextRows(0, labels, values, gridBag, GUI.LabelFace, GUI.ValueFace);
//    }

    private final int TopPad = Util.isMacPlatform() ? 2 : 1;
    private final int BotPad = Util.isMacPlatform() ? 2 : 0;
    private final Insets labelInsets = new Insets(TopPad, 0, BotPad, GUI.LabelGapRight);
    private final Insets fieldInsets = new Insets(TopPad, 0, BotPad, GUI.FieldGapRight);
//     private final Insets labelInsets = new Insets(0,0,0,0);
//     private final Insets fieldInsets = new Insets(0,0,0,0);
   
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
       c.gridheight = 1;

       
       for (int i = 0; i < labels.length; i++) {

           //out("ALTR[" + i + "] label=" + GUI.name(labels[i]) + " value=" + GUI.name(values[i]));
           
           boolean centerLabelVertically = false;
           
           JLabel label = labels[i];
           JComponent field = values[i];                      
           
           if (labelFace != null)
               GUI.apply(labelFace, label);

           if (field instanceof JTextComponent) {
               if (field instanceof JTextField)
                   centerLabelVertically = true;
//                JTextComponent textField = (JTextComponent) field;
//                editable = textField.isEditable();
//                if (field instanceof JTextArea) {
//                    JTextArea textArea = (JTextArea) field;
//                    c.gridheight = textArea.getRows();
//                    } else if (field instanceof JTextField)
           } else {
               if (fieldFace != null)
                   GUI.apply(fieldFace, field);
           }
           
           //-------------------------------------------------------
           // Add the field label
           //-------------------------------------------------------
           
           c.gridx = 0;
           c.gridy = starty++;
           c.insets = labelInsets;
           c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last in row
           c.fill = GridBagConstraints.NONE; // the label never grows
           if (centerLabelVertically)
               c.anchor = GridBagConstraints.EAST;
           else
               c.anchor = GridBagConstraints.NORTHEAST;
           c.weightx = 0.0;                  // do not expand
           gridBag.add(label, c);

           //-------------------------------------------------------
           // Add the field value
           //-------------------------------------------------------
           
           c.gridx = 1;
           c.gridwidth = GridBagConstraints.REMAINDER;     // last in row
           c.fill = GridBagConstraints.HORIZONTAL;
           c.anchor = GridBagConstraints.CENTER;
           //c.anchor = GridBagConstraints.NORTH;
           c.insets = fieldInsets;
           c.weightx = 1.0; // field value expands horizontally to use all space
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
       //this truly doesn't allow users to edit metdata all the time
       // They can edit it only if they have permissions and the metdata will be
       // published to dublin core supported repository
//       JButton editMetadataButton  = new JButton(new String("Edit"));
//       c.weighty =0;
//       c.weightx =0;
//       c.gridx =1;
//       c.fill = GridBagConstraints.NONE;
//       c.anchor = GridBagConstraints.NORTHEAST;
//       gridBag.add(editMetadataButton,c);
       
       
       if (false) {
           JComponent defaultExpander = new JPanel();
           defaultExpander.setPreferredSize(new Dimension(Short.MAX_VALUE, 1));
           if (DEBUG.BOXES) {
               defaultExpander.setOpaque(true);
               defaultExpander.setBackground(Color.orange);
           } else
               defaultExpander.setOpaque(false);
           gridBag.add(defaultExpander, c);
       }
       
     return; 
   }
   /*
	**  Return the number of lines of text, including wrapped lines.
	*/
   public static int getWrappedLines(JTextComponent c)
   {
	   int len = c.getDocument().getLength();
	   int offset = 0;

	   //    	Increase 10% for extra newlines
	   StringBuffer buf = new StringBuffer((int) (len * 1.10));

	   try
	   {
		   while (offset < len)
		   {
			   int end = javax.swing.text.Utilities.getRowEnd(c, offset);
			   if (end < 0)
			   {
				   break;
			   }

			   //    	Include the last character on the line
			   end = Math.min(end + 1, len);
			   
			   String s = c.getDocument().getText(offset, end - offset);
			   buf.append(s);

			   //    	Add a newline if s does not have one
			   if (!s.endsWith("\n"))
			   {
				   buf.append('\n');
			   }
			   offset = end;
		   }
	   }
	   catch (BadLocationException e)
	   {
	   }
	   StringTokenizer token = new StringTokenizer(buf.toString(), "\n");
	   int linesOfText = token.countTokens();
	   
	   if (linesOfText == 0)
		   linesOfText =1;
	   return linesOfText;
   	}


//     public void loadProperties(final MetaMap resourceProperties) {

//         if (DEBUG.THREAD) Log.debug("loadProperties: " + Util.tag(resourceProperties));
                                    
//         if (javax.swing.SwingUtilities.isEventDispatchThread()) {
               
//             // If called from AWT, this should normally be the result of a ResourceSelection change,
//             // and that should get immediate priority
//             doLoadProperties(resourceProperties);
            
//         } else {

//             // If NOT called from AWT (e.g., an ImageLoder), this would normally be a LOW priority
//             // update, which doesn't need to happen immediately.

//             // Does this really make sense?  All we're checking is if the
//             // current properties has changed at all.  If this was NOT a call
//             // from propertyMapChanged, it makes no sense: TODO TODO TODO: MOVE THIS
//             // CODE UP TO PROPERTY MAP CHANGED

//             // TODO TODO: AND: CAN SPLIT OUT LOAD v.s. RELOAD-CODE: RELOAD-CODE
//             // NEVER HAS TO CHECK FOR ADDING/REMOVING LISTENERS, OR SET mProperties!
            
//             final Object lowPriorityProperties = mProperties;
            
//             // 2008-04-12 SMF: if we're in an image loader thread when
//             // we get this callback, we risk deadlock -- this should fix it.
//             GUI.invokeAfterAWT(new Runnable() {
//                     public void run() {
//                         if (lowPriorityProperties == mProperties)
//                             doLoadProperties(resourceProperties);
//                     }
//                 });
//         }
//     }
   
//    private synchronized void doLoadProperties(MetaMap rsrcProps)
//    {
//        // TODO: loops if we don't do this first: not safe!  we should be loading
//        // directly from the props themselves, and by synchronized on them...  tho is
//        // nice that only a single sorted list exists for each resource, tho of course,
//        // then we have tons of cached sorted lists laying about.

//        if (DEBUG.THREAD || DEBUG.RESOURCE) out("loadProperties: " + rsrcProps.size() + " key/value pairs");
       
//        if (DEBUG.SCROLL)
//            Log.debug("scroll model listeners: "
//                          + Arrays.asList(((DefaultBoundedRangeModel)
//                                           mScrollPane.getVerticalScrollBar().getModel())
//                                          .getListeners(ChangeListener.class)));

// 	if (scroll)
//             mScrollPane.getVerticalScrollBar().setValueIsAdjusting(true);

//        mGridBag.setPaintDisabled(true);

//        try {
//            // Description of a dead-lock that has been fixed by having
//            // MetaMap.getTableModel() sync on it's own lock:
           
//            // Example: VUE-ImageLoader49 holds changes on the props, then goes to notify
//            // us here in the MetaData pane.  But The AWT thread had already put is in
//            // here, right below, trying to call getTableModel(), but before we can call
//            // it, the above notification needs to be released.  If the props had CHANGED
//            // to entirely different set, from another resource, this wouldn't have been
//            // a problem, because the update would have been skipped above in
//            // propertyMapChanged.

//            // Put another way: the MetaMap is trying to notify us, but is waiting
//            // for us to break out of this method for the lock to release, so
//            // propertyMapChanged can be called, but then we call getTableModel(), which
//            // is locked on that same MetaMap that is waiting for us, and thus
//            // deadlock...

//            if (DEBUG.RESOURCE || DEBUG.THREAD) out("loadProperties: getTableModel() on " + tufts.Util.tag(rsrcProps));
           
//            TableModel model = rsrcProps.getTableModel();

//            if (DEBUG.RESOURCE) out("loadProperties: model=" + model
//                                    + " modelSlots=" + model.getRowCount()
//                                    + " slotsAvail=" + mLabels.length
//                                    );
           
//            if (mProperties != rsrcProps) {
//                if (mProperties != null)
//                    mProperties.removeListener(this); // *AWT* THREAD: CAN DEADLOCK IN MetaMap.java line 175 (ImageLoader-26 contention)
//                mProperties = rsrcProps;
//                mProperties.addListener(this);
//            }
           
//            if (ensureSlots(model.getRowCount())) {
//                mGridBag.removeAll();
//                addLabelTextRows(0, mLabels, mValues, mGridBag, null, null);
//            }
           
//            loadAllRows(model);
           
//            GUI.invokeAfterAWT(this); // TODO: Do we still need this as an invoke if this.scroll == false ?

//        } catch (Throwable t) {
//            mGridBag.setPaintDisabled(false);
//            tufts.Util.printStackTrace(t);
//        }
           
       
//        /*
//        // none of these sync's seem to making any difference
//        synchronized (mScrollPane.getTreeLock()) {
//        synchronized (mScrollPane.getViewport().getTreeLock()) {
//        synchronized (getTreeLock()) {
//        }}}
//        */
       
//    }


//     private Dimension size = new Dimension(200,100);
   
//     @Override
//     public Dimension getMinimumSize()
//     {
//         if (scroll)
//             return super.getMinimumSize();
	   
//         int height = 5;
//         int lines = 1;
//         for (int i = 0; i < mValues.length; i++)
//             {
//                 lines = getWrappedLines(mValues[i]);
//                 if (mValues[i].isVisible())
//                     {
//                         FontMetrics fm = mValues[i].getFontMetrics(mValues[i].getFont());
//                         height +=((lines * fm.getHeight()) + TopPad + BotPad);
//                     }
//                 //I wasn't taking into account the space between values
//                 height +=4;
//             }
           
//         if (height > size.getHeight())
//             return new Dimension(200,height);
//         else
//             return size;    	
//     }
   
//     @Override
//     public Dimension getPreferredSize()
//     {
//         if (scroll)
//             return super.getMinimumSize();
	   
//         return getMinimumSize();
//     }
   
    
    
   
}

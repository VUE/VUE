package tufts.vue.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseListener;
import java.util.Arrays;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import tufts.vue.DEBUG;
import tufts.vue.PropertyMap;
import tufts.vue.Resource;
import tufts.vue.VUE;
import tufts.vue.gui.GUI;

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
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MetaDataPane.class);
    
    private JLabel[] mLabels;
   //private JLabel[] mValues;
   private JTextArea[] mValues;
   private final ScrollableGrid mGridBag;
   private final JScrollPane mScrollPane = new JScrollPane();   
   private boolean scroll= false;
   
   public MetaDataPane(boolean scroll) {
       super(new BorderLayout());
       this.scroll = scroll;
       //setName("contentInfo");
       ensureSlots(20);
       
       mLabels[0].setText("X"); // make sure label will know it's max height
       mGridBag = new ScrollableGrid(this, mLabels[0].getPreferredSize().height + 4);
       Insets insets = (Insets) GUI.WidgetInsets.clone();
       insets.top = insets.bottom = 0;
       insets.right = 1;
       
       mGridBag.setBorder(new EmptyBorder(insets));
       //mGridBag.setBorder(new LineBorder(Color.red));
       
       addLabelTextRows(0, mLabels, mValues, mGridBag, null, null);

       if (scroll) {
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

      /* if (DEBUG.Enabled)
           mScrollPane.getVerticalScrollBar().getModel().addChangeListener(new ChangeListener() {
                   public void stateChanged(ChangeEvent e) {
                       if (DEBUG.SCROLL) VUE.Log.debug("vertScrollChange " + e.getSource());
                   }
               });
       */
       
   }


   /**
    * Make sure at least this minimum number of slots is available.
    * @return true if # of slots is expanded
    **/
   private boolean ensureSlots(int minSlots) {
       int curSlots;
       if (mLabels == null)
           curSlots = 0;
       else
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

       for (int i = 0; i < mLabels.length; i++) {
           mLabels[i] = new JLabel();
           mValues[i] = new JTextArea();
           mValues[i].setEditable(false);
           mValues[i].setLineWrap(true);
           GUI.apply(GUI.LabelFace, mLabels[i]);
           GUI.apply(GUI.ValueFace, mValues[i]);
           mLabels[i].setOpaque(false);
           mValues[i].setOpaque(false);
           mLabels[i].setVisible(false);
           mValues[i].setVisible(false);

           mValues[i].addMouseListener(CommonURLListener);
           
       }
       
       return true;
       
   }

   private class URLMouseListener extends tufts.vue.MouseAdapter {
           public void mouseClicked(java.awt.event.MouseEvent e) {
               if (e.getClickCount() != 2)
                   return;
               try {
                   final JTextArea value = (JTextArea) e.getSource();
                   String text = value.getText();
                   if (text.startsWith("http://")) {
                       e.consume();
                       final Color c = value.getForeground();
                       value.setForeground(Color.red);
                       value.paintImmediately(0,0, value.getWidth(), value.getHeight());
                       tufts.vue.VueUtil.openURL(text);
                       GUI.invokeAfterAWT(new Runnable() {
                               public void run() {
                                   value.setForeground(c);
                               }
                           });
                   }
               } catch (Throwable t) {
                   Log.error(t);
               }
           }
   }

   private MouseListener CommonURLListener = new URLMouseListener();

   private void loadRow(int row, String labelText, String valueText) {
       if (DEBUG.RESOURCE && DEBUG.META) out("adding row " + row + " " + labelText + "=[" + valueText + "]");

       JLabel label = mLabels[row];
       JTextArea value = mValues[row];       

       label.setText(labelText + ":");
       value.setText(valueText);

       if (valueText != null && valueText.startsWith("http://")) {
           value.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
           //value.setForeground(Color.blue);
       } else {
           //label.removeMouseListener(CommonURLListener);
           value.setCursor(Cursor.getDefaultCursor());
           //GUI.apply(GUI.ValueFace, mValues[i]);
       }
       
       // if value has at least one space, use word wrap
       if (valueText.indexOf(' ') >= 0)
           value.setWrapStyleWord(true);
       else
           value.setWrapStyleWord(false);
       
       label.setVisible(true);
       value.setVisible(true);
       
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
       // TODO: loops if we don't do this first: not safe!  we should be loading
       // directly from the props themselves, and by synchronized on them...  tho is
       // nice that only a single sorted list exists for each resource, tho of course,
       // then we have tons of cached sorted lists laying about.

       if (DEBUG.RESOURCE) out("loadProperties: " + rsrcProps);
       
       if (DEBUG.SCROLL)
           Log.debug("scroll model listeners: "
                         + Arrays.asList(((DefaultBoundedRangeModel)
                                          mScrollPane.getVerticalScrollBar().getModel())
                                         .getListeners(ChangeListener.class)));

	if (scroll)
       mScrollPane.getVerticalScrollBar().setValueIsAdjusting(true);

       mGridBag.setPaintDisabled(true);

       try {
           // Description of a dead-lock that has been fixed by having
           // PropertyMap.getTableModel() sync on it's own lock:
           
           // Example: VUE-ImageLoader49 holds changes on the props, then goes to notify
           // us here in the MetaData pane.  But The AWT thread had already put is in
           // here, right below, trying to call getTableModel(), but before we can call
           // it, the above notification needs to be released.  If the props had CHANGED
           // to entirely different set, from another resource, this wouldn't have been
           // a problem, because the update would have been skipped above in
           // propertyMapChanged.

           // Put another way: the PropertyMap is trying to notify us, but is waiting
           // for us to break out of this method for the lock to release, so
           // propertyMapChanged can be called, but then we call getTableModel(), which
           // is locked on that same PropertyMap that is waiting for us, and thus
           // deadlock...

           if (DEBUG.RESOURCE || DEBUG.THREAD) out("loadProperties: getTableModel() on " + tufts.Util.tag(rsrcProps));
           
           TableModel model = rsrcProps.getTableModel();

           if (DEBUG.RESOURCE) out("metaDataPane; loadProperties: model=" + model
                                   + " modelSlots=" + model.getRowCount()
                                   + " slotsAvail=" + mLabels.length
                                   );
           
           if (mRsrcProps != rsrcProps) {
               if (mRsrcProps != null)
                   mRsrcProps.removeListener(this);
               mRsrcProps = rsrcProps;
               mRsrcProps.addListener(this);
           }
           
           if (ensureSlots(model.getRowCount())) {
               mGridBag.removeAll();
               addLabelTextRows(0, mLabels, mValues, mGridBag, null, null);
           }
           
           loadAllRows(model);
           
           GUI.invokeAfterAWT(this);

       } catch (Throwable t) {
           mGridBag.setPaintDisabled(false);
           tufts.Util.printStackTrace(t);
       }
           
       
       /*
       // none of these sync's seem to making any difference
       synchronized (mScrollPane.getTreeLock()) {
       synchronized (mScrollPane.getViewport().getTreeLock()) {
       synchronized (getTreeLock()) {
       }}}
       */
       
   }

   public synchronized void run() {
       try {
           // Always put the scroll-bar back at the top, as it defaults
           // to moving to the bottom.
           if (scroll)
           mScrollPane.getVerticalScrollBar().setValue(0);
           // Now release all scroll-bar updates.
           if (scroll)
           mScrollPane.getVerticalScrollBar().setValueIsAdjusting(false);
           // Now allow the grid to repaint.
       } finally {
           mGridBag.setPaintDisabled(false);
           mGridBag.repaint();
       }
       //loadAllRows(mRsrcProps.getTableModel());
   }

   private void loadAllRows(TableModel model) {
       int rows = model.getRowCount();
       int row;
       for (row = 0; row < rows; row++) {
           String label = model.getValueAt(row, 0).toString();
           String value = "" + model.getValueAt(row, 1);
           
           // loadRow(row++, label, value); // debug non-HTML display
           
           // FYI, some kind of HTML bug for text strings with leading slashes
           // -- they show up empty.  Right now, we're disable HTML for
           // all synthetic keys, which covers URL.path, which was the problem.
           //if (label.indexOf(".") < 0) 
           //value = "<html>"+value;

           char c = 0;
           try {
               c = label.charAt(0);
           } catch (Throwable t) {}

           if (!DEBUG.Enabled && (c == '@' || c == '~')) {
               mLabels[row].setVisible(false);
               mValues[row].setVisible(false);
               continue;
           }
           
           loadRow(row, label, value);
           
           
       }
       for (; row < mLabels.length; row++) {
           //out(" clear row " + row);
           mLabels[row].setVisible(false);
           mValues[row].setVisible(false);
       }
       //mScrollPane.getViewport().setViewPosition(new Point(0,0));
   }

   private void out(Object o) {
        Log.debug("MetaDataPane: " + (o==null?"null":o.toString()));
   }
   
   //----------------------------------------------------------------------------------------
   // Utility methods
   //----------------------------------------------------------------------------------------
   
   private void addLabelTextPairs(Object[] labelTextPairs, Container gridBag) {
       JLabel[] labels = new JLabel[labelTextPairs.length / 2];
       JComponent[] values = new JComponent[labels.length];
       for (int i = 0, x = 0; x < labels.length; i += 2, x++) {
           //out("ALTP[" + x + "] label=" + labelTextPairs[i] + " value=" + GUI.name(labelTextPairs[i+1]));
           String labelText = (String) labelTextPairs[i];
           labels[x] = new JLabel(labelText + ":");
           values[x] = (JComponent) labelTextPairs[i+1];
       }
       addLabelTextRows(0, labels, values, gridBag, GUI.LabelFace, GUI.ValueFace);
   }

   private final int topPad = 2;
   private final int botPad = 2;
   private final Insets labelInsets = new Insets(topPad, 0, botPad, GUI.LabelGapRight);
   private final Insets fieldInsets = new Insets(topPad, 0, botPad, GUI.FieldGapRight);
   
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
       JComponent defaultExpander = new JPanel();
       defaultExpander.setPreferredSize(new Dimension(Short.MAX_VALUE, 1));
       if (DEBUG.BOXES) {
           defaultExpander.setOpaque(true);
           defaultExpander.setBackground(Color.red);
       } else
           defaultExpander.setOpaque(false);
       gridBag.add(defaultExpander, c);
   }
   
}

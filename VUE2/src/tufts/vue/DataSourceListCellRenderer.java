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

import tufts.vue.gui.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class DataSourceListCellRenderer extends DefaultListCellRenderer //implements ActionListener
{
    public static final int FirstColumn = 28;
        
    private final Icon myComputerIcon = VueResources.getImageIcon("dataSourceMyComputer");
    private final Icon savedResourcesIcon = VueResources.getImageIcon("dataSourceSavedResources");
    private final Icon remoteIcon = VueResources.getImageIcon("dataSourceRemote");
    private final PolygonIcon breakIcon = new PolygonIcon(Color.LIGHT_GRAY);
    private edu.tufts.vue.dsm.DataSource infoDataSource;
    private final static String OFFLINE = "offline";
    private final static String ONLINE = "";
    private JLabel offlineLabel = new JLabel(OFFLINE, JLabel.RIGHT);
    private JLabel onlineLabel = new JLabel(ONLINE, JLabel.RIGHT);

    private JPanel mRow = new JPanel();
    private JLabel mLabel = new DefaultListCellRenderer();
    private JLabel mIconLabel = new DefaultListCellRenderer();
    private CheckBoxRenderer mCheckBox = new CheckBoxRenderer();

    
    private Border DividerBorder = new DashBorder(Color.LIGHT_GRAY,false,true,false,false);
    
    private Border EmptyDividerBorder = new EmptyBorder(1,0,0,0);

    private Color AlternateRowColor = VueResources.getColor("gui.dataSourceList.alternateRowColor", 237,243,253);

    public DataSourceListCellRenderer()
    {
        mRow.setLayout(new BoxLayout(mRow, BoxLayout.X_AXIS));
        mRow.setOpaque(true);
        
        mLabel.setMinimumSize(new Dimension(10, mLabel.getHeight()));
        mLabel.setPreferredSize(new Dimension(Short.MAX_VALUE, mLabel.getHeight()));

        mRow.add(Box.createHorizontalStrut(GUI.WidgetInsets.left));
        mRow.add(mCheckBox);
        mRow.add(Box.createHorizontalStrut(GUI.WidgetInsets.left));
        mRow.add(mLabel);
        mRow.add(mIconLabel);
        mRow.add(Box.createHorizontalStrut(GUI.WidgetInsets.right));
    }
    

    public Component getListCellRendererComponent(JList list,
                                                  Object value,
                                                  int index,
                                                  boolean selected,
                                                  boolean cellHasFocus)
    {
        //-------------------------------------------------------
        // Set the background colors for selection
        //-------------------------------------------------------
        Color bg;
        if (selected) {
            bg = GUI.getTextHighlightColor();
        } else {
            if (index % 2 == 0)
                bg = list.getBackground();
            else
                bg = AlternateRowColor;
        }
        mRow.setBackground(bg);
        mCheckBox.setBackground(bg);        
        mLabel.setBackground(bg);
        mIconLabel.setBackground(bg);
        //-------------------------------------------------------
        // Set the checkbox, label & icon
        //-------------------------------------------------------

        mCheckBox.invisible = true;
        
        String displayName = null;
        if (value instanceof edu.tufts.vue.dsm.DataSource) {
            edu.tufts.vue.dsm.DataSource datasource = (edu.tufts.vue.dsm.DataSource)value;
            displayName = datasource.getRepositoryDisplayName();
            mCheckBox.invisible = false;
            mCheckBox.setSelected(datasource.isIncludedInSearch());
			
			// TODO: cache or maybe return a path in place of an image for getIcon16x16
			if (datasource.getIcon16x16() != null) {
				Icon dsIcon = new javax.swing.ImageIcon(datasource.getIcon16x16());
				mIconLabel.setIcon(dsIcon);
			} else {
				mIconLabel.setIcon(remoteIcon);
			}
			
            /*if (datasource.isOnline())
                namePanel.add(onlineLabel, BorderLayout.EAST);
            } else {
                namePanel.add(offlineLabel, BorderLayout.EAST);
            }*/
            mRow.setBorder(EmptyDividerBorder);
        } else if (value instanceof LocalFileDataSource){
            mRow.setBorder(DividerBorder);
            displayName = ((DataSource)value).getDisplayName();            
            mIconLabel.setIcon(myComputerIcon);
        } else  if (value instanceof FavoritesDataSource) {
            mRow.setBorder(DividerBorder);
            displayName = ((DataSource)value).getDisplayName();            
            //displayName = "My Saved Content";
            mIconLabel.setIcon(savedResourcesIcon);
        } else  if (value instanceof RemoteFileDataSource) {
            mRow.setBorder(DividerBorder);
            displayName = ((DataSource)value).getDisplayName();            
            mIconLabel.setIcon(remoteIcon);
        } else if ( value instanceof edu.tufts.vue.rss.RSSDataSource) { 
            mRow.setBorder(DividerBorder);
            displayName = ((DataSource)value).getDisplayName();            
            mIconLabel.setIcon(remoteIcon);
        }else
            mRow.setBorder(DividerBorder);
            
        if (value == DataSourceList.IndicatedDragOverValue)
            mLabel.setForeground(Color.red);
        else if (selected)
        	mLabel.setForeground(Color.black);
            //mLabel.setForeground(SystemColor.textHighlightText);
        else
            mLabel.setForeground(Color.black);
        
        mLabel.setText(displayName);

        return mRow;

        
    }

    public void actionPerformed(ActionEvent e) {
        GetLibraryInfoDialog getLibraryInfoDialog = new GetLibraryInfoDialog(infoDataSource);
        getLibraryInfoDialog.setVisible(true);

        System.out.println("DSLCR: " + e);
    }


    private static class CheckBoxRenderer extends JCheckBox {

        boolean invisible;

        public CheckBoxRenderer() {
            setBorderPainted(false);            
        }

        public void paint(Graphics g) {
            if (!invisible)
                super.paint(g);
        }
        
        
        public boolean isOpaque() { return false; }
        public void validate() {}
        public void invalidate() {}
        public void repaint() {}
        public void revalidate() {}
        public void repaint(long tm, int x, int y, int width, int height) {}
        public void repaint(Rectangle r) {}
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
        public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
        public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
        public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
        public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
        public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
        public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
    }


        /*
        breakIcon.setIconWidth(1600);
        breakIcon.setIconHeight(1);
        
        panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		
        checkBoxPanel.setOpaque(false);
        checkBoxPanel.setLayout(new BorderLayout());
        checkBoxPanel.setSize(new Dimension(4,4));
		
        BorderLayout nameLayout = new BorderLayout();
        nameLayout.setHgap(10);
        namePanel.setLayout(nameLayout);
        namePanel.add(checkBoxPanel);
        namePanel.add(displayNameLabel, BorderLayout.WEST);
        
        iconPanel.setLayout(new BorderLayout());
		
        checkBoxPanel.add(checkBox, BorderLayout.WEST);
        checkBox.setOpaque(false);

        panel.add(checkBoxPanel);
        panel.add(namePanel);
        panel.add(iconPanel);
        */

        /*
        if (value instanceof String) {                    
            super.getListCellRendererComponent(list,"",index,iss,chf);
        } else if (value instanceof tufts.vue.DataSource) {
            super.getListCellRendererComponent(list,((DataSource)value).getDisplayName(), index, iss, chf);					
        } else if (value instanceof edu.tufts.vue.dsm.DataSource) {
            super.getListCellRendererComponent(list,((edu.tufts.vue.dsm.DataSource)value).getRepositoryDisplayName(), index, iss, chf);					
        }

		
        if (value instanceof String) {
            //setText("[" + value + "]");
            return this;
            /*
            JPanel linePanel = new JPanel() {
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2d = (Graphics2D)g;
                        g2d.setColor(Color.LIGHT_GRAY);
                        float dash1[] = {3.0f};
                        BasicStroke dashed = new BasicStroke(1.0f, 
                                                             BasicStroke.CAP_BUTT, 
                                                             BasicStroke.JOIN_MITER, 
                                                             10.0f, dash1, 0.0f);
                        g2d.setStroke(dashed);
                        int width = getWidth();
                        g2d.drawLine(0, 3, width-10, 3);
                    }
                };
            return linePanel;

        }
        
    
        */


        

        /*
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		
        JPanel checkBoxPanel = new JPanel();			
        checkBoxPanel.setOpaque(false);
        checkBoxPanel.setLayout(new BorderLayout());
        checkBoxPanel.setSize(new Dimension(4,4));
		
        JPanel namePanel = new JPanel();
        BorderLayout nameLayout = new BorderLayout();
        namePanel.setLayout(nameLayout);
		
        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new BorderLayout());
		
        JCheckBox checkBox = new JCheckBox();
        checkBoxPanel.add(checkBox, BorderLayout.WEST);
        checkBox.setOpaque(false);
        */
    

        /*
        if (index == list.getSelectedIndex()) {
            checkBoxPanel.setBackground(SystemColor.textHighlight);
            namePanel.setBackground(SystemColor.textHighlight);
            iconPanel.setBackground(SystemColor.textHighlight);
            panel.setBackground(SystemColor.textHighlight);
        } else {
            Color bg = list.getBackground();
            checkBoxPanel.setBackground(bg);
            namePanel.setBackground(bg);
            iconPanel.setBackground(bg);
            panel.setBackground(bg);
        }
        */

        // adjust the spacing within the name panel (name and status) so labels are flush with edges
        /*
        JLabel displayNameLabel = new JLabel(displayName);
        double displayNameWidth;
        while ((displayNameWidth = displayNameLabel.getPreferredSize().getWidth()) > 155) {
            displayName = displayName.substring(0,displayName.length()-1) + "...";
            displayNameLabel.setText(displayName);
        }
        while ((displayNameWidth = displayNameLabel.getPreferredSize().getWidth()) < 155) {
            displayName = displayName + " ";
            displayNameLabel.setText(displayName);
        }

        namePanel.add(checkBoxPanel);
        namePanel.add(displayNameLabel, BorderLayout.WEST);
		
        panel.add(checkBoxPanel);
        panel.add(namePanel);
        panel.add(iconPanel);

        return panel;
        
        */
    
}

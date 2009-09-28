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
    private final Icon rssIcon = VueResources.getImageIcon("dataSourceRSS");
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
    
    private final Border EmptyDividerBorder = new EmptyBorder(1,0,0,0);

    private final Color AlternateRowColor = VueResources.getColor("gui.dataSourceList.alternateRowColor", 237,243,253);
    private final Color IndicationColor = new Color(144,255,144);


    public DataSourceListCellRenderer()
    {
        mRow.setLayout(new BoxLayout(mRow, BoxLayout.X_AXIS));
        mRow.setOpaque(true);
        
        mLabel.setMinimumSize(new Dimension(10, mLabel.getHeight()));
        mLabel.setPreferredSize(new Dimension(Short.MAX_VALUE, mLabel.getHeight()));
        mLabel.setOpaque(false);

        mIconLabel.setOpaque(false);

        mRow.add(Box.createHorizontalStrut(GUI.WidgetInsets.left));
        mRow.add(mCheckBox);
        mRow.add(Box.createHorizontalStrut(GUI.WidgetInsets.left));
        mRow.add(mLabel);
        mRow.add(Box.createHorizontalStrut(GUI.WidgetInsets.right));
        mRow.add(mIconLabel);
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

        //-------------------------------------------------------
        // Set the checkbox, label & icon
        //-------------------------------------------------------

        boolean isLoading = false;

        if (value instanceof edu.tufts.vue.dsm.DataSource) {
            //edu.tufts.vue.dsm.DataSource datasource = (edu.tufts.vue.dsm.DataSource)value;
            edu.tufts.vue.dsm.impl.VueDataSource ds = (edu.tufts.vue.dsm.impl.VueDataSource) value;
            mLabel.setText(ds.getRepositoryDisplayName());

            if (ds.isOnline()) {
                mLabel.setForeground(Color.black);
                mCheckBox.setEnabled(true);
            } else {
                isLoading = true;
                mLabel.setForeground(Color.gray);
                mCheckBox.setEnabled(false);
            }
            
            mCheckBox.setVisibility(true);
            mCheckBox.setSelected(ds.isIncludedInSearch());
			
            // TODO: cache or maybe return a path in place of an image for getIcon16x16
            if (ds.getIcon16x16() != null) {
                Icon dsIcon = new javax.swing.ImageIcon(ds.getIcon16x16());
                mIconLabel.setIcon(dsIcon);
            } else {
                mIconLabel.setIcon(remoteIcon);
            }
			
//             if (datasource.isOnline())
//                 namePanel.add(onlineLabel, BorderLayout.EAST);
//             } else {
//                 namePanel.add(offlineLabel, BorderLayout.EAST);
//             }
        
            mRow.setBorder(EmptyDividerBorder);
        }
        else if (value instanceof tufts.vue.BrowseDataSource) {

            final tufts.vue.BrowseDataSource ds = (tufts.vue.BrowseDataSource) value;

            mRow.setBorder(DividerBorder);
            
            if (ds.isAvailable()) {
                mLabel.setForeground(Color.black);
            } else {
                isLoading = true;
                if (DEBUG.Enabled)
                    mLabel.setForeground(Color.gray);
                else
                    mLabel.setForeground(Color.black);
            }
            
            mLabel.setText(ds.getDisplayName());

            mCheckBox.setVisibility(false);
        
            if (value instanceof LocalFileDataSource)
                mIconLabel.setIcon(myComputerIcon);
            else if (value instanceof FavoritesDataSource)
                mIconLabel.setIcon(savedResourcesIcon);
            else if (value instanceof RemoteFileDataSource)
                mIconLabel.setIcon(remoteIcon);
            else if (value instanceof edu.tufts.vue.rss.RSSDataSource)
                mIconLabel.setIcon(rssIcon);
            else
                mIconLabel.setIcon(null);
        }
        else {
            System.out.println("DataSourceList: unhandled data source: " + tufts.Util.tags(value));
            mRow.setBorder(DividerBorder);
            mCheckBox.setVisibility(false);
            mIconLabel.setIcon(null);
            mLabel.setText(value.toString());
        }
            
        if (!isLoading && value == DataSourceList.IndicatedDragOverValue)
            mRow.setBackground(IndicationColor);
        
        return mRow;

        
    }

    public void actionPerformed(ActionEvent e) {
        GetLibraryInfoDialog getLibraryInfoDialog = new GetLibraryInfoDialog(infoDataSource);
        getLibraryInfoDialog.setVisible(true);

        System.out.println("DSLCR: " + e);
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

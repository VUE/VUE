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
import javax.swing.*;
import java.awt.event.*;

public class DataSourceListCellRenderer extends DefaultListCellRenderer implements ActionListener
{
    private final Icon myComputerIcon = VueResources.getImageIcon("dataSourceMyComputer");
    private final Icon savedResourcesIcon = VueResources.getImageIcon("dataSourceSavedResources");
    private final Icon remoteIcon = VueResources.getImageIcon("dataSourceRemote");
    private final PolygonIcon breakIcon = new PolygonIcon(Color.LIGHT_GRAY);
    private edu.tufts.vue.dsm.DataSource infoDataSource;
    private final static String OFFLINE = "offline";
    private final static String ONLINE = "";
    private JLabel offlineLabel = new JLabel(OFFLINE, JLabel.RIGHT);
    private JLabel onlineLabel = new JLabel(ONLINE, JLabel.RIGHT);


    // TODO: if this component stays as a JList (as opposed to a JTable),
    // all the object construction should happen in the constructor, as
    // opposed to repeated every time a line is rendered (painted).
    // The idea with renderer's is that just the value-sets
    // happen at render time.
    
    /*
    private JPanel panel = new JPanel();
    private JPanel checkBoxPanel = new JPanel();			
    private JPanel namePanel = new JPanel();
    private JPanel iconPanel = new JPanel();
    private JCheckBox checkBox = new JCheckBox();
    private JLabel displayNameLabel = new JLabel();

    public DataSourceListCellRenderer()
    {
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
    }
    */
    

    public Component getListCellRendererComponent(JList list,Object value, int index, boolean iss,boolean chf)
    {
        breakIcon.setIconWidth(1600);
        breakIcon.setIconHeight(1);
		
        if (value instanceof String) {                    
            super.getListCellRendererComponent(list,"",index,iss,chf);
        } else if (value instanceof tufts.vue.DataSource) {
            super.getListCellRendererComponent(list,((DataSource)value).getDisplayName(), index, iss, chf);					
        } else if (value instanceof edu.tufts.vue.dsm.DataSource) {
            super.getListCellRendererComponent(list,((edu.tufts.vue.dsm.DataSource)value).getRepositoryDisplayName(), index, iss, chf);					
        }
		
        if (value instanceof String) {
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

        String displayName = null;
        if (value instanceof edu.tufts.vue.dsm.DataSource) {
            edu.tufts.vue.dsm.DataSource datasource = (edu.tufts.vue.dsm.DataSource)value;
            displayName = datasource.getRepositoryDisplayName();
            checkBox.setEnabled(true);
            checkBox.setSelected(datasource.isIncludedInSearch());
            iconPanel.add(new JLabel(remoteIcon), BorderLayout.EAST);
            if (datasource.isOnline()) {
                namePanel.add(onlineLabel, BorderLayout.EAST);
            } else {
                namePanel.add(offlineLabel, BorderLayout.EAST);
            }
        } else if (value instanceof LocalFileDataSource){
            displayName = ((DataSource)value).getDisplayName();
            checkBox.setEnabled(false);
            checkBox.setVisible(false);
            iconPanel.add(new JLabel(savedResourcesIcon), BorderLayout.EAST);
        } else  if (value instanceof FavoritesDataSource) {
            displayName = "My Saved Content";
            checkBox.setEnabled(true);
            iconPanel.add(new JLabel(myComputerIcon), BorderLayout.EAST);
        }
				
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

        // adjust the spacing within the name panel (name and status) so labels are flush with edges
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
    }

    public void actionPerformed(ActionEvent e) {
        GetLibraryInfoDialog getLibraryInfoDialog = new GetLibraryInfoDialog(infoDataSource);
        getLibraryInfoDialog.setVisible(true);
    }	
}

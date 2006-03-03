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
		
		if (value instanceof FavoritesDataSource){
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			JCheckBox checkBox = new JCheckBox();
			checkBox.setBackground(VueResources.getColor("FFFFFF"));
			if (index == list.getSelectedIndex()) {
				panel.setBackground(SystemColor.textHighlight);
			} else {
				panel.setBackground(VueResources.getColor("FFFFFF"));								
			}
			panel.add(checkBox);
			panel.add(new JLabel(savedResourcesIcon));
			checkBox.setEnabled(false);
			panel.add(new JLabel(((DataSource)value).getDisplayName()));
			return panel;
		} else if (value instanceof LocalFileDataSource){
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			JCheckBox checkBox = new JCheckBox();
			checkBox.setBackground(VueResources.getColor("FFFFFF"));
			if (index == list.getSelectedIndex()) {
				panel.setBackground(SystemColor.textHighlight);
			} else {
				panel.setBackground(VueResources.getColor("FFFFFF"));								
			}
			panel.add(new JLabel(myComputerIcon));
			checkBox.setEnabled(false);
			panel.add(new JLabel(((DataSource)value).getDisplayName()));
			return panel;
		} else  if (value instanceof String) {
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
		else {
			JPanel panel = new JPanel();
			JPanel westPanel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			JCheckBox checkBox = new JCheckBox();
			checkBox.setBackground(VueResources.getColor("FFFFFF"));
			if (index == list.getSelectedIndex()) {
				panel.setBackground(SystemColor.textHighlight);
			} else {
				panel.setBackground(VueResources.getColor("FFFFFF"));								
			}
			
			try {
				checkBox.setSelected( ((edu.tufts.vue.dsm.DataSource)value).isIncludedInSearch() );
				panel.add(checkBox);
				panel.add(new JLabel(remoteIcon));
				checkBox.setEnabled(true);
				infoDataSource = (edu.tufts.vue.dsm.DataSource)value;
				panel.add(new JLabel(infoDataSource.getRepository().getDisplayName()));
			} catch (Throwable t) {
				t.printStackTrace();
			}
			return panel;
		}
	}

	public void actionPerformed(ActionEvent e) {
		GetLibraryInfoDialog getLibraryInfoDialog = new GetLibraryInfoDialog(infoDataSource);
		getLibraryInfoDialog.setVisible(true);
	}	
}

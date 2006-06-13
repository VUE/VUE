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

public class ProviderListCellRenderer extends DefaultListCellRenderer
{
	private edu.tufts.vue.dsm.OsidFactory factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
    private final Icon myComputerIcon = VueResources.getImageIcon("dataSourceMyComputer");
    private final Icon savedResourcesIcon = VueResources.getImageIcon("dataSourceSavedResources");
    private final Icon remoteIcon = VueResources.getImageIcon("dataSourceRemote");
	private static String MY_COMPUTER = "My Computer";
	private static String MY_SAVED_CONTENT = "My Saved Content";
	private static String FTP = "FTP";
	
    private JPanel mRow = new JPanel();
    private JLabel mLabel = new DefaultListCellRenderer();
    private JLabel mIconLabel = new DefaultListCellRenderer();
    private CheckBoxRenderer mCheckBox = new CheckBoxRenderer();

    private Border DividerBorder = new MatteBorder(1,0,0,0, Color.gray);
    private Border EmptyDividerBorder = new EmptyBorder(1,0,0,0);

    private Color AlternateRowColor = VueResources.getColor("gui.dataSourceList.alternateRowColor", 237,243,253);

    public ProviderListCellRenderer()
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

        //mCheckBox.invisible = true;
        
        String displayName = null;
        if (value instanceof org.osid.provider.Provider) {
            org.osid.provider.Provider provider = (org.osid.provider.Provider)value;
            displayName = provider.getDisplayName();
            //mCheckBox.invisible = false;
            //mCheckBox.setSelected(datasource.isIncludedInSearch());
			
			// TODO: cache or maybe return a path in place of an image for getIcon16x16
			try {
				org.osid.shared.PropertiesIterator propertiesIterator = provider.getProperties();
				while (propertiesIterator.hasNextProperties()) {
					org.osid.shared.Properties props = propertiesIterator.nextProperties();
					org.osid.shared.ObjectIterator objectIterator = props.getKeys();
					while (objectIterator.hasNextObject()) {
						String key = (String)objectIterator.nextObject();
						try {
							if (key.equals("icon16x16")) {
								String path = factory.getResourcePath((String)props.getProperty(key));
								mIconLabel.setIcon(new javax.swing.ImageIcon(path));
								break;
							}
						} catch (Throwable t) {
							mIconLabel.setIcon(remoteIcon);
							break;
						}
					}
				}
			} catch (Throwable t) {
				
			}
			
            /*if (datasource.isOnline())
                namePanel.add(onlineLabel, BorderLayout.EAST);
            } else {
                namePanel.add(offlineLabel, BorderLayout.EAST);
            }*/
            mRow.setBorder(EmptyDividerBorder);
        } else if (value instanceof String) {
			String s = (String)value;
			if (s.equals(MY_COMPUTER)) {
				mRow.setBorder(DividerBorder);
				displayName = MY_COMPUTER;            
				mIconLabel.setIcon(myComputerIcon);
			} else if (s.equals(MY_SAVED_CONTENT)) {
				mRow.setBorder(DividerBorder);
				displayName = MY_SAVED_CONTENT;            
				mIconLabel.setIcon(savedResourcesIcon);
			} else if (s.equals(FTP)) {
				mRow.setBorder(DividerBorder);
				displayName = FTP;            
				mIconLabel.setIcon(remoteIcon);
			}
        } else {
            mRow.setBorder(DividerBorder);
		}

		/* if (value == ProviderList.IndicatedDragOverValue)
            mLabel.setForeground(Color.red);
        else */ if (selected)
            mLabel.setForeground(SystemColor.textHighlightText);
        else
            mLabel.setForeground(Color.black);
        
        mLabel.setText(displayName);
		System.out.println("rendering cell with display name: " + displayName);

        return mRow;
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
}

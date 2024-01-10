
/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

package edu.tufts.vue.compare.ui;

/*
 * ColorsComboBoxRenderer.java
 *
 * Created on February 22, 2007, 3:08 PM
 *
 * @author dhelle01
 */

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class ColorsComboBoxRenderer extends javax.swing.DefaultListCellRenderer {
    
    public ColorsComboBoxRenderer() {
    }
    
    public Component getListCellRendererComponent(javax.swing.JList list,Object value,int index,boolean isSelected,boolean hasFocus)
    {
      JPanel panel = new JPanel();  
      List<Color> colors = new ArrayList<Color>();
      
      //layout horizontally in equally sized units
      BoxLayout layout = new BoxLayout(panel,BoxLayout.X_AXIS);
      panel.setLayout(layout);
      
      //if have Colors as value, load list with Colors
      if(value instanceof Colors)
      {
          colors = ((Colors)value).getColors();
      }
      
      //iterate over Colors, adding opaque JPanel with proper Color for each Color present in List
      Iterator<Color> i = colors.iterator();
      while(i.hasNext())
      {
          JPanel cp = new JPanel()//;
          {
              public java.awt.Dimension getPreferredSize()
              {
                  return new java.awt.Dimension(10,20);
              }
          };
          //make square based on height determined by layout manager? for now use hard coded values from above...
          cp.setSize(cp.getHeight(),cp.getHeight());
          //cp.setSize(30,30);
          cp.setOpaque(true);
          cp.setBackground(i.next());
          panel.setBorder(javax.swing.BorderFactory.createLineBorder(Color.GRAY));
          panel.add(cp);
      }
      
      return panel;  
    }
    
}

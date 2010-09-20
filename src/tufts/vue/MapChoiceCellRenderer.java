
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

/*
 * MapChoiceCellRenderer.java
 *
 * Created on January 29, 2007, 9:51 AM
 *
 * @author dhelle01
 */

package tufts.vue;

import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

public class MapChoiceCellRenderer extends DefaultListCellRenderer {
    
    public MapChoiceCellRenderer() {
    }
    
    public Component getListCellRendererComponent(JList list,Object value,int index,boolean isSelected,boolean cellHasFocus)
    {
        String choiceText = value.toString();        
        if(value instanceof LWMap)
        {
          LWMap map = (LWMap)value;
          choiceText = map.getLabel();
        }
        JLabel label = new JLabel();
        label.setOpaque(true);
        if(isSelected)
        {
            label.setBackground(list.getSelectionBackground());
            label.setForeground(list.getSelectionForeground());
        }
        else
        {
            label.setBackground(list.getBackground());
            label.setForeground(list.getForeground());   
        }
        label.setText(choiceText);
        label.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        return label;
    }
    
}

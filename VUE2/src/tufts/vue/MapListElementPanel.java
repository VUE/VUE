
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

/*
 * MapListElementPanel.java
 *
 * Created on January 22, 2007, 3:45 PM
 *
 * @author dhelle01
 */

package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MapListElementPanel extends JPanel implements ActionListener {
    
    private JCheckBox check;
    private JButton remove;
    private LWMap map;
    
    public final static Color lightBlue = new Color(220,220,255);
    public final static Color lightGray = new Color(220,220,220);
    
    public final static int MAP = 0;
    public final static int FILE = 1;
    
    private int type = 0;
    
    public MapListElementPanel(LWMap map) {
        
        type = MAP;
        setMap(map);
        String text = getMap().getLabel();
        check = new JCheckBox();
        JLabel fileName = new JLabel(text);
        remove = new tufts.vue.gui.VueButton("delete");
        
        check.setSelected(true);
        
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridBag);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        gridBag.setConstraints(check,c);
        add(check);
        c.weightx = 1.0;
        gridBag.setConstraints(fileName,c);
        add(fileName);
        c.weightx = 0.0;
        gridBag.setConstraints(remove,c);
        add(remove);
        
        check.addActionListener(this);
        remove.addActionListener(this);
    }
    
    public int getPlace()
    {
        Component[] comps = getParent().getComponents();
        for(int i=0;i<comps.length;i++)
        {
            if(comps[i]==this)
                return i;
        }
        return 0;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == check)
        {
          adjustColor(getPlace());
        }
        if(e.getSource() == remove)
        {
            MapListPanel parent = (MapListPanel)getParent();
            parent.remove(this);
            parent.adjustColors();
            parent.getParent().validate();
        }
    }
    
    public void adjustColor(int index)
    {
       if(!check.isSelected())
       {
        // disable until also setting VueButton correctly
        // as well resolving issue of interior of checkbox
        // setBackground(lightGray);
        // check.setBackground(lightGray);
       }
       else
       {
          if(index%2==0)
          {
             setBackground(lightBlue);
             check.setBackground(lightBlue);
          }
          else
          {
             setBackground(Color.WHITE);
             check.setBackground(Color.WHITE);
          }
       }
    }
    
    public LWMap getMap()
    {
        return map;
    }
    
    public void setMap(LWMap map)
    {
        this.map = map;
    }
    
    public boolean isActive()
    {
        return check.isSelected();        
    }
}

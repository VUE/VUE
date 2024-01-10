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
 * BaseMapChoiceSelector.java
 *
 * Created on April 3, 2007, 12:00 PM
 *
 * @author dhelle01
 */

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.swing.event.ListDataListener;
import tufts.vue.LWMap;

public class BaseMapChoiceSelector extends javax.swing.JComboBox {

    // might make sense to read these from VueProperties in future?
    public static final String SELECT_STRING = "Select";
    public static final String OTHER_STRING = "Other";
    
    private LWMap userSelectedMap;
    
    public BaseMapChoiceSelector() 
    {
        setModel(new BaseMapChoiceModel());
        
        setRenderer(new BorderSeparatorRenderer());        
    }
    
    public boolean isBaseMapSelected()
    {
        return ((BaseMapChoiceModel)getModel()).isBaseMapSelected();
    }
    
    public LWMap getSelectedMap()
    {
        return ((BaseMapChoiceModel)getModel()).getSelectedMap();
    }
    
    public void setUserSelectedMap(LWMap map)
    {
        userSelectedMap = map;
    }
    
    class BaseMapChoiceModel implements javax.swing.ComboBoxModel
    {
        
        //private int selectedIndex = 0;
        private Object selectedItem = SELECT_STRING;
        
        private java.util.List<ListDataListener> listeners = new java.util.ArrayList<ListDataListener>();
        
        public Object getSelectedItem()
        {
            return selectedItem;     
        }
        
        public void setSelectedItem(Object selected)
        {
            selectedItem = selected;
        }
        
        public Object getElementAt(int index)
        {
            if(index == 0)
                return SELECT_STRING;
            if(index == (getSize() - 1) )
                return OTHER_STRING;
            else 
                return getMaps().get(index-1).getLabel();
        }
        
        public void addListDataListener(ListDataListener listener)
        {
            listeners.add(listener);
        }
        
        public void removeListDataListener(ListDataListener listener)
        {
            listeners.remove(listener);
        }
        
        public int getSize()
        {
            return getMaps().size() + 2;
        }
        
        private int indexOf(Object obj)
        {
            return getMaps().indexOf(obj) + 1;
        }
        
        private java.util.List<LWMap> getMaps()
        {
            java.util.Iterator<LWMap> allMaps = tufts.vue.VUE.getLeftTabbedPane().getAllMaps();
            java.util.ArrayList<LWMap> nonMergeMaps = new java.util.ArrayList<LWMap>();
            while(allMaps.hasNext())
            {
                LWMap obj = allMaps.next();
                if (!obj.hasClientData(tufts.vue.MergeMapFactory.class))
                   nonMergeMaps.add(obj);
            }
            return nonMergeMaps;
        }
        
        private boolean isBaseMapSelected()
        {
            if(!(getSelectedItem().equals(SELECT_STRING)))
                if(getSelectedItem().equals(OTHER_STRING) && userSelectedMap == null)
                  return false;
                else
                  return true;
            else
                return false;
        }
        
        public LWMap getSelectedMap()
        {
            if(isBaseMapSelected())
            {
                if(getSelectedItem().equals(OTHER_STRING))
                    return userSelectedMap;
                else
                {
                    return getMaps().get(getSelectedIndex()-1);
                }
            }
            else
            {
                return null;
            }
        }
    }
    
    class BorderSeparatorRenderer extends javax.swing.DefaultListCellRenderer
    {
        public java.awt.Component getListCellRendererComponent(javax.swing.JList list,Object value,int index,boolean isSelected,boolean hasFocus)
        {
            javax.swing.JLabel label = (javax.swing.JLabel)super.getListCellRendererComponent(list,value,index,isSelected,hasFocus);
            
            if(index==0)
                label.setBorder(javax.swing.BorderFactory.createMatteBorder(0,0,1,0,new java.awt.Color(140,140,140)));
            
            if(index == (getModel().getSize() - 1))
                label.setBorder(javax.swing.BorderFactory.createMatteBorder(1,0,0,0,new java.awt.Color(140,140,140)));
            
            return label;
        }
    }
      
}

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

public class IconRenderer
extends javax.swing.table.DefaultTableCellRenderer
{

    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table
                                                          , Object value
                                                          , boolean isSelected
                                                          , boolean hasFocus
                                                          , int row
                                                          , int column)
    {
        super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        if (value instanceof javax.swing.ImageIcon)
        {
            setOpaque(true);
            setIcon((javax.swing.ImageIcon)value);
            setText("");
        }
        else
        {
            setIcon(null);
        }
        return this;
    }
 }

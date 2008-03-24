
/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

package edu.tufts.vue.metadata.gui;

/**
 *
 * MetaButton.java
 * 
 * @author dhelle01
 */
public class MetaButton extends tufts.vue.gui.VueButton //implements java.awt.event.ActionListener
{
    
    private static boolean DEBUG_LOCAL = true;
    
    private int row;
    private edu.tufts.vue.metadata.ui.MetadataEditor editor;
    
    public MetaButton(edu.tufts.vue.metadata.ui.MetadataEditor editor,String type)
    {
        super("keywords.button.delete");
        //addMouseListener(new MetaButtonMouseListener());
        addActionListener(new MetaButtonActionListener());
        this.editor = editor;
        setBorderPainted(false);
        setContentAreaFilled(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder());
        setSize(new java.awt.Dimension(5,5));
    }
    
    /*public void actionPerformed(java.awt.event.ActionEvent evt)
    {
        if(DEBUG_LOCAL)
        {
            System.out.println("MetaButton: action performed source -- " + evt.getSource());
        }
    }*/
    
    public void setRow(int row)
    {
        this.row = row;
    }
    
    /*class MetaButtonMouseListener extends java.awt.event.MouseAdapter//implements java.awt.event.ActionListener
    {
        
        public void mouseReleased(java.awt.event.MouseEvent evt)
        {
           if(DEBUG_LOCAL)
           {    
             System.out.println("MetaButton - mouseReleased: " + evt.getPoint());
           }
            
           java.util.List<edu.tufts.vue.metadata.VueMetadataElement> metadataList = editor.getCurrent().getMetadataList().getMetadata();
           int selectedRow = editor.getMetadataTable().getSelectedRow();
           metadataList.remove(selectedRow);
           editor.getMetadataTable().repaint();
        }
    }*/
    
    class MetaButtonActionListener implements java.awt.event.ActionListener
    {
        
        public void actionPerformed(java.awt.event.ActionEvent evt)
        {
           /*if(DEBUG_LOCAL)
           {    
             System.out.println("MetaButton - mouseReleased: " + evt.getPoint());
           }*/
            
           java.util.List<edu.tufts.vue.metadata.VueMetadataElement> metadataList = editor.getCurrent().getMetadataList().getMetadata();
           int selectedRow = editor.getMetadataTable().getSelectedRow();
           if(selectedRow != -1 && metadataList.size() > selectedRow)
           {
             metadataList.remove(selectedRow);
             editor.getMetadataTable().repaint();
           }
        }
    }

}

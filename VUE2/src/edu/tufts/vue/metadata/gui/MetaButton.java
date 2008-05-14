
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
 * VueButton for deleting category items
 * todo: make this work for adding new items as well 
 *
 * 
 * @author dhelle01
 */
public class MetaButton extends tufts.vue.gui.VueButton //implements java.awt.event.ActionListener
{
    
    private static boolean DEBUG_LOCAL = false;
    
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
            
           boolean multipleMode = false;
           tufts.vue.LWComponent current = null;
           
           if(editor.getCurrentMultiples() != null)
           {
               current = editor.getCurrentMultiples();
               multipleMode = true;
           }
           else if(editor.getCurrent() != null)
           {
               current = editor.getCurrent();
           }
            
           java.util.List<edu.tufts.vue.metadata.VueMetadataElement> metadataList = 
                   current.getMetadataList().getMetadata();
           
           int selectedRow = editor.getMetadataTable().getSelectedRow();
           boolean isValidRow = (selectedRow != -1) && (metadataList.size() > selectedRow);
           
           edu.tufts.vue.metadata.VueMetadataElement vme = null;
           
           if(isValidRow)
           {
              vme = metadataList.get(selectedRow);
           }
           
           if(multipleMode)
           {
               tufts.vue.LWGroup multiples = (tufts.vue.LWGroup)current;
               java.util.Iterator<tufts.vue.LWComponent> components = multiples.getAllDescendents().iterator();
               while(components.hasNext())
               {
                   tufts.vue.LWComponent component = components.next();
                   java.util.List<edu.tufts.vue.metadata.VueMetadataElement> compMLList = 
                     component.getMetadataList().getMetadata();   
                   
                   if(DEBUG_LOCAL)
                   {
                       System.out.println("MetaButton --  sub component of multiples index of value: " +
                                          compMLList.indexOf(vme));
                   }
                   
                   if(compMLList.indexOf(vme) != -1)
                   {
                    compMLList.remove(compMLList.indexOf(vme));
                   }
                   
                   // really only need to do this if the component now doesn't have any user metadata
                   if(compMLList.size() == 0)
                      component.layout();
                   tufts.vue.VUE.getActiveViewer().repaint();
                  
               }
           }
           
           if(isValidRow)
           {
             metadataList.remove(selectedRow);
             editor.getMetadataTable().repaint();
           }
        }
    }

}

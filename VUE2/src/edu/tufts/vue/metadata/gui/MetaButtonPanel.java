
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

package edu.tufts.vue.metadata.gui;

import tufts.vue.DEBUG;

/**
 *
 * @author dhelle01
 * 
 */
public class MetaButtonPanel extends javax.swing.JPanel 
    implements java.awt.event.MouseListener
{
    private final String type;
    private final edu.tufts.vue.metadata.ui.MetadataEditor editor;
    private final MetaButton button;
    
    private int row;
    
    public MetaButtonPanel(edu.tufts.vue.metadata.ui.MetadataEditor editor, String type) {
        setName(type);
        this.type = type;
        this.editor = editor;
        button = new MetaButton(editor, type);
        setLayout(new java.awt.BorderLayout());
        add(button);
        addMouseListener(this); // This has to be a listener in order for the button to get events
    }
    
    public void setPanelRowForButtonClick(int row) {
        this.row = row;
        button.setRowForButtonClick(row);
    }
    
    public void XXXmousePressed(java.awt.event.MouseEvent evt)
    {
        if (DEBUG.PAIN) {
            java.awt.Point point = evt.getPoint();
            System.out.println("MetaButtonPanel: component at -- " + "(point,component) --" + "(" + point + "," + getComponentAt(point) + ")");
        }
        
        // unless over button, do save
        editor.getModel().setSaved(row, true);
        if (editor.getTable().getCellEditor() != null)
            editor.getTable().getCellEditor().stopCellEditing();
    }
    
    public void mousePressed(java.awt.event.MouseEvent evt) { }
    public void mouseReleased(java.awt.event.MouseEvent evt) { }
    public void mouseClicked(java.awt.event.MouseEvent evt) { }
    public void mouseExited(java.awt.event.MouseEvent evt) { }
    public void mouseEntered(java.awt.event.MouseEvent evt) { }
    
}

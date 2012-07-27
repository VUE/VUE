
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

import edu.tufts.vue.metadata.VueMetadataElement;

import tufts.vue.DEBUG;
import tufts.Util;
import tufts.vue.VUE;

/**
 * MetaButton.java
 *
 * VueButton for deleting category items
 * todo: make this work for adding new items as well 
 *
 * @author dhelle01
 */
public class MetaButton extends tufts.vue.gui.VueButton //implements java.awt.event.ActionListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MetaButton.class);

    private final edu.tufts.vue.metadata.ui.MetadataEditor editor;
    private int lastRequestedRow;
    
    // VueResources.getImageIcon("metadata.editor.delete.up"));
    
    public MetaButton(edu.tufts.vue.metadata.ui.MetadataEditor editor, String type)
    {
        super("keywords.button.delete");
        addMouseListener(new MetaButtonMouseListener());
        //addActionListener(new MetaButtonActionListener());
        this.editor = editor;
        setBorderPainted(false);
        setContentAreaFilled(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder());
        setSize(new java.awt.Dimension(5,5));
    }
    
    public void setRowForButtonClick(int row) {
        this.lastRequestedRow = row;
    }
    
    // class MetaButtonActionListener implements java.awt.event.ActionListener { public void actionPerformed(java.awt.event.ActionEvent evt) {
    
    // We need to see this event before the MOUSE_RELEASE, which is when actionPerformed is called,
    // in order to cancel any active edit WITHOUT automatically saving, this is the default.  Tho
    // it would be much nicer if we actually supported a pressed state and didn't do something like
    // delete until the user did a full press & release.  If we don't catch MOUSE_PRESSED, what
    // will happen is that before we're even called, as we're a new editor being activated by the
    // table, any active text edit will de-focus, and will have no idea it was because of a click
    // on the delete button, which in this case of an active edit means don't save your current
    // changes, as opposed to a full delete.

    private class MetaButtonMouseListener extends tufts.vue.MouseAdapter
    {
        public void mousePressed(java.awt.event.MouseEvent evt)
        {
            if (DEBUG.PAIN) { VUE.diagPush("MBMP"); Log.debug("mousePressed " + MetaButton.this); }

            final javax.swing.CellEditor cellEdit = editor.getTable().getCellEditor();
            if (DEBUG.PAIN) Log.debug("active cell editor: " + Util.tags(cellEdit));
            
            // // Can do this if gets in the way:
            // if (cellEdit != null && cellEdit.getClass() == edu.tufts.vue.metadata.ui.MetadataEditor.ButtonCE.class) {
            //     if (DEBUG.PAIN) Log.debug("canceling needless button editor");
            //     editor.getTable().getCellEditor().cancelCellEditing();
            // }
            
            if (editor.getActiveTextEdit() != null) {
                if (DEBUG.PAIN) Log.debug("no delete: just cancel active edit and return; " + Util.tags(editor.getActiveTextEdit()));
                editor.getActiveTextEdit().cancelCellEditing();
                if (DEBUG.PAIN) VUE.diagPop();
                return;
            }
            
            
            boolean multipleMode = false;
            tufts.vue.LWComponent current = null;
            
            if (editor.getCurrentMultiples() != null) {
                current = editor.getCurrentMultiples();
                multipleMode = true;
            }
            else if(editor.getCurrent() != null)
                current = editor.getCurrent();
            
            final java.util.List<VueMetadataElement> metadataList = current.getMetadataList().getMetadata();
          //final int selectedRow = editor.getMetadataTable().getSelectedRow();
            final int selectedRow = MetaButton.this.lastRequestedRow;
            final boolean isValidRow = (selectedRow >= 0) && (selectedRow < metadataList.size());
            
            edu.tufts.vue.metadata.VueMetadataElement vme = null;
           
            if (isValidRow)
                vme = metadataList.get(selectedRow);
           
            if (multipleMode) {
                final tufts.vue.LWGroup multiples = (tufts.vue.LWGroup) current;
                for ( tufts.vue.LWComponent component : multiples.getAllDescendents()) {
                    final java.util.List<edu.tufts.vue.metadata.VueMetadataElement> compMLList = component.getMetadataList().getMetadata();   
                    
                    if (DEBUG.PAIN) System.out.println("MetaButton --  sub component of multiples index of value: " + compMLList.indexOf(vme));
                    
                    if (compMLList.indexOf(vme) != -1)
                        compMLList.remove(compMLList.indexOf(vme));
                    
                    // really only need to do this if the component now doesn't have any user metadata
                    if(compMLList.size() == 0)
                        component.layout();
                    tufts.vue.VUE.getActiveViewer().repaint();
                }
            }
           
            // if (!tufts.vue.gui.GUI.isDoubleClick(evt)) return;

            // We could take advantge of JTable editor behaviour to make a harder to delete action:
            // On first click, let the button become the editor (and the button editor paints
            // Color.red or something instead of the current identical fashion), and on the second
            // click on an actual editor, do the full delete.
            
            if (isValidRow) {
                if (DEBUG.Enabled) Log.debug("deleting selectdRow=" + selectedRow + " " + vme);
                
                // this should be handled by adding a removeRow to the model instead of mucking the metadataList directly here:
                editor.getModel().setSaved(editor.getModel().getRowCount() - 1, false); // all so bottom border draws correctly
                metadataList.remove(selectedRow);
                
                editor.refreshAll();
                // editor.getMetadataTable().repaint(); If we refresh the model (which should be
                // the way to go, instead of just the repainting), when the delete button below
                // moves up to be under the mouse, clicking on it no longer works till we click
                // elsewhere.  I don't think this was intended.  
                // [FIXED: cleaned up other stuff and this was magically fixed]
                
                // todo: this layout/repaint would be better triggered by some kind of model update
                // event from MetadataLlist up through its LWComponent, which if we had could then
                // even become undoable.
                current.layout();
                current.notify(MetaButton.this, tufts.vue.LWKey.Repaint); // todo: undoable event

                VUE.getActiveMap().markAsModified();
                // VUE.getActiveMap().getUndoManager().mark("Delete meta-data"); // but not undoable

            } else if (DEBUG.Enabled) Log.debug("invalid row for delete, ignoring, selectdRow=" + selectedRow);
            
            if (DEBUG.PAIN) VUE.diagPop();            
        }
    }

    /*class MetaButtonMouseListener extends java.awt.event.MouseAdapter//implements java.awt.event.ActionListener {
        public void mouseReleased(java.awt.event.MouseEvent evt) {
           if(DEBUG_LOCAL) System.out.println("MetaButton - mouseReleased: " + evt.getPoint());
           java.util.List<edu.tufts.vue.metadata.VueMetadataElement> metadataList = editor.getCurrent().getMetadataList().getMetadata();
           int selectedRow = editor.getMetadataTable().getSelectedRow();
           metadataList.remove(selectedRow);
           editor.getMetadataTable().repaint();
        } }*/
    
    
}

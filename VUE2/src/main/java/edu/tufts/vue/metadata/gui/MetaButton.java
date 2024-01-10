
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
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueButton;
import edu.tufts.vue.metadata.ui.MetadataEditor;

/**
 * MetaButton.java
 *
 * VueButton for deleting category items
 * @author dhelle01
 */
public class MetaButton extends javax.swing.JPanel
{
    private final VueButton button;
    
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MetaButton.class);

    private final MetadataEditor mdEditor;
    private javax.swing.JTable table; // is not ready durning init so not final
    private int rowOfLastRequestedEditor;
    
    public MetaButton(edu.tufts.vue.metadata.ui.MetadataEditor editor, String type) {
        super(new java.awt.BorderLayout());
        super.setName(type);
        //super("keywords.button.delete");
        this.mdEditor = editor;
        // if (editor.getTable() == null) throw new Error(getClass() + ": JTable not yet initialized");
        button = new VueButton("keywords.button.delete");
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        //setBorder(javax.swing.BorderFactory.createEmptyBorder());
        setBorder(MetadataEditor.buttonBorder);
        //setSize(new java.awt.Dimension(5,5));
        add(button);
        if (!"renderDelete".equals(type)) {
            // Both must be mouse listeners:
            addMouseListener(MouseListener);
            button.addMouseListener(MouseListener);
            button.addActionListener(ButtonActionListener);
        }
    }
    
    public void setRowForButtonClick(int row) {
        rowOfLastRequestedEditor = row;
        this.table = mdEditor.getTable(); // init order doesn't let us make this final
    }
    
    private boolean ignoreNextAction = false;
        
    // We need to see this event before the MOUSE_RELEASE, which is when actionPerformed is called, in order to
    // cancel any active edit WITHOUT automatically saving, which is the default.  Tho it would be much nicer
    // if we actually supported a pressed state and didn't do something like delete until the user did a full
    // press & release.  If we don't catch MOUSE_PRESSED, what will happen is that before we're even called, as
    // we're a new editor being activated by the table, any active text edit will de-focus / shutdown, and will
    // have no idea it was because of a click on the delete button, which in the case of an active text edit
    // means don't save your current changes, as opposed to a full delete.

    private int clickState = 0;

    private final tufts.vue.MouseAdapter MouseListener = new tufts.vue.MouseAdapter("MB:mouseListener") {
            public void mousePressed(final java.awt.event.MouseEvent me) {
                if (DEBUG.PAIN) { VUE.diagPush("MB-pressIn" + clickState); Log.debug("mousePressed " + GUI.name(MetaButton.this)); }

                if (DEBUG.PAIN) Log.debug("active cell editor: " + Util.tags(table.getCellEditor())); // should be ButtonCE
                // // Can do this if we need to check type
                // if (cellEdit != null && cellEdit.getClass() == edu.tufts.vue.metadata.ui.MetadataEditor.ButtonCE.class) {
                //     if (DEBUG.PAIN) Log.debug("canceling needless button editor");
                //     editor.getTable().getCellEditor().cancelCellEditing(); }

                if (mdEditor.getActiveTextEdit() != null) {
                    if (DEBUG.PAIN) Log.debug("no delete: just cancel active edit and return; " + Util.tags(mdEditor.getActiveTextEdit()));
                    // It's crucial to do this here in mousePressed (or set a flag in the active cell if there is one)
                    // as it's about to stop editing anyway, and this tells it to abort any value modifications.
                    mdEditor.getActiveTextEdit().cancelCellEditing();
                    ignoreNextAction = true;
                } else
                    clickState++;
            }

            public void mouseReleased(final java.awt.event.MouseEvent me) {
                if (DEBUG.PAIN) Log.debug("mouseReleased " + GUI.name(MetaButton.this));
                // Now cancel US, just in case we're drawing differently from a click that didn't directly hit the button
                //if (++clickState == 3 && table.isEditing())
                    table.getCellEditor().stopCellEditing();
                ignoreNextAction = false;
                if (DEBUG.PAIN) VUE.diagPop();
            }};


    private final java.awt.event.ActionListener ButtonActionListener = new java.awt.event.ActionListener() { 
            public void actionPerformed(final java.awt.event.ActionEvent ae) {
                if (DEBUG.PAIN) {
                    VUE.diagPush("MB-actionp");
                    Log.debug("actionPerformed " + MetaButton.this + (ignoreNextAction ? " (ignored-on-cancel)":""));
                }
                // if (clickState < 3) return;
                
                if (ignoreNextAction)
                    ignoreNextAction = false;
                else
                    mdEditor.getModel().deleteAtRow(rowOfLastRequestedEditor);
                    // editor.getMetadataTable().getSelectedRow();

                clickState = 0;
                
                // if (table.isEditing()) table.getCellEditor().stopCellEditing();
                
                if (DEBUG.PAIN) VUE.diagPop();            
            }};

}

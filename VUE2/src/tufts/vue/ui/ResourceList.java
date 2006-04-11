 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue.ui;

import tufts.vue.Resource;

import java.util.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;


/**
 * A list if Resource's with their icons & title's that is selectable, draggable & double-clickable
 * for resource actions.
 *
 * @version $Revision: 1.2 $ / $Date: 2006-04-11 05:41:59 $ / $Author: sfraize $
 */
public class ResourceList extends JList
    implements DragGestureListener
{
    private static ImageIcon DragIcon = tufts.vue.VueResources.getImageIcon("favorites.leafIcon");
    
    private javax.swing.DefaultListModel model = new javax.swing.DefaultListModel();
	
    public ResourceList(Iterator iterator)
    {
        while (iterator.hasNext()) {
            model.addElement(iterator.next());
        }
        
        //setOpaque(false);
        setFixedCellHeight(37);
        setModel(model);
        setCellRenderer(new RowRenderer());
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setSelectionModel(selectionModel);

        selectionModel.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {				
                    tufts.vue.VUE.getResourceSelection().setTo(getPicked());
                }
            });

        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(this, // Component
                                                      DnDConstants.ACTION_COPY |
                                                      DnDConstants.ACTION_MOVE |
                                                      DnDConstants.ACTION_LINK,
                                                      this); // DragGestureListener
        
        
    }

    private Resource getPicked() {
        return (Resource) getSelectedValue();
    }
    
    public void dragGestureRecognized(DragGestureEvent e)
    {
        if (getSelectedIndex() != -1) {
            Resource r = getPicked(); 
            e.startDrag(DragSource.DefaultCopyDrop, // cursor
                        DragIcon.getImage(),
                        new Point(-10,-10), // drag image offset
                        new tufts.vue.gui.GUI.ResourceTransfer(r),
                        new tufts.vue.gui.GUI.DragSourceAdapter());
        }
    }

    private class RowRenderer extends DefaultListCellRenderer
    {
        RowRenderer() {
            //setOpaque(false); // selection stops working!
            //setFont(ResourceList.this.getFont()); // leave default label font
            
            // Border: 1 pix gray at bottom, then 2 pix empty in from left
            setBorder(new CompoundBorder(new MatteBorder(0,0,1,0, new Color(204,204,204)),
                                         new EmptyBorder(0,2,0,0)));
        }
        
        public Component getListCellRendererComponent(
        JList list,
        Object value,            // value to display
        int index,               // cell index
        boolean isSelected,      // is the cell selected
        boolean cellHasFocus)    // the list and the cell have the focus
        {
            Resource r = (Resource) value;
            setIcon(r.getIcon(list));
            setText("<HTML>" + r.getTitle());
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            //setEnabled(list.isEnabled());
            return this;
        }
    }
    
}


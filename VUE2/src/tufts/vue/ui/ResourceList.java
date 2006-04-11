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


/**
 * @version $Revision: 1.1 $ / $Date: 2006-04-11 03:22:23 $ / $Author: sfraize $
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
        setFixedCellHeight(36);
        setModel(model);
        setCellRenderer(new ResourceRenderer());
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setSelectionModel(selectionModel);

        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(this, // Component
                                                      DnDConstants.ACTION_COPY |
                                                      DnDConstants.ACTION_MOVE |
                                                      DnDConstants.ACTION_LINK,
                                                      this); // DragGestureListener
        
        
    }

    public void dragGestureRecognized(DragGestureEvent e)
    {
        System.out.println(getSelectedIndex());
        if (getSelectedIndex() != -1) {
            Resource r = (Resource) getSelectedValue();
            e.startDrag(DragSource.DefaultCopyDrop, // cursor
                        DragIcon.getImage(),
                        new Point(-10,-10), // drag image offset
                        new tufts.vue.gui.GUI.ResourceTransfer(r),
                        new tufts.vue.gui.GUI.DragSourceAdapter());
        }
    }

    private class ResourceRenderer extends DefaultListCellRenderer
    {
        ResourceRenderer() {
            //setOpaque(false); // selection stops working!
            setFont(ResourceList.this.getFont());
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
            //setFont(list.getFont());
            //setOpaque(true);
            return this;
        }
    }
    
}


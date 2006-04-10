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


/*
 * VueDragGrid.java
 *
 * Created on May 5, 2003, 4:08 PM
 */
package tufts.vue;
import tufts.google.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.util.Vector;
import javax.swing.event.*;
import osid.dr.*;

import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;

import java.util.Iterator;

/**
 *
 * @version $Revision: 1.6 $ / $Date: 2006-04-10 19:36:28 $ / $Author: jeff $
 * @author  rsaigal
 */
public class VueDragGrid extends JList
implements DragGestureListener, DragSourceListener
{
	private javax.swing.DefaultListModel model = new javax.swing.DefaultListModel();
    private static ImageIcon nleafIcon = VueResources.getImageIcon("favorites.leafIcon") ;
	
	public VueDragGrid(Iterator iterator)
	{
		while (iterator.hasNext()) {
			Resource resource = (Osid2AssetResource)iterator.next();
			Icon icon = resource.getIcon(76,76);
//			icon.setToolTipText(resource.getTitle());
			model.addElement(icon);
			setFixedCellHeight(80);
		}
		setModel(model);
        //implementDrag(this);
		DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setSelectionModel(selectionModel);
	}

	private void  implementDrag(VueDragGrid list){
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(list,
                                                      DnDConstants.ACTION_COPY |
                                                      DnDConstants.ACTION_MOVE |
                                                      DnDConstants.ACTION_LINK,
                                                      list);
    }
    
    public void dragGestureRecognized(DragGestureEvent e) {
      
		System.out.println(getSelectedIndex());
        if (getSelectedIndex() != -1) {
			Resource resource = (Osid2AssetResource)getSelectedValue();
			e.startDrag(DragSource.DefaultCopyDrop, // cursor
						nleafIcon.getImage(), // drag image
						new Point(-10,-10), // drag image offset
						new tufts.vue.gui.GUI.ResourceTransfer(resource),
						this);  // drag source listener
		}
    }
    
    
    public void dragDropEnd(DragSourceDropEvent e) {
        if (tufts.vue.VUE.dropIsLocal == true){
            //DefaultListModel model = (DefaultListModel)this.getModel();
            //model.removeElementAt(oldItem);
            tufts.vue.VUE.dropIsLocal = false;
        }
    }
    
    public void dragEnter(DragSourceDragEvent e) { }
    public void dragExit(DragSourceEvent e) {}
    public void dragOver(DragSourceDragEvent e) {}
    public void dropActionChanged(DragSourceDragEvent e) {
        if (DEBUG.DND) System.out.println("VueDragGrid: dropActionChanged  to  " + tufts.vue.gui.GUI.dropName(e.getDropAction()));
    }
}


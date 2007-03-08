
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

package edu.tufts.vue.ontology.ui;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import tufts.vue.LWComponent;
import tufts.vue.LWNode;
import tufts.vue.LWLink;

/*
 * TypeList.java
 *
 * modelled on tufts.vue.ui.ResourceList, this List will show
 * mapping of styles to ontologies and allow easy transfer
 * to LWMap - editing of individual  styles should also be possible
 * through the VUE gui.
 *
 * Created on March 7, 2007, 1:05 PM
 *
 * @author dhelle01
 */
public class TypeList extends JList {
    
    private DefaultListModel mDataModel;
    
    /** Creates a new instance of TypeList */
    public TypeList() {
        mDataModel = new DefaultListModel();
        setModel(mDataModel);
        setCellRenderer(new TypeCellRenderer());
    }
    
    public void addType(LWComponent componentType)
    {
        //todo: use getAsImage
        mDataModel.addElement(componentType.createImage(1.0,new java.awt.Dimension(100,100),new java.awt.Color(225,225,225)));
    }
    
    class TypeCellRenderer implements ListCellRenderer
    { 
        public java.awt.Component getListCellRendererComponent(JList jList, Object value, int i, boolean isSelected, boolean hasFocus) 
        {
            JPanel p = new JPanel();
            if(value instanceof java.awt.Image)
            {
                javax.swing.JLabel imageLabel = new javax.swing.JLabel(new javax.swing.ImageIcon((java.awt.Image)value));
                p.add(imageLabel);
            }
            return p;
        }
    }
    
    /**
     *
     * just for testing/demo purposes
     *
     */
    public static void main(String[] args)
    {
        
        // add a mouselistener to bring up a color chooser (and possibly
        // the text formatter or a generic one?).
        // attempt to edit an LWComponent directly on the List
        // if this works, try it all in VUE (and add ability to drag 
        // types to LWMap to create instances)
        
        // really should use a DockWindow..
        javax.swing.JFrame f = new javax.swing.JFrame();
        TypeList tlist = new TypeList();
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(tlist);
        f.getContentPane().add(scroll);
        
        //add some demo data to the List
        LWNode rectangle = new LWNode("Test Node");
        rectangle.setShape( new java.awt.Rectangle(5,5,50,20));
        LWLink link = new LWLink();
        link.setStartPoint(10,10);
        link.setEndPoint(90,10);
        link.setAbsoluteSize(100,15);
        link.setArrowState(LWLink.ARROW_BOTH);
        link.setWeight(2);
        LWNode oval = new LWNode("Test Node 2");
        oval.setShape(new java.awt.geom.Ellipse2D.Double(5,5,50,20));

        tlist.addType(rectangle);
        tlist.addType(link);
        tlist.addType(oval);
        
        //show the frame
        f.setBounds(100,100,150,300);
        f.setVisible(true);
    }
    
}

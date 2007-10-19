
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

package edu.tufts.vue.metadata.ui;

import java.awt.BorderLayout;

import javax.swing.JLabel;

import tufts.vue.*;
import tufts.vue.gui.Widget;

import edu.tufts.vue.ontology.*;

/*
 * OntologicalMembershipPane.java
 *
 * Created on October 18, 2007, 8:45 PM
 *
 * @author dhelle01
 */
public class OntologicalMembershipPane extends javax.swing.JPanel implements ActiveListener {
    
    private static OntologicalMembershipPane global;
    
    private LWComponent current;
    private javax.swing.JList list;
    
    public static OntologicalMembershipPane getGlobal()
    {
        return global;
    }
    
    public OntologicalMembershipPane() 
    {
        
        setLayout(new BorderLayout());
        
        //todo: adjust font (html handles the text wrapping)
        JLabel label = new JLabel("<html>Ontological Terms associated with this node:</html>");
        
        add(label);
        list = new javax.swing.JList(new OntologyTypeListModel());
        add(list,java.awt.BorderLayout.SOUTH);
        
        VUE.addActiveListener(LWComponent.class,this);
        
        if(global == null)
        {
            global = this;
        }
    }
    
    public LWComponent getCurrent()
    {
        return current;
    }
    
    public void activeChanged(ActiveEvent e)
    {
        if(e != null)
        {
            current = (LWComponent)e.active;
            
            if(current == null)
                return;
            
            ((OntologyTypeListModel)list.getModel()).refresh();
            
            adjustVisibility();

        }
    }
    
    public void refresh()
    {
        ((OntologyTypeListModel)list.getModel()).refresh();
            
        adjustVisibility();   
    }
    
    static public void adjustVisibility()
    {
            if(getGlobal().getCurrent().getMetadataList().hasOntologicalMetadata())
            {
              Widget.setHidden(getGlobal(),false);
            }
            else
            {
              Widget.setHidden(getGlobal(),true);
            }
    }
    
    class OntologyTypeListModel extends javax.swing.DefaultListModel
    {        
        public Object getElementAt(int i)
        {
            Object ele = current.getMetadataList().getOntologyListElement(i).getObject();
            if(ele != null && ele instanceof OntType)
                return ((OntType)ele).getLabel();
            else if(ele != null)
                return ele.toString();
            else
                return "";
        }
        
        public int getSize()
        {
            if(current !=null)
              return current.getMetadataList().getOntologyListSize();
            else
              return 0;
        }
        
        public void refresh()
        {
             fireContentsChanged(this,0,getSize());
        }
    }

}


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

import java.awt.Font;

import javax.swing.Box;

import tufts.vue.VueConstants;
import tufts.vue.gui.CheckBoxRenderer;
import tufts.vue.gui.GUI;


/*
 * OntologyList.java
 *
 * Created on April 9, 2007, 11:36 AM
 *
 * @author dhelle01
 */
public class OntologyList extends javax.swing.JList implements OntologySelectionListener {
	
    public OntologyList(OntologyViewer viewer)
    {
        super(new OntologyListModel());
       // setLayout(new java.awt.BorderLayout());
        setCellRenderer(new OntologyListRenderer());
    }
    
    public void refresh()
    {
        ((OntologyListModel)getModel()).refresh();
    }
    
    public void ontologySelected(OntologySelectionEvent ose)
    {
        
        /*int selection = OntologyBrowser.getBrowser().getSelectedIndex(ose.getSelection());
        if(selection > 0 && selection < getModel().getSize())
        {    
          setSelectedIndex(selection);
          repaint();
        }
        
        System.out.println("Ontology List -- ontology selected - " + selection);*/
        
        edu.tufts.vue.ontology.Ontology ont = ose.getSelection().getOntology();
        
        for(int i=0;i<getModel().getSize();i++)
        {
            if(getModel().getElementAt(i).equals(ont))
                setSelectedIndex(i);
        }
        
    }
    
    public static class OntologyListModel extends javax.swing.DefaultListModel
    {
        
       /* public void addListDataListener(javax.swing.event.ListDataListener listener)
        {
            
        }
        
        public void removeListDataListener(javax.swing.event.ListDataListener listener)
        {
            
        }*/
        
        public Object getElementAt(int index)
        {
            //String displayString = (edu.tufts.vue.ontology.OntManager.getOntManager().getOntList().get(index)).toString();
            //return displayString.substring(8);
            if(index > edu.tufts.vue.ontology.OntManager.getOntManager().getOntList().size() - 1 )
                return null;
            else
                return edu.tufts.vue.ontology.OntManager.getOntManager().getOntList().get(index);
        }
        
        public int getSize()
        {
            return edu.tufts.vue.ontology.OntManager.getOntManager().getOntList().size();
        }
        
        public void refresh()
        {
            fireContentsChanged(this,0,getSize());
        }
        
    }
    
    public static class OntologyListRenderer extends javax.swing.DefaultListCellRenderer
    {
        //private javax.swing.JLabel mLabel = new javax.swing.DefaultListCellRenderer();
        private javax.swing.border.Border dividerBorder = new tufts.vue.gui.DashBorder(java.awt.Color.LIGHT_GRAY,false,true,false,false);
        private javax.swing.JLabel importLabel = new javax.swing.JLabel("Add Style Sheet");
        
        private CheckBoxRenderer mCheckBox = new CheckBoxRenderer();
        /*mRow.add(Box.createHorizontalStrut(GUI.WidgetInsets.left));
        mRow.add(mCheckBox);
        mRow.add(Box.createHorizontalStrut(GUI.WidgetInsets.left));
        mRow.add(mLabel);
        mRow.add(mIconLabel);
        mRow.add(Box.createHorizontalStrut(GUI.WidgetInsets.right));*/
        public java.awt.Component getListCellRendererComponent(javax.swing.JList list,Object value,int index,boolean isSelected,boolean hasFocus)
        {
            //javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            //javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
        	Font f = importLabel.getFont();
        	
        		
        	
        	importLabel.setFont(VueConstants.FONT_NARROW);
            javax.swing.JPanel panel = new javax.swing.JPanel()
            {
               public java.awt.Dimension getPreferredSize()
               {
                   return new java.awt.Dimension(100,23);
               }
            };
           // panel.setPreferredSize(new javax.swing.DefaultListCellRenderer().getPreferredSize());
            panel.setLayout(new javax.swing.BoxLayout(panel,javax.swing.BoxLayout.X_AXIS));
            
            //panel.setBorder(javax.swing.BorderFactory.createMatteBorder(0,0,1,0,new java.awt.Color(200,200,200)));
            if(index!=0)
              panel.setBorder(dividerBorder);
            
            //panel.setOpaque(true);
            // panel.setBackground(java.awt.Color.BLUE);
            
            edu.tufts.vue.ontology.Ontology ontology = (edu.tufts.vue.ontology.Ontology)value;
         /*   
            String base = ontology.getBase();
            String baseWithoutFileType = base.substring(0,base.lastIndexOf("."));
            String displayString = "";
            int baseIndex = baseWithoutFileType.lastIndexOf("/");
            if(baseIndex>0)
                displayString = baseWithoutFileType.substring(baseIndex+1);
            else
                displayString = baseWithoutFileType;
          */
            javax.swing.JLabel label = new javax.swing.JLabel(ontology.getLabel());
            
            mCheckBox.setSelected(ontology.isEnabled());
            panel.add(Box.createHorizontalStrut(GUI.WidgetInsets.left));
            panel.add(mCheckBox);           
            panel.add(Box.createHorizontalStrut(GUI.WidgetInsets.left));
            panel.add(label);
     
            panel.setOpaque(true);
            if(value == list.getSelectedValue())
            {
                panel.setBackground(tufts.vue.gui.GUI.getTextHighlightColor());
            }
            else
            {
                panel.setBackground(new java.awt.Color(255,255,255));
            }
            
            if(ontology.getStyle() == null)
            {   
                panel.add(javax.swing.Box.createHorizontalGlue());
                importLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,0,0,5));
                panel.add(importLabel);
            }
            return panel;
        }
    }
    
}

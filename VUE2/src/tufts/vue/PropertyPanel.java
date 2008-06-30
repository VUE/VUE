 /*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

package tufts.vue;

import java.lang.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import tufts.vue.gui.ColorMenuButton;

import java.beans.*;

public class PropertyPanel extends JPanel {
    
    /////////////
    // Fields
    ///////////////
    
    /** the grid bag layout **/
    GridBagLayout mGridBag = null;
    
    
    //////////////
    // Constructor
    ///////////////
    JPanel innerPanel;
    
    public PropertyPanel() {
        innerPanel = new JPanel();
        setBorder( new EmptyBorder( 0,0,0,0) );
        
        mGridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        innerPanel.setLayout(mGridBag);
        setLayout(new BorderLayout());
        add(innerPanel);
        
        
        
    }
    
    
    public JLabel createLabel( String pText) {
        JLabel label = new JLabel( pText);
        label.setFont(tufts.vue.gui.GUI.LabelFace);
        
        return label;
        
    }
    
    /**
     * addProperty
     *
     *
     * @param String pName the proeprty display name
     * @param String pValue the property value
     **/
    public void addProperty( String pName, String pValue) {
        JLabel  value = createLabel( pValue);
        addProperty( pName, (JComponent) value);
    }
    /**
     *
     * /**
     * addProperty
     * This adds a property to be displayed in the property panel
     * @param the property
     **/
    public void addProperty( String pDisplayName, JComponent pEditor ){
        JLabel label = createLabel( pDisplayName);
        addProperty( label, pEditor);
    }
    
    int gridy=0;
    public void addProperty( JComponent pLabel, JComponent pEditor ){
    	    
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        
        // add the property's label string
        //
        c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
        c.fill = GridBagConstraints.NONE;      //reset to default
       // c.weightx = 0.0;                       //reset to default
        c.weighty=1.0;
        c.ipadx=4;
        c.ipady=10;
        c.gridx=0;
        c.gridy=gridy;
        if (pEditor instanceof ColorMenuButton)
        {
        	c.insets = new Insets(2,0,2,0);
        }
        if (pEditor instanceof JScrollPane)
        {
        	c.anchor = GridBagConstraints.NORTHWEST;
        	
        	c.gridwidth=2;
        }
        mGridBag.setConstraints(pLabel, c);        
        innerPanel.add( pLabel);
                
        // add the value renderer or editor/display
        //
        c.gridwidth = GridBagConstraints.REMAINDER;     //end row
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.ipadx=4;
        c.ipady=2;
        c.gridx=1;
        c.gridy=gridy;
        
        if( pEditor == null) {
            JTextField field = new JTextField();
            field.setText( "n/a" );
            pEditor = field;
            
        }        
        
        
        if (pEditor instanceof ColorMenuButton)
        {
        	c.fill = GridBagConstraints.NONE;        	
        	c.ipadx=0;
        	c.ipady=0;
        }
        else
        {
        	c.insets = new Insets(5,0,5,0);
        }
        
        if (pEditor instanceof JScrollPane)
        {
        	c.gridy=gridy++;    
        	c.gridx=0;
        	c.insets = new Insets(30,1,10,1);
        	c.ipadx=0;
        }
        c.anchor = GridBagConstraints.WEST;
        mGridBag.setConstraints( pEditor, c);
        innerPanel.add( pEditor);
        
        gridy++;
    }
    
    
    
    
    
}








package tufts.vue;

import java.lang.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
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

	public PropertyPanel() {

		setBorder( new EmptyBorder( 0,0,0,0) );

		mGridBag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(mGridBag);
		
		

	
	}

	
	public JLabel createLabel( String pText) {
		JLabel label = new JLabel( pText);
		
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

	/**
	 * addProperty
	 * This adds a property to be displayed in the property panel
	 * @param the property
	 **/
	public void addProperty( String pDisplayName, JComponent pEditor ){
		JLabel label = createLabel( pDisplayName);
		addProperty( label, pEditor);
	}
	public void addProperty( JComponent pLabel, JComponent pEditor ){

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;

	        
		// add the property's label string
		//
		c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
		c.fill = GridBagConstraints.NONE;      //reset to default
		c.weightx = 0.0;                       //reset to default
		c.ipadx=4;
		c.ipady=2;

		mGridBag.setConstraints(pLabel, c);
		 this.add( pLabel);

	        
		// add the value renderer or editor/display
		//
		c.gridwidth = GridBagConstraints.REMAINDER;     //end row
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.ipadx=4;
		c.ipady=2;
		
		if( pEditor == null) {
			JTextField field = new JTextField();
			field.setText( "n/a" );
			pEditor = field;
			}
			
		mGridBag.setConstraints( pEditor, c);
		add( pEditor);

	    }





}






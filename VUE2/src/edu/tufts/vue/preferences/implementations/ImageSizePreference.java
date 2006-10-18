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

package edu.tufts.vue.preferences.implementations;


import tufts.vue.MapViewer;
import edu.tufts.vue.preferences.PreferenceConstants;
import edu.tufts.vue.preferences.generics.GenericSliderPreference;

import java.util.Hashtable;
import java.util.prefs.*;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

public class ImageSizePreference extends GenericSliderPreference {

	private final int HASHMARK_0=0;
	private final int HASHMARK_1=83;
	private final int HASHMARK_2=166;
	private final int HASHMARK_3=250;
	private final int HASHMARK_4=332;
	private final int HASHMARK_5=415;
	private final int HASHMARK_6=500;
	
	public ImageSizePreference()
	{
		super();
		configureSlider();
	}	
	
	public void configureSlider()
	{
		JSlider value = getSlider();
		value.setMajorTickSpacing(83);
        value.setPaintTicks(true);        
        value.setSnapToTicks(true);
        value.setMinimum(0);
        value.setMaximum(500);
//        Create the label table
        Hashtable labelTable = new Hashtable();
        labelTable.put( new Integer( HASHMARK_0 ), new JLabel("Off") );
        labelTable.put( new Integer( HASHMARK_1 ), new JLabel("16x16") );
        labelTable.put( new Integer( HASHMARK_2 ), new JLabel("75x75") );
        labelTable.put( new Integer( HASHMARK_3 ), new JLabel("125x125") );
        labelTable.put( new Integer( HASHMARK_4 ), new JLabel("250x250") );
        labelTable.put( new Integer( HASHMARK_5 ), new JLabel("400x400") );
        labelTable.put( new Integer( HASHMARK_6 ), new JLabel("500x500") );
        value.setLabelTable( labelTable );
        
        value.setPaintLabels(true);
        
        return;
	}
		
	public int getValue()
	{
		int val = getSlider().getValue();
		
		switch (val)
		{
			case HASHMARK_0:
				return 0;
			case HASHMARK_1:
				return 16;
			case HASHMARK_2:
				return 75;
			case HASHMARK_3:
				return 125;
			case HASHMARK_4:
				return 250;
			case HASHMARK_5:
				return 500;		
		}
		
		return getDefaultValue();
	}
	public String getPreferenceCategory() {
		return PreferenceConstants.MAPDISPLAY_CATEGORY;
	}

	public String getTitle()
	{
		return new String("Images");
	}
	
	public String getDescription()
	{
		return new String("Controls the size of images displayed on the map.");
	}
	
	public String getPrefName()
	{
		return PreferenceConstants.imageSizePref;
	}

	public String getCategoryKey() {
		return "mapDisplay";
	}
	
	public void preferenceChanged()
	{	
					
	}

	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}
	
}

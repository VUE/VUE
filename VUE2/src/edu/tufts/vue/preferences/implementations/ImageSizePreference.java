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

package edu.tufts.vue.preferences.implementations;


import java.util.Hashtable;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

import tufts.vue.VueResources;
import edu.tufts.vue.preferences.PreferenceConstants;
import edu.tufts.vue.preferences.generics.GenericSliderPreference;

/**
 * @author Mike Korcynski
 * This class supports the MapDisplay -> Image Size preference, the way this works is a bit odd.
 * The major issue being that the scale I was given isn't linear, so I had to sort of rig the 
 * Slider together, this could be improved by picking a linear scale by which to size the images.
 */
public class ImageSizePreference extends GenericSliderPreference {

	private final int TICK_SPACING=85;
	private final int HASHMARK_0=0;
	private final int HASHMARK_1=TICK_SPACING;
	private final int HASHMARK_2=TICK_SPACING*2;
	private final int HASHMARK_3=TICK_SPACING*3;
	private final int HASHMARK_4=TICK_SPACING*4;
	private final int HASHMARK_5=TICK_SPACING*5;
	private final int HASHMARK_6=TICK_SPACING*6;
	
	
	private static ImageSizePreference _instance;	
	
	private ImageSizePreference()
	{
		super();
		JSlider slider = getSlider();
		slider.setOrientation(JSlider.VERTICAL);        
        slider.setMajorTickSpacing(TICK_SPACING);
        slider.setPaintTicks(true);        
        slider.setSnapToTicks(true);
        slider.setMinimum(0);
        slider.setMaximum(510);
        
		configureSlider();
	}	
		
	 // For lazy initialization
	 public static synchronized ImageSizePreference getInstance() {
	  if (_instance==null) {
	   _instance = new ImageSizePreference();
	  }
	  return _instance;
	 }
	 
	 public Object getDefaultValue()
	 {
		 return new Integer(75);
	
	 }
	 
	public void configureSlider()
	{
		JSlider slider = getSlider();
		setDefaultValueMappedToSlider();		
        //Create the label table
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put( new Integer( HASHMARK_0 ), new JLabel(VueResources.getString("jlabel.off")) );
        labelTable.put( new Integer( HASHMARK_1 ), new JLabel("16x16") );
        labelTable.put( new Integer( HASHMARK_2 ), new JLabel("32x32") );
        labelTable.put( new Integer( HASHMARK_3 ), new JLabel("64x64") );
        labelTable.put( new Integer( HASHMARK_4 ), new JLabel("128x128") );
        labelTable.put( new Integer( HASHMARK_5 ), new JLabel("256x256") );
        labelTable.put( new Integer( HASHMARK_6 ), new JLabel("512x512") );        
        slider.setLabelTable( labelTable );
        
        slider.setPaintLabels(true);
        getSlider().addChangeListener(this);
        return;
	}
	
	/*
	 * Lots of mapping back and forth between the linear scale of the Slider and the
	 * possible values we want.
	 */
	public void  setDefaultValueMappedToSlider()
	{
		JSlider slider = getSlider();
		Preferences p = Preferences.userNodeForPackage(getPrefRoot());
		
		int i = p.getInt(getPrefName(), ((Integer)getDefaultValue()).intValue());
	//	System.out.println("DEFAULT VALUE : " + i);
		switch (i)
		{
			case 0:
				slider.setValue(HASHMARK_0);
				break;
			case 16:
				slider.setValue(HASHMARK_1);
				break;
			case 32:
				slider.setValue(HASHMARK_2);
				break;
			case 64:
				slider.setValue(HASHMARK_3);
				break;
			case 128:
				slider.setValue(HASHMARK_4);
				break;
			case 256:
				slider.setValue(HASHMARK_5);
				break;
			case 512:
				slider.setValue(HASHMARK_6);
				break;			
		}
		
		return;
	}	
	
	public int getSliderValueMappedToPref()
	{
		int val = getSlider().getValue();
		//System.out.println("MAP VALUE : " +val);
		switch (val)
		{
			case HASHMARK_0:
				return 0;
			case HASHMARK_1:
				return 16;
			case HASHMARK_2:
				return 32;
			case HASHMARK_3:
				return 64;
			case HASHMARK_4:
				return 128;
			case HASHMARK_5:
				return 256;	
			case HASHMARK_6:
				return 512;
		}
		
		return ((Integer)getDefaultValue()).intValue();
	}
	
	 	
	public String getTitle()
	{
		return VueResources.getString("preferencedailog.images");
	}
	
	public String getDescription()
	{
		return new String("Controls the size of images displayed on the map.");
	}
	
	public String getPrefName()
	{
		return "mapDisplay.imageSize";
	}

	public String getCategoryKey() {
		return PreferenceConstants.MAPDISPLAY_CATEGORY;
	}	

	public void stateChanged(ChangeEvent e) 
	{
		JSlider source = (JSlider)e.getSource();
	    if (!source.getValueIsAdjusting()) {
	    	setValue(Integer.valueOf(getSliderValueMappedToPref()));
	    }
	}
	
}

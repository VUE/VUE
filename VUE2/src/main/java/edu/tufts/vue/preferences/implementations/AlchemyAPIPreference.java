package edu.tufts.vue.preferences.implementations;

import edu.tufts.vue.preferences.PreferenceConstants;

public class AlchemyAPIPreference extends StringPreference{

	protected AlchemyAPIPreference(String category, String key,
			String name, String desc, Object defaultValue, boolean showInUI) {
		super(category, key, name, desc, defaultValue, showInUI);
		// TODO Auto-generated constructor stub
	}
	
private static AlchemyAPIPreference _instance; 
	

	
	 // For lazy initialization
	 public static synchronized AlchemyAPIPreference getInstance() {
	  if (_instance==null) {
	   _instance = new AlchemyAPIPreference(PreferenceConstants.DATA_CATEGORY,
				  "data.AlchemyAPIKey",
				  "AlchemyAPI Key",
				  "Set your AlchemyAPI Key:",
				  (Object)(new String("")),
				  true								  
				  );	   

	  }
	  return _instance;
	 }	

}

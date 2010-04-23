package edu.tufts.vue.preferences.implementations;

import edu.tufts.vue.preferences.PreferenceConstants;

public class SeasrRepositoryPreference extends StringPreference{

	protected SeasrRepositoryPreference(String category, String key,
			String name, String desc, Object defaultValue, boolean showInUI) {
		super(category, key, name, desc, defaultValue, showInUI);
		// TODO Auto-generated constructor stub
	}
	
private static SeasrRepositoryPreference _instance; 
	

	
	 // For lazy initialization
	 public static synchronized SeasrRepositoryPreference getInstance() {
	  if (_instance==null) {
	   _instance = new SeasrRepositoryPreference(PreferenceConstants.DATA_CATEGORY,
				  "data.SeasrRepo",
				  "SEASR Repository",
				  "Set the location of your SEASR repository.",
				  (Object)(new String("http://vue.tufts.edu/seasr.xml")),
				  true								  
				  );	   

	  }
	  return _instance;
	 }	

}

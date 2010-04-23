/*
 * SeaseConfigLoader.java
 *
 * Created on 11:56:53 AM
 *
 * Copyright 2003-2010 Tufts University  Licensed under the
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
package edu.tufts.seasr;

import java.net.MalformedURLException;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Unmarshaller;
import org.xml.sax.InputSource;
import java.net.URL;
import tufts.vue.MapViewer;
import tufts.vue.VUE;
import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;
import edu.tufts.vue.preferences.implementations.SeasrRepositoryPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;

/**
 * @author akumar03
 * This class loads the seasr config file. It also has methods 
 * that return specific types of flows.
 */
public class SeasrConfigLoader {
	private static Unmarshaller unmarshaller = null;
	private static URL XML_MAPPING;
	private static String DEFAULT_SEASR_CONFIG = "seasr.xml";
	private static String SEASR_CONFIG = null;
	public static final String  CREATE_NODES = "Create New Nodes";
	public static final String ADD_METADATA = "Add Metadata";
	public static final String ADD_NOTES = "Add Notes";
	
	public  SeasrConfigLoader() {
		XML_MAPPING  = this.getClass().getResource("seasr_mapping.xml"); 
	}
	
	public SeasrAnalytics loadConfig()  throws Exception  {
		Unmarshaller unmarshaller = getUnmarshaller();
		unmarshaller.setValidation(false);
		SEASR_CONFIG =  (String)edu.tufts.vue.preferences.implementations.SeasrRepositoryPreference.getInstance().getValue();
	    System.out.println("Seasr config: "+SEASR_CONFIG);
	    InputStream inputstream = null;
	    try {
	    	URL url  = new URL(SEASR_CONFIG);
	    	inputstream = url.openStream();
	    } catch(MalformedURLException mex) {
	    	System.out.println("SeasrConfigLoader.loadConfig: "+mex);
	    	inputstream = this.getClass().getResourceAsStream(DEFAULT_SEASR_CONFIG);
	    } catch(IOException iox) {
	    	System.out.println("SeasrConfigLoader.loadConfig: "+iox);
	    	inputstream = this.getClass().getResourceAsStream(DEFAULT_SEASR_CONFIG);
	    }  
	    	SeasrAnalytics sa = (SeasrAnalytics) unmarshaller.unmarshal(new InputSource(inputstream));
	    	return sa;
	  
	}
	
	public FlowGroup getFlowGroup(String label) throws Exception {
		SeasrAnalytics sa = loadConfig();
		for(FlowGroup fg: sa.getFlowGroupList()) {
			if(fg.getLabel().equals(label)){;
				return fg;
			}
		}
		return null;
		
	}
	
	private static Unmarshaller getUnmarshaller() {
		if (unmarshaller == null) {
			unmarshaller = new Unmarshaller();
			Mapping mapping = new Mapping();
			try {
				mapping.loadMapping(XML_MAPPING);
				unmarshaller.setMapping(mapping);
				System.out.println("Loaded Mapping: "+XML_MAPPING);
			} catch (Exception e) {
				System.out.println("edu.tufts.seasr.SeasrConfigLoader.getUnmarshaller: " + XML_MAPPING+e);
			}
		}
		return unmarshaller;
	}
}

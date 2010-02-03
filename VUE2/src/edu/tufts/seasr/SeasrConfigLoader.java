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

import java.net.URL;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Unmarshaller;
import org.xml.sax.InputSource;

/**
 * @author akumar03
 * This class loads the seasr config file. It also has methods 
 * that return specific types of flows.
 */
public class SeasrConfigLoader {
	private static Unmarshaller unmarshaller = null;
	private static URL XML_MAPPING;
	private static String SEASR_CONFIG = "seasr.xml";
	public static final String  CREATE_NODES = "Create New Nodes";
	public static final String ADD_METADATA = "Add Metadata";
	public static final String ADD_NOTES = "Add Notes";
	
	public  SeasrConfigLoader() {
		XML_MAPPING  = this.getClass().getResource("seasr_mapping.xml"); 
	}
	
	public SeasrAnalytics loadConfig()  throws Exception  {
		Unmarshaller unmarshaller = getUnmarshaller();
		unmarshaller.setValidation(false);
		SeasrAnalytics sa = (SeasrAnalytics) unmarshaller.unmarshal(new InputSource(this.getClass().getResourceAsStream(SEASR_CONFIG)));
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

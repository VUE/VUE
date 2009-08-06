package edu.tufts.component.urlextractor;

/*
 *
 * VUEUrlExtractor.java
 *
 * Created on  Aug 3, 2009
 *
 * Copyright 2003-2009 Tufts University  Licensed under the
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

import java.util.Map;
import java.net.URL;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.seasr.meandre.support.parsers.DataTypeParser;

import org.seasr.meandre.components.tools.Names;
/**
 * This class extracts the URLs submitted from VUE
 *
 * @author  Anoop Kumar ;
 */


@Component(
		creator = "Anoop Kumar",
		description = "Extract the urls submitted by VUE, v1.0.1",
		name = "VUE URL Extractor",
		tags = "zotero, authors, information extraction",
		rights = Licenses.UofINCSA,
		mode = Mode.compute,
		firingPolicy = FiringPolicy.all,
		baseURL = "meandre://vue.tufts.edu/urlextractor"
)

public class VUEUrlExtractor extends AbstractExecutableComponent {

	@ComponentInput(
			description = "A map object containing the key elements of the request and the associated values",
			name = Names.PORT_REQUEST_DATA
	)
	protected static final String IN_REQUEST = Names.PORT_REQUEST_DATA;

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
			description = "Item location",
			name = Names.PORT_LOCATION
	)
	protected static final String OUT_ITEM_LOCATION = Names.PORT_LOCATION;

	@ComponentOutput(
			description = "No data to display.",
			name = Names.PORT_NO_DATA
	)
	public final static String OUT_NO_DATA = Names.PORT_NO_DATA;
	public static final String LOCATION_LABEL = "location";


	@Override
	public void disposeCallBack(ComponentContextProperties arg0)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeCallBack(ComponentContext cc) throws Exception {
		Map<String,byte[]> map = DataTypeParser.parseAsStringByteArrayMap(cc.getDataComponentFromInput(IN_REQUEST));
		for ( String sKey:map.keySet() ) {
			 if(sKey.equals(LOCATION_LABEL)) {
				 String location = new String(map.get(LOCATION_LABEL));
				 try{
					 URL url = new URL(location);
				 }catch(Exception ex) {
					 cc.pushDataComponentToOutput(OUT_NO_DATA,"Your items contained no URL information");
					 return;
				 }
				 cc.pushDataComponentToOutput(OUT_ITEM_LOCATION, location);
				 return;
			 }
		}
		 cc.pushDataComponentToOutput(OUT_NO_DATA,"Your items contained no URL information");
		 return;
		
	}

	@Override
	public void initializeCallBack(ComponentContextProperties cc)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

}

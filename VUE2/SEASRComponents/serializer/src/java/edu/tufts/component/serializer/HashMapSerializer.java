package edu.tufts.component.serializer;



/*
 *
 * HashMapSerializer.java
 *
 * Created on  Aug 6, 2009
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

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.parsers.DataTypeParser;


// --------------------------------------------------
@Component(
		baseURL = "meandre://seasr.org/components/hashmapserializer/", 
		creator = "Anoop Kumar", 
		description = "This component takes an intergerhashmap and serialiazes it v1.0.0 ", 
		name = "HashMap Serializer", tags = "WebUI, process request", 
		mode = Mode.webui, firingPolicy = Component.FiringPolicy.all
)
// -------------------------------------------------------------------------

/**
 *  This is a component  takes an hashmap from google protocol ans serializes it
 * 
 * @author Anoop
 */
public class HashMapSerializer extends AbstractExecutableComponent {
	@ComponentInput(
			name = Names.PORT_TOKEN_MAP,
			description = "Sorted map of tokens"
	)
	protected static final String IN_TOKENS = Names.PORT_TOKEN_MAP;

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
			name = Names.PORT_TOKEN_MAP,
			description = "Serialized Sorted List of Tokens"
	)
	protected static final String OUT_TOKENS = Names.PORT_TOKEN_MAP;

	@Override
	public void disposeCallBack(ComponentContextProperties arg0)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeCallBack(ComponentContext cc) throws Exception {
		// TODO Auto-generated method stub
		 Map<String, Integer> inputMap = DataTypeParser.parseAsStringIntegerMap(cc.getDataComponentFromInput(IN_TOKENS));
		 cc.pushDataComponentToOutput(OUT_TOKENS, inputMap);
 
	}

	@Override
	public void initializeCallBack(ComponentContextProperties arg0)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

}

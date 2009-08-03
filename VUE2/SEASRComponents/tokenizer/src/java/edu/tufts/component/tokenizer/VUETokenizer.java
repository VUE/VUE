package edu.tufts.component.tokenizer;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.datatypes.BasicDataTypesTools;
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.parsers.DataTypeParser;

import opennlp.tools.lang.english.Tokenizer;

/*
 *
 * Connector.java
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

@Component(
		name = "VUETokenizer",
		creator = "Anoop Kumar",
		baseURL = "meandre://vue.tufts.edu/tokenizer",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		tags = "semantic, tools, text, opennlp, tokenizer",
		description = "This component breaks a document into tokens v1.0.1 "
		 
)
public class VUETokenizer extends AbstractExecutableComponent {
	@ComponentInput(
			name = Names.PORT_TEXT,
			description = "The text to be tokenized"
	)
	protected static final String IN_TEXT = Names.PORT_TEXT;

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
			name = Names.PORT_TOKENS,
			description = "The sequence of tokens"
	)
	protected static final String OUT_TOKENS = Names.PORT_TOKENS;
	@Override
	public void disposeCallBack(ComponentContextProperties arg0)
			throws Exception {
		// TODO Auto-generated method stub

	}
	@Override
	public void executeCallBack(ComponentContext cc) throws Exception {
		String[] inputs = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_TEXT));
		StringBuilder sb = new StringBuilder();

		for (String text : inputs)
		    sb.append(text).append(" ");

		String[] ta = sb.toString().split("\\W");
		cc.getOutputConsole().println("[INFO]Tokenized: "+ta.length);
		cc.pushDataComponentToOutput(OUT_TOKENS, BasicDataTypesTools.stringToStrings(ta));

	}

	@Override
	public void initializeCallBack(ComponentContextProperties cc)
			throws Exception {
		 

	}

}

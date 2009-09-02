package edu.tufts.component.readability;


import java.util.HashMap;

import org.meandre.components.abstracts.AbstractExecutableComponent;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.seasr.meandre.components.tools.Names;
import org.seasr.meandre.support.parsers.DataTypeParser;
import org.seasr.meandre.support.text.analytics.ReadabilityMeasure;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.Mode;

/*
 *
 * FKReadabilityMeasure.java
 *
 * Created on  Sep 2, 2009
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

/**
 * This class implements the Flesch Kincaid Readability measure as explained
 * at http://en.wikipedia.org/wiki/Flesch-Kincaid_Readability_Test. The code is
 * based on the work done by Daniel Shiffman at
 * http://www.shiffman.net/teaching/a2z/week1/
 * 
 * @author Anoop Kumar 
 * 
 */

@Component(
		creator = "Anoop. This is modified version of original org.seasr.meandre.components.analytics.text.readability.FleschKincaidReadabilityMeasure developed by Xavier Llor&agrave. Returns hashmap instead of FleschDoc",
		description = "Computes the Flesch Kincaid readability measure as explained at http://en.wikipedia.org/wiki/Flesch-Kincaid_Readability_Test. The code is based on the work done by Daniel Shiffman at http://www.shiffman.net/teaching/a2z/week1/",
		name = "Flesch Kincaid Readability Measure",
		tags = "zotero, text readability, measure",
		mode = Mode.compute,
		baseURL = "meandre://seasr.org/components/fkreadabilitymeasure"
)
public class FKReadabilityMeasure extends AbstractExecutableComponent {

	 //------------------------------ INPUTS ------------------------------------------------------

	@ComponentInput(
			description = "Text content of the url page.",
			name = Names.PORT_TEXT
	)
	protected static final String IN_CONTENT = Names.PORT_TEXT;

    //------------------------------ OUTPUTS -----------------------------------------------------

	@ComponentOutput(
			name = Names.PORT_TOKEN_MAP,
			description = "Readability Measures"
	)
	protected static final String OUT_TOKENS = Names.PORT_TOKEN_MAP;
	
		 
	private static final String FLESCH_KINCAID_WIKIPEDIA_URL = "http://en.wikipedia.org/wiki/Flesch-Kincaid_Readability_Test";

	@Override
	public void disposeCallBack(ComponentContextProperties arg0)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeCallBack(ComponentContext cc) throws Exception {
		String content = DataTypeParser.parseAsString(cc.getDataComponentFromInput(IN_CONTENT))[0];
     	ReadabilityMeasure measure = ReadabilityMeasure.computeFleschReadabilityMeasure(content);
		HashMap<String, String> readabilityMap = new HashMap<String,String>();
		readabilityMap.put("Total Syllables",""+measure.getSyllables());
		readabilityMap.put("Total Words",""+measure.getWords());
		readabilityMap.put("Total Sentences",""+measure.getSentences());
		readabilityMap.put("Flesch Reading Ease Score",""+measure.getReadingEaseScore());
		readabilityMap.put("Flesch Grade Level",""+measure.getGradeLevel());	
		readabilityMap.put("FLESCH_KINCAID_WIKIPEDIA_URL", FLESCH_KINCAID_WIKIPEDIA_URL);
		cc.pushDataComponentToOutput(OUT_TOKENS, readabilityMap);
		
	}

	@Override
	public void initializeCallBack(ComponentContextProperties arg0)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

}

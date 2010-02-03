/*
 * FlowGroup.java
 *
 * Created on 5:05:57 PM
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

import java.util.List;

/**
 * @author akumar03
 *
 */
public class FlowGroup {
	List<Flow> flowList;
	String label;
	String input;
	String output;
	
	public void setFlowList(List<Flow> flowList) {
		this.flowList = flowList; 
	}
	
	public List<Flow> getFlowList() {
		return this.flowList;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	public String getLabel(){
		return this.label;
	}
	
	public void setInput(String input) {
		this.input =input;
	}
	public String getInput(){
		return this.input;
	}
	public void setOutput(String output) {
		this.output = output;
	}
	public String getOutput(){
		return this.output;
	}
}

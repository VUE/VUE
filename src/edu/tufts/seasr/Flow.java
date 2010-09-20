/*
 * Flow.java
 *
 * Created on 4:56:16 PM
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

/**
 * @author akumar03
 *
 */

import java.util.List;

public class Flow {
	List<String> inputList;
	String label;
	String url;
	String uri;
	boolean duplicate =true;
	
	public void setInputList(List<String> inputList) {
		this.inputList = inputList; 
	}
	
	public List<String> getInputList() {
		return this.inputList;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	public String getLabel(){
		return this.label;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl(){
		return this.url;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getUri(){
		return this.uri;
	}
	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}
	public boolean getDuplicate(){
		return this.duplicate;
	}
	public String toString() {
		return getLabel();
	}
}

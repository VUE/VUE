/*
 * MeandreResponse.java
 *
 * Created on 11:16:40 AM
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
public class MeandreResponse {
	List<MeandreItem> meandreItemList;
	
	public void setMeandreItemList(List<MeandreItem> meandreItemList) {
		this.meandreItemList = meandreItemList;
	}
	
	public List<MeandreItem> getMeandreItemList() {
		return this.meandreItemList;
	}
	
}

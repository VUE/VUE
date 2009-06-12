/*
 * MeandreItem.java
 *
 * Created on 11:21:14 AM
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
package edu.tufts.seasr;

/**
 * @author akumar03
 *
 */
public class MeandreItem {
   String meandreUriName;
   String description;
   String meandreUri;
   
   public void setMeandreUriName(String meandreUriName) {
	   this.meandreUriName = meandreUriName;
   }
   
   public String getMeandreUriName() {
	   return this.meandreUriName;
   }
   public void setMeandreUri(String meandreUri) {
	   this.meandreUri = meandreUri;
   }
 
   public String getMeandreUri() {
	   return this.meandreUri;
   }
   
   public void setDescription(String description) {
	   this.description = description;
   }
 
   public String getDescription() {
	   return this.description;
   }
}

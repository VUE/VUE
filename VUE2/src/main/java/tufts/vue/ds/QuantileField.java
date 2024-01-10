/**
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

/**  
 * A Quantile field is created for data columns that contain only numerical values.
 * The values are sorted and divided into 4 groups by default and a field the field 
 * is added for every datarow in the dataset.
 * 
 * 
 * The object extends Field and contains reference to the original field that Quantile field
 * is referencing
 * @author akumar03
 *
 */
package tufts.vue.ds;

public class QuantileField extends Field {
	
	private Field referenceField;
	
	QuantileField(String n, Schema schema,Field referenceField) {
		super(n, schema);
		this.referenceField = referenceField;
	 
	}
	
	public Field getReferenceField() {
		return this.referenceField;
	}

}

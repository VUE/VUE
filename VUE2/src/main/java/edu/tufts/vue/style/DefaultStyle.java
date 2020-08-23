/*
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
 *
 * @author akumar03
 */

package edu.tufts.vue.style;

public class DefaultStyle extends Style {
    public static final String[] DEFAULT_KEYS = { "font-size"};
    public static final String[] DEFAULT_VALUES = { "10px" };
    /** Creates a new instance of DefaultStyle */
    public DefaultStyle(String name){
        this.name = name;
        setDefaultAttributes();
    }
    
    public org.osid.shared.Type getType() {
        return SelectorType.getDefaultType();
    }
    
}


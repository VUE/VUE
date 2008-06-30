/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

public class NodeStyle extends Style implements java.io.Serializable {
    public static final Style DEFAULT_NODE_STYLE = new NodeStyle(new String("Default"));
   
    public static final String[] DEFAULT_KEYS = {"background"};
    public static final String[] DEFAULT_VALUES = {"#FFFFFF"};
    
    /** Creates a new instance of NodeStyle */
    public NodeStyle(String name) {
        this.name = name;
        setDefaultAttributes();
    }
    public org.osid.shared.Type getType() {
        return SelectorType.getNodeType();
    }
    
    
}

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

public class SelectorType extends org.osid.shared.Type{
    public static final SelectorType defaultType = new SelectorType("Default Selector","edu.tufts","default");
    public static final SelectorType link = new SelectorType("Link Selector","edu.tufts","link");
    public static final SelectorType node = new SelectorType("Node Selector","edu.tufts","node");
    public SelectorType(String authority
            , String domain
            , String keyword) {
        super(authority,domain,keyword);
    }
    
    public SelectorType(String authority
            , String domain
            , String keyword
            , String description) {
        super(authority,domain,keyword,description);
    }
    
    public static org.osid.shared.Type getLinkType() {
        return link;
    }
    
    public static org.osid.shared.Type getNodeType() {
        return node;
    }
    public static org.osid.shared.Type getDefaultType() {
        return defaultType;
    }
}

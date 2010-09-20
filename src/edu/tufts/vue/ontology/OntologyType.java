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

package edu.tufts.vue.ontology;


public class OntologyType  extends org.osid.shared.Type{
    public static final org.osid.shared.Type RDFS_TYPE = new OntologyType("RDFS type","edu.tufts","rdfs");
    public static final org.osid.shared.Type OWL_TYPE =  new OntologyType("OWL type","edu.tufts","owl");
    public OntologyType(String authority
            , String domain
            , String keyword) {
        super(authority,domain,keyword);
    }
    public OntologyType(String authority
            , String domain
            , String keyword
            , String description) {
        super(authority,domain,keyword,description);
    }
    
    
}

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
package tufts.vue;

/**
 * Any object that implements this interface and VUE restores
 * via Castor will get called as the below events happen.
 */
public interface XMLUnmarshalListener {

    /** object's has been constructed with call to public no-arg constructor */
    public void XML_initialized(Object context);

    /** all attributes and elements have been processed: the values for this object are set */
    public void XML_completed(Object context);

    /** a child field value has been de-serialized and constructed to be set on/provided to it's parent  */
    public void XML_fieldAdded(Object context, String name, Object child);
    
    /** the object has been added to it's parent (the parent's setter was just called with the child) */
    public void XML_addNotify(Object context, String name, Object parent);
}


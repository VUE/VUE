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

package edu.tufts.osidimpl.repository.sakai;

public class SharedProperties implements org.osid.shared.Properties {
    private java.util.Map map = new java.util.HashMap();
    private org.osid.shared.Type type = new Type("mit.edu", "shared", "empty");

    public SharedProperties() throws org.osid.shared.SharedException {
    }

    public SharedProperties(java.util.Map map, org.osid.shared.Type type)
        throws org.osid.shared.SharedException {
        this.map = map;
        this.type = type;
    }

    public org.osid.shared.ObjectIterator getKeys()
        throws org.osid.shared.SharedException {
        return new ObjectIterator(new java.util.Vector(this.map.keySet()));
    }

    public java.io.Serializable getProperty(java.io.Serializable key)
        throws org.osid.shared.SharedException {
        if (this.map.containsKey(key)) {
            return (java.io.Serializable) this.map.get(key);
        } else {
            throw new org.osid.shared.SharedException(org.osid.shared.SharedException.UNKNOWN_KEY);
        }
    }

    public org.osid.shared.Type getType()
        throws org.osid.shared.SharedException {
        return this.type;
    }
}

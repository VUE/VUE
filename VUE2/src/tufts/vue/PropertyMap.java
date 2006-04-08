 /*
  * -----------------------------------------------------------------------------
  *
  * <p><b>License and Copyright: </b>The contents of this file are subject to the
  * Mozilla Public License Version 1.1 (the "License"); you may not use this file
  * except in compliance with the License. You may obtain a copy of the License
  * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
  *
  * <p>Software distributed under the License is distributed on an "AS IS" basis,
  * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
  * the specific language governing rights and limitations under the License.</p>
  *
  * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
  * Tufts University. All rights reserved.</p>
  *
  * -----------------------------------------------------------------------------
  */


package tufts.vue;

/**
 * A general HashMap for storing property values: e.g., meta-data.
 *
 * @version $Revision: 1.1 $ / $Date: 2006-04-08 23:58:58 $ / $Author: sfraize $
 */

public class PropertyMap extends java.util.HashMap {
    public PropertyMap() {}

    public String getProperty(Object key) {
        Object v = get(key);
        return v == null ? null : v.toString();
    }
    
    public void setProperty(String key, String value) {
        put(key, value);
    }

    public java.util.Properties asProperties() {
        java.util.Properties props = new java.util.Properties();
        // todo: this not totally safe: values may not all be strings
        props.putAll(this);
        return props;
    }
}
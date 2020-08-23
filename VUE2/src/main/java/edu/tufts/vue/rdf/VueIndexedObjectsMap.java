
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

package edu.tufts.vue.rdf;

import java.net.URI;
import java.util.WeakHashMap;

/*
 * VueIndexedObjectsMap.java
 *
 * Created on July 19, 2007, 3:39 PM
 *
 * @author dhelle01
 *
 * @deprecated UNSAFE / INAPPROPRIATE -- WeakHashMap means search results could be GC's before we examine them!
 */
public class VueIndexedObjectsMap {
    
    // this is a static class, do not instantiate
    private VueIndexedObjectsMap() 
    {
    }
    
    // RDFIndex (or this class) should do low level priority
    // garbage collection on currently unused objects
    public static WeakHashMap objs = new WeakHashMap();
    
    public static void setID(URI uri,Object obj)
    {
        objs.put(uri,obj);
       // System.out.println("VueIndexedObjectsMap: " + objs);
    }
    
    public static void clear()
    {
        objs = new WeakHashMap();
    }
    
    public static Object getObjectForID(URI id)
    {
        return objs.get(id);
    }
    
}

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
 * Generic interface for accepting / validating an object in any given scenario.
 *
 * @version $Revision: 1.3 $ / $Date: 2008-06-30 20:52:56 $ / $Author: mike $
 */
public interface Acceptor<T> {

    /** @return true if the given object is considered acceptable  */
    public boolean accept(T o);
}

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

package tufts.vue;


/**
 * Transitional interface for PropertyMap -> MetaMap migration.
 *
 * @version $Revision: 1.2 $ / $Date: 2010-02-03 19:17:40 $ / $Author: mike $
 * @author Scott Fraize
 */

public interface TableBag {

    public javax.swing.table.TableModel getTableModel();

    public int size();
    
    public interface Listener {
        void tableBagChanged(TableBag bag);
    }

    public void addListener(Listener l);
    public void removeListener(Listener l);

    
}

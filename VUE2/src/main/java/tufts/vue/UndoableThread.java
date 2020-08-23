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
 *
 * A thread from which the  UndoManager can track asynchronous model updates.
 *
 * @version $Revision: 1.6 $ / $Date: 2010-02-03 19:17:41 $ / $Author: mike $
 * @author Scott Fraize
 *
 */
public class UndoableThread extends Thread
{
    private Object undoActionMarker;

    public UndoableThread(String name, UndoManager undoManager) {
        super("UndoableThread: " + name);
        setDaemon(true);
        if (undoManager != null)
            undoManager.attachThreadToNextMark(this);
        // it's okay if there was no UndoManger: this happens
        // during map loading.
    }


    // called back by undo manager during attach
    void setMarker(Object o) {
        this.undoActionMarker = o;
    }

    // called by undo manager
    Object getMarker() {
        return this.undoActionMarker;
    }

    public void start() {
        if (false && DEBUG.CASTOR)
            run(); // run synchronously
        else
            super.start();
    }
        

}


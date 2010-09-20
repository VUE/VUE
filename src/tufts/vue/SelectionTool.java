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

import java.lang.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;

// TODO: move as much of the functionality currently in MapViewer into this class -- it
// will make for much cleaner code & architecture, and should make MapViewer much easier
// to maintain.  Lots of refactoring will be needed to do this, including probably a
// bunch of extensions and refactorings in VueTool.  Some MapViewer functionality will
// probably want to go right into VueTool, so that all tools can use it, tho we could
// have most everything subclass SelectionTool if we really want (which should be renamed
// something like GeneralTool/EditTool/MoveTool or somesuch).

public class SelectionTool extends VueTool
{
    public SelectionTool() {
        super();
    }

    @Override
    public void handleToolSelection(boolean selected, VueTool otherTool) {
        super.handleToolSelection(selected, otherTool); // for debug
        if (selected) {
            if (otherTool instanceof ZoomTool) {
                ZoomTool zoomTool = (ZoomTool) otherTool;
                if (zoomTool.getZoomedTo() != null && zoomTool.getZoomedTo() instanceof LWSlide) {
                    final MapViewer viewer = VUE.getActiveViewer();
                    if (viewer == null)
                        return;
                    final LWSlide editingFocal = (LWSlide) zoomTool.getZoomedTo();
                    final LWComponent oldFocal = viewer.getFocal();
                    // not sure if we need oldFocal.
                    // anyway, this is a total hack that will be going away...
                    zoomTool.setEditingFocal(editingFocal, oldFocal);
                    viewer.loadFocal(editingFocal);
                }
            }
        }
    }
    
    /** @return 'a' */
    @Override
    public char getBackwardCompatShortcutKey() {

        // TODO: VueToolbarController / VueTool could use some redesign.  Due to the the
        // current architecture, we need to be sure to only return the backward compat
        // key for the version of this tool that does NOT have a sub tool.  As to why
        // there are multiple versions of the tool instance, it has to do with the
        // original design being closely tied to the implementation of our custom menu
        // bar for the tools, where multiple types of the tool can be grouped under a
        // single pull-down.
        
        if (getSelectedSubTool() == null)
            return 'a';
        else
            return 0;
    }


    static final class Direct extends SelectionTool {
        @Override
        public PickContext initPick(PickContext pc, float x, float y) {
            pc.pickDepth = 1;
            return pc;
        }

        /** undo SelectionTool version */
        @Override public char getBackwardCompatShortcutKey() { return 0; }
        
    }

    static final class Browse extends SelectionTool {

        /** @return false */
        @Override public boolean supportsResizeControls() { return false; }

        /** @return false */
        @Override public boolean supportsDrag(java.awt.event.InputEvent e) { return false; }

        /** @return false */
        @Override public boolean supportsEditLabel() { return false; }

        /** @return true -- will force repaint on tool change so selection handles show/hide */
        @Override public boolean hasDecorations() { return true; }
        
        /** undo SelectionTool version */
        @Override public char getBackwardCompatShortcutKey() { return 0; }

        @Override
        public DrawContext getDrawContext(DrawContext dc) {
            dc.setBrowsing(true);

            return dc;
        }
        

    }
    
    
}

/*
 * LayoutAction.java
 *
 * Created on October 8, 2008, 1:01 PM
 *
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
package tufts.vue;

import javax.swing.KeyStroke;
import edu.tufts.vue.layout.*;
import java.util.*;
import java.awt.event.KeyEvent;
import javax.swing.Action;

// contains layout actions. based on ArrangeAction. The default layout is random layout

public abstract  class LayoutAction extends Actions.LWCAction {
    private Layout layout = new edu.tufts.vue.layout.ListRandomLayout(); 
     
    private LayoutAction(edu.tufts.vue.layout.Layout layout, String name,int keyCode) {
       super(name, KeyStroke.getKeyStroke(keyCode, Actions.COMMAND+Actions.SHIFT));
       this.layout = layout;
        
    }
    
    private LayoutAction(edu.tufts.vue.layout.Layout layout, String name,KeyStroke keyStroke) {
        super(name,keyStroke);
        this.layout = layout;
    }
    boolean mayModifySelection() { return true; }
    
    boolean enabledFor(LWSelection s) {
        return s.size() >= 2
                || (s.size() == 1 && s.first().getParent() instanceof LWSlide); // todo: a have capability check (free-layout?  !isLaidOut() ?)
    }
    
    boolean supportsSingleMover() { return true; }
    
    public void act(List<? extends LWComponent> bag) {
        act(new LWSelection(bag));
    }
    
    void act(LWSelection selection)   {
        try {
            layout.layout(selection);
        } catch(Exception ex) {
            
        }
    }
    // random layout. scatters nodes at random
    public static final LayoutAction random = new LayoutAction(new ListRandomLayout(),"Random",KeyEvent.VK_Q) {
        boolean supportsSingleMover() { return false; }
    };
    public static final LayoutAction table = new LayoutAction(new TabularLayout(),"Table",KeyEvent.VK_W) {
         boolean supportsSingleMover() { return false; }
    };
    public static final LayoutAction circle = new LayoutAction(new CircularLayout(),"Circle",KeyEvent.VK_E) {
        boolean supportsSingleMover() { return false; }
    };
    
    public static final Action[] LAYOUT_ACTIONS = {
        random,
        table,
        circle
    };
}

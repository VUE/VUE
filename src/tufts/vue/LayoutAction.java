/*
 * LayoutAction.java
 *
 * Created on October 8, 2008, 1:01 PM
 *
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
package tufts.vue;

import javax.swing.KeyStroke;
import edu.tufts.vue.layout.*;
import java.util.*;
import java.awt.event.KeyEvent;
import javax.swing.Action;

// contains layout actions. based on ArrangeAction. The default layout is random layout

public abstract  class LayoutAction extends Actions.LWCAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LayoutAction.class);
    private Layout layout = new edu.tufts.vue.layout.ListRandomLayout();
    
    private LayoutAction(edu.tufts.vue.layout.Layout layout, String name,int keyCode) {
        super(name, KeyStroke.getKeyStroke(keyCode, Actions.ALT));
        this.layout = layout;  
    }
    
    private LayoutAction(edu.tufts.vue.layout.Layout layout, String name,KeyStroke keyStroke) {
        super(name,keyStroke);
        this.layout = layout;
    }

    private LayoutAction(edu.tufts.vue.layout.Layout layout, String name) {
        super(name);
        this.layout = layout;
    }

    boolean mayModifySelection() { return true; }
    
    boolean enabledFor(LWSelection s) {
        return s.size() >= 2
                || (s.size() == 1 && s.first().getParent() instanceof LWSlide); // todo: a have capability check (free-layout?  !isLaidOut() ?)
    }
    
    boolean supportsSingleMover() { return true; }
    
    public void act(List<? extends LWComponent> bag) {
        act(bag, false);
    }
    
    public void act(List<? extends LWComponent> bag, boolean autoFit) {
        act(new LWSelection(bag), autoFit);
    }
    
    void act(LWSelection selection) {
    	act(selection, false);
    }
    public void act(LWSelection selection, boolean autoFit) {
        if (DEBUG.Enabled) Log.debug(this + "; autoFit=" + autoFit);
        try {
            layout.layout(selection);
            if (DEBUG.Enabled)Log.debug("autoFit: "+autoFit+" s.size "+selection.size()+" map.size:"+VUE.getActiveMap().getAllDescendents(LWContainer.ChildKind.PROPER).size());
            if (autoFit || (selection.size() == VUE.getActiveMap().getAllDescendents(LWContainer.ChildKind.PROPER).size())) {
            	 ZoomTool.setZoomOutFit();
            }
        } catch(Throwable t) {
            Log.debug("LayoutAction.act: "+t.getMessage());
             tufts.Util.printStackTrace(t);
        }
    }
    // random layout. scatters nodes at random
    // random layout. scatters nodes at random
    public static final LayoutAction random = new LayoutAction(new ListRandomLayout(),VueResources.getString("menu.format.arrange.random"), KeyEvent.VK_6) {
        boolean supportsSingleMover() { return false; }
    };
    public static final LayoutAction table = new LayoutAction(new TabularLayout(),VueResources.getString("menu.format.arrange.table"), KeyEvent.VK_3) {
        boolean supportsSingleMover() { return false; }
    };
    public static final LayoutAction circle = new LayoutAction(new CircularLayout(),VueResources.getString("menu.format.arrange.circle"), KeyEvent.VK_4) {
        boolean supportsSingleMover() { return false; }
    };
    public static final LayoutAction filledCircle = new LayoutAction(new FilledCircularLayout(),VueResources.getString("menu.format.arrange.filledcircle"), KeyEvent.VK_5) {
        boolean supportsSingleMover() { return false; }
    };
    public static final LayoutAction force = new LayoutAction(new ForceLayout(),VueResources.getString("menu.format.layout.force"), KeyEvent.VK_QUOTE) {
        boolean supportsSingleMover() { return false; }
    };
    public static final LayoutAction hierarchical = new LayoutAction(new HierarchicalLayout(),VueResources.getString("menu.format.layout.hierarchical"), KeyEvent.VK_BACK_SLASH) {
        boolean supportsSingleMover() { return false; }
        boolean enabledFor(LWSelection selection ) { return true;}
    };
    
    public static final LayoutAction cluster = new LayoutAction(new ClusterLayout(),VueResources.getString("menu.format.layout.cluster"), KeyEvent.VK_SEMICOLON) {
        boolean supportsSingleMover() { return false; }
        boolean enabledFor(LWSelection selection ) { return true;}
    };
    
     public static final LayoutAction ripple = new LayoutAction(new RippleLayout(),VueResources.getString("menu.format.arrange.ripple"), KeyEvent.VK_7) {
        boolean supportsSingleMover() { return false; }
        boolean enabledFor(LWSelection selection ) { return true;}
    };
    public static final LayoutAction stretch = new LayoutAction(new StretchLayout(),VueResources.getString("menu.format.layout.stretch")) {
        boolean supportsSingleMover() { return false; }
    };
    public static final LayoutAction search= new LayoutAction(new SearchLayout(),VueResources.getString("menu.format.layout.search")) {
      boolean supportsSingleMover() { return false; }
    };
    public static final LayoutAction cluster2= new LayoutAction(new Cluster2Layout(),VueResources.getString("menu.format.arrange.gather"), KeyEvent.VK_8) {
        boolean supportsSingleMover() { return false; }
    };


    public static final Action[] LAYOUT_ACTIONS = {
    	cluster,
        Actions.MakeCluster,
        hierarchical,
        force,
        null,
        Actions.PullInLinked,
        Actions.PushOutLinked,
        null,
        Actions.MakeDataLists,
//         null,
//         Actions.MakeDataLinks
    };

}

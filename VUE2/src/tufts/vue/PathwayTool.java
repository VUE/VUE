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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.*;
import java.util.Iterator;

/**
 * PathwayTool.java
 *
 * MapViewer tool functionality plus pathway contextual toolbar tool.
 * The contextual toolbar maintains a JComboBox that stays synchronized
 * with whatever is in the LWPathwayList of the current map, as well
 * as the currently selected/active pathway in the list.
 *
 * @see LWPathwayList
 * @see LWPathway
 *
 * @author  Scott Fraize
 * @version May 2004
 */
public class PathwayTool extends VueSimpleTool
{
    private static JPanel sControlPanel;

    public PathwayTool() {
        super();
    }
    
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (DEBUG.TOOL) System.out.println("PATHWAYTOOL " + e);
        if (VUE.sMapInspector != null)
            VUE.sMapInspector.showTab("Pathway");
    }


    public boolean supportsSelection() { return true; }

    // todo: need selection, but no drag, and click-to-deselect still working
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return true; }

    public JPanel getContextualPanel() {
        if (sControlPanel == null)
            sControlPanel = new PathwayToolPanel();
        return sControlPanel;
    }

    private static class PathwayComboBoxModel extends DefaultComboBoxModel
        implements VUE.ActiveMapListener, LWComponent.Listener
    {
        LWPathwayList mPathwayList;

        PathwayComboBoxModel() {
            VUE.addActiveMapListener(this);
            activeMapChanged(VUE.getActiveMap());
        }

        // TODO FIX: Active map not changing on tab select if the active mapviewer
        // doesn't have focus, cause then it's not knowing it's LOSING focus --
        // the tab switch is going to have to change the active map itself.
        public void activeMapChanged(LWMap map) {
            if (DEBUG.PATHWAY) System.out.println(this + " map changed to " + map);
            setPathwayList(map);
        }

        private void setPathwayList(LWMap map) {
            LWPathwayList pathwayList;
            if (map == null)
                pathwayList = null;
            else
                pathwayList = map.getPathwayList();
                
            if (mPathwayList != pathwayList) {
                if (mPathwayList != null)
                    mPathwayList.removeListener(this);
                mPathwayList = pathwayList;
                if (mPathwayList != null)
                    mPathwayList.addListener(this);
            }
            rebuildModel();
        }

        public void LWCChanged(LWCEvent e) {
            if (DEBUG.PATHWAY) System.out.println(this + ": " + e);
            if (e.getComponent() instanceof LWPathway) {
                if (e.getWhat() == LWKey.Label)
                    rebuildModel();
                else if (e.getWhat().startsWith("pathway.")) {
                    if (e.getWhat().equals("pathway.create") || e.getWhat().equals("pathway.delete")) {
                        rebuildModel();
                    } else if (e.getWhat().equals("pathway.list.active")) {
                        setSelectedItem(e.getComponent().getDisplayLabel());
                    }
                }
            }
        }

        public void setSelectedItem(Object o) {
            if (DEBUG.PATHWAY) System.out.println(this + " setSelectedItem " + o);
            super.setSelectedItem(o);
            if (mPathwayList != null)
                mPathwayList.setCurrentIndex(getIndexOf(o));
        }

        private void rebuildModel() {
            removeAllElements();
            if (mPathwayList != null) {
                Iterator i = mPathwayList.iterator();
                while (i.hasNext()) {
                    addElement(((LWPathway)i.next()).getDisplayLabel());
                }
            }
        }

        
    }
    private static class  PathwayToolPanel extends JPanel {
        
        private static final Color ToolbarColor = VueResources.getColor("toolbar.background");
        
        public PathwayToolPanel() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            if (false) {
                JLabel label = new JLabel("Pathway playback:  ");
                label.setBorder(new EmptyBorder(3,0,0,0));
                add(label);
            }
            JComboBox combo = new JComboBox(new PathwayComboBoxModel());
            Font f = combo.getFont();
            Font menuFont = new Font(f.getFontName(), f.getStyle(), f.getSize() - 2);
            combo.setFont(menuFont);
            combo.setBackground(VueTheme.getVueColor());
            combo.setFocusable(false);

            // A total hack so the visible height of the combo-box is squeezed down a bit
            // Setting the size only appears to work for the width, not the height.
            combo.setBorder(new MatteBorder(2,0,2,0, ToolbarColor));
            //combo.setBorder(new EmptyBorder(2,0,2,0)); // so height get's squeezed
            //combo.setPreferredSize(new Dimension(150, 18));
            //combo.setSize(new Dimension(150, 18));
            
            add(combo);
            add(Box.createHorizontalStrut(5));
            JPanel controls = new PathwayPanel.PlaybackToolPanel();
            //controls.setBackground(Color.red);
            controls.setOpaque(false); // so we use parents background fill color
            add(controls);
            //add(Box.createHorizontalGlue());
            add(Box.createHorizontalStrut(22));
        }
    };

    
    
}

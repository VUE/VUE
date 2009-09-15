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

import tufts.Util;
import tufts.vue.gui.*;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

/**
 * Digital Repository Browser
 *
 * This single panel abstracts the collective handling of a list of
 * selectable data sources, a search pane to be automatically
 * displayed and updated when searchable data sources are selected,
 * and a browse pane to be automatically populated and displayed when
 * browseable data sources are selected.
 *
 * This code, along with DataSourceViewer, DataSourceList, and
 * DataSourceListCellRenderer, are due for collective refactoring.  It
 * would be best to do away with the JList / renderer impl, which adds
 * much complexity, and doesn't get us much: we're unlikely to ever
 * have so many data sources that the GUI performance gain of not
 * having full-time swing components present for each data source line
 * is worth it, and without having to go through a ListCellRenderer,
 * we'd have much more flexibility in layout out, drawing and
 * repainting the data sources (e.g., we could add spinners to loading
 * data sources -- that's wouldn't possible now without adding a
 * special timer thread to repaint the entire least each time the
 * spinner wanted to draw the next image.)
 *
 * Also, it would be very handy to have a single interface or class
 * that abstracts BOTH types of data-sources: OSID's (edu.tufts.vue.dsm.DataSource),
 * and browseable VUE ("old-style") data sources (tufts.vue.DataSource).
 * We'd probably need a delegating impl tho to handle that.
 *
 * @version $Revision: 1.85 $ / $Date: 2009-09-15 22:59:58 $ / $Author: brian $ 
 */
public class DRBrowser extends ContentBrowser
{
	public static final long	serialVersionUID = 1;
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DRBrowser.class);

    public static final Object SEARCH_EDITOR = "search_editor_layout_constraint";
    public static final Object SEARCH_RESULT = "search_result_layout_constraint";

    final JComponent searchPane = new Widget(VueResources.getString("action.search")) {
            private Component	editor,
            					result;

            {
                setOpaque(false);

                // This is to keep the content window from coming to the front when the
                // data sources have loaded and the searchPane is expanded.
                Widget.setLoading(this, true);
            }

            protected void addImpl(Component c, Object constraints, int idx) {
                if (DEBUG.DR) out("SEARCH-WIDGET addImpl: " + GUI.name(c) + " " + constraints + " idx=" + idx);

                JComponent jc = null;

                if (c instanceof JComponent)
                    jc = (JComponent) c;
                if (constraints == SEARCH_EDITOR) {
                    if (editor != null)
                        remove(editor);

                    editor = c;
                    constraints = BorderLayout.NORTH;

                    if (jc != null)
                        jc.setBorder(GUI.WidgetInsetBorder);
                } else if (constraints == SEARCH_RESULT) {
                    // this method of setting this is a crazy hack for now, but
                    // it's perfect for allowing us to try different layouts
                    resultsPane.removeAll();
                    resultsPane.add(jc);
                    resultsPane.setHidden(false);
                    resultsPane.validate();

                    return;
                } else {
                    tufts.Util.printStackTrace("illegal search pane constraints: " + constraints);
                }

                super.addImpl(c, constraints, idx);
                revalidate();
            }
        };

    final JPanel librariesPane = new Widget(VueResources.getString("drbrowser.resources"));
    final Widget browsePane = new Widget(VueResources.getString("button.browse.label"));
    final Widget resultsPane = new Widget(VueResources.getString("jlabel.searchresult"));

    final DockWindow dockWindow;

    private JLabel loadingLabel;

    private DataSourceViewer DSV;

    public DRBrowser(boolean delayedLoading, DockWindow resourceDock)
    {
        super(new BorderLayout());

        setName(VueResources.getString("dockWindow.contentPanel.resources.title"));

        if (DEBUG.DR || DEBUG.INIT) out("Creating DRBrowser");

        dockWindow = resourceDock;

        //-----------------------------------------------------------------------------
        // Resources
        //-----------------------------------------------------------------------------

        if (delayedLoading) {
            
//          if (Util.isMacLeopard()) {
//              JProgressBar bar = new JProgressBar();
//              bar.setAlignmentX(SwingConstants.CENTER); // no effect
//              bar.setIndeterminate(true);
//              bar.putClientProperty("JProgressBar.style", "circular");
//              bar.setString("Loading...");// no effect
//              bar.setStringPainted(true); // no effect
//              add(bar);
//          }

             loadingLabel = new JLabel(VueResources.getString("dockWindow.Resources.loading.label"), SwingConstants.CENTER);
             loadingLabel.setMinimumSize(new Dimension(150, 80));
             loadingLabel.setBorder(new EmptyBorder(8,0,8,0));
             GUI.apply(GUI.StatusFace, loadingLabel);
             librariesPane.add(loadingLabel);

         } else {
             loadDataSourceViewer();
         }

        //-----------------------------------------------------------------------------
        // Search
        //-----------------------------------------------------------------------------

        searchPane.setBackground(Color.white);
        JLabel please = new JLabel(VueResources.getString("jlabel.searchableresource"), JLabel.CENTER);
        GUI.apply(GUI.StatusFace, please);
        searchPane.add(please, SEARCH_EDITOR);

        //-----------------------------------------------------------------------------
        // Browse
        //-----------------------------------------------------------------------------

        browsePane.setBackground(Color.white);
        browsePane.setExpanded(false);
        browsePane.setLayout(new BorderLayout());

        //-----------------------------------------------------------------------------
        // Results
        //-----------------------------------------------------------------------------

        resultsPane.setTitleHidden(true);
        resultsPane.setHidden(true);

        WidgetStack stack = new WidgetStack(getName());

        Widget.setWantsScroller(stack, false);
        Widget.setWantsScrollerAlways(stack, false);

        stack.addPane(librariesPane, 0f);
        stack.addPane(searchPane, 0f);
        stack.addPane(browsePane, 1f);
        stack.addPane(resultsPane, 0f);

        add(stack);
    }

    public DataSourceViewer getDataSourceViewer()
    {
    	return DSV;
    }
    
    public void loadDataSourceViewer()
    {
        Log.debug("loading the DataSourceViewer...");

        try {
            DSV = new DataSourceViewer(this);
            DSV.setName("Data Source Viewer");

            if (loadingLabel != null)
                librariesPane.remove(loadingLabel);

            librariesPane.add(DSV);

            // must do this to get re-laid out: apparently, the hierarchy
            // events from the add don't automatically do this!
            revalidate();
        } catch (Throwable e) {
            Log.error(e);
            e.printStackTrace();
            loadingLabel.setText(e.toString());
        }

        // Done loading, so Content window should now come to front when searchPane is expanded.
        Widget.setLoading(searchPane, false);

        if (DEBUG.DR || DEBUG.Enabled) out("done loading DataSourceViewer");
    }

    private static void out(String s) {
        //System.out.println("DRBrowser: " + s);
        Log.info(s);
    }
}

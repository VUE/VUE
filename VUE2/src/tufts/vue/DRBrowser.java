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

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import tufts.vue.gui.*;

/**
 * Digital Repository Browser
 *
 * @version $Revision: 1.36 $ / $Date: 2006-02-17 20:24:57 $ / $Author: jeff $ 
 */
public class DRBrowser extends JPanel {
    public static DataSourceViewer dsViewer = null;
	public static tufts.vue.gui.DockWindow dockWindow = null;

    private static final int DRBrowserWidth = 319;

    private JLabel loadingLabel;
   
    public DRBrowser(boolean delayedLoading,
					 DockWindow searchDWindow,
					 DockWindow browseDWindow,
					 DockWindow savedResourcesDWindow,
					 DockWindow previewDWindow)
    {
        Dimension startSize = new Dimension(400,240);
        // todo: move this size setting to VUE app init
        //Dimension startSize = new Dimension(tufts.vue.gui.GUI.isSmallScreen() ? 250 : 400,
        //                                    tufts.vue.gui.DockWindow.getMaxContentHeight());
        
        setLayout(new BorderLayout());
        setPreferredSize(startSize);
        
        if (delayedLoading) {
            //setMinimumSize(new Dimension(DRBrowserWidth,100));
            loadingLabel = new JLabel("Loading data sources...");
            loadingLabel.setBorder(new EmptyBorder(22,22,22,22));
            add(loadingLabel, BorderLayout.NORTH);
        } else {
            loadDataSourceViewer(searchDWindow,
								 browseDWindow,
								 savedResourcesDWindow,
								 previewDWindow);
        }
    }
    
    public DRBrowser() {
        this(false,
			 null,
			 null,
			 null,
			 null);
    }
    
	public void setDockWindow(tufts.vue.gui.DockWindow dWindow)
	{
		dockWindow = dWindow;
	}
	
    public void loadDataSourceViewer(DockWindow searchDWindow,
									 DockWindow browseDWindow,
									 DockWindow savedResourcesDWindow,
									 DockWindow previewDWindow)
    {
        try {
            DataSourceViewer dsv = new DataSourceViewer(this,
														searchDWindow,
														browseDWindow,
														savedResourcesDWindow,
														previewDWindow);
            dsv.setName("Data Source Viewer"); 
            if (dsViewer == null) {
                // set the statics to the first initialized DRBrowser only
                dsViewer = dsv;
                //tufts.vue.VUE.dataSourceViewer = dsv;
            }
            if (loadingLabel != null)
                remove(loadingLabel);
            //setMinimumSize(null); some data-sources smaller: don't allow shrinkage
            add(dsv, BorderLayout.NORTH);
            validate();
        } catch (Exception e) {
            e.printStackTrace();
            loadingLabel.setText(e.toString());
        }
    }

/*
	public static void main(String args[]) {
        DEBUG.DR = true;
        DRBrowser drb = new DRBrowser(false);
        tufts.Util.displayComponent(drb);
        try {
            java.util.prefs.Preferences p = tufts.oki.dr.fedora.FedoraUtils.getDefaultPreferences(null);
            p.exportSubtree(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //drb.loadDataSourceViewer();
        
    }

*/
       
    
}

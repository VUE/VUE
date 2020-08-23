/*
 * RelationalDataset.java
 *
 * Created on July 23, 2008, 6:06 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2008
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.dataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import tufts.vue.DataSetViewer;
import tufts.vue.DataSourceViewer;
import tufts.vue.LWComponent;
import tufts.vue.LWLink;
import tufts.vue.LWMap;
import tufts.vue.LWNode;
import tufts.vue.LWSelection;
import tufts.vue.LayoutAction;
import tufts.vue.VUE;
import tufts.vue.ds.DataAction;
import tufts.vue.ds.XmlDataSource;

public class RelationalDataset extends Dataset {
    
    /** Creates a new instance of RelationalDataset */
    public RelationalDataset() {
    }
    public LWMap createMap() throws Exception{
    	String mapName = getMapName(fileName);
        datasource = new XmlDataSource(mapName,getFileName());
        Properties props = new Properties();
 		props.put("displayName", mapName);
 		props.put("name", mapName);
 		props.put("address", getFileName());
 		datasource.setConfiguration(props);
 		VUE.getContentDock().setVisible(true);
 		VUE.getContentPanel().showDatasetsTab();
 		DataSetViewer.getDataSetList().addOrdered(datasource);
 		VUE.getContentPanel().getDSBrowser().getDataSetViewer().setActiveDataSource(datasource);			
 		DataSourceViewer.saveDataSourceViewer();	
 		List<LWComponent> nodes =  DataAction.makeRowNodes(datasource.getSchema());
 		LWMap map  = new LWMap(getMapName(fileName));
        for(LWComponent component: nodes) {
            map.add(component);
        }
       
        LayoutAction.random.act(new LWSelection(nodes));
        return map;
         
    }
    
}

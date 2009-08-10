package edu.tufts.vue.layout;

import edu.tufts.vue.dataset.Dataset;
import tufts.vue.LWMap;
import tufts.vue.LWSelection;

public class HierarchicalLayout extends Layout{

    public   LWMap createMap(Dataset ds,String mapName) throws Exception {
        LWMap map = new LWMap(mapName);
        return map;
    }
    
    public   void layout(LWSelection selection) throws Exception {

    	// Empty method awaiting implementation

    }
}

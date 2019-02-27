/*
 * ListDataset.java
 *
 * Created on July 23, 2008, 6:06 PM
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
package edu.tufts.vue.dataset;
import java.util.*;
import java.io.*;
import tufts.vue.*;
import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.metadata.VueMetadataElement;


public class FolderDataset  extends Dataset {
	 public static String DEFAULT_METADATA_LABEL = "default";
	    
    /** Creates a new instance of ListDataset */
    public FolderDataset() {
    }
    public  void loadDataset() throws Exception{
         
      
    }
    
    
    public LWMap createMap() throws Exception{
    	Map<String, LWNode> nodeMap = new HashMap<String, LWNode>();
    	  ArrayList<String> heading = new ArrayList<String>();
          heading.add("label");
          heading.add("resource");
          setHeading(heading);
          LWMap map  = new LWMap(getMapName(fileName));
          File file = new File(fileName);
          if(!file.isDirectory()) throw new Exception("FolderDataset.loadDataset: The file " + fileName +" is not a folder");
          File[] children = file.listFiles();
          List<LWComponent> nodeList = new ArrayList<LWComponent>();
          for(int i =0;i<children.length;i++) { 
              String nodeLabel = children[i].getName();
              
//               node.setLabel(nodeLabel);
                  String key = ((getHeading() == null) || getHeading().size() < 1) ? DEFAULT_METADATA_LABEL : getHeading().get(1);
                  
 
                      Resource resource = map.getResourceFactory().get(children[i]);
                      LWNode node = new LWNode(nodeLabel);
                      node.setResource(resource);
                      
                      
                      VueMetadataElement vm = new VueMetadataElement();
                      vm.setKey(key);
                      vm.setValue(children[i].getAbsolutePath());
                      vm.setType(VueMetadataElement.CATEGORY);
                      node.getMetadataList().addElement(vm);
                      
                  node.layout();
                  map.add(node);
                  nodeList.add(node);
          }
          LayoutAction.random.act(new LWSelection(nodeList));
        return  map;
    }
}

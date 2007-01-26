
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

/*
 * LWMergeMap.java
 *
 * Created on January 24, 2007, 1:38 PM
 *
 * @author dhelle01
 *
 */

package tufts.vue;

import edu.tufts.vue.compare.ConnectivityMatrix;
import edu.tufts.vue.compare.VoteAggregate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LWMergeMap extends LWMap {
    
    private static int maps = 0;
    List<LWMap> mapList = new ArrayList();
    private String selectionText;
    private LWMap referenceMap;
    
    public static String getTitle()
    {
        return "Merge Map #" + (++maps);
    }
    
    public LWMergeMap()
    {
        super();
    }
    
    public LWMergeMap(String label)
    {
        super(label);
    }
    
    public void addMap(LWMap map)
    {
        mapList.add(map);
    }
    
    public void addMaps(Iterator<LWMap> i)
    {
        while(i.hasNext())
        {    
          mapList.add((LWMap)i.next());
        }
    }
    
    public void clearMaps()
    {
        mapList.clear();
    }
    
    public String getSelectionText()
    {
        return selectionText;
    }
    
    public void setSelectionText(String text)
    {
        selectionText = text;
    }
    
    public LWMap getBaseMap()
    {
        return referenceMap;
    }
    
    public void setBaseMap(LWMap baseMap)
    {
        referenceMap = baseMap;
    }
    
    public void mergeMaps()
    {
        ArrayList<ConnectivityMatrix> cms = new ArrayList();
        Iterator<LWMap> i = mapList.iterator();
        while(i.hasNext())
        {
          cms.add(new ConnectivityMatrix(i.next()));
        }
        VoteAggregate voteAggregate= new VoteAggregate(cms);
        
        //compute and create nodes in Merge Map
        Iterator children = getBaseMap().getNodeIterator();
        while(children.hasNext()) {
           LWComponent comp = (LWComponent)children.next();
           if(voteAggregate.isNodeVoteAboveThreshold(comp.getLabel())) {
                   LWNode node = (LWNode)comp.duplicate();
                   this.addNode(node);
           }
        }
        
        //compute and create links in Merge Map
        Iterator children1 = this.getNodeIterator();
        while(children1.hasNext()) {
           LWNode node1 = (LWNode)children1.next();
           Iterator children2 = this.getNodeIterator();
           while(children2.hasNext()) {
               LWNode node2 = (LWNode)children2.next();
               if(node2 != node1) {
                  int c = voteAggregate.getConnection(node1.getLabel(),node2.getLabel());
                  if(c >0) {
                     this.addLink(new LWLink(node1,node2));
                  }
               }
           }
        }
        
    }
}

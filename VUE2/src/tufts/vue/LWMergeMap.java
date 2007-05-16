
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

import edu.tufts.vue.compare.*;
import edu.tufts.vue.style.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class LWMergeMap extends LWMap {
    
    public static final int THRESHOLD_DEFAULT = 50;
    
    private static int numberOfMaps = 0;
    
    private int mapListSelectionType;
    private int baseMapSelectionType;
    private int visualizationSelectionType;
    
    private String selectionText;
    private LWMap baseMap;
    private String selectChoice;
    private int nodeThresholdSliderValue = THRESHOLD_DEFAULT;
    private int linkThresholdSliderValue = THRESHOLD_DEFAULT;
    private boolean filterOnBaseMap;
    private List<File> fileList;
    private List<Boolean> activeFiles;
    
    // without this next line it seems that Castor library only reads back one element..
    // however, for now not a big deal as
    // this list will still be used as a failsafe (and for dynamic behavior?)
    // but will only persist as a failsafe for now.. (users can dig into the file
    // if not yet saved to get their temporary/since modified data)
    private List<LWMap> mapList = new ArrayList<LWMap>();
    
    private File baseMapFile;
    
    private String styleFile;
    
    private List<Double> intervalBoundaries;
    
    public static String getTitle()
    {
        return "Merge Map" + (++numberOfMaps); //+ "*";
    }
    
    public String getLabel()
    {
        return super.getLabel() + "*";
    }
    
    public LWMergeMap()
    {
        super(); 
    }
    
    public LWMergeMap(String label)
    {
        super(label);
    }
    
    public void setMapFileList(List<File> mapList)
    {
        fileList = mapList;
    }
    
    public List<File> getMapFileList()
    {
        return fileList;
    }
    
    public void setMapListSelectionType(int choice)
    {
        mapListSelectionType = choice;
    }
    
    public int getMapListSelectionType()
    {
        return mapListSelectionType;
    }
    
    public void setActiveMapList(List<Boolean> activeMapList)
    {
        activeFiles = activeMapList;
    }
    
    public List<Boolean> getActiveFileList()
    {
        return activeFiles;
    }
    
    public void setMapList(List<LWMap> mapList)
    {
        this.mapList = mapList; 
    }
    
    public List<LWMap> getMapList()
    {
        return mapList;
    }
    
    public String getSelectionText()
    {
        return selectionText;
    }
    
    public void setSelectionText(String text)
    {
        selectionText = text;
    }
    
    public String getSelectChoice()
    {
        return selectChoice;
    }
    
    public void setSelectChoice(String choice)
    {
        selectChoice = choice;
    }
    
    public void setVisualizationSelectionType(int choice)
    {
        visualizationSelectionType = choice;
    }
    
    public int getVisualizationSelectionType()
    {
        return visualizationSelectionType;
    }
    
    public void setFilterOnBaseMap(boolean doFilter)
    {
        filterOnBaseMap = doFilter;
    }
    
    public boolean getFilterOnBaseMap()
    {
        return filterOnBaseMap;
    }
    
    public void setNodeThresholdSliderValue(int value)
    {
        nodeThresholdSliderValue = value;
    }
    
    public int getNodeThresholdSliderValue()
    {
        return nodeThresholdSliderValue;
    }
    
    public void setLinkThresholdSliderValue(int value)
    {
        linkThresholdSliderValue = value;
    }
    
    public int getLinkThresholdSliderValue()
    {
        return linkThresholdSliderValue;
    }
    
    public void setBaseMapSelectionType(int choice)
    {
        baseMapSelectionType = choice;
    }
    
    public int getBaseMapSelectionType()
    {
        return baseMapSelectionType;
    }
    
    public LWMap getBaseMap()
    {
        return baseMap;
    }
    
    public void setBaseMap(LWMap baseMap)
    {
        this.baseMap = baseMap;
    }
    
    public File getBaseMapFile()
    {
        return baseMapFile;
    }
    
    public void setBaseMapFile(File file)
    {
        baseMapFile = file;
    }
    
    public void setStyleMapFile(String file)
    {
        styleFile = file;
    }
    
    public String getStyleMapFile()
    {
        return styleFile;
    }
    
    public void clearAllElements()
    {
        
       // this deletion code is copied from (an old version of?) LWComponent code fragment
       // todo: see if other code can be leveraged through public or protected method.
       // (probably go back and look at Action code to find the proper methodology
        
        Iterator li = getAllDescendents().iterator();
                
        while(li.hasNext())
        {
            LWComponent c = (LWComponent)li.next();
            
            
            LWContainer parent = c.getParent();
            if (parent == null) {
                //System.out.println("DELETE: " + c + " skipping: null parent (already deleted)");
            } else if (c.isDeleted()) {
                //System.out.println("DELETE: " + c + " skipping (already deleted)");
            } else if (parent.isDeleted()) { // after prior check, this case should be impossible now
                //System.out.println("DELETE: " + c + " skipping (parent already deleted)"); // parent will call deleteChildPermanently
            } else if (parent.isSelected()) { // if parent selected, it will delete it's children
                //System.out.println("DELETE: " + c + " skipping - parent selected & will be deleting");
            } else {
                parent.deleteChildPermanently(c);
            }
        }
    }
    
    
    public void addMergeNodesFromSourceMap(LWMap map,VoteAggregate voteAggregate)
    {
           Iterator children = map.getNodeIterator();    
           while(children.hasNext()) {
             LWNode comp = (LWNode)children.next();
             boolean repeat = false;
             //if(map.findByID(comp.getChildList(),Util.getMergeProperty(comp)) != null)
             if(nodeAlreadyPresent(comp))
             {
               repeat = true;
             }
             
             if(voteAggregate.isNodeVoteAboveThreshold(Util.getMergeProperty(comp)) ){
                   LWNode node = (LWNode)comp.duplicate();
                   if(!repeat)
                   {
                     addNode(node);
                   }
             }         
             
           }
    }

    
    public void fillAsVoteMerge()
    {
        ArrayList<ConnectivityMatrix> cms = new ArrayList<ConnectivityMatrix>();
        
        // why not map.getMapList()? is something wrong here?... 3/15/2007-- lets try it
        // (beware the ides of march!)
        Iterator<LWMap> i = getMapList().iterator(); // /*map.getMapList()*/mapList.iterator();
        while(i.hasNext())
        {
          cms.add(new ConnectivityMatrix(i.next()));
        }
        VoteAggregate voteAggregate= new VoteAggregate(cms);
        
        
        voteAggregate.setNodeThreshold((double)getNodeThresholdSliderValue()/100.0);
        voteAggregate.setLinkThreshold((double)getLinkThresholdSliderValue()/100.0);
        
        //compute and create nodes in Merge Map
        
        addMergeNodesFromSourceMap(baseMap,voteAggregate);
        
        if(!getFilterOnBaseMap())
        {
          Iterator<LWMap> maps = getMapList().iterator();
          while(maps.hasNext())
          {
            LWMap m = maps.next();
            if(m!=baseMap)
            {
                addMergeNodesFromSourceMap(m,voteAggregate);
            }
          }
        }

        
        //compute and create links in Merge Map
        Iterator children1 = getNodeIterator();
        while(children1.hasNext()) {
           LWNode node1 = (LWNode)children1.next();
           Iterator children2 = getNodeIterator();
           while(children2.hasNext()) {
               LWNode node2 = (LWNode)children2.next();
               if(node2 != node1) {
                  boolean addLink = voteAggregate.isLinkVoteAboveThreshold(Util.getMergeProperty(node1),Util.getMergeProperty(node2));
                  if(addLink) {
                     addLink(new LWLink(node1,node2));
                  }
               }
           }
        }
        
    }

    
    // todo: change to-- reFillAsVoteMerge
    public void recreateVoteMerge()
    {
        
        clearAllElements();
        
        fillAsVoteMerge();    
    }
    
    
    
    public boolean nodeAlreadyPresent(LWNode node)
    {
        
        if(getFilterOnBaseMap())
        {
            return false;
        }
        
        Iterator<LWComponent> i = getChildList().iterator();
        while(i.hasNext())
        {
            LWComponent c = i.next();
            if(c!=null && node!=null)
            {    
              if(Util.getMergeProperty(node) != null && Util.getMergeProperty(c) != null )
              {    
                if(Util.getMergeProperty(node).equals(Util.getMergeProperty(c)))
                {
                  return true;
                }
              }
              else
              {
                  System.out.println("LWMergeMap: nodeAlreadyPresent, merge property is null for " + node + " or " + c );
                  System.out.println("node: " + Util.getMergeProperty(node) + "c: (current) " + Util.getMergeProperty(c));
              }
            }
            else
            {
                System.out.println("LWMergeMap-nodeAlreadyPresent: node or c is null: (node,c) (" + node + "," + c + ")" );
            }
        }
        return false;
    }
    
        
    public void setIntervalBoundaries()
    {
        intervalBoundaries = new ArrayList<Double>();
        for(int vai = 0;vai<6;vai++)
        {
            double va =  20*vai + 0.5;
            intervalBoundaries.add(new Double(va));
        } 
    }
    
    public int getInterval(double score)
    {
        Iterator<Double> i = intervalBoundaries.iterator();
        int count = 0;
        while(i.hasNext())
        {
            if(score < i.next().doubleValue())
                return count;
            count ++;
        }
        return 0;
    }
    
    
    public void addMergeNodesForMap(LWMap map,WeightAggregate weightAggregate,List<Style> styles)
    {       
         
           Iterator children = map.getNodeIterator();
           //Iterator children = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
           
           while(children.hasNext()) {
             LWNode comp = (LWNode)children.next();
             boolean repeat = false;
             //if(map.findByID(comp.getChildList(),Util.getMergeProperty(comp)) != null)
             if(nodeAlreadyPresent(comp))
             {
               repeat = true;
             }
             LWNode node = (LWNode)comp.duplicate();
             
             
             if(!repeat)
             {    
               addNode(node);
             }     
             
           }
    }
        
    public void fillAsWeightMerge()
    {
    
        ArrayList<ConnectivityMatrix> cms = new ArrayList<ConnectivityMatrix>();
        Iterator<LWMap> i = getMapList().iterator();
        while(i.hasNext())
        {
          cms.add(new ConnectivityMatrix(i.next()));
        }
        
        ArrayList<Style> nodeStyles = new ArrayList<Style>();
        ArrayList<Style> linkStyles = new ArrayList<Style>();
        
        for(int si=0;si<5;si++)
        {
            nodeStyles.add(StyleMap.getStyle("node.w" + (si +1)));
        }
        
        for(int lsi=0;lsi<5;lsi++)
        {
            linkStyles.add(StyleMap.getStyle("link.w" + (lsi +1)));
        }
        
        WeightAggregate weightAggregate = new WeightAggregate(cms);
        
        addMergeNodesForMap(getBaseMap(),weightAggregate,nodeStyles);
        
        if(!getFilterOnBaseMap())
        {
          Iterator<LWMap> maps = getMapList().iterator();
          while(maps.hasNext())
          {
            LWMap m = maps.next();
            if(m!=baseMap)
            {
                addMergeNodesForMap(m,weightAggregate,nodeStyles);
            }
          }
        }
        
        // todo: use applyCSS(style) -- need to plug in formatting panel
        Iterator children = getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        
        while(children.hasNext())
        {
             LWComponent comp = (LWComponent)children.next();
             if(comp instanceof LWNode)
             {
                  LWNode node = (LWNode)comp;
                  double score = 100*weightAggregate.getNodeCount(Util.getMergeProperty(node))/weightAggregate.getCount();
                  if(score>100)
                  {
                    score = 100;
                  }
                  if(score<0)
                  {
                    score = 0;
                  }
                  Style currStyle = nodeStyles.get(getInterval(score)-1);
                  node.setFillColor(Style.hexToColor(currStyle.getAttribute("background")));
             }
        }
       
        //compute and create links in Merge Map
        //Iterator children1 = map.getNodeIterator();
        //need case for LWLink as well for sub links?
        Iterator<LWComponent> children1 = getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        while(children1.hasNext()) {
           LWComponent comp1 = children1.next();
           if(comp1 instanceof LWImage)
               continue;
           LWNode node1 = (LWNode)comp1;
           Iterator children2 = getNodeIterator();
           while(children2.hasNext()) {
               LWNode node2 = (LWNode)children2.next();
               if(node2 != node1) {
                  int c = weightAggregate.getConnection(Util.getMergeProperty(node1),Util.getMergeProperty(node2));
                  
                  //$
                    // don't know if link already drawn.. need to keep track or explicitly check for link..
                    //int c2 = weightAggregate.getConnection(Util.getMergeProperty(node2),Util.getMergeProperty(node2));
                  //$
                  
                  if(c >0) {
                    double score = 100*c/weightAggregate.getCount();
                    if(score > 100)
                    {
                        score = 100;
                    }
                    if(score < 0)
                    {
                        score = 0;
                    }
                    Style currLinkStyle = linkStyles.get(getInterval(score)-1);
                    //System.out.println("Weighted Merge Demo: " + currLinkStyle + " score: " + score);
                    LWLink link = new LWLink(node1,node2);
                    link.setStrokeColor(Style.hexToColor(currLinkStyle.getAttribute("background")));
                    //also add label to link if present? (will be nonunique perhaps.. might make sense for voting?)
                    addLink(link);
                  }
               }
           }
        }
    }

        
}

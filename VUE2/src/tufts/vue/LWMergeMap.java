
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class LWMergeMap extends LWMap {
    
    public static final int THRESHOLD_DEFAULT = 20;
    public static final boolean RECORD_SOURCE_NODES = false;
    
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
    
    private List<String> fileList = new ArrayList<String>();
    private List<Boolean> activeFiles = new ArrayList<Boolean>();
 
    //todo: recover only file names back into the GUI
    // this list will continue to persist to provide
    // a record of actual map data used for merge
    // (will be overwritten on re-save of file as merge
    // will be recalculated on GUI load)
    // note: this initializer seems to be required for Castor
    // libary to actually persist the list.
    /**
     *
     * Actual maps used to generate the most recent merge.
     *
     **/
    private List<LWMap> mapList = new ArrayList<LWMap>();
    
    private File baseMapFile;
    
    private String styleFile;

    private List<Double> intervalBoundaries = new ArrayList<Double>();
    
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

    /**
     *
     * No guarantee that these filenames
     * were generated from *saved* maps
     * todo: document here the current behavior of
     * VUE in this case.
     * GUI should correctly handle any current or
     * future nulls or otherwise invalid file names
     * that might be stored here.
     *
     **/ 
    public void setMapFileList(List<String> mapList)
    {
        if(mapList != null)
          fileList = mapList;
    }
    
    public List<String> getMapFileList()
    {
        return fileList;
    }
    
    /**
     *
     * no longer relevant. This was for old gui with
     * separation between options to load from file
     * or from all open maps
     * todo: deprecate and/or remove
     *
     **/
    public void setMapListSelectionType(int choice)
    {
        mapListSelectionType = choice;
    }
    
    /**
     *
     * no longer relevant. This was for old gui with
     * separation between options to load from file
     * or from all open maps
     * todo: deprecate and/or remove
     *
     **/
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
    
    /*
    public String getSelectionText()
    {
        return selectionText;
    }
    
    public void setSelectionText(String text)
    {
        selectionText = text;
    } */
    
    // todo: deprecate and/or remove
    // this was for old GUI
    public String getSelectChoice()
    {
        return selectChoice;
    }
    
    // todo: deprecate and/or remove
    // this was for old GUI
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
    
    // todo: deprecate and/or remove
    // this was for old GUI
    public void setBaseMapSelectionType(int choice)
    {
        baseMapSelectionType = choice;
    }
    
    // todo: deprecate and/or remove
    // this was for old GUI
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
    
    /**
     *
     * todo: rename no-arg version below as "default" setup method.
     * This method and the next are for persistence 
     *
     **/
    public void setIntervalBoundaries(List<Double> intervalBoundaries)
    {
        this.intervalBoundaries = intervalBoundaries;
    }
    
    public List<Double> getIntervalBoundaries()
    {
        return intervalBoundaries;
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
        
       // this deletion code was copied from an old version of LWComponent deletion code
       // todo: probably should sync this up with whatever the menu action does.
        
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
    
    
    public void addMergeNodesFromSourceMap(LWMap map,VoteAggregate voteAggregate,HashMap<String,LWNode> nodes)
    {
           Iterator children = map.getNodeIterator();    
           while(children.hasNext()) {
             LWNode comp = (LWNode)children.next();
             boolean repeat = false;
             if(nodeAlreadyPresent(comp))
             {
               repeat = true;
               if(RECORD_SOURCE_NODES)
               {
                 LWNode node = nodes.get(Util.getMergeProperty(comp));
                 if(node.getNotes() !=null)
                 {
                   node.setNotes(node.getNotes()+"\n" + map.getLabel());
                 }
                 else
                 {
                   node.setNotes(map.getLabel());
                 }
               }
             }
             
             if(voteAggregate.isNodeVoteAboveThreshold(Util.getMergeProperty(comp)) ){
                   LWNode node = (LWNode)comp.duplicate();
                   if(!repeat)
                   {
                     if(RECORD_SOURCE_NODES)
                     {
                       if(node.getNotes() !=null)
                       {
                         node.setNotes(node.getNotes()+"\n" + map.getLabel());
                       }
                       else
                       {
                         node.setNotes(map.getLabel());
                       }
                       nodes.put(Util.getMergeProperty(comp),node);
                     }
                     addNode(node);
                   }
             }         
             
           }
    }

    
    public void fillAsVoteMerge()
    {
        
        HashMap<String,LWNode> nodes = new HashMap<String,LWNode>();
        
        ArrayList<ConnectivityMatrix> cms = new ArrayList<ConnectivityMatrix>();
        
        Iterator<LWMap> i = getMapList().iterator();
        Iterator<Boolean> ci = null; 
        if(getActiveFileList()!=null)
            ci = getActiveFileList().iterator();
        while(i.hasNext())
        {
          Boolean b = Boolean.TRUE;
          if(ci!=null && ci.hasNext())
          {    
             b = ci.next();
          }
          if(b.booleanValue())
            cms.add(new ConnectivityMatrix(i.next()));
        }
        VoteAggregate voteAggregate= new VoteAggregate(cms);
        
        voteAggregate.setNodeThreshold((double)getNodeThresholdSliderValue()/100.0);
        voteAggregate.setLinkThreshold((double)getLinkThresholdSliderValue()/100.0);
        
        //compute and create nodes in Merge Map
        
        addMergeNodesFromSourceMap(baseMap,voteAggregate,nodes);
        
        if(!getFilterOnBaseMap())
        {
          Iterator<LWMap> maps = getMapList().iterator();
          ci = null; 
          if(getActiveFileList()!=null)
            ci = getActiveFileList().iterator();
          while(maps.hasNext())
          {
            LWMap m = maps.next();
            Boolean b = Boolean.TRUE;
            if(ci!=null && ci.hasNext())
            {    
               b = ci.next();
            }
            if(m!=baseMap && b.booleanValue())
            {
                addMergeNodesFromSourceMap(m,voteAggregate,nodes);
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

    
    // todo: deprecate and/or remove
    // replace with refillAsVoteMerge
    public void recreateVoteMerge()
    {
        
        clearAllElements();
        
        fillAsVoteMerge();    
    }
    
    public void refillAsVoteMerge()
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
                  //System.out.println("LWMergeMap: nodeAlreadyPresent, merge property is null for " + node + " or " + c );
                  //System.out.println("node: " + Util.getMergeProperty(node) + "c: (current) " + Util.getMergeProperty(c));
              }
            }
            else
            {
                //System.out.println("LWMergeMap-nodeAlreadyPresent: node or c is null: (node,c) (" + node + "," + c + ")" );
            }
        }
        return false;
    }
    
    // todo: this should become the default initialization method
    // for interval boundaries only (see above)
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
           
           while(children.hasNext()) {
             LWNode comp = (LWNode)children.next();
             boolean repeat = false;
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
        Iterator<Boolean> ci = null; 
        if(getActiveFileList()!=null)
          ci = getActiveFileList().iterator();
        while(i.hasNext())
        {
          LWMap m = i.next();
          System.out.println("LWMergeMap, computing matrix array next map is: " + m.getLabel());
          Boolean b = Boolean.TRUE;
          if(ci!=null && ci.hasNext())
          {    
             b = ci.next();
          }
          if(b.booleanValue() || (m==getBaseMap()))
          {
            System.out.println("LWMergeMap, computing matrix array actually adding: " + m.getLabel());
            cms.add(new ConnectivityMatrix(m));
          }
          else
          {
            System.out.println("LWMergeMap, computing matrix array not adding: " + m.getLabel());
          }
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
          ci = null; 
          if(getActiveFileList()!=null)
            ci = getActiveFileList().iterator();
          while(maps.hasNext())
          {
            LWMap m = maps.next();
            System.out.println("LWMergeMap: next map - " + m.getLabel());
            Boolean b = Boolean.TRUE;
            if(ci!=null && ci.hasNext())
            {    
               b = ci.next();
            }
            if(m!=baseMap && b.booleanValue())
            {
                System.out.println("LWMergeMap: actually adding - " + m.getLabel());
                addMergeNodesForMap(m,weightAggregate,nodeStyles);
            }
            else
            {
                System.out.println("LWMergeMap: not adding - " + m.getLabel());
            }
          }
        }
        
        // todo: use applyCSS(style) -- need to plug in formatting panel
        Iterator children = getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        
        while(children.hasNext())
        {
             LWComponent comp = (LWComponent)children.next();
             System.out.println("LWMergeMap, computing colors, next component - " + comp.getLabel());
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
                  //todo: applyCss here instead.
                  node.setFillColor(Style.hexToColor(currStyle.getAttribute("background")));
             }
        }
       
        //compute and create links in Merge Map
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
                  if(c > 0) {
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
                    LWLink link = new LWLink(node1,node2);
                    //todo: applyCSS here
                    link.setStrokeColor(Style.hexToColor(currLinkStyle.getAttribute("background")));
                    addLink(link);
                  }
               }
           }
        }
    }

        
}

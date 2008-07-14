
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
    
    private static final boolean DEBUG_LOCAL = false;
    
    public static final int THRESHOLD_DEFAULT = 20;
    
    // old method for recording source nodes code currently does not compile
    // (its commented out below) - needs adjustment to 
    // LWComponent from LWNode (for both LWImage and LWNode)
    // a relatively minor fix and probably
    // also needs to be stored in special metadata of a new type...
    // (since otherwise this info always appears in the notes and possibly
    // out of sight)
    // **new system is to use VueMetadataElement.OTHER (see below)**
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
    private boolean excludeNodesFromBaseMap;
    
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
    private List<Double> nodeIntervalBoundaries = new ArrayList<Double>();
    private List<Double> linkIntervalBoundaries = new ArrayList<Double>();
    
    public static String getTitle()
    {
        return "Merge Map " + (++numberOfMaps); //+ "*";
    }
    
    public String getLabel()
    {
        return super.getLabel() + " *";
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
    
    public void setExcludeNodesFromBaseMap(boolean doExclude)
    {
        excludeNodesFromBaseMap = doExclude;
    }
    
    public boolean getExcludeNodesFromBaseMap()
    {
        return excludeNodesFromBaseMap;
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
     * This method and the next are for legacy persistence 
     * defaults to node, even though now link has separate interval boundaries
     *
     **/
    public void setIntervalBoundaries(List<Double> intervalBoundaries)
    {
        this.nodeIntervalBoundaries = intervalBoundaries;
    }
    
    public List<Double> getIntervalBoundaries()
    {
        return nodeIntervalBoundaries;
    }
    
    public void setNodeIntervalBoundaries(List<Double> intervalBoundaries)
    {
        this.nodeIntervalBoundaries = intervalBoundaries;
    }
    
    public List<Double> getNodeIntervalBoundaries()
    {
        return nodeIntervalBoundaries;
    }
    
    
    public void setLinkIntervalBoundaries(List<Double> intervalBoundaries)
    {
        this.linkIntervalBoundaries = intervalBoundaries;
    }
        
    public List<Double> getLinkIntervalBoundaries()
    {
        return linkIntervalBoundaries;
    }
    
    // need node and link -- read non specified as both.. 
    
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
           Iterator<LWComponent> children = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();//map.getNodeIterator();    
           while(children.hasNext()) {
             //LWNode comp = (LWNode)children.next();
             LWComponent comp = children.next();
             
             if(comp instanceof LWPortal)
                 continue;
             
             boolean repeat = false;
             if(nodeAlreadyPresent(comp))
             {
               repeat = true;
               /*if(RECORD_SOURCE_NODES)
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
               }*/
             }
             
             if(voteAggregate.isNodeVoteAboveThreshold(Util.getMergeProperty(comp)) ){
                   //LWNode node = (LWNode)comp.duplicate();
                   LWComponent node = comp.duplicate();
                   
                   String sourceLabel = node.getLabel();
               
                   if(sourceLabel == null)
                     sourceLabel = "";
                
                   edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
                   vme.setType(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
                   
                   //todo: looks like this may be needed in future -- may be an issue
                   // of map list in merge maps panel -- or otherwise might require
                   // more generic approach (certainly a seperate method either here or in 
                   // merge maps panel or in VueMetadataElement is in order)
                   /*String mapLabel = comp.getMap().getLabel();
                   
                   if(mapLabel != null)
                   {
                       if(mapLabel.contains(".vue"))
                       {
                           mapLabel = mapLabel.substring(0,mapLabel.indexOf(".vue"));
                       }
                   } 
                   else
                   {
                       mapLabel = "";
                   }*/
                   
                   vme.setObject("source: " + comp.getMap().getLabel() + "," + sourceLabel);
                   node.getMetadataList().getMetadata().add(vme);
                   
                   if(!repeat)
                   {
                     /*if(RECORD_SOURCE_NODES)
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
                     }*/
                     //addNode(node);
                     if(!excludeNodesFromBaseMap || !nodePresentOnBaseMap(node))
                     {    
                       add(node);
                     }
                   }
             }         
             
           }
    }

    
    public void fillAsVoteMerge()
    {
        
        HashMap<String,LWNode> nodes = new HashMap<String,LWNode>();
        
        ConnectivityMatrixList<ConnectivityMatrix> cms = new ConnectivityMatrixList<ConnectivityMatrix>();
        
        Iterator<LWMap> i = getMapList().iterator();
        Iterator<Boolean> ci = null; 
        if(getActiveFileList()!=null)
            ci = getActiveFileList().iterator();
        while(i.hasNext())
        {
          Boolean b = Boolean.TRUE;
          
          LWMap next = i.next();
          
          if(ci!=null && ci.hasNext())
          {    
             b = ci.next();
          }
          if(b.booleanValue() /*&& (next != getBaseMap())*/ )
            cms.add(new ConnectivityMatrix(next));
        }
        VoteAggregate voteAggregate= new VoteAggregate(cms);
        
        voteAggregate.setNodeThreshold((double)getNodeThresholdSliderValue()/100.0);
        voteAggregate.setLinkThreshold((double)getLinkThresholdSliderValue()/100.0);
        
        //compute and create nodes in Merge Map
        
        if(!excludeNodesFromBaseMap && baseMapIsActive())
        {    
          addMergeNodesFromSourceMap(baseMap,voteAggregate,nodes);
        }
        
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
        Iterator<LWComponent> children1 = getAllDescendents(LWComponent.ChildKind.PROPER).iterator(); //getNodeIterator();
        while(children1.hasNext()) {
           /*LWNode*/ LWComponent node1 = /*(LWNode)*/children1.next();
           Iterator<LWComponent> children2 = getAllDescendents(LWComponent.ChildKind.PROPER).iterator(); //getNodeIterator();
           while(children2.hasNext()) {

               /*LWNode*/ LWComponent node2 = /*(LWNode)*/children2.next();
               
               if( ! ((node1 instanceof LWNode || node1 instanceof LWImage) && (node2 instanceof LWNode || node2 instanceof LWImage)) )
               {
                   continue;
               }
               
               if(node2 != node1) {
                  boolean addLink = voteAggregate.isLinkVoteAboveThreshold(Util.getMergeProperty(node1),Util.getMergeProperty(node2));
                  if(addLink) {
                     LWLink link = new LWLink(node1,node2);
                     addLink(link);
                     cms.addLinkSourceMapMetadata(Util.getMergeProperty(node1),Util.getMergeProperty(node2),link);
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
    
    public boolean nodeAlreadyPresent(LWComponent node)
    {
        
        // creating double child nodes now...
        /*if(getFilterOnBaseMap())
        {
            return false;
        }*/
        
        if(DEBUG_LOCAL)
        {    
          System.out.println("nodeAlreadyPresent -- getParent() " + node.getParent());
        }
        
        // also need to check if parent *will* be visible
        //if(! (node.getParent() instanceof LWMap ) )
        if (!node.atTopLevel())
            return true;
       // if(node.getParent() instanceof LWNode && (LWNode.isImageNode((LWNode)(node.getParent()))) )
       //     return true;
        
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
                  if(DEBUG_LOCAL)
                  {    
                    System.out.println("LWMergeMap - returning true in nodeAlreadyPresent - for (node,c) (" +
                            Util.getMergeProperty(node) +"," + Util.getMergeProperty(c) + ")");
                  }
                  
                                    
                  String sourceLabel = node.getLabel();
               
                  if(sourceLabel == null)
                     sourceLabel = "";
                  
                  edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
                  vme.setType(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
                  vme.setObject("source: " + node.getMap().getLabel() + "," + sourceLabel);
                  c.getMetadataList().getMetadata().add(vme);
                  
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
    
    public boolean nodePresentOnBaseMap(LWComponent node)
    {
       
        Iterator<LWComponent> i = getBaseMap().getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        while(i.hasNext())
        {
            LWComponent c = i.next();
            if(c!=null && node!=null)
            {    
              if(Util.getMergeProperty(node) != null && Util.getMergeProperty(c) != null )
              {    
                if(Util.getMergeProperty(node).equals(Util.getMergeProperty(c)))
                {
                  if(DEBUG_LOCAL)
                  {    
                    System.out.println("LWMergeMap - returning true in nodePresentOnBaseMap - for (node,c) (" +
                            Util.getMergeProperty(node) +"," + Util.getMergeProperty(c) + ")");
                  }
                  
                  //String sourceLabel = node.getLabel();
               
                  //if(sourceLabel == null)
                  //   sourceLabel = "";
                  
                  //edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
                  //vme.setType(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
                  
                  //if(DEBUG_LOCAL)
                  //{
                  //   System.out.println("LWMergeMap: about to set source for node -- label: " + node.getLabel());    
                  //}
                  
                  //if(node.getMap() != null)
                  //{    
                  //  vme.setObject("source: " + node.getMap().getLabel() + "," + sourceLabel);
                  //}
                  //else
                  //{
                  //  vme.setObject("source: " + sourceLabel);  
                  //}
                  //c.getMetadataList().getMetadata().add(vme);
                  
                  return true;
                }
              }
              else
              {
                  //System.out.println("LWMergeMap: nodePresentOnBaseMap, merge property is null for " + node + " or " + c );
                  //System.out.println("node: " + Util.getMergeProperty(node) + "c: (current) " + Util.getMergeProperty(c));
              }
            }
            else
            {
                //System.out.println("LWMergeMap-nodePresentOnBaseMap: node or c is null: (node,c) (" + node + "," + c + ")" );
            }
        }
        return false;
    }
    
    // todo: this should become only the default initialization method
    // for interval boundaries -- in fact: implementing 4/1/2008
    public void setIntervalBoundaries()
    {
        nodeIntervalBoundaries = new ArrayList<Double>();
        for(int vai = 0;vai<6;vai++)
        {
            double va =  20*vai + 0.5;
           nodeIntervalBoundaries.add(new Double(va));
        } 
        linkIntervalBoundaries = new ArrayList<Double>();
        for(int vai = 0;vai<6;vai++)
        {
            double va =  20*vai + 0.5;
            linkIntervalBoundaries.add(new Double(va));
        } 
    }
    
    public int getNodeInterval(double score)
    {
        Iterator<Double> i = nodeIntervalBoundaries.iterator();
        int count = 0;
        while(i.hasNext())
        {
            if(score < i.next().doubleValue())
                return count;
            count ++;
        }
        return 0;
    }
    
    public int getLinkInterval(double score)
    {
        Iterator<Double> i = linkIntervalBoundaries.iterator();
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
         
           Iterator<LWComponent> children = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator(); //map.getNodeIterator();
           
           while(children.hasNext()) {
               
             LWComponent component = children.next(); 
             
             if(component instanceof LWPortal)
                 continue;
             
             LWNode comp = null;
             LWImage image = null;
             
            /* if(component instanceof LWImage)
             {
               image = (LWImage)(component.duplicate());
               add(image);
             } */
             
             if(component instanceof LWNode || component instanceof LWImage)
             {
               //comp=(LWNode)component;   
              
               //LWNode comp = (LWNode)children.next();
               //component = children.next();
               boolean repeat = false;
               
               //LWComponent original = nodeAlreadyPresent(component);
               
               //if(original != null)
               if(nodeAlreadyPresent(component))
               {
                 repeat = true;
               }
               LWComponent node = component.duplicate();
               
               
               edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
               vme.setType(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
               
               String sourceLabel = node.getLabel();
               
               if(sourceLabel == null)
                   sourceLabel = "";
               
               String sourceMap = component.getMap().getLabel();
               
               if(sourceMap == null)
                   sourceMap = "";
               
               vme.setObject("source: " + sourceMap + "," + sourceLabel);
               node.getMetadataList().getMetadata().add(vme);
             
               if(!repeat)
               {    
                 //addNode(node);
                 if(!excludeNodesFromBaseMap || !nodePresentOnBaseMap(node))
                 {    
                   add(node);
                 }
               }     
             }
             
           }
    }
        
    public void fillAsWeightMerge()
    {
    
        ConnectivityMatrixList<ConnectivityMatrix> cms = new ConnectivityMatrixList<ConnectivityMatrix>();
        Iterator<LWMap> i = getMapList().iterator();
        Iterator<Boolean> ci = null; 
        if(getActiveFileList()!=null)
          ci = getActiveFileList().iterator();
        while(i.hasNext())
        {
          LWMap m = i.next();
          if(DEBUG_LOCAL)
          {    
            System.out.println("LWMergeMap, computing matrix array next map is: " + m.getLabel());
          }
          Boolean b = Boolean.TRUE;
          if(ci!=null && ci.hasNext())
          {    
             b = ci.next();
          }
          if(b.booleanValue()) //|| (m==getBaseMap()))
          {
            if(DEBUG_LOCAL)
            {    
              System.out.println("LWMergeMap, computing matrix array actually adding: " + m.getLabel());
            }
            cms.add(new ConnectivityMatrix(m));
          }
          else
          {
            if(DEBUG_LOCAL)
            {    
              System.out.println("LWMergeMap, computing matrix array not adding due to check box or base map (already added):" +
                                 " (label, m == getBaseMap()) (" + m.getLabel() + "," + (m==getBaseMap()) +")");
            }
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
        
        if(!excludeNodesFromBaseMap && baseMapIsActive())
        {
          addMergeNodesForMap(getBaseMap(),weightAggregate,nodeStyles);
        }
        
        if(!getFilterOnBaseMap())
        {
          Iterator<LWMap> maps = getMapList().iterator();
          ci = null; 
          if(getActiveFileList()!=null)
            ci = getActiveFileList().iterator();
          while(maps.hasNext())
          {
            LWMap m = maps.next();
            if(DEBUG_LOCAL)
            {
              System.out.println("LWMergeMap: next map - " + m.getLabel());
            }
            Boolean b = Boolean.TRUE;
            if(ci!=null && ci.hasNext())
            {    
               b = ci.next();
            }
            if(m!=baseMap && b.booleanValue())
            {
                if(DEBUG_LOCAL)
                {    
                  System.out.println("LWMergeMap: actually adding - " + m.getLabel());
                }
                addMergeNodesForMap(m,weightAggregate,nodeStyles);
            }
            else
            {
                if(DEBUG_LOCAL)
                {    
                  System.out.println("LWMergeMap: not adding - " + m.getLabel());
                }
            }
          }
        }
        
        // todo: use applyCSS(style) -- need to plug in formatting panel
        Iterator children = getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        
        while(children.hasNext())
        {
             LWComponent comp = (LWComponent)children.next();
             if(DEBUG_LOCAL)
             {    
               System.out.println("LWMergeMap, computing colors, next component - " + comp.getLabel());
             }
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
                  Style currStyle = nodeStyles.get(getNodeInterval(score)-1);
                  //todo: applyCss here instead.
                  node.setFillColor(Style.hexToColor(currStyle.getAttribute("background")));
                  
                  java.awt.Color strokeColor = null;
                  if(currStyle.getAttribute("font-color") != null)
                  {
                    strokeColor = Style.hexToColor(currStyle.getAttribute("font-color"));
                  }
                  if(strokeColor != null)
                  {
                    node.setTextColor(strokeColor);
                  }
             }
        }
       
        //compute and create links in Merge Map
        Iterator<LWComponent> children1 = getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        while(children1.hasNext()) {
           LWComponent comp1 = children1.next();
           //if(comp1 instanceof LWImage)
           //    continue;
           //LWNode node1 = (LWNode)comp1;
           LWComponent node1 = comp1;
           Iterator<LWComponent> children2 = getAllDescendents(LWComponent.ChildKind.PROPER).iterator();//getNodeIterator();
           LWComponent node2 = null;
           while(children2.hasNext() ) {
               /*LWComponent*/ node2 = /*(LWNode)*/children2.next();
               if(!((node2 instanceof LWNode) || (node2 instanceof LWImage)) )
               {
                   continue;
               }
               if(node2 != node1) {
                  int c = weightAggregate.getConnection(Util.getMergeProperty(node1),Util.getMergeProperty(node2));
                  int c2 = weightAggregate.getConnection(Util.getMergeProperty(node2),Util.getMergeProperty(node1));
                  if(c > 0) {
                    double score = 100*c/weightAggregate.getCount();
                    
                    // are either of these ever happenning? If so, why?
                    if(score > 100)
                    {
                        score = 100;
                    }
                    if(score < 0)
                    {
                        score = 0;
                    }
                    
                    Style currLinkStyle = linkStyles.get(getLinkInterval(score)-1);
                    LWLink link = new LWLink(node1,node2);
                    if(c2>0 && !getFilterOnBaseMap())
                    {
                      link.setArrowState(LWLink.ARROW_BOTH);
                      weightAggregate.setConnection(Util.getMergeProperty(node2),Util.getMergeProperty(node1),0);
                    }
                    //todo: applyCSS here
                    link.setStrokeColor(Style.hexToColor(currLinkStyle.getAttribute("background")));
                    addLink(link);
                    
                    cms.addLinkSourceMapMetadata(Util.getMergeProperty(node1),Util.getMergeProperty(node2),link);
                    
                   //edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
                   //vme.setType(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
                   //vme.setObject("source: " + comp.getMap().getLabel() + "," + sourceLabel);
                   //link.getMetadataList().getMetadata().add(vme);
                  }
               }
           }
        }
    }
    
    public boolean baseMapIsActive()
    {
        Iterator<Boolean> actives = activeFiles.iterator();
        Iterator<LWMap> maps = mapList.iterator();
        while(actives.hasNext() && maps.hasNext())
        {
            LWMap map = maps.next();
            boolean active = actives.next().booleanValue();
            if(map == baseMap && !active)
                return false;
        }
        return true;
    }
    

        
}

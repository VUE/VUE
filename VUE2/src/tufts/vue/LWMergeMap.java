
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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class LWMergeMap extends LWMap {
    
    public static final int THRESHOLD_DEFAULT = 50;
    
    private static int maps = 0;
    
    
    private int mapListSelectionType;
    private int baseMapSelectionType;
    private int visualizationSelectionType;
    
    private String selectionText;
    private LWMap baseMap;
    private String selectChoice;
    private int nodeThresholdSliderValue = THRESHOLD_DEFAULT;
    private int linkThresholdSliderValue = THRESHOLD_DEFAULT;
    private List<File> fileList;
    private List<Boolean> activeFiles;
    // without this next line it seems that Castor only reads back one element..
    private List<LWMap> mapList = new ArrayList<LWMap>();
    
    //private Stack<Integer> nodeThresholdValueStack;
    
    private File baseMapFile;
    
    private String styleFile;
    
    public static String getTitle()
    {
        return "Merge Map" + (++maps) + "*";
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
    
    /*public Stack<Integer> getNodeThresholdValueStack()
    {
        return nodeThresholdValueStack;
    }*/
    
    public void recreateVoteMerge()
    {
        
        //produces ConcurrentModificationException
        //removeChildren(getChildIterator());
        
        //copied from Actions:
        
        //create own selection, not vueselection, actions.delete(selection)


        Iterator li = getAllDescendents().iterator();
        

        //Iterator li = getAllLinks().iterator();
        // concurrentModificationException unless do nodes and links seperately...
        //Iterator si = VUE.getActiveMap().getChildIterator();
        
        // this deletion code is from LWComponent
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
                  
        ArrayList<ConnectivityMatrix> cms = new ArrayList<ConnectivityMatrix>();
        //System.out.println("recreate merge: getMapList.size() " + getMapList().size());
        Iterator<LWMap> i = getMapList().iterator();
        //System.out.println("LWMergeMap: " + getMapList().size());
        while(i.hasNext())
        {
          LWMap m = i.next();
          
          //Iterator cl = m.getChildList().iterator();
          //int clc = 0;
          /*while(cl.hasNext())
          {
              LWComponent com = (LWComponent)cl.next();
              //System.out.println("class: " + m.getLabel() + ":" + (clc++) + ":" + com.getClass());
              //System.out.println("label: " + m.getLabel() + ":" + (clc++) + ":" + com.getLabel());
              //System.out.println("ID: " + m.getLabel() + ":" + (clc++) + ":" + com.getID());;
              
              if(com instanceof LWLink)
              {
                  LWLink comlink = (LWLink)com;
                  //System.out.println("component1: " + comlink.getHead());
                  //System.out.println("comlink.getEndPoint1_ID(): " + comlink.getEndPoint1_ID());
                  //System.out.println("component1 label: " + comlink.getHead().getLabel());
                  //System.out.println("component2: " + comlink.getTail());
                  //System.out.println("comlink.getEndPoint2_ID(): " + comlink.getEndPoint2_ID());
                  //System.out.println("component2 label: " + comlink.getTail().getLabel());
                  
                  //System.out.println("arrow state: " + ((LWLink)(com)).getArrowState());
              }
          }*/
          
          Iterator ni = m.getNodeIterator();
          //System.out.println("node iterator: " + ni.hasNext());
          
          Iterator linki = m.getLinkIterator();
          //System.out.println("link iterator: " + linki.hasNext());
          
          //System.out.println(m.getLabel());
          ConnectivityMatrix cm = new ConnectivityMatrix(m);
          //System.out.println(cm);
          cms.add(cm);
        }
        VoteAggregate voteAggregate= new VoteAggregate(cms);
        voteAggregate.setNodeThreshold((double)(getNodeThresholdSliderValue()/100.0));
        voteAggregate.setLinkThreshold((double)(getLinkThresholdSliderValue()/100.0));
        
        //compute and create nodes in Merge Map
        Iterator children = getBaseMap().getNodeIterator();
        while(children.hasNext()) {
           LWComponent comp = (LWComponent)children.next();
           if(voteAggregate.isNodeVoteAboveThreshold(Util.getMergeProperty(comp)) ){
                   LWNode node = (LWNode)comp.duplicate();
                   addNode(node);
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
                  int c = voteAggregate.getConnection(Util.getMergeProperty(node1),Util.getMergeProperty(node2));
                  if(c >0) {
                     addLink(new LWLink(node1,node2));
                  }
               }
           }
        }
        
    }
    
    public void recalculateLinks()
    {
          
        Iterator<LWMap> i = getMapList().iterator();
        //System.out.println("LWMergeMap: " + getMapList().size());
        while(i.hasNext())
        {
          LWMap m = i.next();
        
          //System.out.println(m.getChildList().size());
          
          List<LWComponent> chs = m.getChildList();
          
          
          // adapted from private method in LWMap -- resolvePersistedLinks..
          for (LWComponent currc : chs) {
            if (!(currc instanceof LWLink))
                continue;
            LWLink l = (LWLink) currc;
            try {
                // was: final ep1ID
                String ep1ID = l.getHead_ID();
                //System.out.println("before setting components");
                //System.out.println("l.getHead_ID(): " + l.getHead_ID());
                //System.out.println("l.getHead(): " + l.getHead());
                // was: final ep2ID
                String ep2ID = l.getTail_ID();
                //System.out.println("l.getTail_ID(): " + l.getTail_ID());
                //System.out.println("l.getTail(): " + l.getTail());

                if (ep1ID != null && l.getHead() == null) l.setHead(findByID(chs, ep1ID));
                if (ep2ID != null && l.getTail() == null) l.setTail(findByID(chs, ep2ID));
                if(l.getHead() != null && l.getTail() != null)
                {
                    //markAsSaved();
                    //getUndoManager().mark("links recalculated");
                    //getUndoManager().flush();
                }
                
                ep1ID = l.getHead_ID();
                //System.out.println("after setting components");
                //System.out.println("l.getHead_ID(): " + l.getHead_ID());
                //System.out.println("l.getHead(): " + l.getHead());
                ep2ID = l.getTail_ID();
                //System.out.println("l.getTail_ID(): " + l.getTail_ID());
                //System.out.println("l.getTail(): " + l.getTail());
                
            } catch (Throwable e) {
                tufts.Util.printStackTrace(e, "bad link? + l");
            }
          } // end for 
        } // end while
        
        getUndoManager().mark("recalculate links");
        getUndoManager().flush();

    }
    
    
    public boolean nodeAlreadyPresent(LWNode node)
    {
        Iterator<LWComponent> i = getChildList().iterator();
        while(i.hasNext())
        {
            LWComponent c = i.next();
            if(Util.getMergeProperty(node).equals(Util.getMergeProperty(c)))
            {
                return true;
            }
        }
        return false;
    }
        
}

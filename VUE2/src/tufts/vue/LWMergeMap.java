
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
    private List<LWMap> mapList;
    
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
        System.out.println("LWMergeMap: set map list: " + mapList.size());
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
    
    public void recreateVoteMerge(/*List<LWMap> mapList*/)
    {
        
        //removeChildren(getChildIterator());
        
        ArrayList<ConnectivityMatrix> cms = new ArrayList<ConnectivityMatrix>();
        Iterator<LWMap> i = getMapList().iterator();
        System.out.println("LWMergeMap: " + getMapList().size());
        while(i.hasNext())
        {
          cms.add(new ConnectivityMatrix(i.next()));
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
    
}


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

import java.io.File;
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
}


/*
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
import java.util.*;
import edu.tufts.vue.metadata.VueMetadataElement;

/** todo: this needs to turn into an LWMap builder, not a subclass of LWMap */
public class LWMergeMap extends LWMap {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWMergeMap.class);
    
    private static final boolean DEBUG_LOCAL = false;
    
    public static final int THRESHOLD_DEFAULT = 20;
    
    // [DAN] old method for recording source nodes code currently does not compile
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
    
    //private String selectionText;
    private LWMap baseMap;
    private Set baseMapKeys;
    //private String selectChoice;
    private int nodeThresholdSliderValue = THRESHOLD_DEFAULT;
    private int linkThresholdSliderValue = THRESHOLD_DEFAULT;
    private boolean filterOnBaseMap;
    private boolean excludeNodesFromBaseMap;
    
    private List<String> fileList = new ArrayList<String>();
    private List<Boolean> activeFiles = new ArrayList<Boolean>();
 
    // [DAN] todo: recover only file names back into the GUI this list will continue to persist to
    // provide a record of actual map data used for merge (will be overwritten on re-save of file
    // as merge will be recalculated on GUI load) note: this initializer seems to be required for
    // Castor libary to actually persist the list.
    
    /** Actual maps used to generate the most recent merge. **/
    private List<LWMap> mapList = new ArrayList<LWMap>();
    
    private File baseMapFile;
    
    private String styleFile;

    private final List<Double> intervalBoundaries = new ArrayList<Double>();
    private List<Double> nodeIntervalBoundaries = new ArrayList<Double>();
    private List<Double> linkIntervalBoundaries = new ArrayList<Double>();
    
    public static String getTitle() {
        return "Merge Map " + (++numberOfMaps); //+ "*";
    }
    
    public String getLabel() { return super.getLabel() + " *"; }
    
    public LWMergeMap() {}
    public LWMergeMap(String label) {
        super(label);
    }

    /**
     * No guarantee that these filenames were generated from *saved* maps todo: document here the
     * current behavior of VUE in this case.  GUI should correctly handle any current or future
     * nulls or otherwise invalid file names that might be stored here.
     **/ 
    public void setMapFileList(List<String> mapList) {
        if(mapList != null)
          fileList = mapList;
    }
    
    public List<String> getMapFileList() {
        return fileList;
    }
    
    /**
     * no longer relevant. This was for old gui with separation between options to load from file
     * or from all open maps todo: deprecate and/or remove
     **/
    public void setMapListSelectionType(int choice) {
        mapListSelectionType = choice;
    }
    
    /**
     * no longer relevant. This was for old gui with separation between options to load from file
     * or from all open maps todo: deprecate and/or remove
     **/
    public int getMapListSelectionType() {
        return mapListSelectionType;
    }
    
    public void setActiveMapList(List<Boolean> activeMapList) {
        activeFiles = activeMapList;
    }
    
    public List<Boolean> getActiveFileList() {
        return activeFiles;
    }

    /** @return the list of currently active maps (our maps list adjusted by activeStatus) */
    public List<LWMap> getActiveMaps()
    {
        final List<LWMap> maps = getMapList();
        final List<Boolean> activeStatusList = getActiveFileList();

        if (activeStatusList == null || activeStatusList.size() == 0) {
            // no status list specified, thus, all maps are active:
            return mapList;
        }

        final List<LWMap> activeMaps = new ArrayList(maps.size());
        final Iterator<Boolean> statusIter = activeStatusList.iterator();

        for (LWMap map : maps) {
            if (statusIter.hasNext()) {
                if (statusIter.next().booleanValue())
                    activeMaps.add(map);
                else
                    /* leave this map out */;
            } else {
                activeMaps.add(map);
            }
        }
        return activeMaps;
    }
    
    public void setMapList(List<LWMap> mapList) {
        this.mapList = mapList; 
    }
    
    public List<LWMap> getMapList() {
        return mapList;
    }
    
    // public String getSelectionText() { return selectionText; }
    // public void setSelectionText(String text) { selectionText = text; }
    
    // [DAN?] todo: deprecate and/or remove -- this was for old GUI [SMF: MergeMapsChooser is still calling tho]
    // public String getSelectChoice() { return selectChoice; }
    /** @deprecated todo: deprecate and/or remove -- this was for old GUI (MergeMapsChooser is still calling tho) */
    //public void setSelectChoice(String choice) { selectChoice = choice; }
    
    public void setVisualizationSelectionType(int choice) { visualizationSelectionType = choice; }
    public int getVisualizationSelectionType() { return visualizationSelectionType; }
    public void setFilterOnBaseMap(boolean doFilter) { filterOnBaseMap = doFilter; }
    public boolean getFilterOnBaseMap() { return filterOnBaseMap; }
    public void setExcludeNodesFromBaseMap(boolean doExclude) { excludeNodesFromBaseMap = doExclude; }
    public boolean getExcludeNodesFromBaseMap() { return excludeNodesFromBaseMap; }
    public void setNodeThresholdSliderValue(int value) { nodeThresholdSliderValue = value; }
    public int getNodeThresholdSliderValue() { return nodeThresholdSliderValue; }
    public void setLinkThresholdSliderValue(int value) { linkThresholdSliderValue = value; }
    public int getLinkThresholdSliderValue() { return linkThresholdSliderValue; }
    
    /** @deprecated [DAN?] todo: deprecate and/or remove -- this was for old GUI [SMF: MergeMapsChooser is still calling tho] */
    public void setBaseMapSelectionType(int choice) { baseMapSelectionType = choice; }
    /** @deprecated: deprecate and/or remove -- this was for old GUI (STILL IN CASTOR MAPPING FILE) */
    public int getBaseMapSelectionType() { return baseMapSelectionType; }
    
    public LWMap getBaseMap() { return baseMap; }
    public void setBaseMap(LWMap baseMap) { this.baseMap = baseMap; }
    
    public File getBaseMapFile() { return baseMapFile; }
    public void setBaseMapFile(File file) { baseMapFile = file; }
    
    /**
     * todo: rename no-arg version below as "default" setup method.
     * This method and the next are for legacy persistence 
     * defaults to node, even though now link has separate interval boundaries
     **/
    public void setIntervalBoundaries(List<Double> intervalBoundaries) {
        this.nodeIntervalBoundaries = intervalBoundaries;
    }
    public List<Double> getIntervalBoundaries() {
        return nodeIntervalBoundaries;
    }
    public void setNodeIntervalBoundaries(List<Double> intervalBoundaries) {
        this.nodeIntervalBoundaries = intervalBoundaries;
    }
    public List<Double> getNodeIntervalBoundaries() {
        return nodeIntervalBoundaries;
    }
    public void setLinkIntervalBoundaries(List<Double> intervalBoundaries) {
        this.linkIntervalBoundaries = intervalBoundaries;
    }
    public List<Double> getLinkIntervalBoundaries() {
        return linkIntervalBoundaries;
    }
    public void setStyleMapFile(String file) {
        styleFile = file;
    }
    public String getStyleMapFile() {
        return styleFile;
    }
    
    public void clearAllElements() {
        deleteChildrenPermanently(getChildren());
    }

    private void mergeInNodesForVotes(LWMap sourceMap, Map<Object,LWComponent> keysMerged, VoteAggregate voteAggregate) {
        mergeInNodes(sourceMap, keysMerged, voteAggregate);
    }

    private void mergeInNodesForWeights(LWMap sourceMap, Map<Object,LWComponent> keysMerged, WeightAggregate weightAggregate_IGNORED) {
        mergeInNodes(sourceMap, keysMerged, null);
    }

    private void mergeInNodes(final LWMap sourceMap, final Map<Object,LWComponent> keysMerged, final VoteAggregate voteAggregate)
    {
        for (LWComponent srcNode : sourceMap.getAllDescendents(ChildKind.PROPER)) {
            if (srcNode instanceof LWNode || srcNode instanceof LWImage) ; else continue;
            final Object mergeKey = getMergeKey(srcNode);
            if (mergeKey == null)
                continue;
            final LWComponent alreadyMerged = keysMerged.get(mergeKey);
            if (alreadyMerged != null) {
                annotateWithSource(sourceMap, srcNode, alreadyMerged);
                continue;
            }
            //-----------------------------------------------------------------------------
            // weightAggregate was ignored here -- is only used for styling in our caller
            //-----------------------------------------------------------------------------
            if (voteAggregate != null && !voteAggregate.isNodeVoteAboveThreshold(mergeKey))
                continue;
            
            if (!excludeNodesFromBaseMap || !mergeKeyPresentOnBaseMap(mergeKey)) // todo: base-map check needlessly slow
                keysMerged.put(mergeKey, copyInNode(sourceMap, srcNode));
        }
    }

    /** copy the source node to this map, returning the new duplicate node */
    private LWComponent copyInNode(LWMap sourceMap, LWComponent sourceNode) {
        final LWComponent node = sourceNode.duplicate();
        annotateWithSource(sourceMap, sourceNode, node);
        super.addChild(node);
        return node;
    }

    // TODO: NEED TO HANDLE CASES WHERE NOT ALL NODES HAVE A MERGE PROPERTY (e.g., Resource)

    /** Called by MergeMapsControlPanel */
    public void fillAsWeightMerge()
    {
        final ConnectivityMatrixList<ConnectivityMatrix> cms = new ConnectivityMatrixList<ConnectivityMatrix>();

        final Collection<LWMap> activeMaps = getActiveMaps();
        final LWMap baseMap = getBaseMap();

        if (excludeNodesFromBaseMap)
            this.baseMapKeys = hashMergeKeys(baseMap);

        for (LWMap map : activeMaps) {
            if (DEBUG_LOCAL) Log.debug("computing matrix adding: " + map);
            // old comment had commented out check to skip baseMap...
            cms.add(new ConnectivityMatrix(map));
        }
        
        final List<Style> nodeStyles = new ArrayList<Style>();
        final List<Style> linkStyles = new ArrayList<Style>();
        
        for(int si=0;si<5;si++) 
            nodeStyles.add(StyleMap.getStyle("node.w" + (si +1)));
        
        for(int lsi=0;lsi<5;lsi++)
            linkStyles.add(StyleMap.getStyle("link.w" + (lsi +1)));
        
        final WeightAggregate weightAggregate = WeightAggregate.create(cms);
        final Map<Object,LWComponent> keysMerged = new HashMap();
        
        if (!excludeNodesFromBaseMap && baseMapIsActive()) {
            // Don't know if we need to add baseMap 1st, but leaving this in
            // instead of including in loop below just in case.
            mergeInNodesForWeights(baseMap, keysMerged, weightAggregate /*,nodeStyles*/ ); // styles were ignored
        }
        
        if (!getFilterOnBaseMap()) {
            for (LWMap map : activeMaps) {
                if (map == baseMap) {
                    Log.debug("addMerge; skipping baseMap " + baseMap);
                } else {
                    Log.debug("addMerge;       adding map " + map);
                    mergeInNodesForWeights(map, keysMerged, weightAggregate /*,nodeStyles*/ );
                }
            }
        }

        final Collection<LWComponent> allMergedNodes = getAllDescendents(ChildKind.PROPER); 

        for (LWComponent c : allMergedNodes) {
            if (c instanceof LWNode || c instanceof LWImage) continue;
            // As we've only done addMergeNodesForMap calls up till now, everything in the map should already
            // only be LWNodes or LWImages, but we check here just in case...
            Log.warn("merge results pollution?: " + c);
            //allMergedNodes.remove(c); // iterator would fail...
        }
        
        // todo: use applyCSS(style) -- need to plug in formatting panel
        
        for (LWComponent node : allMergedNodes) {
            if (node instanceof LWNode == false)
                continue;
            double score = 100 * weightAggregate.getNodeCount(getMergeKey(node))/weightAggregate.getCount();
            if(score>100) score = 100; else if(score<0) score = 0;
            final Style currStyle = nodeStyles.get(getNodeInterval(score)-1);
            //todo: applyCss here instead.
            node.setFillColor(Style.hexToColor(currStyle.getAttribute("background")));
            java.awt.Color strokeColor = null;
            if(currStyle.getAttribute("font-color") != null)
                strokeColor = Style.hexToColor(currStyle.getAttribute("font-color"));
            if (strokeColor != null)
                node.setTextColor(strokeColor);
        }

        for (LWComponent head : allMergedNodes) {
            final Object headKey = getMergeKey(head);
            // Again, we're expecting that only LWImages or LWnodes are in allMergedNodes.
            for (LWComponent tail : allMergedNodes) {
                if (head == tail)
                    continue;
                final Object tailKey = getMergeKey(tail);
                final int c = weightAggregate.getConnection(headKey, tailKey);
                if (c <= 0)
                    continue;
                final int c2 = weightAggregate.getConnection(tailKey, headKey);
                double score = 100 * c / weightAggregate.getCount();
                // are either of these ever happenning? If so, why?
                if (score > 100) score = 100; else if (score < 0) score = 0;
                final Style currLinkStyle = linkStyles.get(getLinkInterval(score)-1);
                final LWLink link = new LWLink(head, tail);
                if (c2 > 0 && !getFilterOnBaseMap()) {
                    link.setArrowState(LWLink.ARROW_BOTH);
                    weightAggregate.setConnection(tailKey, headKey, 0);
                }
                // todo: applyCSS here
                link.setStrokeColor(Style.hexToColor(currLinkStyle.getAttribute("background")));
                super.addLink(link);
                cms.addLinkSourceMapMetadata(headKey, tailKey, link);
            }
        }
    }
    
    /** Called by MergeMapsControlPanel */
    public void fillAsVoteMerge()
    {
        final ConnectivityMatrixList<ConnectivityMatrix> matrixList = new ConnectivityMatrixList<ConnectivityMatrix>();
        final List<LWMap> activeMaps = getActiveMaps();

        //-----------------------------------------------------------------------------
        // Create a connectivity matrix for each active map to be fed to the VoteAggregate
        //-----------------------------------------------------------------------------

        for (LWMap map : activeMaps) {
            // if (map != getBaseMap())
            matrixList.add(new ConnectivityMatrix(map));
        }

        final VoteAggregate voteAggregate = VoteAggregate.create(matrixList);
        
        voteAggregate.setNodeThreshold( (double) getNodeThresholdSliderValue() / 100.0 );
        voteAggregate.setLinkThreshold( (double) getLinkThresholdSliderValue() / 100.0 );
        
        //-----------------------------------------------------------------------------
        // compute and create nodes in Merge Map
        //-----------------------------------------------------------------------------

        if (excludeNodesFromBaseMap)
            this.baseMapKeys = hashMergeKeys(getBaseMap());

        final Map<Object,LWComponent> keysMerged = new HashMap();
        
        if (!excludeNodesFromBaseMap && baseMapIsActive())
            mergeInNodesForVotes(baseMap, keysMerged, voteAggregate);
        
        if (! getFilterOnBaseMap()) {
            // TODO: CHECK THIS LOGIC: ONLY ADDING ANY MAPS IF *NOT* "filterOnBaseMap" ?
            for (LWMap map : activeMaps)
                if (map != baseMap)
                    mergeInNodesForVotes(map, keysMerged, voteAggregate);
        }
        
        //-----------------------------------------------------------------------------
        // compute and create links in Merge Map
        //-----------------------------------------------------------------------------

        final Collection<LWComponent> allComponents = getAllDescendents(ChildKind.PROPER);
        final List<LWComponent> linkables = new ArrayList(allComponents.size() / 2);

        for (LWComponent c : allComponents) {
            // We only generate links between nodes and images:
            if (c instanceof LWNode || c instanceof LWImage)
                linkables.add(c);
        }

        // Is there a faster way to do this than O(n^2) ? Shouldn't we be able to iterate the
        // VoteAggregate or track the above threshold relations there?

        for (LWComponent head : linkables) {
            final Object headKey = getMergeKey(head);
            for (LWComponent tail : linkables) {
                if (head != tail) {
                    final Object tailKey = getMergeKey(tail);
                    if (voteAggregate.isLinkVoteAboveThreshold(headKey, tailKey)) {
                        // Vote for the relation between these two nodes was above link threshold: add a new link
                        final LWLink link = new LWLink(head, tail);
                        super.addLink(link);
                        matrixList.addLinkSourceMapMetadata(headKey, tailKey, link);
                    }
                }
            }
        }
    }

    private static Set hashMergeKeys(LWMap map) {
        final Set hashedKeys = new HashSet();
        for (LWComponent c : map.getAllDescendents(ChildKind.PROPER)) {
            if (c instanceof LWNode || c instanceof LWImage) { // not 100% sure of this filter...
                final Object key = getMergeKey(c);
                if (key != null)
                    hashedKeys.add(key);
            }
        }
        return hashedKeys;
    }
    
    public boolean mergeKeyPresentOnBaseMap(Object key) {
        return baseMapKeys.contains(key);
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
    
    private int getLinkInterval(double score)
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
    
    public boolean baseMapIsActive()
    {
        if (activeFiles == null || activeFiles.size() == 0)
            return true;
        
        Iterator<Boolean> actives = activeFiles.iterator();
        Iterator<LWMap> maps = mapList.iterator();
        while(actives.hasNext() && maps.hasNext())
        {
            LWMap map = maps.next();
            boolean active = actives.next().booleanValue();
            if (map == baseMap && !active)
                return false;
        }
        return true;
    }
    
    private static Object getMergeKey(LWComponent c) { return edu.tufts.vue.compare.Util.getMergeProperty(c); }

    /* for LWComponent.putclientData */ private static final class Counter { int count = 0; }
    // Note that since clientData is runtime *instance* information, and not copied over on
    // duplication, we don't have to worry about cleaning these up / having them propagate.

    /*
     * FYI: the first time this method sees @param mergeNew during a merge, it happens to be the actual
     * duplicate of sourceNode, and it wont have any counter set yet.  Each time we see it after that,
     * sourceNode is another node with the same merge-key, possibly from a different source map.  (BTW, the
     * choice of which merge-key matching node to use as the actual duplication source is somewhat random: the
     * merge process simply uses the first it comes across in the order we merge the maps.)
     */
    private static void annotateWithSource(LWMap sourceMap, LWComponent sourceNode, LWComponent mergeNew)
    {
        Counter counter = mergeNew.getClientData(Counter.class);
        if (counter == null)
            counter = mergeNew.putClientData(new Counter());
        
        String mapLabel = sourceMap.getDisplayLabel();
        if (mapLabel.endsWith(".vue"))
            mapLabel = mapLabel.substring(0, mapLabel.length()-4);

        final String annotation = String.format("[in:%d:%s/%s/%s]",
                                                ++counter.count,
                                                mapLabel,
                                                sourceNode.getID(),
                                                sourceNode.getDisplayLabel());

        if (DEBUG.TEST) {
            if (mergeNew.hasNotes()) {
                // note: string manips are slow -- could store a StringBuilder in the client-data
                // and run a final baking pass at the end of all the merges.
                mergeNew.setNotes(mergeNew.getNotes()
                                  + (counter.count > 1 ? "\n" : "\n----\n")
                                  + annotation);
            } else {
                mergeNew.setNotes(annotation);
            }
        }

        // This apparently is a special form a meta-data annotation (#TAG?) that only shows up in the UI via a
        // special Resource icon (which never made it to the preferences) that pulls
        // MetadataList.getMetadataAsHTML(type) to display its rollover content.  It depends on a special
        // format for the meta-data to be broken up and formatted in the HTML (actually, it seems to mainly
        // strip "<word>:" off the front).  It does NOT show up in the "Keywords" InspectorPane tab.  Note that
        // this data DOES, however, end up appaering in the RDFIndex, and this can be searched on, tho the user
        // won't have a place to see where the hit came from except the rollover.
            
        final VueMetadataElement vme = new VueMetadataElement();
        vme.setType(VueMetadataElement.OTHER);
        final String stripped = annotation.substring(3, annotation.length()-1);
        vme.setObject("source" + stripped);
        mergeNew.getMetadataList().getMetadata().add(vme);
    }
    
    // edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
    // vme.setType(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
    // vme.setObject("source: " + node.getMap().getLabel() + "," + sourceLabel);
    // c.getMetadataList().getMetadata().add(vme);
        
    public String toString() { return "LWMerge" + super.toString().substring(2); }
    
    //@Override String getDiagnosticLabel() { return "MergeMap: " + getLabel(); }

    
    // // only called by old MergeMapsChooser
    // public void recreateVoteMerge() {
    //     clearAllElements();
    //     fillAsVoteMerge();
    // }
    // public void refillAsVoteMerge() {
    //     clearAllElements();
    //     fillAsVoteMerge();    
    // }


    /** This class is only there to provide something for the old mapping description for LWMergeMap to refer
     * to, which is NOT, in fact, an LWMergeMap.  Eventualy, we can get rid of this and just delete/comment out
     * the old mapping info, but I'm leaving it for now in case we decide we need it.
     */ public static final class HIDE_FROM_CASTOR_MAPPING extends LWMergeMap {static{if(true)throw new Error("should-never-init");}}
    
        
}

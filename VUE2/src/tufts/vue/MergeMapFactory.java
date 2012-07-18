
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
 * MergeMapFactory.java
 *
 * @created 2012-07-17 
 * @author dhelle01 (original LWMergeMap)
 * @author Scott Fraize
 */

package tufts.vue;

import java.util.*;
import java.io.StringWriter;
import java.io.PrintWriter;

import edu.tufts.vue.compare.*;
import edu.tufts.vue.style.*;

import tufts.vue.LWComponent.Flag;
import tufts.vue.LWComponent.ChildKind;
import edu.tufts.vue.metadata.VueMetadataElement;


/**
 * A parameterized factory for constructing merged LWMaps.  Each factory can only be used to create
 * a single LWMap.  The constructed LWMap will contain LWComponent clientData of type
 * MergeMapFactory.class, which for the remainder of the runtime can be tested to see if the map is
 * a merge-map, or queried to view the generating params.
 *
 * Note that the "base map", if it is active (and not excluded) is always the first map we process,
 * which means any nodes found there will be the first encountered with a given merge key, and thus
 * will be the nodes that are duplicated to the final merge map.  Thus, the base-map also serves as
 * a kind of priority template map.
 *
 */

public class MergeMapFactory {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MergeMapFactory.class);

    public static final int THRESHOLD_DEFAULT = 20;
    
    private int nodeThresholdSliderValue = THRESHOLD_DEFAULT;
    private int linkThresholdSliderValue = THRESHOLD_DEFAULT;
    private boolean filterOnBaseMap;
    private boolean excludeNodesOnBaseMap;

    public final LWMap baseMap;
    public final boolean baseMapInactive;
    
    public final LWMap outputMap;
    
    /** Actual maps used to generate the most recent merge. **/
    public final List<LWMap> mapList;
    /** to declare inactive status of individual maps -- if empty or shorter, status defaults to active */
    public final List<Boolean> activeStatus;
    public final List<LWMap> activeMaps;
    
    private List<Double> nodeIntervalBoundaries;
    private List<Double> linkIntervalBoundaries;

    /** keysMerged will be used during construction to track what's already been merged */
    private final Map<Object,LWComponent> keysMerged = new HashMap();
    
    /** if we will be excluding all keys found on the base map from the output, this will be filled */
    private Set baseMapKeys;
    
    private boolean alreadyUsed = false;

    private static int CreationCount = 0;

    public MergeMapFactory(LWMap baseMap, List<LWMap> mapList, List<Boolean> actives) {
        this.baseMap = baseMap;
        this.mapList = new ArrayList(mapList);
        this.activeStatus = new ArrayList(actives);
        this.outputMap = new LWMap(getNextTitle());
        this.activeMaps = computeActiveMaps();
        boolean bma = false;
        for (LWMap m : activeMaps) if (m == baseMap) { bma = true; break; }
        this.baseMapInactive = !bma;
    }

    public LWMap createAsVoteMerge() { return createMergeMap(true); }
    public LWMap createAsWeightMerge() { return createMergeMap(false); }

    private LWMap createMergeMap(boolean asVote) {
        if (alreadyUsed)
            throw new Error(getClass() + ": already used");
        alreadyUsed = true;

        if (asVote)
            createAggregateAndFillMap(VoteAggregate.class);
        else
            createAggregateAndFillMap(WeightAggregate.class);

        annotateMap(asVote);

        baseMapKeys = null; // gc
        keysMerged.clear(); // gc
        
        // we only allow one creation per factory in case anyone
        // ever wants to pull the params out of this client data
        // later.
        outputMap.putClientData(this);
        
        return outputMap;
    }
    
    public void         setFilterOnBaseMap(boolean doFilter) { filterOnBaseMap = doFilter; }
    public boolean      getFilterOnBaseMap() { return filterOnBaseMap; }
    
    public void         setExcludeNodesFromBaseMap(boolean doExclude) { excludeNodesOnBaseMap = doExclude; }
    public boolean      getExcludeNodesFromBaseMap() { return excludeNodesOnBaseMap; }
    
    public void         setNodeThresholdSliderValue(int value) { nodeThresholdSliderValue = value; }
    public int          getNodeThresholdSliderValue() { return nodeThresholdSliderValue; }
    
    public void         setLinkThresholdSliderValue(int value) { linkThresholdSliderValue = value; }
    public int          getLinkThresholdSliderValue() { return linkThresholdSliderValue; }
    
    public boolean      isBaseMapActive() { return !baseMapInactive; }
    
    
    public void         setNodeIntervalBoundaries(List<Double> nib) {
        if (DEBUG.MERGE) { Log.debug("nodeIntervals:"); tufts.Util.dump(nib); }
        nodeIntervalBoundaries = new ArrayList(nib);
    }
    //public List<Double> getNodeIntervalBoundaries() { return nodeIntervalBoundaries; }

    public void         setLinkIntervalBoundaries(List<Double> lib) {
        if (DEBUG.MERGE) { Log.debug("linkIntervals:"); tufts.Util.dump(lib); }
        linkIntervalBoundaries = new ArrayList(lib);
    }
    public List<LWMap> getMapList() { return mapList; }
    
    //public List<Double> getLinkIntervalBoundaries() { return linkIntervalBoundaries; }
    //public void setMapList(List<LWMap> mapList) { this.mapList = mapList; }
    //public void setActiveMapList(List<Boolean> activeMapList) { activeStatus = activeMapList; }

    private static String getNextTitle() {
        return "Merge Map " + (++CreationCount);
    }
    
    /** @return the list of currently active maps (the maps list adjusted by activeStatus)
     * Note that the baseMap is not special in this regard: in can be inactive and not in this list.*/
    private List<LWMap> computeActiveMaps()
    {
        if (activeStatus == null || activeStatus.size() == 0) {
            // no status list specified, thus, all maps are active:
            return mapList;
        }
        final List<LWMap> maps = getMapList();
        final List<LWMap> actives = new ArrayList(maps.size());
        final Iterator<Boolean> statusIter = activeStatus.iterator();

        for (LWMap map : maps) {
            if (statusIter.hasNext()) {
                if (statusIter.next().booleanValue())
                    actives.add(map);
                else
                    /* leave this map out */;
            } else {
                actives.add(map);
            }
        }
        return actives;
    }
    
    private void createAggregateAndFillMap(final Class clazz)
    {
        // Only one of these two will be set to non-null
        final VoteAggregate voteAggregate;
        final WeightAggregate weightAggregate;
        
        final ConnectivityMatrixList<ConnectivityMatrix> cms = new ConnectivityMatrixList<ConnectivityMatrix>();

        //-----------------------------------------------------------------------------
        // Create a connectivity matrix for each active map to be fed to the Aggregate
        //-----------------------------------------------------------------------------
        if (excludeNodesOnBaseMap) {
            // generate the key set that will be used to exclude any keys
            // that were found on the base map.
            this.baseMapKeys = hashMergeKeys(baseMap);
        }
        
        for (LWMap map : activeMaps) {
            // if (map != getBaseMap()) // TODO: check -- really add baseMap matrix if ignoring baseMap?
            // old comment had commented out check to skip baseMap...
            cms.add(new ConnectivityMatrix(map));
        }
        //-----------------------------------------------------------------------------
        // Create the desired aggregate
        //-----------------------------------------------------------------------------
        if (clazz == VoteAggregate.class) {
            double nodeThresh = (double) getNodeThresholdSliderValue() / 100.0;
            double linkThresh = (double) getLinkThresholdSliderValue() / 100.0;
            weightAggregate = null;
            voteAggregate = VoteAggregate.create(cms, nodeThresh, linkThresh);
        } else {
            weightAggregate = WeightAggregate.create(cms);
            voteAggregate = null;
        }
        //-----------------------------------------------------------------------------


        // The base-map is the first map we process, which means any nodes found there with a given
        // key will be the nodes that are duplicated to the final merge map.  Thus, the base-map
        // also serves as a kind of priority template map.
        
        if (excludeNodesOnBaseMap || baseMapInactive) {
            if (DEBUG.MERGE) Log.debug("excluding base-map nodes from merge; active=" + !baseMapInactive);
        } else
            mergeInNodes(baseMap, voteAggregate);
        
        if (! getFilterOnBaseMap()) {
            for (LWMap map : activeMaps) {
                if (map == baseMap) {
                    Log.info("    loop; skipping baseMap " + baseMap);
                } else {
                    mergeInNodes(map, voteAggregate);
                }
            }
        }

        if (voteAggregate != null)
            installLinksForVotes(cms, voteAggregate);
        else
            installLinksAndStylesForWeights(cms, weightAggregate);
        
    }
    
    private void mergeInNodes(final LWMap sourceMap, final VoteAggregate voteAggregate)
    {
        Log.info("mergeInNodes;   adding map " + sourceMap
                 + (sourceMap == baseMap ? " (BASE-MAP)" : "")
                 + "; voter=" + voteAggregate);

        final boolean isVoting = (voteAggregate != null);
                             
        for (LWComponent srcNode : sourceMap.getAllDescendents(ChildKind.PROPER)) {
            if (srcNode.hasFlag(Flag.ICON)) continue;
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
            // weightAggregate was ignored here
            //-----------------------------------------------------------------------------
            // Note: UI is misleading: vote "style" does't just refer to coloring -- it
            // also refers to what goes in the map.
            //-----------------------------------------------------------------------------
            if (isVoting && !voteAggregate.isNodeVoteAboveThreshold(mergeKey))
                continue;

            if (excludeNodesOnBaseMap && baseMapKeys.contains(mergeKey))
                ; // skip this srcNode
            else
                copyInNode(sourceMap, srcNode, mergeKey);
        }
    }

    /** copy the source node to the output map, returning the new duplicate node
     * and record the mergeKey in the keysMerged map */
    private void copyInNode(LWMap sourceMap, LWComponent sourceNode, Object mergeKey) {
        final LWComponent node = sourceNode.duplicate();
        annotateWithSource(sourceMap, sourceNode, node);
        keysMerged.put(mergeKey, node);
        outputMap.addChild(node);
    }

    private void installLinksAndStylesForWeights(final ConnectivityMatrixList cms, final WeightAggregate weightAggregate)
    {
        final Collection<LWComponent> allMergedNodes = outputMap.getAllDescendents(ChildKind.PROPER); 
        
        // todo: use applyCSS(style) -- need to plug in formatting panel
        
        final List<Style> nodeStyles = new ArrayList<Style>();
        final List<Style> linkStyles = new ArrayList<Style>();
        for(int si=0;si<5;si++) 
            nodeStyles.add(StyleMap.getStyle("node.w" + (si +1)));
        for(int lsi=0;lsi<5;lsi++)
            linkStyles.add(StyleMap.getStyle("link.w" + (lsi +1)));
        
        for (LWComponent node : allMergedNodes) {
            if (node instanceof LWNode == false)
                continue;
            double score = 100 * weightAggregate.getNodeCount(getMergeKey(node))/weightAggregate.getCount();
            if(score>100) score = 100; else if(score<0) score = 0;
            final Style currStyle = nodeStyles.get(getIntervalForNode(score)-1);
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
                final Style currLinkStyle = linkStyles.get(getIntervalForLink(score)-1);
                final LWLink link = new LWLink(head, tail);
                if (c2 > 0 && !getFilterOnBaseMap()) {
                    link.setArrowState(LWLink.ARROW_BOTH);
                    weightAggregate.setConnection(tailKey, headKey, 0);
                }
                // todo: applyCSS here
                link.setStrokeColor(Style.hexToColor(currLinkStyle.getAttribute("background")));
                outputMap.addChild(link);
                cms.addLinkSourceMapMetadata(headKey, tailKey, link);
            }
        }
    }

        
    private void installLinksForVotes(final ConnectivityMatrixList cms, final VoteAggregate voteAggregate)
    {
        //-----------------------------------------------------------------------------
        // compute and create links in Merge Map
        //-----------------------------------------------------------------------------

        final Collection<LWComponent> allComponents = outputMap.getAllDescendents(ChildKind.PROPER);
        final List<LWComponent> linkables = new ArrayList(allComponents.size() / 2);

        for (LWComponent c : allComponents) {
            // We only generate links between nodes and images -- tho this is a double-check
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
                        // note: below is crazy slow... it iterates every link in each and every
                        // input map every time.  This is to install the #TAG mostly-silent meta-data
                        // for any link that it finds whose endpoint merge-keys are a match to
                        // these two keys.
                        cms.addLinkSourceMapMetadata(headKey, tailKey, link);
                        // finally add the link
                        outputMap.addChild(link);
                    }
                }
            }
        }
    }
    

    private static Set hashMergeKeys(LWMap map) {
        final Set hashedKeys = new HashSet();
        for (LWComponent c : map.getAllDescendents(ChildKind.PROPER)) {
            if (c.hasFlag(Flag.ICON)) continue;
            if (c instanceof LWNode || c instanceof LWImage) {
                final Object key = getMergeKey(c);
                if (key != null)
                    hashedKeys.add(key);
            }
        }
        return hashedKeys;
    }
    
    private int getIntervalForNode(double score)
    {
        int count = 0;
        for (Double d : nodeIntervalBoundaries) {
            if (score < d.doubleValue())
                return count;
            count++;
        }
        return 0;
    }
    
    private int getIntervalForLink(double score)
    {
        int count = 0;
        for (Double d : linkIntervalBoundaries) {
            if (score < d.doubleValue())
                return count;
            count++;
        }
        return 0;
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
        
        // We could store a list of all the actual source nodes in the client data, and annotate
        // at the end.  That could even allow us to design a fancier "merge" that somehow lets us
        // combine the properties of the merged nodes, instead of the random "duplicate the 1st
        // found" method we have now.
        
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
                // note: string manips are slow.
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
        // strip "allBefore1stColon:" off the front).  It does NOT show up in the "Keywords" InspectorPane tab.  Note that
        // this data DOES, however, end up appaering in the RDFIndex, and this can be searched on, tho the user
        // won't have a place to see where the hit came from except the rollover.
            
        final VueMetadataElement vme = new VueMetadataElement();
        vme.setType(VueMetadataElement.OTHER);
        final String stripped = annotation.substring(3, annotation.length()-1);
        vme.setObject("source" + stripped);
        mergeNew.getMetadataList().getMetadata().add(vme);
    }

    private void annotateMap(boolean asVote)
    {
        final StringWriter buf = new StringWriter(64);
        final PrintWriter p = new PrintWriter(buf);

        p.println("Merge map: " + new Date());
        p.println("Merge property: " + edu.tufts.vue.compare.Util.getMergeProperty());
        p.println("Merge style: by " +  (asVote ? "vote" : "weight"));
        if (asVote) {
            p.printf("Node vote threshold: %d%%\n", nodeThresholdSliderValue);
            p.printf("Link vote threshold: %d%%\n", nodeThresholdSliderValue);
        } // could include weight intervals../
        p.println("Sources: " + activeMaps.size());
        p.println("Source: " + baseMap.getDisplayLabel() + " (base)");

        for (LWMap map : activeMaps) {
            if (map != baseMap)
                p.println("Source: " + map.getDisplayLabel());
        }

        outputMap.setNotes(buf.toString());
    }
        
    // [DAN] old method for recording source nodes code currently does not compile
    // (its commented out below) - needs adjustment to 
    // LWComponent from LWNode (for both LWImage and LWNode)
    // a relatively minor fix and probably
    // also needs to be stored in special metadata of a new type...
    // (since otherwise this info always appears in the notes and possibly
    // out of sight)
    // **new system is to use VueMetadataElement.OTHER (see below)**
    // public static final boolean RECORD_SOURCE_NODES = false;
    // edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
    // vme.setType(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
    // vme.setObject("source: " + node.getMap().getLabel() + "," + sourceLabel);
    // c.getMetadataList().getMetadata().add(vme);
        
    // // todo: this should become only the default initialization method
    // // for interval boundaries -- in fact: implementing 4/1/2008
    // private void setIntervalBoundaries()
    // {
    //     nodeIntervalBoundaries = new ArrayList<Double>();
    //     for(int vai = 0;vai<6;vai++) {
    //         double va =  20*vai + 0.5;
    //        nodeIntervalBoundaries.add(new Double(va));
    //     } 
    //     linkIntervalBoundaries = new ArrayList<Double>();
    //     for(int vai = 0;vai<6;vai++) {
    //         double va =  20*vai + 0.5;
    //         linkIntervalBoundaries.add(new Double(va));
    //     } 
    // }
    
    public String toString() { return getClass().getSimpleName() + "=" + outputMap.getLabel(); }

}

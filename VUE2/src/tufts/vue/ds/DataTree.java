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

// TODO: see http://publicobject.com/glazedlists/ as a possible baseline for a much
// fancier data interface -- tabular, and big, but may have enough flexibility
// to still be handy.

package tufts.vue.ds;

import tufts.vue.VUE;
import tufts.vue.DEBUG;
import tufts.vue.Resource;
import tufts.vue.VueResources;
import tufts.vue.LWComponent;
import tufts.vue.LWNode;
import tufts.vue.LWMap;
import tufts.vue.LWKey;
import tufts.vue.gui.GUI;
import tufts.Util;
import tufts.vue.VueConstants;

import java.util.List;
import java.util.*;
import java.net.URL;
import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.geom.RectangularShape;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.google.common.collect.*;

/**
 *
 * @version $Revision: 1.71 $ / $Date: 2009-06-01 01:11:35 $ / $Author: sfraize $
 * @author  Scott Fraize
 */

public class DataTree extends javax.swing.JTree
    implements DragGestureListener, LWComponent.Listener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataTree.class);
    
    private final Schema mSchema;

    private DataNode mRootNode;
    private DataNode mAllRowsNode;
    private DataNode mSelectedSearchNode;
    private final AbstractButton mAddNewRowsButton = new JButton(VueResources.getString("button.addnewrectomap.label"));
    private final AbstractButton mApplyChangesButton = new JButton(VueResources.getString("button.appchngtomap.label"));
    private final DefaultTreeModel mTreeModel;

    private volatile LWMap mActiveMap;

    private Thread mAnnotateThread;

    //private final Object annotateLock = new Object();

    public static JComponent create(Schema schema) {
        final DataTree tree = new DataTree(schema);

        //tree.setBorder(new LineBorder(Color.red, 4));
        
        tree.activeChanged(null, VUE.getActiveMap()); // simulate event for initial annotations
        VUE.addActiveListener(LWMap.class, tree);

        return buildControllerUI(tree);
    }

    private void addNewRowsToMap() {
        // failsafe: tho the Schema and our tree nodes should already
        // be updated, make absolutely certian we're current to the
        // active map by running adding new rows based on our detection
        // of the rows already in the map.
        annotateForMap(mActiveMap);
        addNewRowsToMap(mActiveMap);
    }
    private void applyChangesToMap() {
        // failsafe: tho the Schema and our tree nodes should already
        // be updated, make absolutely certian we're current to the
        // active map by running adding new rows based on our detection
        // of the rows already in the map.
        annotateForMap(mActiveMap);
        applyChangesToMap(mActiveMap);
    }
    
    private void applyChangesToMap(final LWMap map) {

        final Map<String,DataRow> freshData = new HashMap();
        final Field keyField = mSchema.getKeyField();
        final String keyFieldName = keyField.getName();

        for (DataNode n : mAllRowsNode.getChildren()) {
            final DataRow row = n.getRow();
            if (row.isContextChanged()) {
                //Log.debug("Context changed: " + Util.tag(row));
                String keyValue = row.getValue(keyField);
                freshData.put(keyValue, row);
            }
        }

        if (DEBUG.Enabled) Log.debug("Found " + freshData.size() + " data rows with newer data for map");

        final Collection<LWComponent> nodes = map.getAllDescendents();
        final Collection<LWComponent> patched = new ArrayList();
        
        for (LWComponent c : nodes) {
            if (c.isDataRow(mSchema)) {
                DataRow newRow = freshData.get(c.getDataValue(keyFieldName));
                if (newRow != null) {
                    //Log.debug("patching " + c);
                    c.setDataMap(newRow.getData());
                    patched.add(c);
                }
            }
        }

        if (DEBUG.Enabled) Log.debug("Updated " + patched.size() + " nodes with fresh data");
        
        runAnnotate();

        VUE.getSelection().setTo(patched);

        map.getUndoManager().mark(String.format("Update %d Data Nodes", patched.size()));
    }
    

    private void addNewRowsToMap(final LWMap map) {

        final List<DataRow> newRows = new ArrayList();

        for (DataNode n : mAllRowsNode.getChildren()) {
            if (!n.isMapPresent()) {
                //Log.debug("ADDING TO MAP: " + n);
                newRows.add(n.getRow());
            }
        }

        final List<LWComponent> nodes = DataAction.makeRowNodes(mSchema, newRows);

        try {
            DataAction.addDataLinksForNodes(map, nodes, null);
        } catch (Throwable t) {
            Log.error("problem creating links on " + map + " for new nodes: " + Util.tags(nodes), t);
        }

        if (nodes.size() > 0) {
            map.getOrCreateLayer("New Data Nodes").addChildren(nodes);

            if (nodes.size() > 1)
                tufts.vue.LayoutAction.table.act(nodes);

            VUE.getSelection().setTo(nodes);
        }

        map.getUndoManager().mark("Add New Data Nodes");
    }

    private static JComponent buildControllerUI(final DataTree tree)
    {
        final Schema schema = tree.mSchema;
        final JPanel wrap = new JPanel(new BorderLayout()) {
                @Override public void firePropertyChange(String property, boolean oldVal, boolean newVal) {
                    if (tufts.vue.gui.GUI.FINALIZE.equals(property)) {
                        if (DEBUG.Enabled) Log.debug("firePropertyChange: " + property);
                        tree.destroy();
                    } else {
                        super.firePropertyChange(property, oldVal, newVal);
                    }
                }
            };

                
        final JPanel toolbar = new JPanel();
        toolbar.setOpaque(true);
        toolbar.setBackground(Color.white);
        //toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setLayout(new BorderLayout());
        //p.add(new JLabel(s.getSource().toString()), BorderLayout.NORTH);

        //addNew.setBorderPainted(false);
        tree.mAddNewRowsButton.setOpaque(false);
        tree.mApplyChangesButton.setOpaque(false);
        tree.mAddNewRowsButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    tree.addNewRowsToMap();
                }
            });
        tree.mApplyChangesButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    tree.applyChangesToMap();
                }
            });

        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        tree.setBorder(GUI.makeSpace(3,0,0,0));

            
        JLabel dataSourceLabel = null;
            
        String imagePath = schema.getSingletonValue("rss.channel.image.url");
        if (imagePath == null)
            imagePath = schema.getSingletonValue("rdf:RDF.image.url");
        if (imagePath != null) {
            URL imageURL = Resource.makeURL(imagePath);
            if (imageURL != null) {
                dataSourceLabel = new JLabel(new ImageIcon(imageURL));
                dataSourceLabel.setBorder(GUI.makeSpace(2,2,1,0));
            }
            //addNew.setIcon(new ImageIcon(imageURL));
            //addNew.setLabel(imageURL);
        }

//         JComboBox keyBox = new JComboBox(schema.getPossibleKeyFieldNames());
//         keyBox.setOpaque(false);
//         keyBox.setSelectedItem(schema.getKeyField().getName());
//         keyBox.addItemListener(new ItemListener() {
//         	public void itemStateChanged(ItemEvent e) {
//                     if (e.getStateChange() == ItemEvent.SELECTED) {
//                         String newKey = (String) e.getItem();
//                         //Log.debug("KEY FIELD SELECTED: " + newKey);
//                         schema.setKeyField(newKey);
//                         tree.refreshRoot();
//                     }
//         	}
//             });
//         toolbar.add(keyBox, BorderLayout.WEST);
//         toolbar.add(addNew, BorderLayout.EAST);
        
        toolbar.add(tree.mAddNewRowsButton, BorderLayout.NORTH);
        toolbar.add(tree.mApplyChangesButton, BorderLayout.SOUTH);

        if (dataSourceLabel != null)
            wrap.add(dataSourceLabel, BorderLayout.SOUTH);
            
//         if (dataSourceLabel == null) {
//             //                 dataSourceLabel = new JLabel(schema.getName());
//             //                 dataSourceLabel.setFont(tufts.vue.VueConstants.SmallFont);
//             //                 dataSourceLabel.setBorder(GUI.makeSpace(0,2,0,0));
//             //                 toolbar.add(dataSourceLabel, BorderLayout.WEST);
//             //                 toolbar.add(addNew, BorderLayout.EAST);
//             toolbar.add(addNew, BorderLayout.CENTER);
//         } else {
//             toolbar.add(dataSourceLabel, BorderLayout.WEST);
//             toolbar.add(addNew, BorderLayout.EAST);
//         }
            
        toolbar.setBorder(new MatteBorder(0,0,1,0, Color.gray));
            
        wrap.add(toolbar, BorderLayout.NORTH);
        // todo: if save entire schema with map, include date of creation (last refresh before save)
        wrap.add(tree, BorderLayout.CENTER);
        return wrap;
    }
    

    @Override
    protected void setExpandedState(final TreePath path, final boolean state) {
        if (DEBUG.Enabled) Log.debug("setExpandedState " + path + " = " + state);
        // we can interrupt tree expansion here on our double-clicks for searches
        // (which may obviate part of the workaround we needed with the ClearSearchMouseListener,
        // tho not for the JScrollPane problem if that is really happening)

        GUI.invokeAfterAWT(new Runnable() { public void run() {
            if (!inDoubleClick) {
                if (DEBUG.FOCUS) Log.debug("setExpandedState " + path + " = " + state + " RELAYING");
                DataTree.super.setExpandedState(path, state);
            } else 
                if (DEBUG.FOCUS) Log.debug("setExpandedState " + path + " = " + state + " SKIPPING");
            inDoubleClick = false;
        }});
    }

    private boolean inDoubleClick;

    private class ClickHandler extends tufts.vue.MouseAdapter
    {
        private TreePath mClickPath;
        
        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
            mClickPath = getPathForLocation(e.getX(), e.getY());
            if (DEBUG.Enabled) {
                //Log.debug("MOUSE PRESSED ON " + Util.tags(mClickPath));
                if (mClickPath != null)
                    Log.debug("MOUSE PRESSED ON: " + Util.tags(mClickPath.getLastPathComponent()));
                else
                    Log.debug("MOUSE PRESSED ON: nothing");
            }
            
            // it's possible that the node under the mouse changes from the time of the
            // first press, to the time mouseClicked is called (e.g., due to tree
            // expansion and/or possible scrolling of the entire tree component), so we
            // capture it here.  Could make this a class a generic subclassable helper
            // class for JTree's.

            inDoubleClick = GUI.isDoubleClick(e);
            if (DEBUG.Enabled) Log.debug("IN DOUBLE CLICK = " + inDoubleClick);
            
        }
                
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e)
        {
            if (mClickPath == null)
                return;

            final DataNode treeNode = (DataNode) mClickPath.getLastPathComponent();
            
            if (GUI.isSingleClick(e)) {
                if (mSelectedSearchNode == treeNode) {
                    // re-run search: we're clicking on already selected, and can't select it again
                    selectMatchingNodes(treeNode, false);
                }
                return;
            }
            
            if (!GUI.isDoubleClick(e))
                return;

            if (DEBUG.Enabled) Log.debug("ACTIONABLE DOUBLE CLICK ON " + Util.tags(treeNode));

            if (treeNode.hasStyle()) {
                final tufts.vue.LWSelection selection = VUE.getSelection();
                selection.setSource(DataTree.this);
                // prevents from ever drawing through on map:
                selection.setSelectionSourceFocal(null);
                selection.setTo(treeNode.getStyle());
            } else if (treeNode.isRow() ||
                       (treeNode.getField() != null && treeNode.getField().isPossibleKeyField())) {
                selectMatchingNodes(treeNode, false);
            }
        }

    }

    private void selectMatchingNodes(final DataNode treeNode, final boolean extendSearch)
    {
        if (mActiveMap == null)
            return;

        // we search only amongst EDITBALE nodes, so that we ignore hidden/locked layers & nodes, etc
        final Collection<LWComponent> searchSet = mActiveMap.getAllDescendents(LWComponent.ChildKind.EDITABLE);

        if (DEBUG.Enabled) Log.debug("SEARCH:\n\nSEARCHING ALL EDITABLE DESCENDENTS of " + mActiveMap + "; count=" + searchSet.size());
        
        findAndSelectMatchingNodes(searchSet, treeNode, extendSearch);
    }

    private SmartSearch mCurrentSearch;
    private Criteria mLastCriteria;

    /**
     * Note: This method also has the side effect of picking an active style record for
     * the selection if the DataNode represents a Field (the style for all enumerated
     * values on the map from that Field), as well as setting a description in the
     * selection of the search that produced it.
     */
    private void findAndSelectMatchingNodes(final Collection<LWComponent> searchSet,
                                            final DataNode treeNode,
                                            final boolean extendSearch)
    {
        Field field = treeNode.getField();
        LWComponent styleRecord = null;

        if (field == null) {
            if (treeNode == mAllRowsNode || treeNode instanceof RowNode) {
                field = mSchema.getKeyField();
                if (treeNode == mAllRowsNode)
                    styleRecord = mSchema.getRowNodeStyle();
            } else {
                // todo: must be root node: select a row-node items
                return;
            }
        }

        if (extendSearch) {
            // only use the style record for single criteria searches; otherwise
            // makes no sense -- can't hang a style off a search, only single Fields
            styleRecord = null;
        } else {
            if (treeNode.isField() && styleRecord == null)
                styleRecord = field.getStyleNode();
        }

        final String fieldName = field.getName();

        final Criteria criteria = dataNodeToSearchCriteria(treeNode);

        final List<LWComponent> hits;
        String desc;
            
        if (extendSearch) {

            if (mCurrentSearch == null) {
                mCurrentSearch = new SmartSearch();
                mCurrentSearch.addCriteria(mLastCriteria);
                mLastCriteria = null;
            }
            mCurrentSearch.addCriteria(criteria);
            
            Log.debug("Running search " + mCurrentSearch);
            hits = mCurrentSearch.search(searchSet);

            desc = mCurrentSearch.toString();
            
        } else {

            // if we've got a single Criteria, no need to mess with a SmartSearch

            mCurrentSearch = null;

            hits = new ArrayList();

            Log.debug("SEARCHING WITH CRITERIA " + criteria);

            for (LWComponent c : searchSet)
                if (criteria.matches(c))
                    hits.add(c);

            mLastCriteria = criteria;

            desc = criteria.description();
            
        }

        desc = "matching<br>" + desc;

        if (DEBUG.Enabled) {
            if (hits.size() == 1)
                Log.debug("hits=" + hits.get(0) + " [single hit]");
            else
                Log.debug("hits=" + hits.size());
            Log.debug("styleRecord: " + styleRecord);
            //if (styleRecord != null) desc += "<p>style: " + styleRecord;
        }


        final tufts.vue.LWSelection selection = VUE.getSelection();
        // make sure selection bounds are drawn in MapViewer:
        selection.setSelectionSourceFocal(VUE.getActiveFocal());
        // now set the selection, along with a description
        selection.setTo(hits, desc, styleRecord);
    }

    private Criteria dataNodeToSearchCriteria(final DataNode treeNode) {

        final Field field = treeNode.getField();
        final String fieldName = field == null ? null : field.getName();

        Criteria criteria = null;

        if (treeNode == mAllRowsNode) {

            // search for ANY row-node in the schema
            Log.debug("searching for all data records in schema " + mSchema);

            criteria = new SchemaMatch(mSchema);
        }

        else if (treeNode.isRow()) {
            // search for a particular row-node in the schema based on the key field -- this will
            // normally only find a single node on the map, unless there are duplicate nodes on
            // the map referencing the same row
            final String keyValue = treeNode.getRow().getValue(fieldName);

            criteria = new ValueMatch(fieldName, keyValue);
        }

        else if (treeNode.isField()) {

            // search for all nodes anchoring a particular value for the given Field
            Log.debug("searching for any occurance of a field named " + fieldName);

            criteria = new FieldMatch(fieldName);
        }

        else if (treeNode.isValue()) {

            // search for a particular key=value
            
            final String fieldValue = treeNode.getValue();
            
            Log.debug(String.format("searching for %s=[%s]", fieldName, fieldValue));

            criteria = new ValueMatch(fieldName, fieldValue);
        }

        return criteria;
    }

    // todo: better to move all this Search stuff to a DataSearch.java or some such.
    
    public abstract static class Criteria {
        boolean matches(LWComponent c) {
            throw new UnsupportedOperationException("unimplemented matches in " + this);
        }
        
        abstract String description();
        String getKey() { return null; }

        public List<LWComponent> search(final Collection<LWComponent> searchSet) {
            final List<LWComponent> hits = new ArrayList();

            for (LWComponent c : searchSet)
                if (matches(c))
                    hits.add(c);

            return hits;
        }
        
        @Override public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), description());
        }
        
    }

    public static class SchemaMatch extends Criteria {
        final Schema schema;
        public SchemaMatch(Schema s) {
            schema = s;
        }
        @Override public boolean matches(LWComponent c) {
            return c.isDataRow(schema);
        }
        @Override public String description() {
            return String.format("in data set: <i>%s</i>", schema.getName());
        }
    }

    public static class FieldMatch extends Criteria {
        final String key;
        public FieldMatch(String fieldName) {
            key = fieldName;
        }
        @Override public boolean matches(LWComponent c) {
            return c.isDataValueNode(key);
        }
        @Override public String description() {
            return String.format("enumerated values of: <b>%s</b>", key);
        }
        //@Override String getKey() { return key; }

        @Override public String toString() {
            return String.format("enumerated values of <i>%s</i>", key);
        }
        
    }
    
    public static class ValueMatch extends Criteria {
        final String key;
        final String value;
        public ValueMatch(String k, String v) {
            key = k;
            value = v;
        }
        @Override public boolean matches(LWComponent c) {
            return c.hasDataValue(key, value);
        }
        @Override public String description() {
            return String.format("<b>%s: <i>%s</i>", key, valueName(value));
        }
        @Override String getKey() { return key; }

        @Override public String toString() {
            return String.format("%s=%s", key, value);
        }
        
    }

    /**
     * A multiple criteria search that is automagically smart about how to create boolean
     * AND and OR groups to create reasonable searches.
     */
    public static class SmartSearch /*extends Crtieria*/ {

        /** a boolean AND group of OR lists for each key used in any key=value searches present */
        final Multimap<String,Criteria> criteriaByKey = Multimaps.newHashMultimap();
        /** a special OR group that takes priority: anything matching criteria in this group
         * is "hit" no matter what */
        final Collection<Criteria> globalBooleanOr = new ArrayList();
        
        public void addCriteria(Criteria criteria) {

            if (DEBUG.Enabled) Log.debug("SmartSearch adding " + criteria);
            
            if (criteria == null)
                return;

            final String key = criteria.getKey();
            if (key != null) {
                criteriaByKey.put(key, criteria);
            } else {
                
                // note: this impl means any search containing a SchemaMatch will match ALL
                // rows in the schema, and it will only be meaninful to add Fields to
                // the search (not values selecting particular rows, as they'll allready
                // be all selected).

                globalBooleanOr.add(criteria);
            }
        }

        public List<LWComponent> search(final Collection<LWComponent> searchSet) {
            final List<LWComponent> hits = new ArrayList();

            if (DEBUG.Enabled) {

                for (Criteria c : globalBooleanOr)
                    Log.debug("GlobalOR: " + c);
                
                final Collection<Map.Entry<String,Collection<Criteria>>> allKeyEntries = criteriaByKey.asMap().entrySet();
                
                for (Map.Entry e : allKeyEntries) {
                    Log.debug(String.format("Key: %-12s criteria=%s", e.getKey(), e.getValue()));
                }
            }
                
            final Collection<Collection<Criteria>> keyBasedCriteria;

            if (criteriaByKey.size() > 0)
                keyBasedCriteria = criteriaByKey.asMap().values();
            else
                keyBasedCriteria = null;

            if (globalBooleanOr.size() > 0) {
                // search method 1: includes the global OR tests
                for (LWComponent c : searchSet) {
                    if (anyCriteriaMatches(c, globalBooleanOr))
                        hits.add(c);
                    else if (keyBasedCriteria != null && allGroupsMatch(c, keyBasedCriteria))
                        hits.add(c);
                }
            } else if (keyBasedCriteria != null) {
                // search method 2: just the AND'd group of OR tests
                for (LWComponent c : searchSet) {
                    if (allGroupsMatch(c, keyBasedCriteria)) {
                        hits.add(c);
                    }
                }
            }

            return hits;
        }

        /** perform a boolean AND of a bunch of OR groups: at least one criteria must match from each collection of criteria
         * Note: if the group is empty (no collections of Criteria), this will always return TRUE */
        private boolean allGroupsMatch(final LWComponent c,
                                       final Collection<Collection<Criteria>> booleanAndGroup)
        {
            for (Collection<Criteria> booleanOrGroup : booleanAndGroup) {
                boolean atLeastOneMatched = false;
                
                //Log.debug(c.getUniqueComponentTypeLabel() + "; against " + booleanOrGroup);
                
                // todo: could make this even faster by exploiting the underlying
                // Multimap impl in the LWComponent MetaMap to have it pull all values
                // for the key, which should be a Set, and just check for set membership
                // for each of the values we're looking for under that key.  Could pull
                // the key to use from the first criteria in the group, or just pass in
                // Map.Entry's of the keys and their collections of criteria.  We'd
                // abandon the matches(c) API and manually pull the value from key=value
                // criteria to check the set membership.
                
                for (Criteria criteria : booleanOrGroup) {
                    if (criteria.matches(c)) {
                        //Log.debug(c.getUniqueComponentTypeLabel() + "; ok");
                        atLeastOneMatched = true;
                        break;
                    }
                }
                
                if (!atLeastOneMatched) {
                    // nothing matched in this or group: entire search immediately fails
                    //Log.debug(c.getUniqueComponentTypeLabel() + " failed; no values matched in key");
                    return false;  // immediate boolean short-circuit
                }
            }

            return true;
        }

        /** perform a boolean OR for a group of criteria */
        private boolean anyCriteriaMatches(final LWComponent c, final Collection<Criteria> booleanOrGroup)
        {
            for (Criteria criteria : booleanOrGroup) {
                if (criteria.matches(c)) {
                    //Log.debug(criteria + " hit " + c);
                    return true; // immediate boolean short-circuit
                }
            }
            
            return false;
        }
        
        
        @Override
        public String toString() {

            final Collection<Collection<Criteria>> groupedByKey = criteriaByKey.asMap().values();
            
            StringBuilder b = new StringBuilder("search terms:<br>");
            for (Criteria c : globalBooleanOr) {
                b.append(c.description());
                b.append("<br>");
            }
            
            for (Collection<Criteria> eachKey : groupedByKey) {
                for (Criteria c : eachKey) {
                    b.append(c.description());
                    b.append("<br>");
                }
            }
            return b.toString();
        }
        
    }

    /** if the active map changes, we need to wake the annotation thread to re-annotate against the newly active map,
     * as well as start listening for changes in the active map for running future annotation updates */
    public void activeChanged(tufts.vue.ActiveEvent e, final LWMap map)
    {
        if (mActiveMap == map)
            return;

        if (mActiveMap != null)
            mActiveMap.removeLWCListener(this);

        mActiveMap = map;

        if (map == null)
            return;

        runAnnotate(); // mActiveMap must be pre-set
        
        mActiveMap.addLWCListener(this);
    }

    // TODO: create a single handler for listening to user changes to the current map
    // (UseActionCompleted), that keeps a background thread running for the annotations
    // & refresh triggers.  It will always FIRST annotate and refresh the VISIBLE
    // DataTree(s), then continue with the others.  Ultimately interruptable, so if a
    // second update comes through before the current pass is completed, the prior
    // update can be aborted.  Also, the running on a special thread handling could
    // somehow be build into generic change support, where when the listener is added it
    // can either request to happen on it's own thread, and/or provide one that will be
    // woken up / interrupted.
    
    public void LWCChanged(tufts.vue.LWCEvent e) {

        if (mActiveMap != null && e.key == LWKey.UserActionCompleted) {
            
            // technically, don't need to check after ANY action has been completed:
            // only if a data node was added/removed from the map.  todo: we'll need
            // a data-changed LWCEvent.
            
            runAnnotate();
        }
    }

    private void runAnnotate() {

        GUI.invokeOnEDT(new Runnable() { public void run() {
            mAddNewRowsButton.setEnabled(false);
            mAddNewRowsButton.setLabel("Comparing to " + mActiveMap.getLabel() + "...");
            mApplyChangesButton.setEnabled(false);
        }});
        //Log.info("WAKING " + mAnnotateThread, new Throwable("HERE"));
        if (DEBUG.Enabled) Log.debug("**WAKING ANNOTATION THREAD " + mAnnotateThread + "; pri=" + mAnnotateThread.getPriority());

        if (mAnnotateThread.getPriority() > Thread.NORM_PRIORITY) {
            // first time it will be at MAX_PRIORITY
            mAnnotateThread.setPriority(Thread.NORM_PRIORITY);
        }

        // note: if we're already on a thread that's NOT the AWT EDT, we could
        // assume we're in a builder thread and instead of waking the annotation
        // thread just run in the builder thread, tho better to keep all that
        // done there as sometimes that can take a whilte to run, and this will
        // allow initial tree creations to run faster.
        
        synchronized (mAnnotateThread) {
            mAnnotateThread.notify();
        }
        if (DEBUG.Enabled) Log.debug("NOTIFIED ANNOTATION THREAD " + mAnnotateThread);
    }

    private boolean annotateForMap(final LWMap map)
    {
        if (DEBUG.Enabled) Log.debug("ANNOTATING against " + map);
        
        DataAction.annotateForMap(mSchema, map);

        if (map != null) {
            final String annot = map.getLabel();
            for (DataNode n : mRootNode.getChildren()) {
                if (Thread.interrupted()) return true;
                n.annotate(map);
                if (!n.isLeaf())
                    for (DataNode cn : n.getChildren()) {
                        if (Thread.interrupted()) return true;
                        cn.annotate(map);
                    }
            }
        }

        int _newRowCount = 0;
        int _changedRowCount = 0;
        for (DataNode n : mAllRowsNode.getChildren()) {
            if (!n.isMapPresent())
                _newRowCount++;
            if (n.isContextChanged())
                _changedRowCount++;
        }

        final int newRowCount = _newRowCount;
        final int changedRowCount = _changedRowCount;
        
        if (DEBUG.Enabled) Log.debug("annotateForMap: newRows " + newRowCount + "; changedRows " + changedRowCount);

        GUI.invokeOnEDT(new Runnable() { public void run() {
            
            if (newRowCount > 0) {
                mAddNewRowsButton.setLabel(String.format("Add %d new records to Map", newRowCount));
                //mAddNewRowsButton.setIcon(NewToMapIcon);
                mAddNewRowsButton.setEnabled(true);
            } else {
                //mAddNewRowsButton.setIcon(null);
                mAddNewRowsButton.setLabel("All Records are represented on Map");
                mAddNewRowsButton.setEnabled(false);
            }
            if (changedRowCount > 0) {
                mApplyChangesButton.setLabel(String.format("Update %d records on Map", changedRowCount));
                mApplyChangesButton.setEnabled(true);
            } else {
                mApplyChangesButton.setLabel("No Changed Records");
                mApplyChangesButton.setEnabled(false);
            }

            // TODO: don't bother with refresh if annotations didn't change at all
            refreshAll();
        }});

        return false;
    }

//     private void refreshRoot() {
//         if (DEBUG.Enabled) Log.debug("REFRESHING " + Util.tags(mRootNode));
//         refreshAllChildren(mRootNode);
//     }

    private void refreshAll()
    {
        //mTreeModel.reload(mRootNode);
        
        // using nodesChanged instead of reload preserves the expanded state of nodes in the tree

        if (DEBUG.Enabled) Log.debug("REFRESHING " + Util.tags(mRootNode.getChildren()));
        refreshAllChildren(mRootNode);
        //refreshRoot();
        for (TreeNode n : mRootNode.getChildren())
            if (!n.isLeaf())
                refreshAllChildren(n);
        if (DEBUG.Enabled) Log.debug(" REFRESHED " + Util.tags(mRootNode.getChildren()));

        // This gets close, but doesn't always handle updating NON expanded nodes, plus
        // it often leaves labels truncated with "..."
        // invalidate();
        // super.treeDidChange();
    }

    private void refreshAllChildren(TreeNode node)
    {
        final int[] childIndexes = new int[node.getChildCount()];
        for (int i = 0; i < childIndexes.length; i++)
            childIndexes[i] = i; // why there's isn't an API to do this automatically, i don't know...

        // using nodesChanged instead of mTreeModel.reload preserves the expanded state of nodes in the tree
        if (DEBUG.META) Log.debug("refreshing " + childIndexes.length + " children of " + node);
        mTreeModel.nodesChanged(node, childIndexes);
    }

    private void refreshRootNode() {
        mTreeModel.nodesChanged(mRootNode, new int[] { 0 });
    }
    

    @Override
    public String toString() {
        return String.format("DataTree[%s]", mSchema.toString());
    }

    private static volatile int AnnotationThreadCount = 0;

    private void destroy() {
        if (DEBUG.Enabled) Log.debug("destroying w/" + mAnnotateThread);
        mAnnotateThread.interrupt();
        mAnnotateThread = null;

        // it's crucial to flush the old schema so that if it isn't reloaded with new
        // data (a new Schema instance is created when/if this schema is reloaded), the
        // old schema will be empty and will no longer match to any nodes on any map.
        // todo: the XmlDataSource impl current decides which happens (e.g., re-loaded
        // for .csv, new instances for XML) -- if we keep the new-instance
        // functionality, eventually we should actually remove the defunct schema's from
        // the Schema global instance lists, instead of just leaving them in there but
        // empty.
        
        mSchema.flushData();
        
        if (mActiveMap != null)
            mActiveMap.removeLWCListener(this);
        VUE.removeActiveListener(LWMap.class, this);
    }

    private DataTree(final Schema schema) {

        mSchema = schema;

        setCellRenderer(new DataRenderer());
        //setSelectionModel(null);

        setModel(mTreeModel = new DefaultTreeModel(buildTree(schema), false));

        final int ac = AnnotationThreadCount++;

        mAnnotateThread = new Thread(String.format("Annotate%d: %s", ac, schema.getName())) {
                { setPriority(MAX_PRIORITY); }
                public synchronized void run() {
                    while (true) {
                        try {
                            // must be careful: if we get a notify before the 1st time
                            // we go to sleep, we'll never wake up!  So we start this
                            // thread at high priority, and kick it off immediately,
                            // because as soon as the DataTree is done constructing,
                            // we're going to get notified the first time -- still
                            // theoretically risky but should work.
                            
                            if (DEBUG.Enabled) Log.debug("annotation thread sleeping, pri=" + getPriority());
                            wait();
                        } catch (InterruptedException e) {
                            Log.error("interrupted; exiting; " + schema);
                            return;
                        }
                        if (DEBUG.Enabled) Log.debug("annotation thread woke, pri=" + getPriority() + "; running...");
                        final boolean interrupted = annotateForMap(mActiveMap);
                        if (DEBUG.Enabled) {
                            if (interrupted)
                                Log.debug("annotation aborted");
                            else
                                Log.debug("annotation completed");
                        }
                    }
                }
            };

        if (DEBUG.Enabled) Log.debug("STARTING " + mAnnotateThread + "; (tree constructing)");
        mAnnotateThread.start();
        
        setRowHeight(0);
        setRootVisible(false);
        setShowsRootHandles(true);
        
        java.awt.dnd.DragSource.getDefaultDragSource()
            .createDefaultDragGestureRecognizer
            (this,
             java.awt.dnd.DnDConstants.ACTION_COPY |
             java.awt.dnd.DnDConstants.ACTION_MOVE |
             java.awt.dnd.DnDConstants.ACTION_LINK,
             this);

        addMouseListener(new ClickHandler());

        addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
                public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
                    //final TreePath[] paths = e.getPaths();
                    final TreePath[] paths = getSelectionModel().getSelectionPaths();

                    if (DEBUG.Enabled) Log.debug("valueChanged: isAddedPath=" + e.isAddedPath() + "; PATHS:");
                    if (DEBUG.Enabled) Util.dumpArray(paths);
                    //if (DEBUG.Enabled) Log.debug("OLD LeadPath: " + e.getOldLeadSelectionPath());
                    //if (DEBUG.Enabled) Log.debug("NEW LeadPath: " + e.getNewLeadSelectionPath());

                    // TODO: change from checking getPaths to model.getSelectionPaths & ignoring isAddedPath
//                     if (!e.isAddedPath() || e.getPath().getLastPathComponent() == null)
//                         return;

                    final DataNode treeNode = (DataNode) e.getPath().getLastPathComponent();
                    if (treeNode instanceof RowNode) {
                        VUE.setActive(tufts.vue.MetaMap.class,
                                      DataTree.this,
                                      treeNode.getRow().getData());
                    }
                    else if (treeNode instanceof ValueNode && treeNode.getField().isPossibleKeyField()) {
                        DataRow row = mSchema.findRow(treeNode.getField(), treeNode.getValue());
                        VUE.setActive(tufts.vue.MetaMap.class,
                                      DataTree.this,
                                      row.getData());
                    }
                    //                         else if (treeNode.hasStyle()) {
                    //                             final tufts.vue.LWSelection selection = VUE.getSelection();
                    //                             selection.setSource(DataTree.this);
                    //                             // prevents from ever drawing through on map:
                    //                             selection.setSelectionSourceFocal(null);
                    //                             selection.setTo(treeNode.getStyle());
                    //                         }
                    else {

                        // TODO: not a very efficient way to do multi-term searches:
                        // we search all nodes each time for each node, and we
                        // have no control over AND v.s. OR -- we should of course
                        // auto OR terms in the same Field (AND would always be false),
                        // but likewise auto-AND terms across fields, narrowing the selection.
                        // Still need to figure out how to get discontiguous tree selection.

                        boolean multipleSearchTerms = false;
                        DataNode node = null;
                        for (TreePath path : paths) {
                            node = (DataNode) path.getLastPathComponent();
                            selectMatchingNodes(node, multipleSearchTerms);
                            multipleSearchTerms = true;
                        }
                        if (paths.length == 1)
                            mSelectedSearchNode = node;
                        else
                            mSelectedSearchNode = null;
                            
                    }
                        
                    //                         else if (treeNode instanceof ValueNode) {
                    //                             if (treeNode.getField().isPossibleKeyField()) {
                    //                                 DataRow row = mSchema.findRow(treeNode.getField(), treeNode.getValue());
                    //                                 VUE.setActive(tufts.vue.MetaMap.class,
                    //                                               DataTree.this,
                    //                                               row.getData());
                    //                             } else {
                    //                                 selectMapForNode(treeNode, false);
                    //                             }
                    //                         }
                    //VUE.setActive(LWComponent.class, this, node.styleNode);
                }
            });

    }

    @Override
    public void addNotify() {
        if (DEBUG.Enabled) Log.debug("ADDNOTIFY " + this + "; thread=" + mAnnotateThread);
        mAnnotateThread.setPriority(Thread.NORM_PRIORITY);
        super.addNotify();
    }
    @Override
    public void removeNotify() {
        if (DEBUG.Enabled) Log.debug("REMOVENOTIFY " + this + "; thread=" + mAnnotateThread);
        if (mAnnotateThread != null)
            mAnnotateThread.setPriority(Thread.MIN_PRIORITY);
        super.removeNotify();
    }

    private static String HTML(String s) {
        //if (true) return s;
        final StringBuilder b = new StringBuilder(s.length() + 6);
        //b.append("<html>");
        // we add space before and after to widen the painted background selection around the text a bit
        b.append("<html>&nbsp;");
        b.append(s);
        b.append("&nbsp;");
        return b.toString();
    }

    private static int sortPriority(Field f) {
        if (f.isSingleton())
            return -4;
        else if (f.isSingleValue())
            return -3;
        else if (f.isUntrackedValue())
            return -2;
        else if (f.getName().contains(":") && !f.getName().startsWith("dc:"))
            return -1;
        else
            return 0;
    }

    /** build the model and return the root node */
    private TreeNode buildTree(final Schema schema)
    {
        mAllRowsNode = new AllRowsNode(schema, this);
        
        final DataNode root =
            new DataNode("Data Set: " + schema.getName());
            //new VauleNode("Data Set: " + schema.getName());
//             new DataNode(null, null,
//                          String.format("%s [%d %s]",
//                                        schema.getName(),
//                                        schema.getRowCount(),
//                                        "items"//isCSV ? "rows" : "items"));

        final Field keyField = schema.getKeyField();
        Field labelField = schema.getField("title");
        if (labelField == null)
            labelField = keyField;
        for (DataRow row : schema.getRows()) {
            mAllRowsNode.add(new RowNode(row, labelField));
            //String label = row.getValue(labelField);
            //rowNodeTemplate.add(new ValueNode(keyField, row.getValue(keyField), label));
        }
        
        root.add(mAllRowsNode);
        mRootNode = root;

        final Field sortedFields[] = new Field[schema.getFieldCount()];

        schema.getFields().toArray(sortedFields);        

        Arrays.sort(sortedFields, new Comparator<Field>() {
                public int compare(Field f1, Field f2) {
                    return sortPriority(f2) - sortPriority(f1);
                }
            });

        final LWComponent.Listener repainter = new LWComponent.Listener() {
                // todo: schema style nodes are currently parentless, which means
                // their property change events don't go up through the map to
                // the undo-manager, making changes to them not undoable -- either
                // manually relay style property change events up through the appropriate
                // map, or have a way for a map to have hidden list of style children.
                public void LWCChanged(tufts.vue.LWCEvent e) {
                    if (e.getName() == LWKey.UserActionCompleted) {
                        // changes to style nodes need to repaint the tree
                        // todo: don't need to refresh everything, could just refresh top-levels
                        DataTree.this.refreshAll();
                        //DataTree.this.repaint();
                    }
                }
            };
                    
        
        for (Field field : sortedFields) {
            
//             if (field.isSingleton())
//                 continue;

            DataNode fieldNode = new FieldNode(field, repainter, null);
            root.add(fieldNode);
            
//             if (field.uniqueValueCount() == schema.getRowCount()) {
//                 //Log.debug("SKIPPING " + f);
//                 continue;
//             }

            final Set values = field.getValues();
            
            // could add all style nodes to the schema node to be put in an internal layer for
            // persistance: either that or store them with the datasources, which
            // probably makes more sense.
            
            if (values.size() > 1) {
                buildValueChildren(field, fieldNode);
            }
            
        }
    
        return root;
    }

    private static void buildValueChildren(Field field, DataNode fieldNode)
    {

        final Multiset<String> valueCounts = field.getValueSet();
        final Set<Multiset.Entry<String>> entrySet = valueCounts.entrySet();

        final Iterable<Multiset.Entry<String>> valueEntries;

        if (!field.isPossibleKeyField()) {

            // don't need to bother sorting if field is a possible key field (all value counts == 1)
            
            final ArrayList<Multiset.Entry<String>> sortedValues = new ArrayList(entrySet);

            Collections.sort(sortedValues, new Comparator<Multiset.Entry>() {
                    public int compare(Multiset.Entry e1, Multiset.Entry e2) {
                        // always put any empty value item last, otherwise sort on frequency
                        if (e1.getElement() == Field.EMPTY_VALUE)
                            return 1;
                        else if (e2.getElement() == Field.EMPTY_VALUE)
                            return -1;
                        else
                            return e2.getCount() - e1.getCount();
                    }
                });

            valueEntries = sortedValues;
        } else {
            valueEntries = entrySet;
        }
        
        //-----------------------------------------------------------------------------
        // Add the enumerated values
        //-----------------------------------------------------------------------------
        
        for (Multiset.Entry<String> e : valueEntries) {

            final String value = e.getElement();
            final String display;
                    
            if (field.isPossibleKeyField()) {
                        
                display = valueName(value);
                        
            } else {
                final String count = String.format("%3d", e.getCount()).replaceAll(" ", "&nbsp;");
                display = String.format(HTML("<code><font color=#888888>%s</font></code> %s"),
                                        count,
                                        valueName(value));

            }

            final ValueNode valueNode = new ValueNode(field, value, display, e.getCount());

            fieldNode.add(valueNode);

        }
    }
    

    public void dragGestureRecognized(DragGestureEvent e) {
        if (getSelectionPath() != null) {
            Log.debug("SELECTED: " + Util.tags(getSelectionPath().getLastPathComponent()));
            final DataNode treeNode = (DataNode) getSelectionPath().getLastPathComponent();
            //                          if (resource != null) 
            //                              GUI.startRecognizedDrag(e, resource, this);
                         
            //tufts.vue.gui.GUI.startRecognizedDrag(e, Resource.instance(node.value), null);

            // TODO: how are we going to persist these styles?  Most
            // natural way would be inside a hidden layer, but that's on
            // the MAP, which would mean each map would need it's schema
            // recorded with styled nodes, that can be hooked up
            // DIFFERENTLY to the data source panel.

            final LWComponent dragNode;
            final Field field = treeNode.getField();
            boolean stylesAlreadyApplied = false;

            if (treeNode.isValue()) {
                //dragNode = new LWNode(String.format(" %s: %s", field.getName(), treeNode.value));
                dragNode = DataAction.makeValueNode(field, treeNode.getValue());
                //dragNode.setLabel(String.format(" %s: %s ", field.getName(), treeNode.value));
                //dragNode.setLabel(String.format(" %s ", field.getName());

            } else if (treeNode.isField()) {
//                 if (field.isPossibleKeyField())
//                     return;
                dragNode = new LWNode(String.format("  %d unique  \n  '%s'  \n  values  ",
                                                    field.uniqueValueCount(),
                                                    field.getName()));
//                 dragNode.setClientData(java.awt.datatransfer.DataFlavor.stringFlavor,
//                                        " ${" + field.getName() + "}");

            } else if (treeNode instanceof RowNode) {
                
                
                final DataRow row = ((RowNode)treeNode).getRow();
                final List<LWComponent> nodes = DataAction.makeRowNodes(treeNode.getSchema(), row);
                if (DEBUG.Enabled) Log.debug("made row nodes: " + Util.tags(nodes));
                if (nodes.isEmpty()) {
                    Log.error("no row node made from row: " + row);
                    dragNode = null;
                } else
                    dragNode = nodes.get(0);
                stylesAlreadyApplied = true;
                                        
            } else {
                //assert treeNode instanceof TemplateNode;
                final Schema schema = treeNode.getSchema();
                dragNode = new LWNode(String.format("  '%s'  \n  dataset  \n  (%d items)  ",
                                                    schema.getName(),
                                                    schema.getRowCount()
                                                    ));
            }

            if (dragNode == null) {
                Log.warn("Unable to create nodes from drag of " + treeNode);
                return;
            }

            dragNode.copyStyle(treeNode.getStyle(), ~LWKey.Label.bit);
                                     
            //dragNode.setFillColor(null);
            //dragNode.setStrokeWidth(0);
            if (!treeNode.isValue()) {
                dragNode.mFontSize.setTo(24);
                dragNode.mFontStyle.setTo(java.awt.Font.BOLD);
//                 dragNode.setClientData(LWComponent.ListFactory.class,
//                                        new NodeProducer(treeNode));
            }
            dragNode.setClientData(LWComponent.Producer.class,
                                   new NodeProducer(treeNode, DataTree.this));
                         
            tufts.vue.gui.GUI.startRecognizedDrag(e, dragNode);
                         
        }
    }

    private static String valueName(Object value) {
        return DataAction.valueName(value);
    }

    private static class NodeProducer implements LWComponent.Producer, Runnable {

        private final DataNode treeNode;
        private final DataTree tree;
        private LWMap mMap;
        private List<LWComponent> mNodes;

        NodeProducer(DataNode n, DataTree tree) {
            this.treeNode = n;
            this.tree = tree;
        }

        public java.util.List<LWComponent> produceNodes(final LWMap map)
        {
            final Field field = treeNode.getField();
            final Schema schema = treeNode.getSchema();
            
            Log.debug("PRODUCING NODES FOR FIELD: " + field);
            Log.debug("                IN SCHEMA: " + schema);
            
            final java.util.List<LWComponent> nodes;

            LWNode n = null;

            if (treeNode instanceof RowNode) {

                nodes = DataAction.makeRowNodes(schema, treeNode.getRow());
                
            } else if (treeNode.isRecordNode()) {

                List<LWComponent> _nodes = null;
                Log.debug("PRODUCING ALL DATA NODES");
                try {
                    _nodes = DataAction.makeRowNodes(schema);
                    Log.debug("PRODUCED ALL DATA NODES; nodeCount="+_nodes.size());
                } catch (Throwable t) {
                    Util.printStackTrace(t);
                }

                nodes = _nodes;
                
            } else if (treeNode.isValue()) {
                
                Log.debug("PRODUCING A SINGLE VALUE NODE");
                // is a single value from a column
                nodes = Collections.singletonList(DataAction.makeValueNode(field, treeNode.getValue()));
                    
            } else {

                Log.debug("PRODUCING ALL VALUE NODES FOR FIELD: " + field);
                nodes = new ArrayList();

                // handle all the enumerated values for a column
                
                for (String value : field.getValues())
                    nodes.add(DataAction.makeValueNode(field, value));
            }

            mNodes = nodes;
            mMap = map;

            return nodes;
        }


        /** interface LWComponent.Producer impl */
        public void postProcessNodes() { adjustNodesAfterAdding(mMap, mNodes); }

        /** add data-links, layout nodes, and update (re-annotate) the DataTree */
        private void adjustNodesAfterAdding(final LWMap map, List<LWComponent> nodes) {

            //for (LWComponent c : nodes)c.setToNaturalSize();
            // todo: some problem editing template values: auto-size not being handled on label length shrinkage
            
            final boolean addedLinks = DataAction.addDataLinksForNodes(map, nodes, treeNode.getField());

            if (nodes.size() > 1) {
                if (treeNode.isRecordNode()) {
                    tufts.vue.LayoutAction.random.act(nodes);
                } else if (addedLinks) {
                    // cluster will currently fail (NPE) if no data-links exist
                    tufts.vue.LayoutAction.cluster.act(nodes);
                } else
                    tufts.vue.LayoutAction.filledCircle.act(nodes);
            }

            // for re-annotating the tree
            GUI.invokeAfterAWT(this); 
        }

        public void run() {

            // todo: this would be more precisely handled by the DataTree having a
            // listener on the active map for any hierarchy events that involve the
            // creation/deletion of any data-holding nodes, and running an annotate at
            // the end if any are detected -- adding a cleanup task the first time (and
            // checking for before adding another: standard cleanup task semantics)
            // should handle our run-once needs.  E.g., undoing this action will fail to
            // update the tree unless we have an impl such as this.
            
            if (mMap == VUE.getActiveMap()) // only if is still the active map
                tree.annotateForMap(mMap);
        }        
    }

    private static String makeFieldLabel(final Field field)
    {
    
        final Set values = field.getValues();
        //Log.debug("EXPANDING " + colNode);

        //LWComponent schemaNode = new LWNode(schema.getName() + ": " + schema.getSource());
        // add all style nodes to the schema node to be put in an internal layer for
        // persistance: either that or store them with the datasources, which
        // probably makes more sense.

        String label = field.toString();
            
        if (values.size() == 0) {

            if (field.getMaxValueLength() == 0) {
                //label = String.format("<html><b><font color=gray>%s", field.getName());
                label = String.format(HTML("<font color=gray>%s"), field.getName());
            } else {
                //label = String.format("<html><b>%s (max size: %d bytes)",
                label = String.format(HTML("%s (max size: %d bytes)"),
                                      field.getName(), field.getMaxValueLength());
            }
        } else if (values.size() == 1) {
                
            label = String.format(HTML("%s: <font color=green>%s"),
                                  field.getName(), field.getValues().toArray()[0]);

        } else if (values.size() > 1) {

            //final Map<String,Integer> valueCounts = field.getValueMap();
                
//             if (field.isPossibleKeyField())
//                 //label = String.format("<html><i><b>%s</b> (%d)", field.getName(), field.uniqueValueCount());
//             else
//             // we add space before and after to widen the painted background selection around the text a bit
//             label = String.format(HTML("&nbsp;%s (%d)&nbsp;"), field.getName(), field.uniqueValueCount());
            label = String.format(HTML("%s (%d)"), field.getName(), field.uniqueValueCount());
            
        }

        return label;
    }

    private static class DataNode extends DefaultMutableTreeNode {

        String display;
        
        protected DataNode(String description) {
            setDisplay(description);
        }

        protected DataNode() {}

        Vector<DataNode> getChildren() {
            return super.children;
        }

        Schema getSchema() {
            Util.printStackTrace("getSchema: unimplemented");
            return null;
        }

        DataRow getRow() {
            Util.printStackTrace("getRow: unimplemented");
            return null;
        }

        /** @return false -- override for row nodes */
        boolean isRow() { return false; }
        /** @return null -- override for value nodes */
        String getValue() { return null; }
        /** @return null -- override for field nodes */
        Field getField() { return null; }
        
        /** @return true if this node represents the collection of all possible values found in a column of data */
        boolean isField() { return false; }
        /** @return true if this node represents a paricular enumerated value from a given column */
        boolean isValue() { return !isField(); }

        
        LWComponent getStyle() { return null; }
        boolean hasStyle() { return false; }

        
        /** set the label visually displayed in the tree (unannotated) */
        void setDisplay(String s) {
            display = s;
            setUserObject(s);  // sets display label
        }

        /** noop -- override to provide annotations againast the given map */
        void annotate(LWMap map) {}

        void setAnnotation(String s) {
            setPostfix(s);
        }

        void setPostfix(String s) {
            //Log.debug("postfix " + this + " with [" + s + "]");
            if (s == null || s.length() == 0) {
                setUserObject(display);
            } else {
                setUserObject(display + " " + s);
            }
        }
        

        void setPrefix(String s) {
            //Log.debug("prefix " + this + " with [" + s + "]");
            if (s == null || s.length() == 0) {
                setUserObject(display);
            } else {
                //final String cs = (String) getUserObject();
                if (display.startsWith("<html>")) {
                    setUserObject("<html>" + s + " " + display.substring(6));
                } else
                    setUserObject(s + " " + display);
            }
        }
        

        /** @return true if this node is tracked for presence in the active map */
        boolean isMapTracked() {
            return isValue();
        }
        
        boolean isRecordNode() {
            return getField() == null;
        }

        /** @return false -- override for semantics */
        boolean isMapPresent() {
            return false;
        }
        /** @return false -- override for semantics */
        boolean isContextChanged() {
            return false;
        }

    }

    private final class RowNode extends DataNode {

        final DataRow row;
        boolean isMapPresent;

        RowNode(DataRow row, Field labelField) {
            this.row = row;
            //setDisplay(row.getValue(labelField));
            setDisplay(row.toString());
        }

        @Override boolean isRow() { return true; }
        @Override DataRow getRow() { return row; }
        @Override boolean isMapPresent() { return isMapPresent; }
        @Override boolean isContextChanged() { return row.isContextChanged(); }

        @Override
        void annotate(LWMap map) {
//             final Field keyField = getSchema().getKeyField();
//             final String keyValue = row.getValue(keyField);
//             isMapPresent = keyField.countContextValue(keyValue) > 0;
            
            isMapPresent = row.getContextCount() > 0;
        }


        @Override boolean isMapTracked() { return true; }
        @Override boolean isField() { return false; }
        @Override boolean isValue() { return false; }
        @Override Schema getSchema() {
            // return row.getSchema() -- row's don't currently encode the schema
            // if pull a schema stored in the root template node from parent.parent,
            // could skip making this an inner class, and save 4 bytes per row-node at runtime
            return mSchema;
        }
                
    }

    /**
     * A "field" is really a column from a particular data set, with the additional
     * semantics that we usually always keep around an enumerated list of all the possible
     * unique values that appear in that column.  A FieldNode node will have a list of ValueNodes
     * as children to represent these values.
     */
    private static class FieldNode extends DataNode {

        final Field field;

        FieldNode(Field field, LWComponent.Listener repainter, String description) {
            this.field = field;

            if (description == null) {
                if (field != null)
                    setDisplay(makeFieldLabel(field));
            } else
                setDisplay(description);

            //if (field != null && field.isEnumerated() && !field.isPossibleKeyField())
            if (field != null && !field.hasStyleNode() && !field.isSingleValue() && field.isEnumerated())
                field.setStyleNode(DataAction.makeStyleNode(field, repainter));
        }

        protected FieldNode(Field field) {
            this.field = field;
        }

        @Override Field getField() { return field; }
        @Override Schema getSchema() { return field.getSchema(); }
        @Override LWComponent getStyle() { return field == null ? null : field.getStyleNode(); }
        @Override boolean hasStyle() { return field != null && field.getStyleNode() != null; }
        @Override boolean isField() { return field != null; }
    }
    

    private static final class ValueNode extends FieldNode {

        final String value;
        final int dataSetCount;
        boolean isMapPresent;

        ValueNode(Field field, String value, String label, int dataSetValueCount) {
            super(field);
            setDisplay(label);
            this.value = value;
            this.dataSetCount = dataSetValueCount;
        }
        
        @Override
        String getValue() {
            return value;
        }

        @Override
        void annotate(LWMap map) {

            final int mapCount = field.countContextValue(value);
            if (mapCount > 0) {
                isMapPresent = true;
                if (mapCount != dataSetCount)
                    setPostfix(String.format("[%+d]", mapCount - dataSetCount));
                else
                    setPostfix(null);
                //setPrefix("=");
            } else {
                isMapPresent = false;
                setPostfix(null);
                //setPrefix("-");
            }
            
//             // TODO: INCLUDE CURRENT MAP COUNTS
//             if (field.hasContextValue(value))
//                 setPrefix("=");
//             //setAnnotation(null);
//             else
//                 setPrefix("+");
//             //setAnnotation("<font color=red>(new)");
        }
        
        @Override
        public boolean isField() { return false; }
        @Override
        public boolean hasStyle() { return false; }
//         @Override
//         public LWComponent getStyle() { return null; }

        @Override
        boolean isMapPresent() {
            return isMapPresent;
        }

//         @Override
//         public String toString() { return "ValueNode[" + super.toString() + "; value=" + getValue() + "]"; }
        
    }

    private final class AllRowsNode extends FieldNode {

        final Schema schema;

        AllRowsNode(Schema schema, LWComponent.Listener repainter) {
            super(null, repainter, "All Rows");
            //String.format(HTML("<b><u>All Records in %s (%d)"), schema.getName(), schema.getRowCount()));
            this.schema = schema;
            schema.setRowNodeStyle(DataAction.makeStyleNode(schema));
            schema.getRowNodeStyle().addLWCListener(new LWComponent.Listener() {
                    public void LWCChanged(tufts.vue.LWCEvent e) {
                        updateLabel(true);
                    }
                },
                LWKey.Label);
            updateLabel(false);
        }


        private void updateLabel(boolean refresh) {
            String labelFormat = schema.getRowNodeStyle().getLabel().trim();
            if (labelFormat.startsWith("${") && labelFormat.endsWith("}"))
                labelFormat = labelFormat.substring(2, labelFormat.length()-1);

            //setDisplay(String.format(HTML("<b><u>All Records in %s</b><u> (%d)</u> : <b><font color=red>%s"),
            //setDisplay(String.format(HTML("<b>All Records in %s</b> (%d) : <b><font color=red>%s"),
            setDisplay(String.format(HTML("<b>All Records</b> (%d) : <b><font color=red>%s"),
                                     schema.getRowCount(),
                                     labelFormat));
        
            if (refresh)
                DataTree.this.refreshRootNode();
        }

        @Override void annotate(LWMap map) {
            //if (DEBUG.Enabled) setAnnotation(String.format("[%s]", map.getLabel()));
        }
        
        @Override Schema getSchema() { return schema; }
        @Override boolean isField() { return false; }
        @Override boolean isValue() { return false; }
        @Override boolean hasStyle() { return true; }
        @Override LWComponent getStyle() { return schema.getRowNodeStyle(); }
    }


    private static final int IconWidth = 32;
    private static final int IconHeight = 20;

    //private static final Border TopBorder = BorderFactory.createLineBorder(Color.gray);
//     private static final Border TopBorder = new CompoundBorder(new MatteBorder(3,0,3,0, Color.white),
//                                                                new CompoundBorder(new LineBorder(Color.gray),
//                                                                                   GUI.makeSpace(1,0,1,2)));
    //private static final Border TopBorder = GUI.makeSpace(3,0,2,0);
    //private static final Border TopBorder = GUI.makeSpace(0,0,2,0);
    //private static final Border TopBorder = new CompoundBorder(GUI.makeSpace(0,0,10,0), new MatteBorder(0,0,1,0, Color.gray));
    private static final Border TopBorderCollapsed = new CompoundBorder(new CompoundBorder(GUI.makeSpace(0,0,7,0),
                                                                                           new MatteBorder(0,0,1,0, Color.gray)),
                                                                        GUI.makeSpace(0,0,7,0));
                                                                                         
    private static final Border TopBorderExpanded = null;

    private static final Border TopTierBorder = GUI.makeSpace(0,0,2,0);
    private static final Border LeafBorder = GUI.makeSpace(0,IconWidth-16,2,0);
    
    //private static final Icon IncludedInMapIcon = VueResources.getIcon(VUE.class, "images/data_onmap.png");
    //private static final Icon IncludedInMapIcon = GUI.reframeIcon(VueResources.getIcon(VUE.class, "images/data_onmap.png"), 8, 16);
    //private static final Icon NewToMapIcon = VueResources.getIcon(VUE.class, "images/data_offmap.png");

    private static final int RIPS = Util.isMacPlatform() ? 20 : 16; // RowIconPointSize

    private static final Icon RowHasChangedIcon = makeIcon(0x229B, RIPS, Color.green.darker(), -2, -1);
    private static final Icon RowOnMapIcon = makeIcon(0x229B, RIPS, VueConstants.COLOR_SELECTION, -2, -1);
    private static final Icon RowOffMapIcon = makeIcon(0x229B, RIPS, Color.lightGray, -2, -1);
    private static final Icon ValueOnMapIcon = makeIcon(0x25C9, 12, VueConstants.COLOR_SELECTION, 0, 0);
    private static final Icon ValueOffMapIcon = makeIcon(0x25C9, 12, Color.lightGray, 0, 0);
//     private static final Icon RowOnMapIcon = makeIcon(0x25C9, 14, VueConstants.COLOR_SELECTION);
//     private static final Icon RowOffMapIcon = makeIcon(0x25C9, 14, Color.lightGray);
//     private static final Icon ValueOnMapIcon = makeIcon(0x229B, 18, VueConstants.COLOR_SELECTION, 0, -1);
//     private static final Icon ValueOffMapIcon = makeIcon(0x229B, 18, Color.lightGray, 0, -1);

    //private static final Icon UniqueValueOnMapIcon = makeIcon(0x29BF, 16, VueConstants.COLOR_SELECTION, 0, -1);
    //private static final Icon UniqueValueOffMapIcon = makeIcon(0x29BF, 16, Color.lightGray, 0, -1);
    private static final Icon UniqueValueOnMapIcon = makeIcon(0x229A, 16, VueConstants.COLOR_SELECTION, 0, 0);
    private static final Icon UniqueValueOffMapIcon = makeIcon(0x229A, 16, Color.lightGray, 0, 0);
    
    // 29BE: â¦¾
    // 29BF: â¦¿
    // 25C9: â—‰ 
    // 25CE: â—Ž
    // 229A: âŠš
    // 25E6: â—¦
    // 229D: âŠ�
    // 229B: âŠ›    


    
    private static Icon makeIcon(int code, int pointSize, Color color) {
        return makeIcon(code, pointSize, color, 0, 0);
    }
    
    private static Icon makeIcon(int code, int pointSize, Color color, int xoff, int yoff) {
        return GUI.makeUnicodeIcon(code,
                                   pointSize,
                                   color,
                                   16, // fixed width
                                   16, // fixed height
                                   4+xoff, // xoff
                                   4+yoff // yoff
                                   );
    
        
    }
    
//     private static final GUI.ResizedIcon NewToMapIcon =
//         new GUI.ResizedIcon(VueResources.getIcon(GUI.class, "icons/MacSmallCloseIcon.gif"), 16, 16);

    private static final Color KeyFieldColor = Color.green.darker();

    private static final Icon TestIcon = VueResources.getImageIcon("dataSourceRSS");        

    private class DataRenderer extends DefaultTreeCellRenderer {

        {
            //setIconTextGap(2);
            //setBorder(LeafBorder);
            setVerticalTextPosition(SwingConstants.CENTER);
            //setTextSelectionColor(Color.blue); // text color selected
            //setTextNonSelectionColor(Color.green); // text color normal
            //setBackgroundSelectionColor(VueConstants.COLOR_SELECTION.brighter());
            setBackgroundSelectionColor(VueResources.getColor("dataTree.selected.background", Color.blue));
            setBorderSelectionColor(VueConstants.COLOR_SELECTION);
            //setBackgroundSelectionColor(VueConstants.COLOR_HIGHLIGHT);
            //setFont(tufts.vue.VueConstants.SmallFixedFont);
        }

        //@Override public int getWidth() { return 500; }

        public Component getTreeCellRendererComponent(
                final JTree tree,
                final Object value,
                final boolean selected,
                final boolean expanded,
                final boolean leaf,
                final int row,
                final boolean hasFocus)
        {
            //Log.debug(Util.tags(value));
            final DataNode node = (DataNode) value;
            final Field field = node.getField();
            
            //setIconTextGap(4); // pre &nbsp; standard HTML
            setIconTextGap(1);

            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            setForeground(Color.black); // must do every time for some reason, or de-selected text goes invisible

            if (node.hasStyle()) {
                //setIconTextGap(4);
                // Note: icons at top level do not get the background selection color painted
                // behind them for some reason, whereas leaf node icons do, so this icon
                // needs to take that into account, and should be careful not to paint over
                // the selected & focus-active border of the row item.
                setIcon(FieldIconPainter.load(node.getStyle(),
                                              selected ? backgroundSelectionColor : null));

            } else {
                
                if (field != null && field.isSingleton()) {
                    
                    setIcon(null);
                    
                } else if (node.isMapTracked()) {
                    
                    setIconTextGap(1);

                    if (node.isRow()) {
                        if (node.isContextChanged())
                            setIcon(RowHasChangedIcon);
                        else if (node.isMapPresent())
                            setIcon(RowOnMapIcon);
                        else
                            setIcon(RowOffMapIcon);
                    } else {
                        if (field != null && field.isPossibleKeyField()) {
                            if (node.isMapPresent())
                                setIcon(UniqueValueOnMapIcon);
                            else
                                setIcon(UniqueValueOffMapIcon);
                        } else {
                            if (node.isMapPresent())
                                setIcon(ValueOnMapIcon);
                            else
                                setIcon(ValueOffMapIcon);
                        }
                    }
                }
            }

            if (row == 0) {
                if (expanded)
                    setBorder(TopBorderExpanded);
                else
                    setBorder(TopBorderCollapsed);
                //setBorder(TopBorder);
                //setBackgroundNonSelectionColor(Color.lightGray);
                //setFont(EnumFont);
            } else {
                //setBackgroundNonSelectionColor(null);
                //setFont(null);
                //setBorder(leaf ? LeafBorder : null);
                if (leaf) {
                    if (node.isField() && node.getField().isSingleton())
                        setBorder(TopTierBorder);
                    else
                        setBorder(LeafBorder);
                } else {
                    setBorder(null);
                }
            }
            
            return this;
        }
    }

    private static final java.awt.geom.Rectangle2D IconSize
        = new java.awt.geom.Rectangle2D.Float(0,0,IconWidth,IconHeight);
//     private static final java.awt.geom.Rectangle2D IconInsetSize
//         = new java.awt.geom.Rectangle2D.Float(1,2,IconWidth-2,IconHeight-4);
    
    private static final NodeIconPainter FieldIconPainter = new NodeIconPainter();

    private static final Icon EmptyIcon = new GUI.EmptyIcon(IconWidth, IconHeight);

    private static final double ViewScaleDown = 0.5;
    private static final double ViewScale = 1 / ViewScaleDown;
    private static final java.awt.geom.Rectangle2D IconViewSize
        = new java.awt.geom.Rectangle2D.Double(2*ViewScale,
                                              4*ViewScale+0.5,
                                              (IconWidth-4) * ViewScale,
                                              (IconHeight-8) * ViewScale);

    private static final Stroke NodeIconBorder = new BasicStroke(0.5f);
    
    private static class NodeIconPainter implements Icon {

        LWComponent node;
        Color fill;

        public Icon load(LWComponent c, Color fill) {
            this.node = c;
            this.fill = fill;
            return this;
        }
        
        public int getIconWidth() { return IconWidth; }
        public int getIconHeight() { return IconHeight; }
        
        public void paintIcon(Component c, Graphics _g, int x, int y) {
            //Log.debug("x="+x+", y="+y);
            
            java.awt.Graphics2D g = (java.awt.Graphics2D) _g;
            
            g.setRenderingHint
                (java.awt.RenderingHints.KEY_ANTIALIASING,
                 java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            if (fill != null) {
                if (DEBUG.BOXES) {
                    g.setColor(Color.red);
                    g.fillRect(x,y,IconWidth,IconHeight);
                } else {
//                     g.setColor(fill);
//                     g.setColor(Color.red);
//                     // add to width to also fill the IconTextGap
//                     // TODO: this is painting over edge of active selected border color
//                     g.fillRect(0,0,IconWidth+8,IconHeight+8);
                }
            }

            // we should only be seeing LWNode's, which always have RectanularShape
            final RectangularShape shape = (RectangularShape) node.getZeroShape();

            shape.setFrame(IconViewSize);
            g.setColor(node.getFillColor());
            g.scale(ViewScaleDown, ViewScaleDown);
            g.fill(shape);
            g.setStroke(NodeIconBorder);
            g.setColor(Color.gray);
            g.draw(shape);
            g.scale(ViewScale, ViewScale);

            //node.setSize(IconSize);
            
//             node.drawFit(new DrawContext(g.create(), node),
//                          IconSize,
//                          2);
//             //node.drawFit(g, x, y);
        }
    
    }
}

    


//     // this type of node was only for intial prototype
//     private static LWComponent makeDataNodes(Schema schema, Field field)
//     {
        
//         Log.debug("PRODUCING KEY FIELD NODES " + field);
//         int i = 0;
//         for (DataRow row : schema.getRows()) {
//             n = new LWNode();
//             n.setClientData(Schema.class, schema);
//             n.getMetadataList().add(row.entries());
//             if (field != null) {
//                 final String value = row.getValue(field);
//                 n.setLabel(makeLabel(field, value));
//             } else {
//                 //n.setLabel(treeNode.getStyle().getLabel()); // applies initial style
//             }
//             nodes.add(n);
//             //Log.debug("setting meta-data for row " + (++i) + " [" + value + "]");
//             //                     for (Map.Entry<String,String> e : row.entries()) {
//             //                         // todo: this is slow: is updating UI components, setting cursors, etc, every time
//             //                         n.addMetaData(e.getKey(), e.getValue());
//             //                     }
//         }
//         Log.debug("PRODUCED META-DATA IN " + field);

//     }
    
//     private static LWComponent makeDataNode(Schema schema)
//     {
//         int i = 0;
//         LWNode node;
//         for (DataRow row : schema.getRows()) {
//             node = new LWNode();
//             node.setClientData(Schema.class, schema);
//             node.getMetadataList().add(row.entries());
//             node.setStyle(schema.getStyleNode()); // must have meta-data set first to pick up label template
            
//             nodes.add(n);
//             //Log.debug("setting meta-data for row " + (++i) + " [" + value + "]");
//             //                     for (Map.Entry<String,String> e : row.entries()) {
//             //                         // todo: this is slow: is updating UI components, setting cursors, etc, every time
//             //                         n.addMetaData(e.getKey(), e.getValue());
//             //                     }
//         }
//         Log.debug("PRODUCED META-DATA IN " + field);

//     }


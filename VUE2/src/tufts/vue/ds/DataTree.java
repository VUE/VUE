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

package tufts.vue.ds;

import tufts.vue.VUE;
import tufts.vue.DEBUG;
import tufts.vue.Resource;
import tufts.vue.VueResources;
import tufts.vue.LWComponent;
import static tufts.vue.LWComponent.Flag;
import tufts.vue.LWNode;
import tufts.vue.LWLink;
import tufts.vue.LWMap;
import tufts.vue.Actions;
import tufts.vue.LWKey;
import tufts.vue.DrawContext;
import tufts.vue.gui.GUI;
//import edu.tufts.vue.metadata.VueMetadataElement;
import tufts.Util;

import java.util.List;
import java.util.*;
import java.net.URL;
import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.google.common.collect.*;

/**
 *
 * @version $Revision: 1.22 $ / $Date: 2008-12-04 06:10:25 $ / $Author: sfraize $
 * @author  Scott Fraize
 */

public class DataTree extends javax.swing.JTree
    implements DragGestureListener
               , LWComponent.Listener
               //,DragSourceListener
               //,TreeSelectionListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataTree.class);
    
    private final Schema mSchema;

    private DataNode mRootNode;
    private final DefaultTreeModel mTreeModel;

    private LWMap mActiveMap;

    public static JComponent create(Schema schema) {
        final DataTree tree = new DataTree(schema);

        //tree.setBorder(new LineBorder(Color.red, 4));
        
        tree.activeChanged(null, VUE.getActiveMap()); // simulate event for initial annotations
        VUE.addActiveListener(LWMap.class, tree);

        if (false) {

            return tree;

        } else {

            return buildControllerUI(tree);
            
        }
    }

    private static JComponent buildControllerUI(final DataTree tree)
    {
        final Schema schema = tree.mSchema;
        final JPanel wrap = new JPanel(new BorderLayout());
        final JPanel toolbar = new JPanel();
        toolbar.setOpaque(true);
        toolbar.setBackground(Color.white);
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        //toolbar.setLayout(new BorderLayout());
        //p.add(new JLabel(s.getSource().toString()), BorderLayout.NORTH);

        AbstractButton addNew = new JButton("Add New Items to Map");
        addNew.setIcon(NewToMapIcon);
        //addNew.setBorderPainted(false);
        addNew.setOpaque(false);
            
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

        List<String> possibleKeyFields = new ArrayList();

        for (Field field : schema.getFields())
            if (field.isPossibleKeyField())
                possibleKeyFields.add(field.getName());

        JComboBox keyBox = new JComboBox(possibleKeyFields.toArray());
        keyBox.setOpaque(false);
        keyBox.setSelectedItem(schema.getKeyField().getName());

        keyBox.addItemListener(new ItemListener() {
        	public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        schema.setKeyField((String) e.getItem());
                        tree.refreshRoot();
                    }
        	}
            });
        

        toolbar.add(keyBox, BorderLayout.WEST);
        toolbar.add(addNew, BorderLayout.EAST);

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
                if (DEBUG.Enabled) Log.debug("setExpandedState " + path + " = " + state + " RELAYING");
                DataTree.super.setExpandedState(path, state);
            } else 
                if (DEBUG.Enabled) Log.debug("setExpandedState " + path + " = " + state + " SKIPPING");
            inDoubleClick = false;
        }});
    }

    private boolean inDoubleClick;

    private class ClickSearchMouseListener extends tufts.vue.MouseAdapter
    {
        private TreePath mClickPath;
        
        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
            mClickPath = getPathForLocation(e.getX(), e.getY());
            if (DEBUG.Enabled) Log.debug("MOUSE PRESSED ON " + Util.tags(mClickPath));
            
            // it's possible that the node under the mouse changes from the time of the
            // first press, to the time mouseClicked is called (e.g., due to tree
            // expansion and/or possible scrolling of the entire tree component), so we
            // capture it here.  Could make this a class a generic subclassable helper
            // class for JTree's.

            inDoubleClick = GUI.isDoubleClick(e);
            if (DEBUG.Enabled) Log.debug("IN DOUBLE CLICK = " + inDoubleClick);
            
        }
                
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {

            if (!GUI.isDoubleClick(e))
                return;
            
//             final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
//             //final TreePath path = tree.getSelectionPath();
//             if (path == null)
//                 return;

            if (mClickPath == null)
                return;

            final DataNode treeNode = (DataNode) mClickPath.getLastPathComponent();
            if (DEBUG.Enabled) Log.debug("ACTIONABLE DOUBLE CLICK ON " + Util.tags(treeNode));
            
            final DataTree tree = DataTree.this;
            
            if (tree.mActiveMap == null)
                return;

            final Field field = treeNode.getField();

            if (field == null) {
                // todo: must be root node: select a row-node items
                return;
            }

            //tree.setSelectionPath(path);
                
            final String fieldName = field.getName();
            final List<LWComponent> hits = new ArrayList();
            boolean matching = false;
            String desc = "";
            if (treeNode.isField()) {
                desc = String.format("anchoring field <b>%s", fieldName);
                Log.debug("searching for " + fieldName + " in " + tree.mActiveMap);
                for (LWComponent c : tree.mActiveMap.getAllDescendents()) {
                    if (c.isSchematicField(fieldName))
                        hits.add(c);
                }
            }
            else if (treeNode.isValue()) {
                matching = true;
                final String fieldValue = treeNode.getValue();
                //desc = String.format("matching<br><b>%s</b> = \'%s\'", fieldName, fieldValue);
                //desc = String.format("matching<br><b>%s: <i>%s", fieldName, fieldValue);
                desc = String.format("<b>%s: <i>%s</i>", fieldName, fieldValue);
                Log.debug("searching for " + fieldName + "=" + fieldValue + " in " + tree.mActiveMap);
                for (LWComponent c : tree.mActiveMap.getAllDescendents()) {
                    if (c.hasDataValue(fieldName, fieldValue) && !c.isSchematicField())
                        hits.add(c);
                }
            }
            final tufts.vue.LWSelection selection = VUE.getSelection();
            if (DEBUG.Enabled) {
                if (hits.size() == 1)
                    Log.debug("hits=" + hits.get(0));
                else
                    Log.debug("hits=" + hits.size());
            }
            if (hits.size() > 0 || e.isShiftDown()) {
                if (hits.size() == 0)
                    return;
                // make sure selection bounds are drawn in MapViewer:
                selection.setSelectionSourceFocal(VUE.getActiveFocal());
                // now set the selection, along with a description
                if (e.isShiftDown()) {
                    //                         String moreDesc = selection.getDescription();
                    //                         if (moreDesc.endsWith("matching"))
                    //                             moreDesc = moreDesc.substring(0, moreDesc.length() - 8);
                    selection.setDescription(selection.getDescription() + "<br>" + desc);
                    selection.add(hits);
                    //selection.setTo(hits, selection.getDescription() + "; " + desc);
                } else {
                    if (matching)
                        desc = "matching<br>" + desc;
                    selection.setTo(hits, desc);
                }
            } else
                selection.clear();
        }
    }
        
    

    public void activeChanged(tufts.vue.ActiveEvent e, final LWMap map) {
        mActiveMap = map;

        if (map == null)
            return;

        annotateForMap(map);
    }

    private void annotateForMap(final LWMap map)
    {
//         if (mActiveMap != null) {
//             String annot = Integer.toHexString(mActiveMap.hashCode());
//             //mActiveMap.getName());
//             Log.debug("annotating " + node + " with " + annot);
//             node.setAnnotation(annot);
//         }

        final Collection<LWComponent> allNodes;

        if (map == null)
            allNodes = Collections.EMPTY_LIST;
        else
            allNodes = map.getAllDescendents();

        mSchema.annotateFor(allNodes);

        if (map != null) {
            final String annot = map.getLabel();
            for (DataNode n : mRootNode.getChildren()) {
                n.annotate(map);
                if (!n.isLeaf())
                    for (DataNode cn : n.getChildren())
                        cn.annotate(map);
            }
//             final String annot = map.getLabel();
//             for (DataNode n : mRootNode.getChildren()) {
//                 n.setAnnotation(annot);
//                 if (!n.isLeaf())
//                     for (DataNode cn : n.getChildren())
//                         cn.setAnnotation(annot);
//             }
        }

        refreshAll();
    }

    private void refreshRoot() {
        Log.debug("REFRESHING " + Util.tags(mRootNode));
        refreshAllChildren(mRootNode);
    }

    private void refreshAll()
    {
        //mTreeModel.reload(mRootNode);
        
        // using nodesChanged instead of reload preserves the expanded state of nodes in the tree

        refreshRoot();
        Log.debug("REFRESHING " + Util.tags(mRootNode.getChildren()));
        for (TreeNode n : mRootNode.getChildren())
            if (!n.isLeaf())
                refreshAllChildren(n);

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
    

    @Override
    public String toString() {
        return String.format("DataTree[%s]", mSchema.toString());
    }

    private DataTree(final Schema schema) {

        mSchema = schema;

        setCellRenderer(new DataRenderer());
        //setSelectionModel(null);

        setModel(mTreeModel = new DefaultTreeModel(buildTree(schema), false));

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

        addMouseListener(new ClickSearchMouseListener());

        addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
                public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
                    if (e.isAddedPath() && e.getPath().getLastPathComponent() != null ) {
                        final DataNode treeNode = (DataNode) e.getPath().getLastPathComponent();
                        if (treeNode.hasStyle()) {
                            VUE.getSelection().setSource(DataTree.this);
                            VUE.getSelection().setSelectionSourceFocal(null); // prevents from ever drawing through on map
                            VUE.getSelection().setTo(treeNode.getStyle());
                        }
                        //VUE.setActive(LWComponent.class, this, node.styleNode);
                    }
                }
            });

        
        
    }

    private static String HTML(String s) {
        //if (true) return s;
        final StringBuilder b = new StringBuilder(s.length() + 6);
        b.append("<html>");
        b.append(s);
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
        final DataNode rowNodeTemplate = new TemplateNode(schema, this);
        
        final DataNode root =
            new DataNode(null, null, "Data Set: " + schema.getName());
//             new DataNode(null, null,
//                          String.format("%s [%d %s]",
//                                        schema.getName(),
//                                        schema.getRowCount(),
//                                        "items"//isCSV ? "rows" : "items"));
        root.add(rowNodeTemplate);
        mRootNode = root;

        final Field sortedFields[] = new Field[schema.getFieldCount()];

        schema.getFields().toArray(sortedFields);        

        Arrays.sort(sortedFields, new Comparator<Field>() {
                public int compare(Field f1, Field f2) {
                    return sortPriority(f2) - sortPriority(f1);
                }
            });
                    
        
        for (Field field : sortedFields) {
            
//             if (field.isSingleton())
//                 continue;

            DataNode fieldNode = new DataNode(field, this, null);
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
                
        //-----------------------------------------------------------------------------
        // Add the enumerated values
        //-----------------------------------------------------------------------------
                
        for (Multiset.Entry<String> e : valueCounts.entrySet()) {

            final String value = e.getElement();
            final ValueNode valueNode;
                    
            if (field.isPossibleKeyField()) {
                        
                valueNode = new ValueNode(field, value, value);
                        
            } else {
                final String count = String.format("%3d", e.getCount()).replaceAll(" ", "&nbsp;");
                final String display = String.format(HTML("<code>%s</code> <b>%s</b>"),
                                                     count,
                                                     value);
//                 final String display = String.format(HTML("<code>%3d</code> <b>(%s)</b>"),
//                                                      e.getValue(),
//                                                      e.getKey());

                valueNode = new ValueNode(field, value, display);
                
            }

            fieldNode.add(valueNode);
                    
//             //fmt = HTML("<b>%s</b> [%d]");

//             fieldNode.add(new ValueNode(field,
//                                         e.getKey(),
//                                         String.format(fmt, e.getValue(), e.getKey())));
//             //String.format(fmt, e.getKey(), e.getValue())));
                    
//             fieldNode.add(new ValueNode(field, e.getKey(), String.format("<html>%s%s</b> (%s)", bold, e.getKey(), e.getValue())));

//             if (e.getValue() == 1) {
//                 fieldNode.add(new ValueNode(field, e.getKey(), String.format("<html>%s%s", bold, e.getKey())));
//             } else {
//                 fieldNode.add(new ValueNode(field, e.getKey(), String.format("<html>%s%s</b> (%s)", bold, e.getKey(), e.getValue())));
//             }
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
            final Field field = treeNode.field;

            if (treeNode.isValue()) {
                //dragNode = new LWNode(String.format(" %s: %s", field.getName(), treeNode.value));
                dragNode = makeValueNode(field, treeNode.getValue());
                //dragNode.setLabel(String.format(" %s: %s ", field.getName(), treeNode.value));
                //dragNode.setLabel(String.format(" %s ", field.getName());
            } else if (treeNode.isField()) {
//                 if (field.isPossibleKeyField())
//                     return;
                dragNode = new LWNode(String.format("  %d unique  \n  '%s'  \n  values  ",
                                                    field.uniqueValueCount(),
                                                    field.getName()));
                dragNode.setClientData(java.awt.datatransfer.DataFlavor.stringFlavor,
                                       " ${" + field.getName() + "}");
            } else {
                assert treeNode instanceof TemplateNode;
                final Schema schema = treeNode.getSchema();
                dragNode = new LWNode(String.format("  '%s'  \n  dataset  \n  (%d items)  ",
                                                    schema.getName(),
                                                    schema.getRowCount()
                                                    ));
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
            dragNode.setClientData(LWComponent.ListFactory.class,
                                   new NodeProducer(treeNode));
                         
            tufts.vue.gui.GUI.startRecognizedDrag(e, dragNode);
                         
        }
    }

    private static String makeLabel(Field f, Object value) {

        return value.toString();
        
//         //return String.format("%s:\n%s", f.getName(), value.toString());
//         if (f.isKeyField())
//             return value.toString();
//         else
//             return value.toString() + "  ";
//         //return "  " + value.toString() + "  ";
    }

    private static LWComponent makeValueNode(Field field, String value) {
        
        LWComponent node = new LWNode(makeLabel(field, value));
        //node.setDataInstanceValue(field.getName(), value);
        node.setDataInstanceValue(field, value);
        //node.setClientData(Field.class, field);
        if (field.getStyleNode() != null)
            node.setStyle(field.getStyleNode());
//         else
//             tufts.vue.EditorManager.targetAndApplyCurrentProperties(node);
        return node;

    }

    private static List<LWComponent> makeAllDataNodes(Schema schema)
    {
        final java.util.List<LWComponent> nodes = new ArrayList();

        final Field linkField = schema.getField("link");
        final Field descField = schema.getField("description");
        final Field titleField = schema.getField("title");
        final Field mediaField = schema.getField("media:group.media:content.media:url");

        Log.debug("PRODUCING ALL DATA NODES FOR " + schema);
        int i = 0;
        LWNode node;
        for (DataRow row : schema.getRows()) {
            
            node = LWNode.createRaw();
            // node.setFlag(Flag.EVENT_SILENT); // todo performance: have nodes do this by default during init
            //node.setClientData(Schema.class, schema);
            //node.getMetadataList().add(row.entries());
            node.addDataValues(row.entries());
            node.setStyle(schema.getStyleNode()); // must have meta-data set first to pick up label template

            if (linkField != null) {
                node.setResource(row.getValue(linkField));
                final tufts.vue.Resource r = node.getResource();
//                 if (descField != null) // now redundant with data fields, may want to leave out for brevity
//                     r.setProperty("Description", row.getValue(descField));
                if (titleField != null) {
                    String title = row.getValue(titleField);
                    r.setTitle(title);
                    r.setProperty("Title", title);
                }
                if (mediaField != null) {
                    // todo: if no per-item media field, use any per-schema media field found
                    // (e.g., RSS content provider icon image)
                    // todo: refactor so cast not required
                    ((tufts.vue.URLResource)r).setURL_Thumb(row.getValue(mediaField));
                }
                
            }
            
            nodes.add(node);
        }
        Log.debug("PRODUCED ALL DATA NODES FOR " + schema);
        
        return nodes;
    }
    
    
    private static class NodeProducer implements LWComponent.ListFactory {

        private final DataNode treeNode;

        NodeProducer(DataNode n) {
            treeNode = n;
        }

        public java.util.List<LWComponent> produceNodes()
        {
            Log.debug("PRODUCING NODES FOR " + treeNode.field);
            
            final Field field = treeNode.field;
            final Schema schema = treeNode.getSchema();
            final java.util.List<LWComponent> nodes;

            LWNode n = null;

            if (treeNode.isSchematic()) {

                nodes = makeAllDataNodes(schema);
                
            } else if (treeNode.isValue()) {
                
                // is a single value from a column
                nodes = Collections.singletonList(makeValueNode(field, treeNode.getValue()));
                    
            } else {

                nodes = new ArrayList();

                // handle all the enumerated values for a column
                
                for (String value : field.getValues())
                    nodes.add(makeValueNode(field, value));
            }

            final LWMap map = VUE.getActiveMap(); // hack
            final Collection linkTargets = Util.extractType(map.getAllDescendents(), LWNode.class);

            java.util.List<LWComponent> links = null;

            if (linkTargets.size() > 0) {
                links = new ArrayList();
                for (LWComponent c : nodes) {
                    links.addAll(DataAction.makeLinks(linkTargets, c, field));
                }
            }

           
            
            //for (LWComponent c : nodes)c.setToNaturalSize();
            // todo: some problem editing template values: auto-size not being handled on label length shrinkage

            if (links != null && links.size() > 0)
                VUE.getActiveMap().getInternalLayer("*Data Links*").addChildren(links);
            GUI.invokeAfterAWT(new Runnable() { public void run() {  
             if (nodes.size() > 1) {
                if (treeNode.isSchematic()) {
                    tufts.vue.LayoutAction.table.act(nodes);
                } else {
                    tufts.vue.LayoutAction.cluster.act(nodes);
                }
             }
            }});
                //Actions.MakeCluster.act(nodes); // todo: broken
                //Actions.MakeColumn.act(nodes);
                // Actions.ZoomFit.fire(this); not added to map yet
                
                // TODO: below should be a post-add action to be called
                // on the list-factory
//                 GUI.invokeAfterAWT(new Runnable() { public void run() {
//                     // todo: we want a zoom-fit action that will zoom-fit
//                     // only if MORE space is needed, but not zoom IN if
//                     // everything is already visible
//                     Actions.ZoomFit.fire(this); 
//                 }});

            
            return nodes;
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
                label = String.format(HTML("<b><font color=gray>%s"), field.getName());
            } else {
                //label = String.format("<html><b>%s (max size: %d bytes)",
                label = String.format(HTML("<b>%s (max size: %d bytes)"),
                                      field.getName(), field.getMaxValueLength());
            }
        } else if (values.size() == 1) {
                
            label = String.format(HTML("<b>%s: <font color=green>%s"),
                                  field.getName(), field.getValues().toArray()[0]);

        } else if (values.size() > 1) {

            //final Map<String,Integer> valueCounts = field.getValueMap();
                
//             if (field.isPossibleKeyField())
//                 //label = String.format("<html><i><b>%s</b> (%d)", field.getName(), field.uniqueValueCount());
//             else
            label = String.format(HTML("<b>%s</b> (%d)"), field.getName(), field.uniqueValueCount());
            
        }

        return label;
    }

    //private static final Color[] DataColors = tufts.vue.VueResources.getColorArray("dataColorValues");
    private static final Color[] DataColors = tufts.vue.VueResources.getColorArray("fillColorValues");
    private static final int FirstRotationColor = 22;
    private static final int SecondRotationColor = 18;
    private static int NextColor = FirstRotationColor;
    private static boolean FirstRotation = true;

    private static LWComponent initStyleNode(LWComponent style) {
        style.setFlag(Flag.INTERNAL);
        style.setFlag(Flag.DATA_STYLE); // must set before setting label, or template will atttempt to resolve
        style.setID(style.getURI().toString());
        return style;
    }
    
    private static LWComponent createStyleNode(final Field field, LWComponent.Listener repainter)
    {
        final LWComponent style;

        if (field.isPossibleKeyField()) {

            style = new LWNode(); // creates a rectangular node
            //style.setLabel(" ---");
            style.setFillColor(Color.gray);
            style.setFont(DataFont);
        } else {
            //style = new LWNode(" ---"); // creates a round-rect node
            style = new LWNode(""); // creates a round-rect node
            //style.setFillColor(Color.blue);
            style.setFillColor(DataColors[NextColor]);
//             if (++NextColor >= DataColors.length)
//                 NextColor = 0;
            NextColor += 8;
            if (NextColor >= DataColors.length) {
                if (FirstRotation) {
                    NextColor = SecondRotationColor;
                    FirstRotation = false;
                } else {
                    NextColor = FirstRotationColor;
                    FirstRotation = true;
                }
            }
            style.setFont(EnumFont);
        }
//         style.setFlag(Flag.INTERNAL);
//         style.setFlag(Flag.DATA_STYLE); 
        initStyleNode(style); // must set before setting label, or template will atttempt to resolve
        //style.setLabel(String.format("%.9s: \n${%s} ", field.getName(),field.getName()));
        style.setLabel(String.format("${%s}", field.getName()));
        style.setNotes(String.format
                       ("Style node for field '%s' in data-set '%s'\n\nSource: %s\n\n%s\n\nvalues=%d; unique=%d; type=%s",
                        field.getName(),
                        field.getSchema().getName(),
                        field.getSchema().getSource(),
                        field.valuesDebug(),
                        field.valueCount(),
                        field.uniqueValueCount(),
                        field.getType()
                       ));
        style.setTextColor(Color.white);
        //style.disableProperty(LWKey.Label);
        style.addLWCListener(repainter);
        style.setFlag(Flag.STYLE); // set last so creation property sets don't attempt updates
        
        return style;
    }

    public void LWCChanged(tufts.vue.LWCEvent e) {
        repaint();
    }


//     public static final java.awt.datatransfer.DataFlavor DataFlavor =
//         tufts.vue.gui.GUI.makeDataFlavor(DataNode.class);

    private static final Font EnumFont = new Font("SansSerif", Font.BOLD, 24);
    private static final Font DataFont = new Font("SansSerif", Font.PLAIN, 12);
        

    private static class DataNode extends DefaultMutableTreeNode {

        final Field field;
        String display;
        
        DataNode(Field field, LWComponent.Listener repainter, String description) {
            this.field = field;

            if (description == null) {
                if (field != null)
                    setDisplay(makeFieldLabel(field));
            } else
                setDisplay(description);

            //if (field != null && field.isEnumerated() && !field.isPossibleKeyField())
            if (field != null && !field.hasStyleNode() && !field.isSingleValue() && field.isEnumerated())
                field.setStyleNode(createStyleNode(field, repainter));
        }

        Vector<DataNode> getChildren() {
            return super.children;
        }
        
//         DataNode(String description) {
//             field = null;
//             setDisplay(description);
//         }

        protected DataNode(Field field) {
            this.field = field;
        }

        Schema getSchema() {
            return field.getSchema();
        }

        String getValue() {
            return null;
        }

        Field getField() {
            return field;
        }

        void setDisplay(String s) {
            display = s;
            setUserObject(s);  // sets display label
        }

        void annotate(LWMap map) {
            if (field != null) {

                // skip this summary entirely for now: we'd need Field to track
                // instances of schematic field nodes for it to work anyway
                // a good compromise for now would be to have a single indicator
                // on this node if any of it's children are present in the map
                
//                 if (field.isSingleValue()) {
//                     // tecnically, map could contain items that *don't* match the single value, in 
//                     // which case we could annotate the # of NON matching fields -- we do nothing for now
//                     //setAnnotation(null);
//                     setAnnotation("<single>");
//                 } else {
//                     final int count = field.getContextValueCount();
//                     if (count > 0) {
//                         if (field.getEnumValueCount() == count)
//                             setAnnotation("[all present]");
//                         else 
//                             setAnnotation(String.format("[%d present, %d new]",
//                                                         count,
//                                                         field.getEnumValueCount() - count));
//                     } else
//                         setAnnotation(null);
//                 }
                
            } else if (DEBUG.Enabled)
                setAnnotation(String.format("[%s]", map.getLabel()));
            //if (DEBUG.Enabled) Log.debug("annotate: no action for " + Util.tags(this));
        }

        void setAnnotation(String s) {

            setPostfix(s);
            
//             //Log.debug("annotating " + this + " with [" + s + "]");
//             if (s == null || s.length() == 0) {
//                 setUserObject(display);
//             } else {
//                 if (false) {
//                     setUserObject(display + " " + s);
//                 } else {
//                     //final String cs = (String) getUserObject();
//                     if (display.startsWith("<html>")) {
//                         setUserObject("<html>" + s + " " + display.substring(6));
//                     } else
//                         setUserObject(s + " " + display);
//                 }
//             }
//             //setUserObject(display + " {" + s + "}");
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
        

        LWComponent getStyle() {
            return field == null ? null : field.getStyleNode();
        }

        boolean hasStyle() {
            //return isField();
            return field != null && field.getStyleNode() != null;
        }

        boolean isField() {
            return field != null;
            //return value == null && field != null;
        }
        
        boolean isValue() {
            //return value != null;
            return !isField();
        }

        boolean isSchematic() {
            return field == null;
        }

        boolean isMapPresent() {
            return false;
        }

    }

    private static final class ValueNode extends DataNode {

        String value;
        boolean isMapPresent;

        ValueNode(Field field, String value, String label) {
            super(field);
            setDisplay(label);
            this.value = value;
        }
        
        String getValue() {
            return value;
        }

        @Override
        void annotate(LWMap map) {
            
            final int count = field.countContextValue(value);
            if (count > 0) {
                isMapPresent = true;
                setPostfix("(" + count + ")");
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
        
    }

    private static final class TemplateNode extends DataNode {

        Schema schema;

        TemplateNode(Schema schema, LWComponent.Listener repainter) {
            super(null,
                  repainter,
                  String.format(HTML("<b><font color=red>All Data Nodes (%d)"), schema.getRowCount()));
            //String.format("<html><b>All Data Nodes in '%s' (%d)", schema.getName(), schema.getRowCount()));
            //String.format(HTML("<b><font color=red>All Data Nodes in '%s' (%d)"), schema.getName(), schema.getRowCount()));
            
            this.schema = schema;
            
//             LWComponent style = new LWNode();
//             style.setFlag(Flag.INTERNAL);
//             style.setFlag(Flag.DATA_STYLE);
            final LWComponent style = initStyleNode(new LWNode());

            String titleField;

            if (schema.getRowCount() <= 25 && schema.hasField("title"))
                titleField = "title";
            else
                titleField = schema.getKeyFieldGuess().getName();
            
            style.setLabel(String.format("${%s}", titleField));
            
            style.setFont(DataFont);
            style.setTextColor(Color.white);
            style.setFillColor(Color.darkGray);
            style.setStrokeWidth(0);
            //style.disableProperty(LWKey.Notes);
            style.setNotes("Style for all " + schema.getRowCount() + " data items in " + schema.getName()
                           + "\n\nSchema: " + schema.getDump());
            style.setFlag(Flag.STYLE); // do last

            schema.setStyleNode(style);
        }

        @Override
        Schema getSchema() {
            return schema;
        }
        @Override
        boolean isField() { return false; }
        @Override
        boolean isValue() { return false; }
        @Override
        boolean hasStyle() { return true; }
        @Override
        LWComponent getStyle() { return schema.getStyleNode(); }
    }

    private static final int IconWidth = 32;
    private static final int IconHeight = 20;

    //private static final Border TopBorder = BorderFactory.createLineBorder(Color.gray);
//     private static final Border TopBorder = new CompoundBorder(new MatteBorder(3,0,3,0, Color.white),
//                                                                new CompoundBorder(new LineBorder(Color.gray),
//                                                                                   GUI.makeSpace(1,0,1,2)));
    private static final Border TopBorder = GUI.makeSpace(3,0,2,0);

    private static final Border TopTierBorder = GUI.makeSpace(0,0,2,0);
    private static final Border LeafBorder = GUI.makeSpace(0,IconWidth-16,2,0);
    
    //private static final Icon IncludedInMapIcon = VueResources.getImageIcon("vueIcon16");
    private static final Icon IncludedInMapIcon = VueResources.getIcon(VUE.class, "images/data_onmap.png");
    //private static final Icon IncludedInMapIcon = GUI.reframeIcon(VueResources.getIcon(VUE.class, "images/data_onmap.png"), 8, 16);
    private static final Icon NewToMapIcon = VueResources.getIcon(VUE.class, "images/data_offmap.png");
//     private static final GUI.ResizedIcon NewToMapIcon =
//         new GUI.ResizedIcon(VueResources.getIcon(GUI.class, "icons/MacSmallCloseIcon.gif"), 16, 16);

    private static final Color KeyFieldColor = Color.green.darker();


    private class DataRenderer extends DefaultTreeCellRenderer {

        {
            //setIconTextGap(2);
            //setBorder(LeafBorder);
            setVerticalTextPosition(SwingConstants.CENTER);
            //setTextNonSelectionColor(Color.black);
            //setFont(tufts.vue.VueConstants.SmallFixedFont);
        }

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
            
//             if (node.isField() && !leaf) {
//                 if (node.field.isPossibleKeyField())
//                     //setForeground(Color.red);
//                     setForeground(Color.black);
//                 else
//                     setForeground(Color.blue);
//             } else {
//                 setForeground(Color.black);
//             }

            if (field != null && field.isKeyField())
                setForeground(KeyFieldColor);
            else
                setForeground(Color.black); // must do every time for some reason, or de-selected text goes invisible
            
            setIconTextGap(4);

            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (node.hasStyle()) {
                //setIconTextGap(4);
                setIcon(FieldIconPainter.load(node.getStyle(),
                                              selected ? backgroundSelectionColor : null));
            } else {
                
                if (field != null && field.isSingleton()) {
                    setIcon(null);
                } else if (node.isValue()) {
                    setIconTextGap(1);
                    if (node.isMapPresent())
                        setIcon(IncludedInMapIcon);
                    else
                        setIcon(NewToMapIcon);
                }

//                 if (node.isValue() || node.isField() && node.field.isSingleton()) {
//                     setIconTextGap(4);
//                 } else {
//                     // this will alingn non-styled (non-enumerated) fields, that
//                     // are part of the data-set, but not tracked because they're too long
//                     setIconTextGap(IconWidth - 16 + 4);
//                 }
                
//                 if (leaf && node.isValue())
//                     setIcon(EmptyIcon);
            }

            if (row == 0) {
                //setBorder(null);
                setBorder(TopBorder);
                //setBackgroundNonSelectionColor(Color.lightGray);
                //setFont(EnumFont);
            } else {
                //setBackgroundNonSelectionColor(null);
                //setFont(null);
                //setBorder(leaf ? LeafBorder : null);
                if (leaf) {
                    if (node.isField() && node.field.isSingleton())
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
    
    private static final NodeIconPainter FieldIconPainter = new NodeIconPainter();

    private static final Icon EmptyIcon = new GUI.EmptyIcon(IconWidth, IconHeight);
    
    
    private static class NodeIconPainter implements Icon {

        LWComponent node;
        Color fill;

//         NodeIcon(LWComponent c) {
//             node = c;
//         }

        public Icon load(LWComponent c, Color fill) {
            this.node = c;
            this.fill = fill;
            return this;
        }
        
        public int getIconWidth() { return IconWidth; }
        public int getIconHeight() { return IconHeight; }
        
        public void paintIcon(Component c, Graphics g, int x, int y) {
            //Log.debug("x="+x+", y="+y);
            
            ((java.awt.Graphics2D)g).setRenderingHint
                (java.awt.RenderingHints.KEY_ANTIALIASING,
                 java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            if (fill != null) {
                if (DEBUG.BOXES) {
                    g.setColor(Color.red);
                    g.fillRect(x,y,IconWidth,IconHeight);
                } else {
                    g.setColor(fill);
                    // add to width to also fill the IconTextGap
                    g.fillRect(0,0,IconWidth+8,IconHeight+8);
                }
            }
            
            node.drawFit(new DrawContext(g.create(), node),
                         IconSize,
                         2);
            //node.drawFit(g, x, y);
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


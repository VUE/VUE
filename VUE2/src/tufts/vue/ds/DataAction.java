package tufts.vue.ds;

import tufts.Util;
import tufts.Util.Picker;
import tufts.vue.DEBUG;
import tufts.vue.LWMap;
import tufts.vue.LWComponent;
import tufts.vue.LWLink;
import tufts.vue.LWNode;
import tufts.vue.VueResources;
import tufts.vue.Resource;
import tufts.vue.MetaMap;

import static tufts.vue.ds.Schema.*;
import static tufts.vue.ds.Relation.*;

import static tufts.vue.LWComponent.Flag;

//import edu.tufts.vue.metadata.VueMetadataElement;

import java.awt.Color;
import java.awt.Font;

import java.util.*;

import com.google.common.collect.Multiset;

import org.apache.commons.lang.StringEscapeUtils;


/**
 * @version $Revision: 1.30 $ / $Date: 2010-01-20 19:56:37 $ / $Author: sfraize $
 * @author  Scott Fraize
 */

public final class DataAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataAction.class);

    public static final Object ClusterTimeKey = new tufts.vue.LWComponent.PersistClientDataKey("clusterTime");

    public static String valueText(Object value) {
        return StringEscapeUtils.escapeHtml(Field.valueText(value));
    }
    
//     private static List<LWLink> makeLinks(LWComponent node)
//     {
//         if (node.isDataRowNode())
//             return makeRowNodeLinks(getLinkTargets(node.getMap()), node);
//         else if (node.isDataValueNode())
//             return makeValueNodeLinks(getLinkTargets(node.getMap()), node, node.getDataValueField());
//         else
//             return Collections.EMPTY_LIST;
//     }
    
//     private static List<LWLink> makeLinks
//         (LWComponent node, Collection<LWComponent> linkTargets) {
//         if (node.isDataRowNode())
//             return makeRowNodeLinks(linkTargets, node);
//         else if (node.isDataValueNode())
//             return makeValueNodeLinks(linkTargets, node, node.getDataValueField());
//         else
//             return Collections.EMPTY_LIST;
//     }
        
    
    
    private static List<LWLink> makeDataLinksForNode
        (final LWComponent node,
         final Collection<? extends LWComponent> linkTargets,
         final Multiset<LWComponent> targetsUsed)
    {
        if (linkTargets.size() > 0) {
            return makeLinks(linkTargets, node, node.getDataValueField(), targetsUsed); // seems wrong to be providing this last argumen
        } else {
            return Collections.EMPTY_LIST;
        }

    }

    private static final Picker DataNodePicker = new Picker<LWComponent>() {
            public boolean match(LWComponent c) {
                return c.getClass() == LWNode.class;
                //return c.getClass() == LWNode.class && c.isDataNode();
            }};

    /** @return a list of all *possible* targets we may want to be linking to */
    private static List<? extends LWComponent> getLinkTargets(LWMap map) {
        return Util.extract(map.getAllDescendents(),
                            DataNodePicker);
        
    }


    /** @field -- if null, will make exaustive row-node links */
    public static Multiset<LWComponent> addDataLinksForNodes
        (final LWMap map,
         final List<? extends LWComponent> nodes,
         final Field field)
    {
        if (DEBUG.Enabled) Log.debug("addDataLinksForNodes; field=" + quoteKey(field));
        
        final Multiset<LWComponent> targetsUsed = com.google.common.collect.HashMultiset.create();
        final List<LWLink> links = makeDataLinksForNodes(map, nodes, field, targetsUsed);

        if (links.size() > 0)
            map.getInternalLayer("*Data Links*").addChildren(links);

        return targetsUsed;
    }

    private static List<LWLink> makeDataLinksForNodes
        (final LWMap map,
         final List<? extends LWComponent> nodes,
         final Field field,
         final Multiset<LWComponent> targetsUsed)
    {
        final Collection linkTargets = getLinkTargets(map);

        Log.debug("LINK-TARGETS: " + Util.tags(linkTargets));
        
        List<LWLink> links = Collections.EMPTY_LIST;
        
        if (linkTargets.size() > 0) {
            links = new ArrayList();
            for (LWComponent newNode : nodes) {
                links.addAll(makeLinks(linkTargets, newNode, field, targetsUsed));
            }
        }

        return links;
    }
    
    /** @param field -- if null, will defer to makeRowNodeLinks, and assume the given node is a row node */
    private static List<LWLink> makeLinks
        (final Collection<? extends LWComponent> linkTargets,
         final LWComponent node,
         final Field field,
         final Multiset<LWComponent> targetsUsed
        )
    {
        //Log.debug("makeLinks: " + field + "; " + node);

        if (field == null)
            return makeRowNodeLinks(linkTargets, node, targetsUsed);
        else
            return makeValueNodeLinks(linkTargets, node, field, targetsUsed);
    }

    public static List<LWComponent> makeRelatedNodes(Field dragField, LWComponent dropTarget) {
        if (DEBUG.Enabled) Log.debug("makeRelatedNodes: " + quoteKey(dragField) + "; target=" + dropTarget);
        return makeRelatedValueNodes(dragField, dropTarget.getRawData());
    }
    
    // Now that this takes a MetaMap, it could allow us to simply substitue a batch of
    // Resource meta-data instead, Tho there's no schema in that case, so we'd have to
    // deal with that...
    
    /**
     * Used for dragging a schema Field (column) onto an on-map row-node.  This
     * will use the Field to extract any values matching it from the row-node
     * (e.g., 7 different "category" values), or values pulled from a joined schema.
     */
    static List<LWComponent> makeRelatedValueNodes(Field dragField, MetaMap onMapRowData) {

        if (DEBUG.Enabled) Log.debug("makeRelatedValueNodes: " + quoteKey(dragField) + "; row=" + onMapRowData);

        // TODO: if rowData is from a single value node, this makes no sense -- should move
        // the methods for identifying the type of "row" (MetaMap) it is to MetaMap itself
        // (instead of LWComponent)

        final List<LWComponent> nodes = new ArrayList();

        for (String value : onMapRowData.getValues(dragField.getName())) {
            //-----------------------------------------------------------------------------
            // Note: We pull ALL values for the given Field: e.g., there may be 20 different
            // "category" values.
            //-----------------------------------------------------------------------------
            Log.debug("makeRelatedValueNodes: " + quoteKV(dragField, value));
            nodes.add(makeValueNode(dragField, value));
        }

//         for (String joinedValue : Relation.getCrossSchemaJoinedValues(dragField, onMapRowData, ALL_VALUES)) {
//             LWComponent newNode = makeValueNode(dragField, joinedValue);
//             //newNode.addDataValue("@index", String.format("%s=%s", indexKey, indexValue));
//             nodes.add(newNode);
//         }
        for (Relation join : Relation.getCrossSchemaJoinedValues(dragField, onMapRowData, ALL_VALUES)) {
            LWComponent newNode = makeValueNode(dragField, join.value);
            //newNode.addDataValue("@index", String.format("%s=%s", indexKey, indexValue));
            nodes.add(newNode);
        }

        
        return nodes;
    }


    /**
     * Make links from the given node, which is a value node for the given Field,
     * to any nodes in linkTargets for which a Relation can be found.
     */
    private static List<LWLink> makeValueNodeLinks
        (final Collection<? extends LWComponent> linkTargets,
         final LWComponent node,
         final Field field,// todo: remove arg? is not immediately clear this MUST be the Field in node (which MUST be a value node, yes?)
         final Multiset<LWComponent> targetsUsed) 
    {
        if (DEBUG.SCHEMA || DEBUG.WORK) {
            Log.debug("makeValueNodeLinks:"
                      + "\n\t  field: " + quoteKey(field)
                      + "\n\t   node: " + node
                      + "\n\ttargets: " + Util.tags(linkTargets));
            
            if (node.getDataValueField() != field)
                Util.printStackTrace("field mis-match: nodeField="
                                     + node.getDataValueField()
                                     + "; argField=" + field
                                     + "; node=" + node);
        }
        
        final List<LWLink> links = Util.skipNullsArrayList();

        final String fieldName = field.getName();
        final String fieldValue = node.getDataValue(fieldName);
        
        final Schema dragSchema = field.getSchema();

        for (LWComponent target : linkTargets) {

            if (target == node)
                continue;

            Log.debug("makeValueNodeLinks: processing " + target);

            try {

                // TODO: NEEDS TO USE ASSOCIATIONS
                // HANDLE VIA RELATIONS???
            
                // This is where ALSO where a JOIN needs to take place.  We want a way to do
                // that which is generic to schemas instead of just here, so dropping the
                // Rockwell.Medium FIELD on a Rockwell.Painting ROW will extract the right
                // value, as well as be discovered later here to create the link.
            
                if (target.hasDataValue(fieldName, fieldValue)) {
                    // if the target node c is schematic at all, it should only have
                    // one piece of meta-data, and it should be an exact match already
                    //boolean sameField = fieldName.equals(c.getSchematicFieldName());
                    final boolean sameField = target.isDataValueNode();
                    links.add(makeLink(node, target, fieldName, fieldValue, sameField ? Color.red : null));
                    targetsUsed.add(target);
                }

                final Relation relation = Relation.getCrossSchemaRelation(field, target.getRawData(), fieldValue);
                if (relation != null) {
                    links.add(makeLink(node, target, relation));
                }
//                 final String relatedValue = Relation.getCrossSchemaRelation(field, target.getRawData(), fieldValue);
//                 if (relatedValue != null) {
//                     final String relation = String.format("matched joined value \"%s\"", relatedValue);
//                     links.add(makeLink(node, target, null, relation, Color.green));
//                 }
            } catch (Throwable t) {
                Log.error("exception scanning for links from " + field + " to " + target + ":", t);
            }

        }

        if (DEBUG.SCHEMA || DEBUG.WORK) Log.debug("makeValueNodeLinks: returning:\n\t" + Util.tags(links) + "\n\n");
            
        return links;
    }

    

    // Auto-add actual associations for fields with the same name from different schemas?

    /** make links from row nodes (full data nodes) to any schematic field nodes found in the link targets,
     or between row nodes from different schema's that are considered "auto-joined" (e.g., a matching key field appears) */
    private static List<LWLink> makeRowNodeLinks
        (final Collection<? extends LWComponent> linkTargets,
         final LWComponent rowNode,
         final Multiset<LWComponent> targetsUsed)
    {
        if (!rowNode.isDataRowNode())
            Log.warn("making row links to non-row node: " + rowNode, new Throwable("FYI"));
        
        final Schema sourceSchema = rowNode.getDataSchema();
        final MetaMap sourceRow = rowNode.getRawData();

        if (DEBUG.Enabled) {
            String targets;
            if (linkTargets.size() == 1)
                targets = Util.getFirst(linkTargets).toString();
            else
                targets = Util.tags(linkTargets);
            Log.debug("makeRowNodeLinks: " + rowNode + "; " + rowNode.getRawData() + "; " + targets);
        }
        
        final List<LWLink> links = Util.skipNullsArrayList();

        final List<LWComponent> singletonTargetList = new ArrayList(2);
        singletonTargetList.add(null);
        
        for (LWComponent target : linkTargets) {

            if (target == rowNode) // never link to ourself
                continue;

            try {
            
                final Schema targetSchema = target.getDataSchema();

                if (targetSchema == null) {
                    //-----------------------------------------------------------------------------
                    // CHECK FOR RESOURCE META-DATA AND LABEL META-DATA
                    //-----------------------------------------------------------------------------
                    continue;
                }
            
                final Field singleValueField = target.getDataValueField();

                if (singleValueField != null) {
                    singletonTargetList.set(0, rowNode);
                    final List<LWLink> valueLinks
                        = makeValueNodeLinks(singletonTargetList, target, singleValueField, targetsUsed);
                    if (valueLinks.size() > 1)
                        Log.warn("more than 1 link added for single value node: " + Util.tags(valueLinks), new Throwable("HERE"));
                    links.addAll(valueLinks);
                    
                }
                
//                 final String singleValueFieldName = c.getDataValueFieldName();
//                 if (singleValueFieldName != null) {
//                     //-----------------------------------------------------------------------------
//                     // The target being inspected is a value node - create a link
//                     // if there's any matching value in the row node.  We don't
//                     // currently care if it's from the same schema or not: identical
//                     // field names currently always provide a match (sort of a weak auto-join)
//                     //-----------------------------------------------------------------------------
//                     final String fieldValue = c.getDataValue(singleValueFieldName);
//                     // TODO: USE ASSOCIATIONS
//                     if (rowNode.hasDataValue(singleValueFieldName, fieldValue)) {
//                         links.add(makeLink(c, rowNode, singleValueFieldName, fieldValue, null));
//                     }
//                 }

                else if (sourceSchema == targetSchema) {

                    final MetaMap targetRow = target.getRawData();

                    if (Relation.isSameRow(targetSchema, targetRow, sourceRow)) {
                        links.add(makeLink(rowNode, target, null, null, Color.orange));
                        targetsUsed.add(target);
                    }
                }

                else { // if (sourceSchema != targetSchema) {

                    final MetaMap targetRow = target.getRawData();

                    //Log.debug("looking for x-schema relation: " + sourceRow + "; " + targetRow);

                    final Relation relation = Relation.getRelation(sourceRow, targetRow);
                    
                    if (relation != null) {
                        links.add(makeLink(rowNode, target, relation));
                        targetsUsed.add(target);
                    }
                    
                    //makeCrossSchemaRowNodeLinks(links, sourceSchema, targetSchema, rowNode, c);
                }
                                  
                
            } catch (Throwable t) {
                Log.warn("makeRowNodeLinks: processing target: " + target, t);
            }
        }
        
        return links;
    }

    private static LWLink makeLink
        (final LWComponent src,
         final LWComponent dest,
         final Relation r)
    {
        if (DEBUG.Enabled) Log.debug("makeLink: " + r);
        
        final Color color;

        final LWLink link = makeLink(src, dest, null, r.getDescription(), null);

        if (link == null) { Log.error("link=null " + r); return null; }
        
        if (r.isCrossSchema()) {
            link.mStrokeStyle.setTo(LWComponent.StrokeStyle.DASH3);
            link.setStrokeWidth(2);
        }

        // todo: count style priority over join style
        
        if (r.type == Relation.AUTOMATIC) {
            color = Color.lightGray;
        } else if (r.type == Relation.USER) {
            color = Color.black;
        } else if (r.type == Relation.COUNT) {
            //if (true) return null;
            color = Color.lightGray;
            if (r.count == 1)
                link.setStrokeWidth(0.3f);
            else
                link.setStrokeWidth((float) Math.log(r.count));
            link.setTextColor(color);
            link.setLabel(String.format(" %d ", r.getCount()));
        } else if (r.type == Relation.JOIN) {
            color = Color.orange;
        } else
            color = Color.magenta; // unknown type!

        
        link.setStrokeColor(color);
        
//         if (r.type == Relation.JOIN)
//         else if (r.type == Relation.COUNT)
        
        return link;
    }

    private static LWLink makeLink
        (final LWComponent src,
         final LWComponent dest,
         final String fieldName, // if null, puts raw fieldValue as the entire relationship name
         final String fieldValue,
         final Color specialColor)
    {
        if (src.hasLinkTo(dest)) {
            // don't create a link if there already is one of any kind
            return null;
        }
        
        LWLink link = new LWLink(src, dest);
        link.setArrowState(0);
        if (specialColor != null) {
            link.mStrokeStyle.setTo(LWComponent.StrokeStyle.DASH3);
            link.setStrokeWidth(3);
            link.setStrokeColor(specialColor);
            if (specialColor == Color.red) {
                // complate hack for now till specialColor is a more semantically meaningful argument.
                // Disabling label on a "same" link so we can double-click it to collapse it.
                link.disableProperty(tufts.vue.LWKey.Label);
            }
        } else {
            link.setStrokeColor(Color.lightGray);
        }

        final String relationship;

        if (fieldName == null)
            relationship = fieldValue;
        else
            relationship = String.format("%s=%s",
                                         fieldName,
                                         StringEscapeUtils.escapeCsv(fieldValue));
        
        link.setAsDataLink(relationship);

        return link;

    }


    private static String makeLabel(Field f, Object value) {

        return f.valueDisplay(value);
        
//Log.debug("*** makeLabel " + f + " [" + value + "] emptyValue=" + (value == Field.EMPTY_VALUE));
// // This will be overriden by the label-style: this could would need to go there to work
// //         if (value == Field.EMPTY_VALUE)
// //             return String.format("(no [%s] value)", f.getName());
// //         else
//             return Field.valueName(value);
    }

    private static final int DataNodeLabelLength = VueResources.getInt("dataNode.labelLength", 30);
    
    public static LWComponent makeValueNode(Field field, String value)
    {
        final String displayLabel = makeLabel(field, value);
        
        final LWComponent node = new LWNode(displayLabel);

        node.setDataInstanceValue(field, value);

        if (field.hasStyleNode())
            node.setStyle(field.getStyleNode());

        // The set-style may have re-set the label, so we must wrap after / in case of that

        node.wrapLabelToWidth(DataNodeLabelLength);
        
        return node;
    }
    
//     public static LWComponent makeValueNode(Field field, String value)
//     {
//         final String displayLabel = makeLabel(field, value);
        
//         //Log.debug("DISPLAY LABEL: " + Util.tags(displayLabel) + " in field " + field + " with style " + field.getStyleNode());
        
//         //final String wrappedLabel = Util.formatLines(displayLabel, VueResources.getInt("dataNode.labelLength"));
        
//         //Log.debug("WRAPPED LABEL: " + Util.tags(wrappedLabel));
        
//         final LWComponent node = new LWNode(displayLabel);

// //         Log.debug("MADE VALUE NODE0: " + node);
// //         Log.debug("WITH LABEL0: " + Util.tags(node.getLabel()));
        
//         node.setDataInstanceValue(field, value);

//         if (field.hasStyleNode())
//             node.setStyle(field.getStyleNode());

//         // The set-style may have re-set the label, so we must wrap after / in case of that

//         // do need to set label afterwords to ensure any templating?

// //         Log.debug("MADE VALUE NODE1: " + node);
// //         Log.debug("WITH LABEL1: " + Util.tags(node.getLabel()));

//         node.wrapLabelToWidth(DataNodeLabelLength);
        
//         return node;

//     }

    public static List<LWComponent> makeSingleRowNode(Schema schema, DataRow singleRow) {

        Log.debug("PRODUCING SINGLE ROW NODE FOR " + schema + "; row=" + singleRow);

        List<DataRow> rows = Collections.singletonList(singleRow);

        return makeRowNodes(schema, rows);
        
    }
    
    public static List<LWComponent> makeRowNodes(Schema schema) {

        Log.debug(" " + schema + "; rowCount=" + schema.getRows().size());

        return makeRowNodes(schema, schema.getRows());
        
    }

//     public static List<LWComponent> makeRelatedRowNodes(Schema schema, MetaMap rowFilter) {
//     }
    
    public static List<LWComponent> makeRelatedRowNodes(Schema schema, LWComponent dropTargetFilter) {

        Log.debug("makeRelatedRowNodes " + schema + "; filter=" + dropTargetFilter);

        return makeRowNodes(schema,
                            Relation.findRelatedRows(schema, dropTargetFilter));
    }


    public static List<LWComponent> makeRowNodes
        (final Schema schema,
         final Collection<DataRow> rows)
    {
        if (rows.isEmpty())
            return Collections.EMPTY_LIST;

        if (schema.getRowNodeStyle() == null) {
            schema.setRowNodeStyle(makeStyleNode(schema));
            Log.info("auto-applied row-style to " + schema + ": " + schema.getRowNodeStyle());
        }
        
        final java.util.List<LWComponent> nodes = new ArrayList();

        // TODO: findField should find case-independed values -- wasn't our key hack supposed to handle that?

        final Field linkField = schema.findField("Link");
        final Field descField = schema.findField("Description");
        final Field titleField = schema.findField("Title");

        // Note: these fields are RSS specific -- just using them as defaults for now.
        // Not a big deal if their not found.
        //final Field mediaField = schema.findField("media:group.media:content.media:url");
        final Field mediaField = schema.findField("media:content@url");
        Field mediaDescription = schema.findField("media:description");
        if (mediaDescription == null)
            mediaDescription = schema.findField("media:content.media:description");
        final Field imageField = schema.getImageField();
        
        //final int maxLabelLineLength = VueResources.getInt("dataNode.labelLength", 50);
//         final Collection<DataRow> rows;
//         if (singleRow != null) {
//             Log.debug("PRODUCING SINGLE ROW NODE FOR " + schema + "; row=" + singleRow);
//             rows = Collections.singletonList(singleRow);
//         } else {
//             Log.debug("PRODUCING ALL DATA NODES FOR " + schema + "; rowCount=" + schema.getRows().size());
//             rows = schema.getRows();
//         }

        if (DEBUG.Enabled) {
            Log.debug("makeRowNodes " + schema + "; " + Util.tags(rows));
            Log.debug("IMAGE FIELD: " + imageField);
            Log.debug("MEDIA FIELD: " + mediaField);
        }

        final boolean singleRow = (rows.size() == 1);
        
        int i = 0;
        LWNode node;
        for (DataRow row : rows) {

            try {
            
                node = LWNode.createRaw();
                node.takeAllDataValues(row.getData());
                node.setStyle(schema.getRowNodeStyle()); // must have meta-data set first to pick up label template

//                 if (singleRow) {
//                     // if handling a single node (e.g., probably a single drag),
//                     // also apply & override with the current on-map creation style
//                     tufts.vue.EditorManager.targetAndApplyCurrentProperties(node);
//                 }

                String link = null;

                if (linkField != null) {
                    link = row.getValue(linkField);
                    if ("n/a".equals(link)) link = null; // TEMP HACK
                }
                
                if (link != null) {
                    node.setResource(link);
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
                        String media = row.getValue(mediaField);
                        if (DEBUG.WORK) Log.debug("attempting to set thumbnail " + Util.tags(media));
                        ((tufts.vue.URLResource)r).setURL_Thumb(media);
                    }
                }

                Resource IR = null;

                if (imageField != null) {
                    String image = row.getValue(imageField);
                    if (Resource.looksLikeURLorFile(image)) {
                        if (!image.equals("n/a")) // tmp hack for one of our old test data sets
                            IR = Resource.instance(image);
                    }
                }
                
                if (IR == null && mediaField != null) {
                    String media = row.getValue(mediaField);
                    if (Resource.looksLikeURLorFile(media))
                        IR = Resource.instance(media);
                }

                if (IR != null) {

                    String mediaDesc = null;
                        
                    if (mediaDescription != null)
                        mediaDesc = row.getValue(mediaDescription);
                        
                    if (mediaDesc == null && titleField != null)
                        mediaDesc = row.getValue(titleField);

                    if (mediaDesc != null) {
                        IR.setTitle(mediaDesc);
                        IR.setProperty("Title", mediaDesc);
                    }
                    
                    if (DEBUG.WORK) Log.debug("image resource: " + IR);
                    node.addChild(tufts.vue.LWImage.createNodeIcon(IR));
                }

                // This is now handled by LWComponent.fillLabelFormat for all data value replacements
                //String label = node.getLabel();
                //label = Util.formatLines(label, maxLabelLineLength);
                //node.setLabel(label);
                
                nodes.add(node);
            } catch (Throwable t) {
                Log.error("failed to create node for row " + row, t);
            }
            
            
        }
        
        Log.debug("makeRowNodes: PRODUCED NODE(S) FOR " + schema + "; count=" + nodes.size());
        
        return nodes;
    }

    private static final Color DataNodeColor = VueResources.getColor("node.dataRow.color", Color.gray);
    private static final float DataNodeStrokeWidth = VueResources.getInt("node.dataRow.stroke.width", 0);
    private static final Color DataNodeStrokeColor = VueResources.getColor("node.dataRow.stroke.color", Color.black);
    private static final Font DataNodeFont = VueResources.getFont("node.dataRow.font");

    private static final Color ValueNodeTextColor = VueResources.getColor("node.dataValue.text.color", Color.black);
    private static final Font ValueNodeFont = VueResources.getFont("node.dataValue.font");
    private static final Color[] ValueNodeDataColors = VueResources.getColorArray("node.dataValue.color.cycle");
    private static int NextColor = 0;

    static LWComponent initNewStyleNode(LWComponent style) {

        //style.setID(style.getURI().toString());

        //style.setFlag(Flag.DATA_STYLE); // must set before setting label, or template will atttempt to resolve
        
        Schema.runtimeInitStyleNode(style);
        
        // we use the persisted visible bit to store a bit for DataTree node expanded state
        // -- the actual visibility of the style node will never come into play as it's never
        // on a map
        style.setVisible(false); 
        return style;
    }
    
    /** @return an empty styling node (appearance values to be set elsewhere) */ 
    private static LWComponent makeStyleNode() {
        return initNewStyleNode(new LWNode());
    }

    /** @return a row-styling node for the given schema */
    public static LWComponent makeStyleNode(Schema schema)
    {
        final LWComponent style = makeStyleNode();

        String titleField;
            
        // Make a guess at what might be the best field to use for the node label text
        if (schema.getRowCount() <= 42 && schema.hasField("title")) {
            // if we have hundreds of nodes, title may be too long to use -- the key
            // field may well be shorter.
            titleField = "title";
        } else {
            titleField = schema.getKeyFieldGuess().getName();
        }
            
        style.setLabel(String.format("${%s}", titleField));
            
        style.setFont(DataNodeFont);
        style.setTextColor(Color.black);
        style.setFillColor(DataNodeColor);
        style.setStrokeWidth(DataNodeStrokeWidth);
        style.setStrokeColor(DataNodeStrokeColor);
        //style.disableProperty(LWKey.Notes);
        //String notes = String.format("Style for all %d data items in %s",
        String notes = String.format("Style for row nodes in Schema '%s'",
                                     schema.getName());

        //if (DEBUG.Enabled) notes += ("\n\nSchema: " + schema.getDump());
        style.setNotes(notes);
        //style.setFlag(Flag.STYLE); // do last

        return style;
    }

//     public static LWComponent makeStyleNode(final Field field) {
//         return makeStyleNode(field, null);
//     }
//     public static LWComponent makeStyleNode(final Field field, LWComponent.Listener repainter)
    public static LWComponent makeStyleNode(final Field field)
    {
        final LWComponent style;

        if (field.isPossibleKeyField()) {

            style = new LWNode(); // creates a rectangular node
            //style.setLabel(" ---");
            style.setFillColor(Color.lightGray);
            style.setFont(DataNodeFont);
        } else {
            //style = new LWNode(" ---"); // creates a round-rect node
            style = new LWNode(""); // creates a round-rect node
            //style.setFillColor(Color.blue);
            style.setFillColor(ValueNodeDataColors[NextColor]);
            if (++NextColor >= ValueNodeDataColors.length)
                NextColor = 0;
//             NextColor += 8;
//             if (NextColor >= DataColors.length) {
//                 if (FirstRotation) {
//                     NextColor = SecondRotationColor;
//                     FirstRotation = false;
//                 } else {
//                     NextColor = FirstRotationColor;
//                     FirstRotation = true;
//                 }
//             }
            style.setFont(ValueNodeFont);
        }
        initNewStyleNode(style); // must set before setting label, or template will atttempt to resolve
        //style.setLabel(String.format("%.9s: \n${%s} ", field.getName(),field.getName()));
//         if (field.isQuantile())
//             style.setLabel(String.format("%s\n${%s}", field.getName(), field.getName()));
//         else
            style.setLabel(String.format("${%s}", field.getName()));
        style.setNotes(String.format
                       ("Style node for field '%s' in data-set '%s'\n\nSource: %s\n\n%s\n\nvalues=%d; unique=%d; type=%s",
                        field.getName(),
                        field.getSchema().getName(),
                        field.getSchema().getResource(),
                        field.valuesDebug(),
                        field.valueCount(),
                        field.uniqueValueCount(),
                        field.getType()
                       ));
        style.setTextColor(ValueNodeTextColor);
        //style.disableProperty(LWKey.Label);
//         if (repainter != null)
//             style.addLWCListener(repainter);
        //style.setFlag(Flag.STYLE); // set last so creation property sets don't attempt updates
        
        return style;
    }


    
}

//         final Schema dragSchema = dragField.getSchema();
//         final Schema dropSchema = onMapRowData.getSchema();
//         Log.debug("Looking for joins between:"
//                   + "\n\t" + dragSchema
//                   + "\n\t" + dropSchema);
//         //for (DataRow row : dragSchema.getJoinedMatchingRows(join, dragField) {
            
//         int i = 0;
//         for (Association join : Association.getBetweens(dragSchema, dropSchema)) {

//             Log.debug("JOIN #" + i + ": " + Util.tags(join));
//             i++;
            
//             // This works for the Rockwell-Mediums case, tho only for initial node
//             // creation of course -- NEED TO GENERALIZE

//             final Field indexKey = join.getFieldForSchema(dragSchema);
//             final String indexValue = onMapRowData.getString(join.getKeyForSchema(dropSchema)); // todo: multi-values

//             Log.debug("JOIN: indexKey=" + indexKey + "; indexValue=" + indexValue);

//             final Collection<DataRow> matchingRows = dragSchema.getMatchingRows(indexKey, indexValue);

//             Log.debug("found rows: " + Util.tags(matchingRows));

//             final String extractKey = dragField.getName();

//             for (DataRow row : matchingRows) {
//                 // todo: use Schema.searchData?
//                 final Collection<String> joinedValues = row.getValues(extractKey);
//                 Log.debug("extracted " + Util.tags(joinedValues));
//                 for (String extractValue : joinedValues) {
//                     final LWComponent newNode = makeValueNode(dragField, extractValue);
//                     newNode.addDataValue("@index", String.format("%s=%s", indexKey, indexValue));
//                     nodes.add(newNode);
//                 }
//             }
                                       
//         }
    
//             //----------------------------------------------------------------------------------------
//             // TODO: below is essentially cut & paste from above makeRelatedValueNodes
//             //----------------------------------------------------------------------------------------

//             final MetaMap onMapRowData = target.getRawData();
//             final Schema dropSchema = onMapRowData.getSchema();
//             final Field dragField = field;

//             if (dragSchema != dropSchema) {

//                 int i = 0;
//                 for (Association join : Association.getBetweens(dragSchema, dropSchema)) {

//                     Log.debug("JOIN #" + i + ": " + Util.tags(join));
//                     i++;
            
//                     // This works for the Rockwell-Mediums case, tho only for initial node
//                     // creation of course -- NEED TO GENERALIZE

//                     final Field indexKey = join.getFieldForSchema(dragSchema);
//                     final String indexValue = onMapRowData.getString(join.getKeyForSchema(dropSchema)); // todo: multi-values

//                     Log.debug("JOIN: indexKey=" + indexKey + "; indexValue=" + indexValue);

//                     final Collection<DataRow> matchingRows = dragSchema.getMatchingRows(indexKey, indexValue);

//                     Log.debug("found rows: " + Util.tags(matchingRows));

//                     final Field extractKey = dragField;

//                     for (DataRow row : matchingRows) {
//                         // todo: use Schema.searchData?
//                         final Collection<String> joinedValues = row.getValues(extractKey);
//                         Log.debug("extracted " + Util.tags(joinedValues));
//                         for (String extractValue : joinedValues) {

//                             //------------------------------------------------------------------
//                             if (!fieldValue.equals(extractValue)) // ** THE EXCLUSION **
//                                 continue;
//                             //------------------------------------------------------------------
                            
//                             final String relation = String.format("%s=\"%s\"\n%s=\"%s\"",
//                                                                   indexKey, indexValue,
//                                                                   extractKey, extractValue);
//                             links.add(makeLink(node, target, null, relation, Color.green));
//                         }
//                     }
                                       
//                 }
                
//            }


// temp hack: if we keep this, have it populate an existing container
//     public LWMap.Layer createSchematicLayer() {

//         final LWMap.Layer layer = new LWMap.Layer("Schema: " + getName());

//         Field keyField = null;

//         for (Field field : getFields()) {
//             if (field.isPossibleKeyField() && !field.isLenghtyValue()) {
//                 keyField = field;
//                 break;
//             }
//         }
//         // if didn't find a "short" key field, find the shortest
//         if (keyField == null) {
//             for (Field field : getFields()) {
//                 if (field.isPossibleKeyField()) {
//                     keyField = field;
//                     break;
//                 }
//             }
//         }

//         boolean labelGuessed = false;

//         LWNode keyNode = null;
//         if (keyField != null) {
//             keyNode = new LWNode("itemNode");
//             keyNode.setShape(java.awt.geom.Ellipse2D.Float.class);
//             keyNode.setProperty(tufts.vue.LWKey.FontSize, 32);
//             keyNode.setNotes(getDump());
//         }

//         int y = Short.MAX_VALUE;
//         for (Field field : Util.reverse(getFields())) { // reversed to preserve on-map stacking order
//             if (field.isSingleton())
//                 continue;
//             LWNode colNode = new LWNode(field.getName());
//             colNode.setLocation(0, y--);
//             if (field.isPossibleKeyField()) {
//                 if (keyNode != null && !labelGuessed) {
//                     keyNode.setLabel("${" + field.getName() + "}");
//                     labelGuessed = true;
//                 }
//                 colNode.setFillColor(java.awt.Color.red);
//             } else
//                 colNode.setFillColor(java.awt.Color.gray);
//             colNode.setShape(java.awt.geom.Rectangle2D.Float.class);
//             colNode.setNotes(field.valuesDebug());
//             layer.addChild(colNode);

//             if (keyNode != null)
//                 layer.addChild(new tufts.vue.LWLink(keyNode, colNode));
            
//         }

//         if (keyNode != null) {
//             layer.addChild(keyNode);
//             //tufts.vue.Actions.MakeColumn.act(layer.getChildren());
//             tufts.vue.Actions.MakeCircle.act(Collections.singletonList(keyNode));
//         } else {
//             tufts.vue.Actions.MakeColumn.act(layer.getChildren());
//         }

//         return layer;
//     }

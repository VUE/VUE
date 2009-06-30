package tufts.vue.ds;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.LWMap;
import tufts.vue.LWComponent;
import tufts.vue.LWLink;
import tufts.vue.LWNode;
import tufts.vue.VueResources;
import tufts.vue.Resource;
import tufts.vue.MetaMap;

import static tufts.vue.ds.Schema.*;

import static tufts.vue.LWComponent.Flag;

import edu.tufts.vue.metadata.VueMetadataElement;

import java.awt.Color;
import java.awt.Font;

import java.util.*;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * @version $Revision: 1.18 $ / $Date: 2009-06-30 17:30:11 $ / $Author: sfraize $
 * @author  Scott Fraize
 */

public final class DataAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataAction.class);

    /** annotate the given schema (note what is present and what isn't) based on the data nodes found in the given map */
    public static void annotateForMap(final Schema schema, final LWMap map)
    {
        //if (DEBUG.Enabled) Log.debug("ANNOTATING for " + map + "; " + schema);

        final Collection<LWComponent> allDataNodes;

        if (map == null) {
            // if map is null, we annotate with an empty list, which will clear all annotation data
            // (everything in the schema will be marked as newly present)
            allDataNodes = Collections.EMPTY_LIST;
        } else {
            final Collection<LWComponent> allNodes = map.getAllDescendents();
            allDataNodes = new ArrayList(allNodes.size());
            for (LWComponent c : allNodes)
                if (c.isDataNode())
                    allDataNodes.add(c);
        }

        schema.annotateFor(allDataNodes);
    }
    

    public static String valueName(Object value) {
        return StringEscapeUtils.escapeHtml(Field.valueName(value));
    }
    
    private static List<LWLink> makeDataLinksForNodes(LWMap map, List<? extends LWComponent> nodes, Field field)
    {
        final Collection linkTargets = getLinkTargets(map);
        
        java.util.List<LWLink> links = Collections.EMPTY_LIST;
        
        if (linkTargets.size() > 0) {
            links = new ArrayList();
            for (LWComponent c : nodes) {
                links.addAll(makeLinks(linkTargets, c, field));
            }
        }

        return links;
    }
    
    private static List<LWLink> makeDataLinksForNode(final LWComponent node, final Collection<? extends LWComponent> linkTargets)
    {
        if (linkTargets.size() > 0) {
            return makeLinks(linkTargets, node, node.getDataValueField()); // seems wrong to be providing this last argumen
        } else {
            return Collections.EMPTY_LIST;
        }

    }

    /** @return a list of all *possible* targets we way want to be linking to */
    public static List<? extends LWComponent> getLinkTargets(LWMap map) {
        // todo: don't need to pull all LWNodes -- only those with data in them
        return Util.extractType(map.getAllDescendents(), LWNode.class);
    }


    /** @field -- if null, will make exaustive row-node links */
    public static boolean addDataLinksForNodes
        (LWMap map,
         List<? extends LWComponent> nodes,
         Field field)
    {
        
        final List<LWLink> links = makeDataLinksForNodes(map, nodes, field);

        if (links.size() > 0) {
            map.getInternalLayer("*Data Links*").addChildren(links);
            return true;
        } else
            return false;
    }

    private static List<LWLink> makeLinks(LWComponent node)
    {
        if (node.isDataRowNode())
            return makeRowNodeLinks(getLinkTargets(node.getMap()), node);
        else if (node.isDataValueNode())
            return makeValueNodeLinks(getLinkTargets(node.getMap()), node, node.getDataValueField());
        else
            return Collections.EMPTY_LIST;
    }
    
    private static List<LWLink> makeLinks(LWComponent node, Collection<LWComponent> linkTargets) {

        if (node.isDataRowNode())
            return makeRowNodeLinks(linkTargets, node);
        else if (node.isDataValueNode())
            return makeValueNodeLinks(linkTargets, node, node.getDataValueField());
        else
            return Collections.EMPTY_LIST;
    }
        
    /** @param field -- if null, will defer to makeRowNodeLinks, and assume the given node is a row node */
    public static List<LWLink> makeLinks(final Collection<? extends LWComponent> linkTargets, LWComponent node, Field field)
    {
        //Log.debug("makeLinks: " + field + "; " + node);

        if (field == null)
            return makeRowNodeLinks(linkTargets, node);
        else
            return makeValueNodeLinks(linkTargets, node, field);
    }




    // Now that this takes a MetaMap, it could allow us to simply substitue a batch of
    // Resource meta-data instead, Tho there's no schema in that case, so we'd have to
    // deal with that...
    
    public static List<LWComponent> makeRelatedValueNodes(Field field, MetaMap rowData) {

        Log.debug("PRODUCING RELATED VALUE NODES FOR FIELD: " + field + "; src=" + rowData);

        // TODO: if rowData is from a single value node, this makes no sense

        final java.util.List<LWComponent> nodes = new ArrayList();

        //-----------------------------------------------------------------------------
        // Note: We pull ALL values for the given Field: e.g., there may be 20 different
        // "category" values.
        //-----------------------------------------------------------------------------

        //-----------------------------------------------------------------------------
        //
        // IMPLEMENT FULL JOIN HERE: if I drag Rockwell-Mediums.medium onto a
        // Rockwell-Paintings.<row-node> and Mediums has been joined to Paintings via
        // "titles", then I should be able to search for all Mediums rows with
        // a title that matches the drop-target row-node "titles" (doesn't have to be same name),
        // and those will be the value nodes.

        // So we should probably replace the below collection source with something
        // like Field.getMatchingValues(rowNode.getRawData()), which will work like
        // Schema.getMatchingValues (or put it right in the schema code).
        //
        //-----------------------------------------------------------------------------
        
        for (String value : rowData.getValues(field.getName())) {
            Log.debug("makeRelatedValueNodes: " + field + "=" + value);
            nodes.add(makeValueNode(field, value));
        }

        if (field.getSchema() != rowData.getSchema()) {
            Log.debug("LOOKING FOR FULL JOINS");
        }
        
        return nodes;
    }


    

    public static List<LWLink> makeValueNodeLinks
        (final Collection<? extends LWComponent> linkTargets,
         LWComponent node,
         Field field)
    {
        final List<LWLink> links = Util.skipNullsArrayList();

        final String fieldName = field.getName();
        final String fieldValue = node.getDataValue(fieldName);

        for (LWComponent c : linkTargets) {
            if (c == node)
                continue;

            if (c.hasDataValue(fieldName, fieldValue)) {
                // if the target node c is schematic at all, it should only have
                // one piece of meta-data, and it should be an exact match already
                //boolean sameField = fieldName.equals(c.getSchematicFieldName());
                final boolean sameField = c.isDataValueNode();
                links.add(makeLink(node, c, fieldName, fieldValue, sameField ? Color.red : null));
            }
                
        }
            
        return links;
    }

    //----------------------------------------------------------------------------------------
    // todo: all these methods take DataRows, which are really a wrapper of a MetaMap --
    // should chage all this stuff to work from MetaMaps (which actually have the
    // darn schema in them now anyway!)  And then we could also use some of the util
    // functions / link-creating funtions based on LWComponent to use MetaMap's as well.

    // TODO: need to move isDataValueNode / isDataRowNode to MetaMap
    //----------------------------------------------------------------------------------------
    /**
     * @param schema - the schema to search through
     * @param filterNode - the data-node (either a data-row-node or a data-value-node),
     * to use as a basis for searching the schema for related rows
     */

    private static Collection<DataRow> findRelatedRows(Schema schema, LWComponent filterNode) {

        Log.debug("FINDING ROWS IN " + schema);

        if (filterNode.isDataValueNode()) {

            Log.debug("FINDING ROWS RELATED TO A SINGLE VALUE NODE: " + filterNode);
            
            //-----------------------------------------------------------------------------
            // TODO: Support "merged" or compound value nodes.  A node that either has multiple
            // values, or multiple keys and values.  This will complicate this code, tho
            // maybe it will actually all become more similar in some respects as it
            // will all behave like a row-based search?  Actually, all we need to do is
            // get all (non-internal) key/values from the filterNode, and accumulate the
            // matching rows here, or impl that in getMatchingRows.
            //
            // Compound nodes would make the HIERARCHY use case much easier.  E.g.,
            // in ClubZora test set, drop "Role" onto "Latin America", where "Latin America"
            // is surrounded by it's row nodes, and we simply create compound value
            // nodes like this:
            //  Compound 1: Region="Latin America", Role="user"
            //  Compound 2: Region="Latin America", Role="mentor"
            //  Compound 3: Region="Latin America", Role="admin"
            //  etc...
            //
            // And then we'd cluster these compounds around Latin America, with extra
            // distance for the 2nd tier clustering, and then re-cluster the row nodes
            // around each of the compound nodes.  The relationship from each compound
            // node to it's set of row nodes would naturally be found by the search
            // routines once their modified to handle compound matching, which shouldn't
            // be too hard.  In this particular use case, the links from "Latin America"
            // to all the row nodes may become pretty messy/noisy tho -- need helper
            // actions for cleaning those out.
            //
            // Or, could hack this at the map level by MakeCluster on a parent node
            // w/no links does a joined cluster from all data-value children.
            // OR even just by putting them in a group.  Cluster nodes could be made
            // smart to prefer clustering for each of the grouped relations
            // in the "pie-slice" closest / most facing the appropriate node, tho
            // that would only really work with 2 nodes when stacked vertically,
            // more nodes than that would work best laid out in a row (as nodes are
            // usually wider than they are tall, especially value nodes).
            // -----------------------------------------------------------------------------
        
            final Field filterField = filterNode.getDataValueField();
            final String filterValue = filterNode.getDataValue(filterField.getName());

            return schema.getMatchingRows(filterField, filterValue);
        }
        else if (filterNode.isDataRowNode()) {

            Log.debug("FINDING ROWS MATCHING KEY FIELD OF FILTERING ROW NODE: " + filterNode);
            
            final Schema filterSchema = filterNode.getDataSchema();
            
            if (filterSchema == schema)
                throw new Error("can't do row-node filter from same schema");

            return schema.getMatchingRows(filterNode.getRawData());
            
        } else {
            throw new Error("unhandled filter case: " + filterNode);
        }

    }

    // Auto-add actual associations for fields with the same name from different schemas?

    /** make links from row nodes (full data nodes) to any schematic field nodes found in the link targets,
     or between row nodes from different schema's that are considered "auto-joined" (e.g., a matching key field appears) */
    public static List<LWLink> makeRowNodeLinks
        (final Collection<? extends LWComponent> linkTargets,
         final LWComponent rowNode)
    {
        if (!rowNode.isDataRowNode())
            Log.warn("making row links to non-row node: " + rowNode, new Throwable("FYI"));
        
        final Schema sourceSchema = rowNode.getDataSchema();
        final MetaMap sourceRow = rowNode.getRawData();

        //final Collection<LWComponent> results = new ArrayList();  // need the relationship as well?
        //Schema.searchDataWithJoin(sourceSchema, null, rowNode.getRawData(), linkTargets, results);

        if (DEBUG.Enabled) {
            String targets;
            if (linkTargets.size() == 1)
                targets = Util.extractFirstValue(linkTargets).toString();
            else
                targets = Util.tags(linkTargets);
            Log.debug("makeRowNodeLinks: " + rowNode + "; " + rowNode.getRawData() + "; " + targets);
        }
        
        final List<LWLink> links = Util.skipNullsArrayList();
        
        for (LWComponent c : linkTargets) {

            if (c == rowNode) // never link to ourself
                continue;

            try {
            
                final Schema targetSchema = c.getDataSchema();

                if (targetSchema == null) {
                    // if no data schema, nothing to do (someday: check for resource meta-data)
                    continue;
                }
            
                final String singleValueFieldName = c.getDataValueFieldName();

                if (singleValueFieldName != null) {

                    //-----------------------------------------------------------------------------
                    // The target being inspected is a value node - create a link
                    // of there's any matching value in the row node.  We don't
                    // currently care if it's from the same schema or not: identical
                    // field names currently always provide a match (sort of a weak auto-join)
                    //-----------------------------------------------------------------------------
                    
                    final String fieldValue = c.getDataValue(singleValueFieldName); 
                
                    //-----------------------------------------------------------------------------
                    // TODO: USE ASSOCIATIONS?
                    //-----------------------------------------------------------------------------
                    if (rowNode.hasDataValue(singleValueFieldName, fieldValue)) {
                        links.add(makeLink(c, rowNode, singleValueFieldName, fieldValue, null));
                    }
                }

                else if (sourceSchema == targetSchema) {

                    final MetaMap targetRow = c.getRawData();

                    if (Schema.isSameRow(targetSchema, targetRow, sourceRow)) {
                        links.add(makeLink(rowNode, c, null, null, Color.orange));
                    }
                }

                else { // if (sourceSchema != targetSchema) {

                    final MetaMap targetRow = c.getRawData();

                    //Log.debug("looking for x-schema relation: " + sourceRow + "; " + targetRow);

                    final Relation relation = Schema.getRelation(sourceRow, targetRow);
                    
                    if (relation != null)
                        links.add(makeLink(rowNode, c, relation));
                    
                    //makeCrossSchemaRowNodeLinks(links, sourceSchema, targetSchema, rowNode, c);
                }
                                  
                
            } catch (Throwable t) {
                Log.warn("makeRowNodeLinks: " + t + "; processing target: " + c.getUniqueComponentTypeLabel());
            }
        }
        
        return links;
    }

    private static LWLink makeLink
        (final LWComponent src,
         final LWComponent dest,
         final Relation r)
    {
        final Color color;

        if (r.type == RELATION_JOIN)
            color = Color.darkGray;
        else if (r.type == RELATION_AUTO_JOIN)
            color = Color.lightGray;
        else
            color = Color.green;
        
        return makeLink(src, dest, r.key, r.value, color);
        
//         final LWLink link = makeLink(src, dest, r.key, r.value, color);
// //         if (r.isForward)
// //             link.setArrowState(LWLink.ARROW_HEAD);
// //         else
// //             link.setArrowState(LWLink.ARROW_TAIL);
//         if (DEBUG.Enabled) link.setNotes(r.toString());
//         return link;
    }

    private static LWLink makeLink
        (final LWComponent src,
         final LWComponent dest,
         final String fieldName,
         final String fieldValue,
         final Color specialColor)
    {
        if (src.hasLinkTo(dest)) {
            // don't create a link if there already is one of any kind
            return null;
        }
        
        LWLink link = new LWLink(src, dest);
        link.setArrowState(0);
        link.setStrokeColor(java.awt.Color.lightGray);
        if (specialColor != null) {
            link.mStrokeStyle.setTo(LWComponent.StrokeStyle.DASH3);
            link.setStrokeWidth(3);
            link.setStrokeColor(specialColor);
        }

        final String relationship = String.format("%s=%s",
                                                  fieldName,
                                                  StringEscapeUtils.escapeCsv(fieldValue));
        link.setAsDataLink(relationship);

        return link;

    }


    private static String makeLabel(Field f, Object value) {
        //Log.debug("*** makeLabel " + f + " [" + value + "] emptyValue=" + (value == Field.EMPTY_VALUE));

// This will be overriden by the label-style: this could would need to go there to work
//         if (value == Field.EMPTY_VALUE)
//             return String.format("(no [%s] value)", f.getName());
//         else
            return Field.valueName(value);
    }

    public static LWComponent makeValueNode(Field field, String value) {
        
        LWComponent node = new LWNode(makeLabel(field, value));
        //node.setDataInstanceValue(field.getName(), value);
        node.setDataInstanceValue(field, value);
        //node.setClientData(Field.class, field);
        if (field.getStyleNode() != null)
            node.setStyle(field.getStyleNode());
//         else
//             tufts.vue.EditorManager.targetAndApplyCurrentProperties(node);
        String target = node.getLabel();
        
        target = Util.formatLines(target, VueResources.getInt("dataNode.labelLength"));
        
        node.setLabel(target);
        return node;

    }

    public static List<LWComponent> makeSingleRowNode(Schema schema, DataRow singleRow) {

        Log.debug("PRODUCING SINGLE ROW NODE FOR " + schema + "; row=" + singleRow);

        List<DataRow> rows = Collections.singletonList(singleRow);

        return makeRowNodes(schema, rows);
        
    }
    
    public static List<LWComponent> makeRowNodes(Schema schema) {

        Log.debug("PRODUCING ALL DATA NODES FOR " + schema + "; rowCount=" + schema.getRows().size());

        return makeRowNodes(schema, schema.getRows());
        
    }

//     public static List<LWComponent> makeRelatedRowNodes(Schema schema, MetaMap rowFilter) {
//     }
    
    public static List<LWComponent> makeRelatedRowNodes(Schema schema, LWComponent rowFilter) {

        Log.debug("PRODUCING FILTERED ROW NODES FOR " + schema + "; filter=" + rowFilter);

        return makeRowNodes(schema, findRelatedRows(schema, rowFilter));
    }


    public static List<LWComponent> makeRowNodes
        (Schema schema, final Collection<DataRow> rows)
    {
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
        
        final int maxLabelLineLength = VueResources.getInt("dataNode.labelLength", 50);
        
//         final Collection<DataRow> rows;

//         if (singleRow != null) {
//             Log.debug("PRODUCING SINGLE ROW NODE FOR " + schema + "; row=" + singleRow);
//             rows = Collections.singletonList(singleRow);
//         } else {
//             Log.debug("PRODUCING ALL DATA NODES FOR " + schema + "; rowCount=" + schema.getRows().size());
//             rows = schema.getRows();
//         }

        Log.debug("PRODUCING ROW NODE(S) FOR " + schema + "; " + Util.tags(rows));
        Log.debug("IMAGE FIELD: " + imageField);
        Log.debug("MEDIA FIELD: " + mediaField);

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
                        Log.debug("attempting to set thumbnail " + Util.tags(media));
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
                    
                    Log.debug("image resource: " + IR);
                    node.addChild(new tufts.vue.LWImage(IR));
                }

                //Log.debug("produced node " + node);
                String label = node.getLabel();

                label = Util.formatLines(label, maxLabelLineLength);
                node.setLabel(label);
                nodes.add(node);
            } catch (Throwable t) {
                Log.error("failed to create node for row " + row, t);
            }
            
            
        }
        
        Log.debug("PRODUCED NODE(S) FOR " + schema + "; count=" + nodes.size());
        
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

    private static LWComponent initNewStyleNode(LWComponent style) {
        //style.setFlag(Flag.INTERNAL);
        style.setFlag(Flag.DATA_STYLE); // must set before setting label, or template will atttempt to resolve
        style.setID(style.getURI().toString());
        // we use the persisted visible bit to store a bit for DataTree node expanded state
        // -- the actual visibility of the style node will never come into play as it's never
        // on a map
        style.setVisible(false); 
        return style;
    }
    
    /** @return an empty styling node (appearance values to be set elsewhere) */ 
    public static LWComponent makeStyleNode() {
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
        style.setFlag(Flag.STYLE); // do last

        return style;
    }

    public static LWComponent makeStyleNode(final Field field) {
        return makeStyleNode(field, null);
    }
    public static LWComponent makeStyleNode(final Field field, LWComponent.Listener repainter)
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
        if (repainter != null)
            style.addLWCListener(repainter);
        style.setFlag(Flag.STYLE); // set last so creation property sets don't attempt updates
        
        return style;
    }


    
}

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

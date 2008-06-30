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
package tufts.vue;

import tufts.Util;
import tufts.vue.NodeTool.NodeModeTool;
import static tufts.vue.LWSlide.*;

import java.util.*;
import java.awt.Color;



/**
 *
 * Utility methods for auto-generating and synchronizing slides for pathway entries.
 * (A pathway entry usually pairs a node with a slide, although they don't require a slide).
 *
 * @author Scott Fraize
 * @version $Revision: 1.10 $ / $Date: 2008-06-30 20:52:55 $ / $Author: mike $
 */
class Slides {

    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Slides.class);

//     private static LWNode createNode(Resource r) {
//         LWNode newNode = NodeModeTool.createNewNode(r.getTitle());
//         newNode.setResource(r);
//         return newNode;
//     }

    // todo: resource should always be from the node, so can eventually have this take just one arg when done refactoring:
    private static LWNode createNode(LWComponent node, Resource r) {

        if (!r.equals(node.getResource()))
            Log.warn("createNode: resource not on node: " + node + "; r=" + r);

        String label;

        if (node.hasLabel())
            label = node.getLabel();
        else
            label = r.getTitle();
        LWNode newNode = NodeModeTool.createNewNode(label);
        newNode.setResource(r);
        return newNode;
    }
    
    
    
    protected enum Sync { ALL, TO_NODE, TO_SLIDE };
    
    private static LWSlide CreatePathwaySlide(LWPathway.Entry entry)
    {
        final LWSlide s = LWSlide.instance();
        s.setStrokeWidth(0f);
        s.setFillColor(null);
        s.setPathwayEntry(entry);
        return s;
    }
    
//     public static LWSlide CreateForPathwayEntry(LWPathway.Entry e) {
//         return CreateForPathwayEntry(e, e.node.getDisplayLabel(), e.node, e.node.getAllDescendents(), false);
//     }
        
//     public static LWSlide CreateForPathwayEntry(LWPathway.Entry entry,
//                                            String titleText,
//                                            LWComponent mapNode,
//                                            Iterable<LWComponent> contents,
//                                            boolean syncTitle) 
//     {
        
//         final LWSlide slide = CreatePathwaySlide(entry);
//         final LWNode title = NodeModeTool.buildTextNode(titleText);
//         final MasterSlide master = entry.pathway.getMasterSlide();
//         final CopyContext cc = new CopyContext(false);
//         final LinkedList<LWComponent> toLayout = new java.util.LinkedList();

    
    public static LWSlide CreateForPathwayEntry(LWPathway.Entry entry)
    {
        final LWSlide slide = buildPathwaySlide(entry);
        
        entry.pathway.ensureID(slide);

        return slide;
    }

    private static List<LWComponent> getContentToCopy(LWComponent node) {

        final List<LWComponent> content = new ArrayList();
        node.getAllDescendents(ChildKind.PROPER, content);

        // remove any image that's a node icon, as it'll be
        // added via it's node:

        Iterator<LWComponent> i = content.iterator();
        while (i.hasNext()) {
            LWComponent c = i.next();
            if (c instanceof LWImage && ((LWImage)c).isNodeIcon())
                i.remove();
            else if (c instanceof LWLink)
                i.remove();
        }
        
        return content;
        
    }

    /** helpder class used during auto-slide layout that pairs an image with a title for it */
    private static class TitledImage {
        final LWImage image;
        final LWNode title;
        TitledImage(LWComponent imageNode, LWImage i) {
            image = i;
            image.setSyncSource(imageNode);
            if (imageNode != null) {
                String txt;
                if (imageNode.hasLabel())
                    txt = imageNode.getLabel();
                else
                    txt = i.getLabel();
                if (txt == null || txt.length() == 0)
                    txt = "Image";
                title = new LWNode(txt);
                title.setAsTextNode(true);
                title.setSyncSource(imageNode);
            } else
                title = null;
        }

        TitledImage(LWImage syncSource, LWImage i) {
            image = i;
            title = new LWNode(image.getLabel());
            title.setAsTextNode(true);
            title.setSyncSource(syncSource);
        }
    }

    private static LWSlide buildPathwaySlide(LWPathway.Entry entry) {
        return buildPathwaySlide(CreatePathwaySlide(entry), entry);
    }
    
    private static LWSlide buildPathwaySlide(final LWSlide slide, final LWPathway.Entry entry)
    {
        //final LWSlide slide = CreatePathwaySlide(entry);
        final LWNode title = NodeModeTool.buildTextNode(entry.node.getDisplayLabel());
        final MasterSlide master = entry.pathway.getMasterSlide();
        final CopyContext cc = new CopyContext(false);
        final List<LWComponent> added = new ArrayList();

        title.setStyle(master.getTitleStyle());
        
// SMF: No syncing of slide titles, as per Melanie as of 2007-11-13
//         if (entry.node != null) {
//             if (LWNode.isImageNode(entry.node) || entry.node instanceof LWImage)
//                 ; // don't sync titles of images
//             else
//                 title.setSyncSource(entry.node);
//         }

        // we must apply slide styles to all components before laying them out,
        // as the style may change their size

        title.setLocation(SlideMargin, SlideMargin);
        slide.applyStyle(title);
        added.add(title);

        //final List<LWImage> images = new ArrayList();
        //final Map<LWImage,LWComponent> imageLabels = new HashMap();
        final List<TitledImage> images = new ArrayList();
        final List<LWComponent> text = new ArrayList();

        for (LWComponent c : getContentToCopy(entry.node)) {

            if (LWNode.isImageNode(c)) {
                Log.debug(" SPLITTING " + c);

                images.add(new TitledImage(c, ((LWNode)c).getImage().duplicate(cc)));

            } else {
                Log.debug("   COPYING " + c);
                final LWComponent copy = c.duplicate(cc);
                copy.setScale(1);
                copy.setSyncSource(c);

                if (copy instanceof LWImage)
                    images.add(new TitledImage((LWImage)c, (LWImage)copy));
                else
                    text.add(copy);
            }
        }

        // TODO: if the source node is an image node, need to make sure
        // it's image gets added (is filtered by getContentToCopy)
        if (LWNode.isImageNode(entry.node)) {
            if (images.size() == 0)
                images.add(new TitledImage((LWComponent)null, ((LWNode)entry.node).getImage().duplicate(cc)));
            else
                images.add(new TitledImage(entry.node, ((LWNode)entry.node).getImage().duplicate(cc)));
        } else if (entry.node.hasResource()) {
            LWNode titleLink = new LWNode(entry.node.getLabel()); // could use resource title instead
            titleLink.setResource(entry.node.getResource());
            titleLink.setAsTextNode(true);
            text.add(titleLink);
        }


        //-------------------------------------------------------
        // Now lay everything out
        //-------------------------------------------------------

        int x, y;

        // Add as children, providing seed relative locations
        // for layout, and establish z-order (images under text)
        // Nodes will be auto-styled with the master slide style
        // in addView.

        x = y = SlideMargin;

        for (TitledImage t : images) {
            if (t.title != null) {
                t.title.takeLocation(x++,y++);
                slide.applyStyle(t.title);
            }
            t.image.takeLocation(x++,y++);
            slide.applyStyle(t.image);
            added.add(t.image);
            if (t.title != null)
                added.add(t.title); // keep titles over images (add after)
        }

        // differences in font sizes mean text below title looks to left of title unless slighly indented
        final int textIndent = 2;
        
        x = SlideMargin + textIndent; 
        y = SlideMargin * 2 + (int) title.getHeight();
        for (LWComponent c : text) {
            c.takeLocation(x++,y++);
            slide.applyStyle(c);
        }

        added.addAll(text);
            
        if (DEBUG.PRESENT || DEBUG.STYLE) Log.debug("LAYING OUT CHILDREN in " + slide + "; parent=" + slide.getParent());

        slide.setSize(SlideWidth, SlideHeight);

        if (text.size() > 1)
            layoutText(slide, text);

        // TODO: change image layouts to create a label right-aligned node (and do same for drop)

        // TODO: on restore, traverse all children (cept thru groups) and set slide-style bits

        if (images.size() > 0)
            layoutImages(slide, images);

        slide.takeScale(SlideIconScale);

        slide.addChildren(added);

        return slide;
    }

    private static void layoutText(LWSlide slide, List<? extends LWComponent> text) {

        final LWSelection selection = new LWSelection(text);
        
        // Better to distribute text items in their total height + a border IF they'd all fit on the slide:
        // selection.setSize((int) bounds.getWidth(),
        //                   (int) bounds.getHeight() + 15 * selection.size());
        
        selection.setSize(SlideWidth - SlideMargin*2,
                          (int) (slide.getHeight() - SlideMargin*1.5 - text.get(0).getY()));
        selection.setTo(text);
        Actions.DistributeVertically.act(selection);
        Actions.AlignLeftEdges.act(selection);
    }

    private static void layoutImages(LWSlide slide, List<TitledImage> images)
    {
        final float imageRegionTop = SlideMargin * 2;
        final float imageRegionBottom = slide.getHeight();
        final float imageRegionHeight = imageRegionBottom - imageRegionTop;
        
        if (images.size() == 1) {

            // Special layout for a single image:
            
            final LWImage image = images.get(0).image;
            image.userSetSize(0,
                              slide.getHeight() - SlideMargin * 6, null); // aspect preserving
            image.setLocation((slide.getWidth() - image.getWidth()) / 2,
                              imageRegionTop + (imageRegionHeight - image.getHeight()) / 2);

            LWComponent label = images.get(0).title;
            if (label != null) {
                label.setLocation(image.getX() + (image.getWidth() - label.getWidth()) / 2,
                                  image.getY() + image.getHeight());
            }
            //image.getY() - label.getHeight());
                
        } else if (images.size() > 1) {

            // TitledImage.title should never be null if there is more than one image
            // to layout

            final float labelHeight = images.get(0).title.getHeight();
            final float verticalMargin = labelHeight / 2;
            final float maxImageHeight = (imageRegionHeight -
                                          (((images.size()-1)*verticalMargin) + images.size() * labelHeight))
                / images.size();

            for (TitledImage t : images)
                t.image.userSetSize(0, maxImageHeight, null); // aspect preserving

            float y = imageRegionTop / 2;
            for (TitledImage t : images) {
                float imageX = slide.getWidth() - t.image.getWidth() - SlideMargin;
                float titleX = slide.getWidth() - t.title.getWidth() - SlideMargin + 5;
                t.title.setLocation(titleX, y+3);
                y += t.title.getHeight();
                t.image.setLocation(imageX, y);
                y+= t.image.getHeight() + verticalMargin;
            }

        }
    }
    

    public static void synchronizeAll(LWSlide slide) {
        synchronizeResources(slide, Sync.ALL);
    }
    public static void synchronizeSlideToNode(LWSlide slide) {
        synchronizeResources(slide, Sync.TO_NODE);
    }
    public static void synchronizeNodeToSlide(LWSlide slide) {
        synchronizeResources(slide, Sync.TO_SLIDE);
    }


    protected static void synchronizeResources(final LWSlide slide, Sync type) {

        if (slide.getSourceNode() == null) {
            Log.warn("Can't synchronize a slide w/out a source node: " + slide);
            return;
        }

        if (slide.getEntry() != null && slide.getEntry().isMapView()) {
            Util.printStackTrace("cannot synchronize virtual slides");
            return;
        }

        if (!slide.canSync()) {
            Util.printStackTrace("Sync not permitted: " + slide + " type(" + type + ")");
            return;
        }

        if (slide.numChildren() == 0 && slide.getEntry() != null && (type == Sync.ALL || type == Sync.TO_SLIDE)) {
            // special case: completely rebuild this slide:
            buildPathwaySlide(slide, slide.getEntry());
            return;
        }

        

        final LWComponent node = slide.getSourceNode();

        if (DEBUG.Enabled) outf("NODE/SILDE SYNCHRONIZATION; type(%s) ---\n\t NODE: %s\n\tSLIDE: %s", type, node, slide);
        
        final Set<Resource> slideUnique = new LinkedHashSet();
        final Set<Resource> nodeUnique = new LinkedHashSet();
        
        final Map<Resource,LWComponent> slideSources = new HashMap();
        final Map<Resource,LWComponent> nodeSources = new HashMap();

        // First add all resources in any descendent of the node to nodeUnique (include
        // the node's resource itself), then iterate through all the resources found
        // anywhere inside the slide, removing duplicates from nodeUnique, and adding the
        // remainder to slideUnique.

        if (node.hasResource()) {
            if (nodeUnique.add(node.getResource()))
                nodeSources.put(node.getResource(), node);
        }
        for (LWComponent c : node.getAllDescendents()) {
            if (c.hasResource()) {
                if (nodeUnique.add(c.getResource()))
                    nodeSources.put(c.getResource(), c);
            }
        }

        if (DEBUG.Enabled) {
            for (Resource r : nodeUnique)
                outf("%40s: %s", "UNIQUE NODE RESOURCE", Util.tags(r));
        }
            
        final Set<Resource> nodeDupes = new HashSet();

        for (LWComponent c : slide.getAllDescendents()) {
            if (c.hasResource()) {
                final Resource r = c.getResource();
                if (nodeUnique.contains(r)) {
                    nodeDupes.add(r);
                    if (DEBUG.Enabled) outf("%40s: %s", "ALREADY ON NODE, IGNORE FOR SLIDE", Util.tags(r));
                } else {
                    if (slideUnique.add(r)) {
                        slideSources.put(r, c);
                        if (DEBUG.Enabled) outf("%40s: %s", "ADDED UNIQUE SLIDE", Util.tags(r));
                    }
                }
            }
        }

        nodeUnique.removeAll(nodeDupes);

        if (DEBUG.Enabled) {
            //this.outf("  NODE DUPES: " + nodeDupes + "\n");
            outf("SLIDE UNIQUE: " + slideUnique);
            outf(" NODE UNIQUE: " + nodeUnique);
        }


        // TODO: if a resource was added to BOTH the slide and the node
        // extra-sync (e.g., cut/paste or drag/drop), and then during sync 
        // we also probably want to connect these up via sync-source.
                
        if (type == Sync.ALL || type == Sync.TO_NODE) {
            for (Resource r : slideUnique) {
                // TODO: merge MapDropTarget & NodeModeTool node creation code into NodeTool, including resource handling
                LWNode newNode = createNode(slideSources.get(r), r);
                slideSources.get(r).setSyncSource(newNode);
                node.addChild(newNode);
            }
        }

        if (type == Sync.ALL || type == Sync.TO_SLIDE) {
            List<LWComponent> added = new ArrayList();
            for (Resource r : nodeUnique) {
                final LWComponent newNode;
                // TODO: merge this code into something common with buildPathwaySlide
                if (false && r.isImage()) {
                    newNode = new LWImage(r);
                } else {
                    newNode = createNode(nodeSources.get(r), r);
                    newNode.setSyncSource(nodeSources.get(r));
                    if (LWNode.isImageNode(newNode))
                        ((LWNode)newNode).getImage().setSyncSource(nodeSources.get(r));
                }
                added.add(newNode);
            }

            if (added.size() > 0) {

                LWSelection s = new LWSelection(added);
                
                float
                    x = slide.getWidth() / 4,
                    y = SlideMargin;
                
                // seed locations
                for (LWComponent c : added) {
                    slide.applyStyle(c);
                    c.setLocation(x, y++);
                }
                
                if (added.size() > 1) {
                    s.setSize(1, (int) slide.getHeight() - SlideMargin * 2);
                    Actions.DistributeVertically.act(s);
                }
                
                slide.addChildren(added);

                VUE.getSelection().setTo(added);
            }
        }
    }


    protected static void outf(String format, Object ... args) {
        Util.outf(Log, format, args);
    }
    


    
}


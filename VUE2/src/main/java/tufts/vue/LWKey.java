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

package tufts.vue;


// todo: a Key class, and some are marked as model-changing (default),
// and some not (e.g., repaints, user-action-completed)
// final flags: isChange & isSignal (one or other), isBoundsChange
// by default always safe with isChange=true, isSignal=false, isBoundsChange=true
// can provide isSignal=true & isBoundsChange=false for optimization.

/**
 * Some pre-defined property types.  Any string may be used as an
 * property or event identifier, but you must be sure to use the constant
 * object here for any of these events or they may not be
 * recognized .
 *
 * @version $Revision: 1.31 $ / $Date: 2007/08/30 21:09:29 $ / $Author: sfraize $
 */
public interface LWKey {

    String UserActionCompleted = "user.action.completed";
    
    LWComponent.Key FillColor = LWComponent.KEY_FillColor;
    LWComponent.Key TextColor = LWComponent.KEY_TextColor;
    LWComponent.Key PresentationColor = LWMap.KEY_PresentationColor;
    LWComponent.Key StrokeColor = LWComponent.KEY_StrokeColor;
    LWComponent.Key StrokeWidth = LWComponent.KEY_StrokeWidth;
    LWComponent.Key StrokeStyle = LWComponent.KEY_StrokeStyle;
    LWComponent.Key Font =  LWComponent.KEY_Font;
    LWComponent.Key FontSize =  LWComponent.KEY_FontSize;
    LWComponent.Key FontUnderline =  LWComponent.KEY_FontUnderline;
    LWComponent.Key FontName =  LWComponent.KEY_FontName;
    LWComponent.Key FontStyle =  LWComponent.KEY_FontStyle;
    LWComponent.Key Alignment =  LWComponent.KEY_Alignment;
    LWComponent.Key Shape = LWNode.KEY_Shape;

    LWComponent.Key Label = LWComponent.KEY_Label;
    LWComponent.Key Notes = LWComponent.KEY_Notes;
    LWComponent.Key Collapsed = LWComponent.KEY_Collapsed;
    
    // a handy hack: if we want a "key" type more specific than object, but
    // can also refer to a String (which is a final class), String implements
    // CharSequence, so we could use that as a narrower generic for both
    // our LWComonent.Key object, and Strings.
    
    String Location = "location";
    String Size = "size";
    String Frame = "frame"; // location & size
    
    String Scale = "scale";
    String Resource = "resource";
    String Hidden = "hidden";


    String Created = "new.component"; // any LWComponent's creation event
    String ChildrenAdded = "hier.childrenAdded";// the parent component's group add-notify
    String ChildrenRemoved = "hier.childrenRemoved";// the parent component's group remove-notify
    String HierarchyChanging = "hier.changing"; // pre-change event for any hierarchy change
    String HierarchyChanged = "hier.changed"; // post-change event for hierarchy changes during undo operations

    String Deleting = ":deleting"; // the component's just-before-delete notify
    //public String Deleted = "deleted"; // the component's after-delete notify

    String LinkAdded = "lwc.link.added"; // a link has been added to this component
    String LinkRemoved = "lwc.link.removed"; // a link has been removed from this component
    
    String Repaint = "repaint"; // general: visual change but no permanent data change
    String RepaintComponent = "repaint.component"; // IMMEDIATELY repaint (don't wait for AWT), but just the component
    String RepaintAsync = "repaint.async"; // a repaint from an auxillary thread: all visual listeners need immediate repaint
    String RepaintRegion = "repaint.region";

    /** link arrow state: 0=none, 1=start arrow, 2=end arrow, 3=both arrows */
    //public String LinkArrows = "link.arrows";
    LWComponent.Key LinkArrows = LWLink.KEY_LinkArrows;
    /** link curve state: 0=straight, 1=1 control point (Quadric), 2=2 control points (Cubic) */
    LWComponent.Key LinkShape = LWLink.KEY_LinkShape;
    //public String LinkCurves = "link.curves";

    /* the map filter has changed somehow */
    String MapFilter = "map.filter";
    
    /** the fast, non-editable Schema / data-import (google Multimap impl) based data set */
    String DataUpdate = "data.update";
    /** the editable VueMetadataElement / Ontology / OntType / RDF based data set */
    String MetaData = "meta.data";

    //-----------------------------------------------------------------------------
    // client-data keys:
    //-----------------------------------------------------------------------------
    
    Class<LWContainer> OLD_PARENT = LWContainer.class;
    
    
    
}

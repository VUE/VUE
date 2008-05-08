 /*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

import tufts.vue.gui.*;

import tufts.Util;

import java.util.*;
import java.lang.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Base class for a VUE tool, that may have sub-tools.
 *
 * Sub-classes AbstractAction for addActionListener, although
 * that usage is probably on it's way out when we get around
 * to cleaning up the VueTool code & it's supporting GUI classes.
 *
 * @version $Revision: 1.83 $ / $Date: 2008-05-08 04:48:50 $ / $Author: sfraize $
 */

public abstract class VueTool extends AbstractAction
    implements PickContext.Acceptor
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueTool.class);
    
    /** the tool's unique id **/        protected String mID = null;
    /** the tool name **/               protected String mToolName = null;
    /** the tool tip text, if any **/   protected String mToolTipText = null;
    
    
    /** the default icon to draw in the up or idle states **/   protected Icon mUpIcon;
    /** the icon to use for mouse down or press states **/      protected Icon mDownIcon;
    /** the icon to use for selected state **/                  protected Icon mSelectedIcon;
    /** the icon to use for disabled state **/                  protected Icon mDisabledIcon;
    /** the rollover icon **/                                   protected Icon mRolloverIcon;
    /** the menu item state (if any) **/                        protected Icon mMenuItemIcon;
    /** the menu item selected state **/                        protected Icon mMenuItemSelectedIcon;
    /** the icon to overlay if there are any sub items **/      protected Icon mOverlayUpIcon;
    /** the icon to overlay if there are any sub items **/      protected Icon mOverlayDownIcon;
    /** the raw icon used as a base for the generated icons */  protected Icon mRawIcon;

    /** Short-cut key to active this tool */
    protected char mShortcutKey = 0;
    
    /** Short-cut key-code to temporarily active this tool while this key is held down */
    protected int mActiveWhileDownKeyCode = 0;
    
    /** A cursor to use with this tool */
    protected java.awt.Cursor mCursor;
    
    protected HashMap mResourceMap = new HashMap();

    protected AbstractButton mLinkedButton;

    private boolean isTemporary;
    private JPanel mToolPanel;

    /** tool display order by keyname **/               protected Vector mSubToolIDs = new Vector();
    /** the sub tool map with toolname as key **/       protected Map<String,VueTool> mSubToolMap = new HashMap();    
    /** the parent tool -- if this is a subtool **/     protected VueTool mParentTool = null;
    /** the currently active subtool name **/           protected VueTool mSelectedSubTool = null;

    private static final Map<Class<? extends VueTool>, VueTool> InstanceMap = new HashMap();

    /** for storing all the tools by name, including sub-tools */
    private static final java.util.List<VueTool> mAllTools = new java.util.ArrayList();
	
    //private LWComponent mStyleCache;
    
    public VueTool() {
        super();
        VueTool old = InstanceMap.put(getClass(), this);
        if (DEBUG.TOOL) {
            if (old != null) {
                System.out.println("already instanced: " + getClass() + "; was=" + old);
                // need to permanently clear out this item for the multiple instance cases
                //tufts.Util.printStackTrace("already instanced: " + getClass() + "; was=" + old);
            } else
                System.out.println("instanced " + this);
        }
        //mStyleCache = createStyleCache();
        mAllTools.add(this);
    }

    /** @return list of all tool instances  */
    public static java.util.List<VueTool> getTools() {
        return mAllTools;
    }

    public boolean isActive() {
        return VueToolbarController.getActiveTool() == this;
    }

    public static VueTool getInstance(Class<? extends VueTool> clazz) {
        final VueTool tool = InstanceMap.get(clazz);
        if (tool == null)
            tufts.Util.printStackTrace("failed to find tool instance of type: " + clazz);
        return tool;
    }

	
    /**
     * getID
     * Gets the unique tool id.
     * @return String the tool's id.
     **/
    public String getID() {
        return mID;
    }
	
    /**
     * setID
     * Sets the tool's unique id.
     * @param pID the id String
     **/
    public void setID(String pID ) {
        mID = pID;
        //System.out.println("Initializing tool " + this);
        ResourceBundle rb = VueResources.getBundle();
        Enumeration e = rb.getKeys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.startsWith(pID)) {
                String localKey = key.substring(mID.length()+1, key.length());
                //System.out.println("\tkey: " + localKey + "=" + rb.getString(key));
                mResourceMap.put(localKey, rb.getString(key));// todo: use rb.getObject?
            }
        }
        //tool.setToolName( VueResources.getString(( pName+".name") ) );
    }

    public String getAttribute(String key)
    {
        return (String) mResourceMap.get(key);
    }

    /** Initialize tool from resource properties.  Must be called after setID. */
    void initFromResources()
    {
        setToolName(getAttribute("name"));
        setToolTipText(getAttribute("tooltip"));

        Icon rawIcon =  VueResources.getImageIcon(mID+".raw");
        if (rawIcon != null) {
            setGeneratedIcons(rawIcon);
        } else {
            Icon i;
            if ((i = VueResources.getImageIcon(mID+".up")) != null)               setIcon(i);
            if ((i = VueResources.getImageIcon(mID+".down")) != null)             setDownIcon(i);
            if ((i = VueResources.getImageIcon(mID+".selected")) != null)         setSelectedIcon(i);
            if ((i = VueResources.getImageIcon(mID+".disabled")) != null)         setDisabledIcon(i);
            if ((i = VueResources.getImageIcon(mID+".rollover")) != null)         setRolloverIcon(i);
            if ((i = VueResources.getImageIcon(mID+".menu")) != null)             setMenuItemIcon(i);
            if ((i = VueResources.getImageIcon(mID+".menuselected")) != null)     setMenuItemSelectedIcon(i);
        }
        
        setShortcutKey(VueResources.getChar(mID+".shortcutKey"));
        //setActiveWhileDownKey(VueResources.getChar(mID+".activeWhileDownKey")); // need to parse key code's...

        int cursorID = VueResources.getInt(mID+".cursorID", -1);
        if (cursorID >= 0) {
            //System.out.println(tool + " found cursor ID: " + cursorID);
            setCursorByID(cursorID);
        } else {
            Cursor cursor = VueResources.getCursor(mID+".cursor");
            if (cursor != null)
                setCursor(cursor);
            /*
              ImageIcon icon = VueResources.getImageIcon( mID+".cursor");
              if (icon != null) {
              //System.out.println(tool + " found cursor icon: " + icon);
              //System.out.println(tool + " cursor icon image: " + icon.getImage());
              Toolkit toolkit = Toolkit.getDefaultToolkit();
              //System.out.println("Creating cursor for " + icon);
              tool.setCursor(toolkit.createCustomCursor(icon.getImage(), new Point(0,0), mID+":"+icon.toString()));
              }
            */
        }
    }

    /** @return true if the tool requests a button the toolbar */
    public boolean hasToolbarButton() {
        return mUpIcon != null;
    }
    
    
    /**
     * getSelectionID
     * This method returns the full id of the selected tool
     * For example, a selected parent and subtool would return
     * partenID.subToolID.  This is obttained by getting
     * the subtools id, ir there is one selected, or the partent if not.
     *
     * @return String t-the selection ID string
     **/
    public String getSelectionID() {
        String id = getID();
        if( getSelectedSubTool()  != null ) {
            id = getSelectedSubTool().getID();
        }
        return id;
    }
    /**
     * getToolName
     * Gets a display name for the tool
     * @return String the display name of the tool
     **/
    public String getToolName() {
        return mToolName;
    }
	
    /**
     * setToolName
     * Sets the tool's display name
     * @param pName - the tool's name
     **/
    public void setToolName( String pName ) {
        mToolName = pName;
    }
	
    /**
     * getToolTipText
     * Gets the tool tip text string for the tool
     * @return String the tool tip text
     **/
    public String getToolTipText() {
        if (mToolTipText == null)
            return null;
        else if (getShortcutKey() == 0)
            return mToolTipText;
        else 
            return mToolTipText + " (" + getShortcutKey() + ")";
    }

    /**
     * setToolTipText
     * Sets the tool tip text string
     *  @param pText the tip text
     **/
    public void setToolTipText( String pText ) {
        mToolTipText = pText;
    }

    /**
     * setCursorByID
     * Gets the cursor for the tool based on the cursor ID
     * @param id a cursor constant from java.awt.Cursor in the range 0-13 (as of java 1.4.1)
     **/
    public void setCursorByID(int id) {
        if (id < 0 || id > 13)
            throw new IllegalArgumentException("cursor id outside range 0-13: " + id);
        setCursor(Cursor.getPredefinedCursor(id));
    }

    /**
     * setCursor
     * Gets the cursor for the tool based on the cursor ID
     * @param pCursor a java.awt.Cursor object
     **/
    public void setCursor(Cursor pCursor) {
        mCursor = pCursor;
    }
    
    /**
     * getCursor
     * Gets the cursor for the tool
     * @return an instance of java.awt.Cursor
     **/
    public Cursor getCursor()
    {
        Cursor cursor = null;
        if (hasSubTools()) 
            cursor = getSelectedSubTool().getCursor();
        if (cursor == null)
            cursor = mCursor;
        return cursor;
    }

    public void setShortcutKey(char pChar) {
        mShortcutKey = pChar;
    }
    public char getShortcutKey() {
        return mShortcutKey;
    }

    public char getBackwardCompatShortcutKey() {
        return 0;
    }
    

    public void setActiveWhileDownKeyCode(int keyCode) {
        mActiveWhileDownKeyCode = keyCode;
    }
    public int getActiveWhileDownKeyCode() {
        return mActiveWhileDownKeyCode;
    }
    

    /** if this returns non-null, only objects of the given type will be selected
      * by the dragged selector */
    public Object getSelectionType() { return null; }

    /**
     * override this for subclasses to provide limits on what can be selected
     * (interface LWSelection.Acceptor)
     */
    public boolean accept(PickContext pc, LWComponent c)
    {
        if (pc.isRegionPick()) {
            if (getSelectionType() == null)
                return true;
            else
                return getSelectionType() == c.getTypeToken();
        } else {
            // always accept point-picks
            return true;
        }
        
//         if (getSelectionType() != null)
//             return getSelectionType().isInstance(c);
//         else
//             return true;
    }

    public void setLinkedButton(AbstractButton b) {
        mLinkedButton = b;
    }

    /** @return true of this tool supports any edit actions */
    public boolean supportsEditActions() { return true; }
    
    /** supports the click-selection of objects */
    public boolean supportsSelection() { return true; }

    /** does tool make use of a dragged box for selecting objects or regions of the map? */
    public boolean supportsDraggedSelector(MapMouseEvent e) { return true; }

    /** @return true by deafult if tool supports drag operations -- override to change */
    public boolean supportsDrag(MapMouseEvent e) { return true; }
    
    
    /** does tool make use of the resize controls? If false, They will still be drawn, but
     * will not respond to mouse drags.*/
    public boolean supportsResizeControls() { return supportsSelection(); }
    
    /** does tool make use of right click -- meaning the
     * viewer shouldn't pop a context menu on right-clicks */
    public boolean usesRightClick() { return false; }

    /** does the tool draw it's own decorations, reqiring a repaint when it's selected/deselected? */
    public boolean hasDecorations() { return false; }

    public final boolean supportsXORSelectorDrawing() {
        return false;
    }
    
    /** @return true -- sub-impl's return false if this tool is currently preventing changes to any other active tool */
    public boolean permitsToolChange() {
        return true;
    }
    

    /** what to do, if anything, when the tool is selected */
    public void handleToolSelection(boolean selected, VueTool otherTool) {
        if (DEBUG.TOOL) Log.debug("handleToolSelection: " + this + ": " + selected + "; from=" + otherTool);
    }

    public DrawContext getDrawContext(DrawContext dc) {
        return dc;
    }

    public void handlePreDraw(DrawContext dc, MapViewer viewer) {}
    public void handlePostDraw(DrawContext dc, MapViewer viewer) {}
    
    /**
     * called upon entering/exiting full screen
     * @param entering -- true if entering full screen, false if exiting
     * @param nativeMode -- true if entering or exiting native (non-working) full screen mode
     */
    public void handleFullScreen(boolean entering, boolean nativeMode) {}


    /** mark this tool as temporarily activated */
    public void setTemporary(boolean t) {
        isTemporary = t;
    }
    
    public boolean isTemporary() {
        return isTemporary;
    }

//     // possible: default true, but zoom / pres tool / hand tool return false.
//     // Tools that aren't editors should not allow temporary
//     // activation to other tools that are editors
//     // e.g., zoom tool can use ctrl/alt for zoom modifiers w/out activating link tool on accidental drag
//     public boolean isEditor() {
//         return true;
//     }
    
    
    public void drawSelector(DrawContext dc, java.awt.Rectangle r)
    {
        //g.fill(r);//if mac?
        dc.g.draw(r);
    }

    public boolean handleKeyPressed(java.awt.event.KeyEvent e) { return false; }
    public boolean handleKeyReleased(java.awt.event.KeyEvent e) { return false; }
    
    /** @return false by default: returning true will have the effect of disabling the tool-tip style
     * node rollovers on the map.
     */
    public boolean handleMouseMoved(MapMouseEvent e) {
        if (DEBUG.MOUSE && DEBUG.META) System.out.println(this + " handleMouseMoved " + e);
        return false;
    }
    public boolean handleMousePressed(MapMouseEvent e) {
        if (DEBUG.TOOL) System.out.println(this + " handleMousePressed " + e);
        return false;
    }
    public boolean handleComponentPressed(MapMouseEvent e) {
        return false;
    }
    public boolean handleMouseDragged(MapMouseEvent e) {
        //System.out.println(this + " handleMouseDragged " + e);
        return false;
    }
    public boolean handleMouseReleased(MapMouseEvent e) {
        if (DEBUG.TOOL) System.out.println(this + " handleMouseReleased " + e);
        return false;
    }
    public boolean handleMouseClicked(MapMouseEvent e) {
        if (DEBUG.TOOL) System.out.println(this + " handleMouseClicked " + e);
        return false;
    }

    public void handleDragAbort() {}
    
    /** @return false by default: impl's can return true if they don't
     * want the viewer to change the display on focal load (e.g., do a zoom fit)
     */
    public boolean handleFocalSwitch(MapViewer viewer, LWComponent oldFocal, LWComponent newFocal) {
        return false;
    }

    public boolean handleSelectorRelease(MapMouseEvent e) {
        if (DEBUG.TOOL) System.out.println(this + " handleSelectorRelease " + e);
        return false;
    }

    public PickContext initPick(PickContext pc, float x, float y) {
        return pc;
    }
    public PickContext initPick(PickContext pc, java.awt.geom.Rectangle2D.Float rect) {
        return pc;
    }
    

    /*
    private static class SlideProxyMap extends LWMap {
        private LWMap srcMap;
        SlideProxyMap(LWMap srcMap) {
            super("SlideViewer");
            this.srcMap = srcMap;
        }

        public LWPathwayList getPathwayList() {
            return srcMap.getPathwayList();
        }
        
    }

    private static MapViewer SlideViewer = null;
    private static LWMap SlideMap = null;
    private static LWComponent CurSlide = null;
    */

    public void handleSelectionChange(LWSelection s) {

        /*
        
        if (s.size() == 1
            && !(s.getSource() instanceof tufts.vue.ui.SlideViewer)
            && !(s.first().getParent() instanceof LWSlide)
            ) {
            // don't display in slide viewer if is a child of an existing
            // slide: either we actually just selected in the slide viewer
            // itself, or we selected a child of a LWSlide, which for now
            // is just going to show that slide.
            tufts.vue.ui.SlideViewer.getInstance().loadFocal(s.first());
            return;
        }
        */
        
        /*
        if (s.size() == 1 && VUE.isActiveViewerOnLeft())
            tufts.vue.ui.SlideViewer.setFocused(s.first());

        if (s.size() == 1 && s.first().getSlide() != null
            && VUE.multipleMapsVisible()
            && VUE.isActiveViewerOnLeft())
        {
        */

            /*
              // Experimental creation of a slide in the right viewer
              
            if (SlideViewer == null) {
                //SlideMap = new LWMap("SlideViewer");
                SlideMap = new SlideProxyMap(s.first().getMap());
                SlideViewer = new MapViewer(SlideMap);
                VUE.getRightTabbedPane().addViewer(SlideViewer);
            } else {
                SlideMap.removeChild(CurSlide);
            }
                
            CurSlide = s.first().getSlide();
            SlideMap.addChild(CurSlide);

            ZoomTool.setZoomFitRegion(SlideViewer, CurSlide.getBounds(), 16, false);
            */

            /* OLD
            if (rightViewer.getMap() == VUE.getActiveMap()) {
                tufts.Util.printStackTrace("CREATING NEW SLIDE VIEWER");
                slideMap = new LWMap("SlideView");
                slideMap.addChild(slide);
                slideViewer = new MapViewer(slideMap);
                VUE.getRightTabbedPane().addViewer(slideViewer);
            } else if (rightViewer.getMap().getLabel().equals("SlideView")) {
                tufts.Util.printStackTrace("USING EXISTING SLIDE VIEWER");
                slideViewer = rightViewer;
                slideMap = slideViewer.getMap();
                slideMap.removeChildren(slideMap.getChildIterator());
                slideMap.addChild(slide);
            }
            if (slideMap != null)
                ZoomTool.setZoomFitRegion(slideViewer, slide.getBounds(), 16);
            */
        //}


            //-----------------------------------------------------------------------------
            // The right viewer tracks & zooms to whatever is selected
            // in the left viewer if it's showing the same map as the left viewer.
            // TEST ONLY FOR NOW.
            //-----------------------------------------------------------------------------
            
        if (false &&
            !s.isEmpty() &&
            VUE.multipleMapsVisible() &&
            VUE.isActiveViewerOnLeft() &&
            VUE.getRightTabbedPane().getSelectedViewer().getMap() == VUE.getActiveMap()
            )
            {
                MapViewer viewer = VUE.getRightTabbedPane().getSelectedViewer();
                ZoomTool.setZoomFitRegion(viewer, s.getBounds(), 16, false);
            }
    }

//     public LWComponent pickNodeAt(PickContext pc, float mapX, float mapY) {
//         return LWTraversal.PointPick.pick(pc, mapX, mapY);
//     }
//     public LWComponent pickNodeAt(PickContext pc) {
//         return LWTraversal.PointPick.pick(pc);
//     }

	
    /**
     * setParentTool
     * Sets the parent tool if this is a subtool
     * @param pTool - the VueTool of the parent
     **/
    public void setParentTool( VueTool pTool) {
        mParentTool = pTool;
    }
	
    /**
     * getParentTool
     * Gets the parent tool if the tool is a subtool
     * @return VueTool the parent tool
     **/
    public VueTool getParentTool() {
        return mParentTool;
    }
	
    /**
     * setSelectedSubTool
     *( This sets teh active subtool.
     * @param pTool - the selected subtool
     **/
    public void setSelectedSubTool( VueTool pTool) {
        if (DEBUG.TOOL) out("setSelectedSubTool: " + pTool);
        if (DEBUG.TOOL && DEBUG.META) tufts.Util.printStackTrace("setSelectedSubTool: " + pTool);
        mSelectedSubTool = pTool;
        
        if (VUE.getActiveViewer() !=null)
        	VUE.getActiveViewer().setCursor(mSelectedSubTool.getCursor());
    }
	
    /**
     * getCurrentSubTool
     * Gets the active selected subtool
     * @return VueTool the active subtool
     **/
    public VueTool getSelectedSubTool() {
        return mSelectedSubTool;
    }
	
    /**
     * hasSubTools
     * Determines if this tool has sub tools.
     * @return boolean - true if has subtools; false if no sub tools
     **/
    public boolean hasSubTools() {
        return !( mSubToolMap.isEmpty());
    }
	
    /**
     * getSubToolIIDs
     * Returns an ordered Vector of subtool ids for this tool
     * @return Vector - the display order set of sub tool ids
     **/
    public Vector getSubToolIDs() {
        return mSubToolIDs;
    }
	
    /**
     * getSubTool
     * Gets the subtool with the given ID.
     * @param pID the unique string ID of the subtool
     * @return VueTool the subtool with the passed ID
     **/
    public VueTool getSubTool( String pID) {
		
        Object tool = null;
        tool = mSubToolMap.get( pID);
        return (VueTool) tool;
    }
	
    /**
     * addSubTool
     * Adds the passed VueTool as a subtool of this tool
     * @param VueTool - the new subtool
     **/
    public void addSubTool(  VueTool pTool) {
        mSubToolMap.put( pTool.getID(), pTool);
        mSubToolIDs.add( pTool.getID() );
    }
	
    /**
     * removeSubTool
     * This removes teh current tool from the set of subtools
     * @param pID - the subtool 's IDto remove
     **/
    public void removeSubTool( String pID) {
        mSubToolMap.remove( pID);
        mSubToolIDs.remove( pID);
    }
	
	
    /////////////////////////////
    //
    // The follow are a slew of icon properties used for tool GUI
    //
    ////////////////////////////////////////
	
    public void setIcon( Icon pIcon) {
        mUpIcon = pIcon;
    }
	
    public Icon getIcon() {
        return mUpIcon;
    }
	
    public void setDownIcon( Icon pIcon) {
        mDownIcon = pIcon;
    }
	
    public Icon getDownIcon() {
        return mDownIcon;
    }
	
    public void setSelectedIcon( Icon pIcon) {
        mSelectedIcon = pIcon;
    }
	
    public Icon getSelectedIcon() {
        return mSelectedIcon;
    }
	
    public void setDisabledIcon( Icon pIcon) {
        mDisabledIcon = pIcon;
    }
	
    public Icon getDisabledIcon( ) {
        return mDisabledIcon;
    }
	
    public void setMenuItemSelectedIcon( Icon pIcon) {
        mMenuItemSelectedIcon = pIcon;
    }
	
    public Icon getMenuItemSelectedIcon() {
        return mMenuItemSelectedIcon;
    }
	
    public void setMenuItemIcon( Icon pIcon) {
        mMenuItemIcon = pIcon;
    }
	
    public Icon getMenuItemIcon() {
        return mMenuItemIcon;
    }
	
    public void setRolloverIcon( Icon pIcon) {
        mRolloverIcon = pIcon;
    }
	
    public Icon getRolloverIcon() {
        return mRolloverIcon;
    }
	
    public void setOverlayUpIcon( Icon pIcon) {
        mOverlayUpIcon = pIcon;
    }
	
    public Icon getOverlayUpIcon() {
        return mOverlayUpIcon;
    }
	
    public void setOverlayDownIcon( Icon pIcon) {
        mOverlayDownIcon = pIcon;
    }
	
    public Icon getOverlayDownIcon() {
        return mOverlayDownIcon;
    }

    public void setGeneratedIcons(Icon pIcon) {
        //System.out.println(this + " generating icons from " + pIcon);
        mRawIcon = pIcon;
        setIcon(new ToolIcon(mRawIcon, ToolIcon.UP));
        setDownIcon(new ToolIcon(mRawIcon, ToolIcon.PRESSED));
        setSelectedIcon(new ToolIcon(mRawIcon, ToolIcon.SELECTED));
        setDisabledIcon(new ToolIcon(mRawIcon, ToolIcon.DISABLED));
        setRolloverIcon(new ToolIcon(mRawIcon, ToolIcon.ROLLOVER));
        setMenuItemIcon(new ToolIcon(mRawIcon, ToolIcon.MENU));
        setMenuItemSelectedIcon(new ToolIcon(mRawIcon, ToolIcon.MENU_SELECTED));
    }

    public void out(String s) {
        //System.out.println(this + ": " + s);
        //Log.debug(getClass().getSimpleName() + ": " + s);
        Log.debug(this + ": " + s);
    }

    protected static void outln(String s) {
        System.out.println("VueTool: " + s);
    }
    static class ToolIcon extends tufts.vue.gui.VueButtonIcon
    {
        static final int Width = 38;
        static final int Height = 26;
        
        protected ToolIcon(Icon rawIcon, int type) {
            super(rawIcon, type, Width, Height);
            super.isRadioButton = true;
        }
    }
	
	
    public Icon getRawIcon() {
        return mRawIcon;
    }

    public String toString()
    {
        final String name =
            tufts.Util.TERM_PURPLE
            + getClass().getSimpleName() 
            + "@" + Integer.toHexString(System.identityHashCode(this))
            + tufts.Util.TERM_CLEAR
            ;
        
        
        String params = getID();

        if (getSelectedSubTool() != null)
            params += " sub=" + getSelectedSubTool();
        if (getSelectionType() != null) {
            if (params != null) params += "; ";
            params += "type=" + getSelectionType();
        }
        if (isTemporary()) {
            if (params != null) params += "; ";
            params += "TEMPORARY";
        }
        
        if (params != null)
            return name + "[" + params + "]";
        else
            return name;

    }

    /**
     * actionPerformed
     * Action interface
     * This method is called when the tool is activated by a user action
     * It notifies the parent tool, if any, and calls the abstract hook
     * handleSelection
     * @param pEvent the action event
     **/
    public void actionPerformed(ActionEvent pEvent) {
		
        if (DEBUG.TOOL) System.out.println("\n" + this + " " + pEvent);
		
        VueTool parent = this.getParentTool();
		
        if (parent != null)
            parent.setSelectedSubTool(this);

        if (parent != null) {
            //if (DEBUG.Enabled) Util.printStackTrace("actionPerformed tool switch to " + parent + " from " + VueToolbarController.getController().getActiveTool());
            parent.handleToolSelection(true, VueToolbarController.getController().getActiveTool());
        }
        if (this.hasSubTools())
            VueToolbarController.getController().handleToolSelection(this.getSelectedSubTool());
        else
            VueToolbarController.getController().handleToolSelection(this);
    }
	
    
    public JPanel getContextualPanel() {
        if (mToolPanel == null)
            mToolPanel = createToolPanel();
        return mToolPanel;
    }
    
    public JPanel createToolPanel() {
        JPanel p = new JPanel();
        p.add(new JLabel(getToolName()));
        return p;
    }
}

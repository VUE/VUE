 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import tufts.vue.gui.*;

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
 */

public abstract class VueTool extends AbstractAction
{
    /** the tool's unique id **/
    protected String mID = null;
	
    /** the tool name **/
    protected String mToolName = null;
	
    /** the tool tip text, if any **/
    protected String mToolTipText = null;
	
    /** the currently active subtool name **/
    protected VueTool mSelectedSubTool = null;
	
    /** the sub tool map with toolname as key **/
    protected Map mSubToolMap = new HashMap();    
    
    /** tool display order by keyname **/
    protected Vector mSubToolIDs = new Vector();
    
    /** the parent tool -- if this is a subtool **/
    protected VueTool mParentTool = null;
    
    /** the default icon to draw in the up or idle states **/
    protected Icon mUpIcon = null;
    
    /** the icon to use for mouse down or press states **/
    protected Icon 	mDownIcon = null;
    
    /** the icon to use for selected state **/
    protected Icon  mSelectedIcon = null;
    
    /** the icon to use for disabled state **/
    protected Icon mDisabledIcon = null;
    
    /** the rollover icon **/
    protected Icon mRolloverIcon = null;
    
    /** the menu item state (if any) **/
    protected Icon mMenuItemIcon = null;
    
    /** the menu item selected state **/
    protected Icon mMenuItemSelectedIcon = null;
    
    /** the icon to overlay if there are any sub items **/
    protected Icon mOverlayUpIcon = null;
    
    /** the icon to overlay if there are any sub items **/
    protected Icon mOverlayDownIcon = null;

    /** the raw icon used as a base for the generated icons */
    protected Icon mRawIcon = null;

    protected char mShortcutKey = 0;

    protected java.awt.Cursor mCursor = null;
    
    /** thedeafult  property bean state of any tool subproperties **/
    protected Object mBeanState = null;
    
    /** is the tool currently enabled **/
    protected boolean mEnabled = true;

    protected HashMap mResourceMap = new HashMap();

    protected AbstractButton mLinkedButton;

    private JPanel mToolPanel;
    
    public VueTool() {
        super();
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
        return mToolTipText;
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

    public void setShortcutKey(char pChar)
    {
        mShortcutKey = pChar;
    }
    public char getShortcutKey()
    {
        return mShortcutKey;
    }

    /** if this returns non-null, only objects of the given type will be selected
     * by the dragged selector */
    public Class getSelectionType() { return null; }

    public void setLinkedButton(AbstractButton b) {
        mLinkedButton = b;
    }

    /** supports the click-selection of objects */
    public boolean supportsSelection() { return true; }

    /** does tool make use of a dragged box for selecting objects or regions of the map? */
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return true; }
    
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

    /** what to do, if anything, when the tool is selected */
    public void handleToolSelection() {}

    public void handleDraw(DrawContext dc, LWMap map) {
        dc.g.setColor(map.getFillColor());
        dc.g.fill(dc.g.getClipBounds());
        map.draw(dc);
    }
    
    public void handleFullScreen(boolean fullScreen) {}
    
    public void drawSelector(java.awt.Graphics2D g, java.awt.Rectangle r)
    {
        //g.fill(r);//if mac?
        g.draw(r);
    }

    public boolean handleKeyPressed(java.awt.event.KeyEvent e) { return false; }
    
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
    public void handleMouseClicked(MapMouseEvent e) {
        if (DEBUG.TOOL) System.out.println(this + " handleMouseClicked " + e);
    }
    public void handleDragAbort() {}

    public boolean handleSelectorRelease(MapMouseEvent e) {
        if (DEBUG.TOOL) System.out.println(this + " handleSelectorRelease " + e);
        return false;
    }

    //public void handleSelectionChange(LWSelection s) {}
    // temporary: give this to everyone
    public void handleSelectionChange(LWSelection s) {
        if (//s.size() == 1 &&
            VUE.multipleMapsVisible() &&
            VUE.getLeftTabbedPane().getSelectedViewer() == VUE.getActiveViewer() &&
            VUE.getRightTabbedPane().getSelectedViewer().getMap() == VUE.getActiveMap()
            )
        {
            MapViewer viewer = VUE.getRightTabbedPane().getSelectedViewer();
            ZoomTool.setZoomFitRegion(viewer, s.getBounds(), 16);
        }
    }

    public LWComponent findComponentAt(LWMap map, float mapX, float mapY) {
        return map.findChildAt(mapX, mapY);
    }

	
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
        mSelectedSubTool = pTool;
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
        System.out.println(this + ": " + s);
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
        String s = "VueTool[" + getID();
        if (getSelectedSubTool() != null)
            s += " subSelected=" + getSelectedSubTool();
        s += "]";
        return s;
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

        if (parent != null)
            parent.handleToolSelection();

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

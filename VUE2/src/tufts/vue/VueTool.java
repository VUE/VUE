package tufts.vue;

import java.util.*;
import java.lang.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

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
    public void setID( String pID ) {
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

    // rename supportsClickSelection?
    public boolean supportsSelection() { return false; }
    //public boolean supportsClick() { return false; }
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return true; }
    /** does tool make use of right click -- meaning the
     * viewer shouldn't pop a context menu on right-clicks */
    public boolean usesRightClick() { return false; }

    public final boolean supportsXORSelectorDrawing()
    // temporarily disabled feature
    {
        return false;
    }

    //public abstract void handleSelection( );
    public void handleSelection() {}

    public void handlePaint(DrawContext dc) {}
    public void drawSelector(java.awt.Graphics2D g, java.awt.Rectangle r)
    {
        //g.fill(r);//if mac?
        g.draw(r);
    }

    public boolean handleKeyPressed(java.awt.event.KeyEvent e) { return false; }
    
    public boolean handleMousePressed(MapMouseEvent e) {
        if (debug) System.out.println(this + " handleMousePressed " + e);
        return false;
    }
    public boolean handleMouseDragged(MapMouseEvent e) {
        //System.out.println(this + " handleMouseDragged " + e);
        return false;
    }
    public boolean handleMouseReleased(MapMouseEvent e) {
        if (debug) System.out.println(this + " handleMouseReleased " + e);
        return false;
    }
    public void handleMouseClicked(MapMouseEvent e) {
        if (debug) System.out.println(this + " handleMouseClicked " + e);
    }
    public void handleDragAbort() {}

    public boolean handleSelectorRelease(MapMouseEvent e) {
        if (debug) System.out.println(this + " handleSelectorRelease " + e);
        return false;
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

    static class ToolIcon extends VueButtonIcon
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
    public void actionPerformed( ActionEvent pEvent) {
		
        //System.out.println("!!! VueTool.performAction "+getID() );
		
        VueTool parent = this.getParentTool();
		
        if( parent != null) {
            parent.setSelectedSubTool( this );
        }
        this.handleSelection();
		
        if( parent != null) {
            parent.handleSelection();
        }
        VueToolbarController.getController().handleToolSelection( this);
    }
	
    public abstract JPanel getContextualPanel();

    private final boolean debug = false;
    
}

package tufts.vue;

import java.util.*;
import java.lang.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public abstract class VueTool extends AbstractAction
{
	
//////////////////////
// Properties and Fields
//////////////////////
	

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
    

//////////////////
// Constructors
///////////////////
    
	public VueTool() {
            super();
	}
	
	
	
	
/////////////////////
// Methods
////////////////////

	
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
	 * ?@param pName - the tool's name
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
            System.out.println(this + " generating icons from " + pIcon);
            mRawIcon = pIcon;
            setIcon(new ToolIcon(mRawIcon, ToolIcon.UP));
            setDownIcon(new ToolIcon(mRawIcon, ToolIcon.DOWN));
            setSelectedIcon(new ToolIcon(mRawIcon, ToolIcon.SELECTED));
            setDisabledIcon(new ToolIcon(mRawIcon, ToolIcon.DISABLED));
            setRolloverIcon(new ToolIcon(mRawIcon, ToolIcon.ROLLOVER));
            setMenuItemIcon(new ToolIcon(mRawIcon, ToolIcon.MENU));
            setMenuItemSelectedIcon(new ToolIcon(mRawIcon, ToolIcon.MENU_SELECTED));
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

    private static final Color sButtonColor = new Color(222,222,222);
    private static final Color sOverColor = Color.gray;
    //private static final Color sDownColor = new Color(211,211,211);
    private static final Color sDownColor = sOverColor;
    //private static final Color ToolbarColor = VueResources.getColor("toolbar.background");
    private static final EtchedBorder sEtchedBorder = new EtchedBorder();
    
    class ToolIcon implements Icon
    {
        static final int UP = 0;        // TOOLBAR: unselected/default
        static final int DOWN = 1;      // TOOLBAR: only while being held down by a mouse press
        static final int SELECTED = 2;  // TOOLBAR: selected (after mouse click)
        static final int DISABLED = 3;  // DISABLED
        static final int ROLLOVER = 4;  // TOOLBAR: rollover
        static final int MENU = 5;      // SUB-MENU: default (palette menu)
        static final int MENU_SELECTED = 6; // SUB-MENU: rollover (palette menu)
            
        static final int width = 38;
        static final int height = 26;
        //static final int arc = 15; // arc of rounded toolbar button border

        private Insets insets = new Insets(1,1,0,0);
        private int mType = UP;
        private Color mColor = sButtonColor;
        //private Color mColor = new Color(230,230,230);
        //private Color mColor = new Color(200,200,200);
        //private Color mColor = Color.gray;

        private Icon mRawIcon;
        private boolean mIsDown;
        //private boolean mPaintGradient = false;

        // OffsetWhenDown: nudge the icon when in the down state.
        // Set to true of "up" state appears as a button -- can
        // turn on otherwise but will need to adjust whole button so
        // icon stays centered.
        private final static boolean OffsetWhenDown = false;
        private final static boolean debug = false;

        protected ToolIcon(Icon rawIcon, int t)
        {
            mRawIcon = rawIcon;
            mType = t;
            mIsDown = (t == DOWN || t == SELECTED || t == MENU_SELECTED);
            //mPaintGradient = mIsDown || t == ROLLOVER;
            //if (mPaintGradient)
            if (mIsDown)
                mColor = Color.lightGray;
            //mColor = Color.gray;
                //mColor = ToolbarColor;
            if (debug) {
                if (t == MENU) mColor = Color.pink;
                if (t == MENU_SELECTED) mColor = Color.magenta;
                if (t == ROLLOVER) mColor = Color.green;
            }
        }
            
        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }

        /**
         * paint the entire button as an icon plus it's visible icon graphic
         */
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (debug) System.out.println("painting " + mRawIcon + " type = " + mType);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (mType >= MENU) {
                // the drop-down menus have GC bugs on the PC such
                // that we need to be sure to paint something in
                // the entire region, or we appear to get another
                // version of the icon painted *under* us.
                g2.setColor(c.getBackground());
                g2.fillRect(0,0, width,height);
            }

            if (debug) {
                g2.setColor(Color.red);
                g2.fillRect(0,0, 99,99);
            }
            x += insets.top;
            y += insets.left;
            //if (VueUtil.isMacPlatform()) { x += 2; y += 2; }// try now that attend to x/y above
            g2.translate(x,y);

            int w = width - (insets.left + insets.right);
            int h = height - (insets.top + insets.bottom);
                
            float gw = width;
            //GradientPaint gradient = new GradientPaint(gw/2,0,Color.white,gw/2,h/2,mColor,true);
            //GradientPaint gradient = new GradientPaint(gw/6,0,Color.white,gw/2,h/2,mColor,true);
            GradientPaint gradient = new GradientPaint(gw/6,0,Color.white,gw*.33f,h/2,mColor,true);
            // Set gradient for the whole button.

            if (mIsDown) {
                // Draw the 3d button border -- raised/lowered depending on down state                
                g2.setColor(c.getBackground());
                g2.draw3DRect(0,0, w-1,h-1, !mIsDown);
            } else if (mType == ROLLOVER) {
                // Draw an etched rollover border:
                sEtchedBorder.paintBorder(c, g, 0, 0, w, h);
                // this make it look like button-pressed:
                //g2.draw3DRect(0,0, w-1,h-1, false);
            }

            // now fill the icon, but don't fill if we're just holding the mouse down
            if (mIsDown && mType != DOWN) {
                g2.setPaint(gradient);
                g2.fillRect(1,1, w-2,h-2);
            }
            else
                g2.setColor(mColor);
            //g2.fillRect(1,1, w-2,h-2);
            // skipping the fill here creates the flush-look
                
            // if we're down, nudge icon
            if (OffsetWhenDown && mIsDown)
                g2.translate(1,1);

            // now draw the actual graphic in the center
            int ix = (w - mRawIcon.getIconWidth()) / 2;
            int iy = (h - mRawIcon.getIconHeight()) / 2;
            if (mType == DISABLED)
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            if (DEBUG.BOXES) {
                g2.setColor(Color.red);
                g2.fillRect(ix, iy, mRawIcon.getIconWidth(), mRawIcon.getIconHeight());
            }
            drawGraphic(c, g2, ix, iy);
        }

        // can be overriden to do anything really fancy
        void drawGraphic(Component c, Graphics2D g, int x, int y)
        {
            mRawIcon.paintIcon(c, g, x, y);
        }

        public String toString()
        {
            return "ToolIcon[" + mType + " " + mRawIcon + "]";
        }
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

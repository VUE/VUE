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

package tufts.vue.ui;
import tufts.vue.DEBUG;

import tufts.vue.Actions;
import tufts.vue.DataSourceViewer;
import tufts.vue.FavoritesDataSource;
import tufts.vue.FavoritesWindow;
import tufts.vue.LWComponent;
import tufts.vue.LWContainer;
import tufts.vue.LWImage;
import tufts.vue.LWLink;
import tufts.vue.LWNode;
import tufts.vue.LWPathway;
import tufts.vue.LWSelection;
import tufts.vue.LWSlide;
import tufts.vue.NodeTool;
import tufts.vue.PathwayTableModel;
import tufts.vue.Resource;
import tufts.vue.VUE;
import tufts.vue.VueResources;
import tufts.vue.gui.GUI;
import tufts.vue.gui.WidgetStack;
import tufts.vue.gui.WindowDisplayAction;

import java.util.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

/**
 * A list of Resource's with their icons & title's that is selectable, draggable & double-clickable
 * for resource actions.  Also uses a "masking" data-model that can abbreviate the results
 * until a synthetic model item at the end of this shortened list is selected, at which
 * time the rest of the items are "unmaksed" and displayed.
 *
 * @version $Revision: 1.21 $ / $Date: 2009-09-15 18:06:05 $ / $Author: anoop $
 */
public class ResourceList extends JList
    implements DragGestureListener, /*tufts.vue.ResourceSelection.Listener,*/ MouseListener,ActionListener
{
    public static final Color DividerColor = VueResources.getColor("ui.resourceList.dividerColor", 204,204,204);
    
    private static ImageIcon DragIcon = tufts.vue.VueResources.getImageIcon("favorites.leafIcon");

    private static int PreviewItems = 4;
    private static int PreviewModelSize = PreviewItems + 1;

    private static int LeftInset = 2;
    private static int IconSize = 32;
    private static int IconTextGap = new JLabel().getIconTextGap();
    private static int RowHeight = IconSize + 5;

    private DefaultListModel mDataModel;

    private boolean isMaskingModel = false; // are we using a masking model?
    private boolean isMasking = false; // if using a masking model, is it currently masking most entries?

    private final String mName;

    private static class MsgLabel extends JLabel {
        MsgLabel(String txt) {
            super(txt);
            setFont(GUI.TitleFace);
            setForeground(WidgetStack.BottomGradient);
            setPreferredSize(new Dimension(getWidth(), RowHeight / 2));

            int leftInset = LeftInset + IconSize + IconTextGap;

            setBorder(new CompoundBorder(new MatteBorder(0,0,1,0, DividerColor),
                                         new EmptyBorder(0,leftInset,0,0)));
        }
    }

    private JLabel mMoreLabel = new MsgLabel("?"); // failsafe
    private JLabel mLessLabel = new MsgLabel("Show top " + PreviewItems);
    
    /**
     * A model that can intially "mask" out all but a set of initial
     * items to preview the contents of the list, and provide a
     * special list item at the end of the preview range, that when
     * selected, unmasks the rest of the items in the model.
     */
    private class MaskingModel extends javax.swing.DefaultListModel
    {
        public int getSize() {
            return isMasking ? Math.min(PreviewModelSize, size()) : size() + 1;
        }
        
        public Object getElementAt(int index) {
            if (isMasking) {
                if (index == PreviewItems) {
                    return mMoreLabel;
                } else if (index > PreviewItems)
                    return "MASKED INDEX " + index; // should never see this
            } else if (index == size()) {
                return mLessLabel;
            }
            return super.getElementAt(index);
        }

        private void expand() {
            isMasking = false;
            //fireIntervalAdded(this, PreviewItems, size() - 1);
            fireContentsChanged(this, PreviewItems, size());
        }
        private void collapse() {
            isMasking = true;
            fireIntervalRemoved(this, PreviewItems, size());
        }
    }
    
    public ResourceList(Collection resourceBag)
    {
        this(resourceBag, null);
    }
    
    public ResourceList(Collection resourceBag, String name)
    {
        setName(name);
        mName = name;
        setFixedCellHeight(RowHeight);
        setCellRenderer(new RowRenderer());
        addMouseListener(this);
        
        // Set up the data-model

        //final javax.swing.DefaultListModel model;
        
        if (resourceBag.size() > PreviewModelSize) {
            mDataModel = new MaskingModel();
            isMaskingModel = true;
            mMoreLabel = new MsgLabel((resourceBag.size() - PreviewItems) + " more...");
            isMasking = true;
        } else
            mDataModel = new javax.swing.DefaultListModel();

        // can easily change this to faster ArrayList v.s. vector by subclassing AbstractListModel
        // We don't need synchronized as list only in use one at a time, by the awt.
        
        Iterator i = resourceBag.iterator();
        while (i.hasNext())
            mDataModel.addElement(i.next());
        
        setModel(mDataModel);

        // Set up the selection-model
        
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setSelectionModel(selectionModel);

        selectionModel.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (DEBUG.RESOURCE || DEBUG.SELECTION || DEBUG.FOCUS) {
                        System.out.println(ResourceList.this + " valueChanged: " + e
                                           + " index=" + getSelectedIndex()
                                           + " picked=" + getPicked());

                        // TODO: Leopard java 1.5 2008-05-28 SMF: a tiny drag on a
                        // resource leading to an aborted drag leaves us in a state such
                        // that all the events coming in as a result of up/down arrow
                        // key movement (which do move the selection), claim
                        // valueIsAdjusting!  So the arrows break after doing this,
                        // until the mouse click is used to change the resource
                        // selection, which resets us.  This is looking like a java bug.
                        
                        if (e.getValueIsAdjusting())
                            System.out.println(ResourceList.this + " isAdjusting: ignoring");
                    }

                    if (e.getValueIsAdjusting())
                        return;
                    
                    if (DEBUG.SELECTION) tufts.Util.printStackTrace("ResourceList valueChanged " + ResourceList.this);
                    if (isMaskingModel) {
                        if (isMasking && getSelectedIndex() >= PreviewItems)
                            ((MaskingModel)mDataModel).expand();
                        else if (!isMasking && getSelectedIndex() == mDataModel.size()) {

                            // For now: do nothing: force them to click on the collapse
                            // (keyboarding to it creates a user-loop when holding the
                            // down arrow, where it collpases, goes back to the top, then
                            // selects till it expands, then selects down to end and collapses
                            // again...
                            
                            //((MaskingModel)model).collapse();
                            // "selected" item doesn't exist at this point, so nothing is "picked"
                            // this will follow with a second selection event, which will
                            // set the resource selection to null, as nothing is picked by default.

                            return;
                        }
                    }
                    if (getPicked() != null)
                        tufts.vue.VUE.setActive(Resource.class, ResourceList.this, getPicked());
                    //tufts.vue.VUE.getResourceSelection().setTo(getPicked(), ResourceList.this);
                }
            });

        setDragEnabled(false);

        // Set up double-click handler for displaying content
        
        addMouseListener(new tufts.vue.MouseAdapter("resourceList") {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Resource r = getPicked();
                        if (r != null)
                            r.displayContent();
                    } else if (e.getClickCount() == 1) {
                        if (isMaskingModel && getSelectedValue() == mLessLabel)
                            ((MaskingModel)mDataModel).collapse();
                    }
                }
            });

        // attempt mouseDragged tracking ourselves...
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {    
                public void mouseDragged(java.awt.event.MouseEvent me) {
                    Resource picked = getPicked();
                    if (picked != null) {
                        Image image = null;
                        Object o = getSelectedValue();
                        if (getSelectedValue() instanceof ResourceIcon)
                            image = ((ResourceIcon)o).getImage(); // todo: more generic on Resource class
                        // TODO: If Resource's were uniquely atomic via a Factory and real ID's,
                        // we could maybe make the ResourceIcon simpler, and have the Resource itself
                        // cache the thumbnail image (and we could thusly also request it here, instead of
                        // from the ResourceIcon)
                        GUI.startSystemDrag(ResourceList.this, me, image, new GUI.ResourceTransfer(picked));
                    }
                }
            });
        
        tufts.vue.VUE.addActiveListener(Resource.class, this);
        //tufts.vue.VUE.getResourceSelection().addListener(this);

        // Set up the drag handler

        /*

        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(this, // Component
                                                      DnDConstants.ACTION_COPY |
                                                      DnDConstants.ACTION_MOVE |
                                                      DnDConstants.ACTION_LINK,
                                                      this); // DragGestureListener
        */
    }

    @Override
    public void removeNotify() {
        //tufts.Util.printStackTrace("removeNotify " + this);
        tufts.vue.VUE.removeActiveListener(Resource.class, this);
        //tufts.vue.VUE.getResourceSelection().removeListener(this);
    }

    public void activeChanged(final tufts.vue.ActiveEvent e, final tufts.vue.Resource resource) {
        if (getPicked() == resource) {
            ; // do nothing; already selected
        } else {
            // TODO: if list contains selected item, select it!
            clearSelection();
        }
            
    }
    
    private Resource getPicked() {
        Object o = getSelectedValue();
        if (o instanceof Resource)
            return (Resource) o;
        else if (o instanceof ResourceIcon)
            return ((ResourceIcon)o).getResource();
        else
            return null;
        //return (Resource) getSelectedValue();
    }
    
    public void dragGestureRecognized(DragGestureEvent e)
    {
        if (getSelectedIndex() != -1) {
            Resource r = getPicked();
            if (DEBUG.DND || DEBUG.SELECTION) System.out.println("ResourceList: startDrag: " + r);
            e.startDrag(DragSource.DefaultCopyDrop, // cursor
                        DragIcon.getImage(),
                        new Point(-10,-10), // drag image offset
                        new tufts.vue.gui.GUI.ResourceTransfer(r),
                        new tufts.vue.gui.GUI.DragSourceAdapter());
        }
    }

    public String toString() {
        String tag;
        if (mName == null)
            tag = "";
        else
            tag = "[" + mName + "]";

        return "ResourceList@" + Integer.toHexString(hashCode()) + tag; 
    }

    private class RowRenderer extends DefaultListCellRenderer
    {
        RowRenderer() {
            //setOpaque(false); // selection stops working!
            //setFont(ResourceList.this.getFont()); // leave default label font
            
            // Border: 1 pix gray at bottom, then LeftInset in from left
            setBorder(new CompoundBorder(new MatteBorder(0,0,1,0, DividerColor),
                                         new EmptyBorder(0,LeftInset,0,0)));
            setAlignmentY(0.5f);
        }
        
        public Component getListCellRendererComponent(
        JList list,
        Object value,            // value to display
        int index,               // cell index
        boolean isSelected,      // is the cell selected
        boolean cellHasFocus)    // the list and the cell have the focus
        {
            if (value == mMoreLabel)
                return mMoreLabel;
            else if (value == mLessLabel)
                return mLessLabel;

            ResourceIcon icon;
            Resource r;

            // The model starts as a list of Resources, but if asked to render
            // we replace it with a ResourceIcon, with painter set to this JList.
            // (We can still get the Resource later from the ResourceIcon)
            
            if (value instanceof Resource) {
                r = (Resource) value;
                icon = new ResourceIcon(r, IconSize, IconSize, list);
                mDataModel.set(index, icon); // ideally, wouldn't want to trigger a model change tho...
            } else {
                icon = (ResourceIcon) value;
                r = icon.getResource();
            }
            
            setIcon(icon);

            if (false)
                setText("<HTML>" + r.getTitle());
            else
                setText(r.getTitle());
          /*  if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }*/
            Color bg = null;
            if (isSelected) {
                bg = GUI.getTextHighlightColor();
            } else {
                
                bg = list.getBackground();
            }
            setBackground(bg);
            //setEnabled(list.isEnabled());
            return this;
        }
    }
	private void displayContextMenu(MouseEvent e) {
        getPopup(e).show(e.getComponent(), e.getX(), e.getY());
	}
	
	private JPopupMenu m = null;
	private final JMenuItem launchResource = new JMenuItem("Open resource");
	private final WindowDisplayAction infoAction = new WindowDisplayAction(VUE.getInfoDock());
    private final JCheckBoxMenuItem infoCheckBox = new JCheckBoxMenuItem(infoAction);
    private final JMenuItem addToMap = new JMenuItem("Add to map");
    private final JMenuItem addToNode = new JMenuItem("Add to selected node");
    private final JMenuItem addToSlide = new JMenuItem("Add to slide");
    private final JMenuItem addToSavedContent = new JMenuItem("Add to \"My Saved Content\"");
    
	private JPopupMenu getPopup(MouseEvent e) 
	{
		if (m == null)
		{
			m = new JPopupMenu(VueResources.getString("menu.popup.resource"));
		
			infoCheckBox.setLabel("Resource Info");
			if (VUE.getInfoDock().isShowing())
				infoCheckBox.setSelected(true);
			m.add(infoCheckBox);
			m.addSeparator();
			m.add(addToMap);
			m.add(addToNode);
			m.add(addToSlide);
			m.add(addToSavedContent);
			m.addSeparator();
			m.add(launchResource);
			launchResource.addActionListener(this);
			addToMap.addActionListener(this);
			addToNode.addActionListener(this);
			addToSlide.addActionListener(this);
			addToSavedContent.addActionListener(this);
		}
		LWSelection sel = VUE.getActiveViewer().getSelection();
		LWComponent c = sel.only();
		
		if (c != null && c instanceof LWNode)
		{
			addToNode.setEnabled(true);
			addToSlide.setEnabled(false);
			if (c.hasResource())
				addToNode.setLabel("Replace resource on node");
		}
		else if (c != null && c instanceof LWSlide)
		{
			addToNode.setEnabled(false);
			addToSlide.setEnabled(true);
			if (c.hasResource())
				addToNode.setLabel("Add to selected node");
		}
		else
		{
			addToNode.setEnabled(false);
			addToSlide.setEnabled(false);
			if (c != null && c.hasResource())
				addToNode.setLabel("Add to selected node");
		}
		return m;
	}
	Point lastMouseClick = null;
	
	public void mouseClicked(MouseEvent arg0) {
		 
		
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource().equals(launchResource))
		{
			int index = this.locationToIndex(lastMouseClick);
			ResourceIcon o = (ResourceIcon)this.getModel().getElementAt(index);
			o.getResource().displayContent();
			//this.get
			//System.out.println(o.toString());
		} else if (e.getSource().equals(addToMap))
		{
			
			int index = this.locationToIndex(lastMouseClick);
			ResourceIcon o = (ResourceIcon)this.getModel().getElementAt(index);
			
			LWNode end = NodeTool.NodeModeTool.createNewNode(o.getResource().getName());
			end.setResource(o.getResource());
			VUE.getActiveMap().addNode(end);
			setXYByClustering(end);
		} else if (e.getSource().equals(addToSlide))
		{
			int index = this.locationToIndex(lastMouseClick);
			ResourceIcon o = (ResourceIcon)this.getModel().getElementAt(index);
			 
			LWSelection sel = VUE.getActiveViewer().getSelection();
			LWSlide c = (LWSlide)sel.only();
			
			LWImage end = new LWImage();
			end.setResource(o.getResource());
			
			end.setStyle(c);
			end.setResource(o.getResource());
			end.setLabel(o.getResource().getName());
			c.addChild(end);
			
      	  	
		} else if (e.getSource().equals(addToNode))
		{
			int index = this.locationToIndex(lastMouseClick);
			ResourceIcon o = (ResourceIcon)this.getModel().getElementAt(index);
			 
			LWSelection sel = VUE.getActiveViewer().getSelection();
			LWComponent c = sel.only();
			VUE.setActive(LWComponent.class, this, null);            
			c.setResource(o.getResource());
			c.setLabel(o.getResource().getName());
      	  	VUE.setActive(LWComponent.class, this, c);
			
			
		} else if (e.getSource().equals(addToSavedContent))
		{
			int index = this.locationToIndex(lastMouseClick);
			ResourceIcon o = (ResourceIcon)this.getModel().getElementAt(index);
			FavoritesDataSource repository = DataSourceViewer.getDefualtFavoritesDS();
		    ((FavoritesWindow)repository.getResourceViewer()).getFavoritesTree().addResource(o.getResource());
		}				    
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger())
		 {
			 	lastMouseClick = e.getPoint();
				displayContextMenu(e);
		 }
	}

	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
		 {
			 	lastMouseClick = e.getPoint();
				displayContextMenu(e);
		 }
	}
    
	private void setXYByClustering(LWNode node) {
		Iterator<LWComponent> i = VUE.getActiveMap().getAllDescendents(
				LWContainer.ChildKind.PROPER).iterator();
		float  xNumerator = 0 ;
		float yNumerator = 0 ;
		float denominator = 0 ;
		while (i.hasNext()) {
			LWComponent c = i.next();
			if (c instanceof LWNode) {
				LWNode mapNode = (LWNode)c;
				if(mapNode != node) {
				double score = computeScore(node,mapNode);
					xNumerator += score*mapNode.getX();
					yNumerator += score*mapNode.getY();
					denominator += score;
				}
			}
		}
		if(denominator != 0) {
			float x = xNumerator/denominator;
			float y = yNumerator/denominator;
			node.setX(x);
			node.setY(y);
		}
		
	  i = VUE.getActiveMap().getAllDescendents(
				LWContainer.ChildKind.PROPER).iterator();
		while (i.hasNext()) {
			LWComponent c = i.next();
			if (c instanceof LWNode) {
				LWNode mapNode = (LWNode)c;
				if(checkCollision(mapNode,node)) {
					Object PUSH_ALL = "pushAll";
					Actions.projectNodes(node,  24,PUSH_ALL );
				}
			}
		}
		
	}
	
	private double computeScore (LWNode n1,LWNode n2) {
		double score = 0.0;
		String[] words1 = n1.getLabel().split("\\s+");
		String[] words2 = n2.getLabel().split("\\s+");
		int matches = 0;
		for(int i = 0;i<words1.length;i++) {
			if(n2.getLabel().contains(words1[i])){
				matches++;
			}
		}
		double p1 = (double) matches / words1.length;
		double p2 = (double) matches/words2.length;
		if(p1== 0 && p2 == 0 ){
			score = 0.0; 
		} else {
			score = 2*p1*p2/(p1+p2); // harmonic mean
		}
		return score;
	}
	
	private boolean checkCollision(LWComponent c1, LWComponent c2) {
		boolean collide = false;
		if(c2.getX()>= c1.getX() && c2.getX() <= c1.getX()+c1.getWidth() && c2.getY() >= c1.getY() && c2.getY() <=c1.getY()+c2.getHeight()) {
			collide = true;
		}
		return collide;
	}
}


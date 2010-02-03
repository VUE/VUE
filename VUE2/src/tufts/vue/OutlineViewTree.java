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

import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.UIManager;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.ImageIcon;
import java.util.Iterator;
import java.util.ArrayList;

/**
 *
 * A class that represents a tree structure which holds the outline view model
 *   
 * Todo: render right from the node labels so all we have to do is repaint to refresh.
 * (still need to modify tree for hierarchy changes tho).
 *
 * @version $Revision: 1.52 $ / $Date: 2010-02-03 19:17:40 $ / $Author: mike $
 * @author  Daisuke Fujiwara
 */

public class OutlineViewTree extends JTree
    implements LWComponent.Listener, LWSelection.Listener, ActiveListener<LWMap>
{
    private boolean selectionFromVUE = false;
    private boolean valueChangedState = false;
    
    //a variable that controls the label change of nodes
    private boolean labelChangeState = false;
    
    private LWContainer currentContainer = null;
    private tufts.oki.hierarchy.OutlineViewHierarchyModel hierarchyModel = null;
    
    private ImageIcon  nodeIcon = VueResources.getImageIcon("outlineIcon.node");
    private ImageIcon linkIcon = VueResources.getImageIcon("outlineIcon.link");
    private ImageIcon   mapIcon = VueResources.getImageIcon("outlineIcon.map");
    private ImageIcon   textIcon = VueResources.getImageIcon("outlineIcon.text");
   
    /** Creates a new instance of OverviewTree */
    public OutlineViewTree()
    {    	 
         setModel(null);
         setEditable(true);
         setFocusable(true);
         getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
         setCellRenderer(new OutlineViewTreeRenderer());
         //setCellRenderer(new DefaultTreeCellRenderer());
         setCellEditor(new OutlineViewTreeEditor());
         setInvokesStopCellEditing(true); // so focus-loss during edit triggers a save instead of abort edit

         setRowHeight(-1); // ahah: must do this to force variable row height
         
         //tree selection listener to keep track of the selected node 
         addTreeSelectionListener(
            new TreeSelectionListener() 
            {
                public void valueChanged(TreeSelectionEvent e) 
                {        
                    ArrayList selectedComponents = new ArrayList();
                    TreePath[] paths = getSelectionPaths();
                    
                    //if there is no selected nodes
                    if (paths == null)
                    {
                        valueChangedState = false;
                        selectionFromVUE = false;
                    
                        return;
                    }
                    
                    for(int i = 0; i < paths.length; i++)
                    {   
                        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)paths[i].getLastPathComponent();
                        Object o = treeNode.getUserObject();
                        if (DEBUG.FOCUS) System.out.println(this
                                                            + " valueChanged in treeNode["
                                                            + treeNode
                                                            + "] userObject=" + o.getClass() + "[" + o + "]");
                        tufts.oki.hierarchy.HierarchyNode hierarchyNode = (tufts.oki.hierarchy.HierarchyNode) o;
                        
                        LWComponent component = hierarchyNode.getLWComponent();
                        
                        //if it is not LWMap, add to the selected components list
                        if (!(component instanceof LWMap))
                        {
                            selectedComponents.add(component);
                        }
                    }
                    
                    if(!selectionFromVUE)
                    {
                        valueChangedState = true;
                        
//                         if(selectedComponents.size() != 0)
//                           VUE.getSelection().setTo(selectedComponents.iterator());
//                         else
//                           VUE.getSelection().clear();
                    }
                    
                    valueChangedState = false;
                    selectionFromVUE = false;
                }
            }
        );
    }
    
    public OutlineViewTree(LWContainer container)
    {
        this(); 
        switchContainer(container);
    }
    
    public void activeChanged(ActiveEvent<LWMap> e) {
        switchContainer(e.active);
    }
    
    /**A method which switches the displayed container*/
    public void switchContainer(LWContainer newContainer)
    {
        //removes itself from the old container's listener list
        if (currentContainer != null)
        {
          currentContainer.removeLWCListener(this);
          currentContainer.removeLWCListener(hierarchyModel);
        }
        
        //adds itself to the new container's listener list
        if (newContainer != null)
        {
            currentContainer = newContainer;
            
            //creates the new model for the tree with the given new LWContainer
            hierarchyModel = new tufts.oki.hierarchy.OutlineViewHierarchyModel(newContainer);
            DefaultTreeModel model = hierarchyModel.getTreeModel();
            
            setModel(model);
            
            currentContainer.addLWCListener(this, LWKey.Label);
            currentContainer.addLWCListener(hierarchyModel,
                                            new Object[] { LWKey.ChildrenAdded,
                                                           LWKey.ChildrenRemoved,
                                                           LWKey.HierarchyChanged,
                                                           LWKey.LinkAdded,
                                                           LWKey.LinkRemoved
                                            }
                                            );
        } else
            setModel(null);
    }
    
    /**A method that sets the current tree path to the one designated by the given LWComponent*/
    public void setSelectionPath(LWComponent component)
    {     
        //in case the node inspector's outline tree is not initalized
        
        TreePath path = hierarchyModel.getTreePath(component);
        super.setSelectionPath(path);
        super.expandPath(path);
        super.scrollPathToVisible(path);
        
    }
    
    /**A method that sets the current tree paths to the ones designated by the given list of components*/
    public void setSelectionPaths(ArrayList list)
    {
        TreePath[] paths = new TreePath[list.size()];
        int counter = 0;
            
        TreePath path;
            
        for(Iterator i = list.iterator(); i.hasNext();)
        {  
            if ((path = hierarchyModel.getTreePath((LWComponent)i.next())) != null)
            {
                paths[counter] = path;
                counter++;
            }
        }
            
        super.setSelectionPaths(paths);       
    }
    
    /**A wrapper method which determines whether the underlying model contains a node with the given component*/
    public boolean contains(LWComponent component)
    {
       return hierarchyModel.contains(component);
    }
    
    /**A method which returns whether the model has been intialized or not*/
    public boolean isInitialized()
    {
        if (hierarchyModel != null)
          return true;
        
        else
          return false;
    }
    
    /**A method for handling a LWC event*/
    public void LWCChanged(LWCEvent e)
    {
        if (e.getComponent() instanceof LWPathway)
            return;

        //when a label on a node was changed
        //Already label filtered. 
       
        if(!labelChangeState)
        { 
            labelChangeState = true;
            hierarchyModel.updateHierarchyNodeLabel(e.getComponent().getLabel(), e.getComponent().getID());
            repaint();
            labelChangeState = false;
        }
    }
    
    /** A method for handling LWSelection event **/
    public void selectionChanged(LWSelection selection)
    {
        if (!isShowing()) // TODO: now we should do an auto-update when made visible
            return;
        
        if (!valueChangedState)
        {   
            selectionFromVUE = true;
        
            if (!selection.isEmpty())
              setSelectionPaths(selection);
        
            //else deselect
            else 
              super.setSelectionPath(null);
            
            //hacking
            selectionFromVUE = false;
        }
    }

    
    /**A class that specifies the rendering method of the outline view tree*/
    private class OutlineViewTreeRenderer extends OutlineViewRenderElement implements TreeCellRenderer
    {   
        public Component getTreeCellRendererComponent(
                        JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) 
        {
        	String label = null;
        	
            if (((DefaultMutableTreeNode)value).getUserObject() instanceof tufts.oki.hierarchy.HierarchyNode)
            {
                tufts.oki.hierarchy.HierarchyNode hierarchyNode = (tufts.oki.hierarchy.HierarchyNode)(((DefaultMutableTreeNode)value).getUserObject());
                LWComponent component = hierarchyNode.getLWComponent();
                
                if (component instanceof LWMap)
                  setIcon(mapIcon);
                
                else if (component instanceof LWNode)
                  setIcon(nodeIcon);
                
                else if (component instanceof LWText)
                   setIcon(textIcon);
                
                else if (component instanceof LWLink)
                  setIcon(linkIcon);

                //setText(component.getDisplayLabel());
                // need to update size (but only if label has changed)
                //setPreferredSize(getPreferredSize());
                // doesn't appear to get right size if there's a '.' in the name!
                label = tree.convertValueToText(value, sel, expanded, leaf, row, hasFocus);
                //System.out.println("render text[" + label + "]");
                if (component instanceof LWText)
                {
                	//System.out.println("NODE");
                	label = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(label);
                	//label = java.net.URLDecoder.decode();
                }
            }
            //else
            	//label = tree.convertValueToText(value, sel, expanded, leaf, row, hasFocus);
            
            if (sel) 
              setIsSelected(true);  
           
            else
              setIsSelected(false);
            
            if (hasFocus) 
              setIsFocused(true);
            
            else      
              setIsFocused(false);
         
         
            setText(label);
            
            return this;
        }
    }

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }   
    
    /**A class that specifies the editing method of the outline view tree*/
    private class OutlineViewTreeEditor extends AbstractCellEditor
        implements TreeCellEditor, KeyListener, FocusListener
    {
        // This is the component that will handle the editing of the cell value
        private OutlineViewEditorElement editorElement = null;
        private tufts.oki.hierarchy.HierarchyNode hierarchyNode = null;
        
        private boolean modified = false;
        private final int clickToStartEditing = 2;
        
        public OutlineViewTreeEditor()
        {
            editorElement = new OutlineViewEditorElement(this);
        }
        
        // This method is called when a cell value is edited by the user.
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) 
        {
        	//editorElement.setFont(VueConstants.FONT_MEDIUM);
            editorElement.setBackground(Color.white);   
            editorElement.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
            
            // Configure the component with the specified value
            String label = tree.convertValueToText(value, isSelected, expanded, leaf, row, true);
            editorElement.setText(label);
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
            hierarchyNode = (tufts.oki.hierarchy.HierarchyNode)node.getUserObject();
            LWComponent selectedLWComponent = hierarchyNode.getLWComponent();
            
            if (selectedLWComponent instanceof LWText)
              editorElement.setIcon(nodeIcon);
            
            else if (selectedLWComponent instanceof LWText)
                editorElement.setIcon(textIcon);
            
            else if (selectedLWComponent instanceof LWLink)
              editorElement.setIcon(linkIcon);
            
            else
              editorElement.setIcon(mapIcon);
            
            // Return the configured component
            return editorElement;
        }
    
        // This method is called when editing is completed.
        // It must return the new value to be stored in the cell.
        public Object getCellEditorValue() 
        {
            try
            {
                String text = editorElement.getText();
                if (DEBUG.FOCUS) System.out.println(this + " getCellEditorValue returns [" + text + "]");
                
                hierarchyNode.changeLWComponentLabel(text);
            }
            
            catch (osid.hierarchy.HierarchyException he)
            {}
            
            return hierarchyNode;
        }
        
        /** When any key is pressed on the text area, then it sets the flag that the value needs to be modified,
        and when a certain combination of keys are pressed then the tree node value is modified */
        public void keyPressed(KeyEvent e) 
        {
            // if we hit return key either on numpad ("enter" key), or
            // with any modifier down except a shift alone (in case of
            // caps lock) complete the edit.
            if ( e.getKeyCode() == KeyEvent.VK_ENTER &&
                 ( e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD
                   || (e.getModifiersEx() != 0 && !e.isShiftDown())
                 )
               )
            {
                e.consume();
                this.stopCellEditing();
                modified = false;
            } else
                modified = true;
        }
        
        /** notused */
        public void keyReleased(KeyEvent e) {}
        /** not used **/
        public void keyTyped(KeyEvent e) {}
        
        /** The tree node is only editable when it's clicked on more than a specified value */
        public boolean isCellEditable(java.util.EventObject anEvent) 
        { 
            if (anEvent instanceof java.awt.event.MouseEvent) 
            {
		return ((java.awt.event.MouseEvent)anEvent).getClickCount() >= clickToStartEditing;
	    }
            
	    return true;
        }

        public void focusGained(FocusEvent e) 
        {
            if (DEBUG.FOCUS) System.out.println(this + " focusGained from " + e.getOppositeComponent());
        }
        
        // When the focus is lost and if the text area has been modified, it changes the tree node value
        public void focusLost(FocusEvent e) 
        {
            if (DEBUG.FOCUS) System.out.println(this + " focusLost to " + e.getOppositeComponent() + " modified="+modified);
            
            if (modified) {   
                this.stopCellEditing();
                modified = false;
            }
        }
    }
    
    /** A class which displays the specified icon*/
    private static class IconPanel extends JPanel
    {
        private ImageIcon icon = null;
        
        public IconPanel()
        {
            setBackground(Color.white);
        }
        
        public void setIcon(ImageIcon icon)
        {
            this.icon = icon;
            
            if (icon != null)
              setPreferredSize(new Dimension(icon.getIconWidth() + 4, icon.getIconHeight() + 4));
        }
        
        public ImageIcon getIcon()
        {
            return icon;
        }
        
        protected void paintComponent(Graphics g)
        {
            if (icon != null)
              icon.paintIcon(this, g, 0, 0);
        }
    }

    
    /**A class which is used to display the rednerer for the outline view tree*/
    private static class OutlineViewRenderElement extends JPanel
    {
        private final JTextArea label;
        private final IconPanel iconPanel;
        
        private static final Color selectedColor = VueResources.getColor("outlineTreeSelectionColor");
        private static final Color nonSelectedColor = Color.white;
        
        public OutlineViewRenderElement()
        {
            //super(new FlowLayout(FlowLayout.LEFT, 5, 2));
        	super();
        	BoxLayout box = new BoxLayout(OutlineViewRenderElement.this,BoxLayout.X_AXIS);
        	setLayout(box);
        	setBorder(new EmptyBorder(new Insets(2,2,2,2)));
            //super(new BorderLayout());
            setBackground(Color.white);
            
            
            label = new JTextArea();
            
            
            label.setFont(VueResources.getFont("toolbar.font"));
            label.setEditable(false);
            iconPanel = new IconPanel();
            
            add(iconPanel);
            add(label);
            //add(iconPanel, BorderLayout.WEST);
            //add(label, BorderLayout.CENTER);
        }

        
        /**A method which gets called with the value of whether the renderer is focused */
        public void setIsFocused(boolean value) {}
        
        /**A method which gets called with the value of whether the renderer is selected */
        public void setIsSelected(boolean value)
        {   
            if (value)
                label.setBackground(selectedColor);
            else
                label.setBackground(nonSelectedColor);
        }

        public void setText(String text) {
            label.setText(text);
        }
        public String getText() {
            return label.getText();
        }
        public void setIcon(ImageIcon icon) {
            iconPanel.setIcon(icon);
        }
    }
    
    /**A class which is used to display the editor for the outline view tree*/
    private class OutlineViewEditorElement extends JPanel
    {
        private IconPanel iconPanel = null;
        private JTextArea label = null;
        private JViewport viewPort = null;
        private JScrollPane scrollPane = null;
        private OutlineViewTreeEditor editor = null;
        
        public OutlineViewEditorElement(OutlineViewTreeEditor editor)
        {
        	BoxLayout box = new BoxLayout(OutlineViewEditorElement.this,BoxLayout.X_AXIS);
        	setLayout(box);
        	setBorder(new EmptyBorder(new Insets(2,2,2,2)));
         
            label = new JTextArea();
            label.setFont(VueResources.getFont("toolbar.font"));
            label.setEditable(true);
            label.setLineWrap(true);
            label.setColumns(30);
            
            this.editor = editor;
            addKeyListener(editor);
            //addFocusListener(editor);
            label.addFocusListener(this.editor);
            
            iconPanel = new IconPanel();

            scrollPane = new JScrollPane(label);
            //viewPort = new JViewport();
            //viewPort.setView(label);
            
            add(iconPanel);
            add(scrollPane);
            //add(viewPort);     
        }
        
        public JViewport getViewPort()
        {
            return viewPort;
        }
        
        public void addKeyListener(KeyListener l)
        {
            label.addKeyListener(l);
        }
        
        public void removeKeyListener(KeyListener l)
        {
            label.removeKeyListener(l);
        }
        
        public void setText(String text)
        {   
            label.setText(text);
        }
        
        public String getText()
        {
            return label.getText();
        }
        
        public void setIcon(ImageIcon icon)
        {
            iconPanel.setIcon(icon);
        }
    }
}


/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

package edu.tufts.vue.ontology.ui;

import edu.tufts.vue.style.*;
import edu.tufts.vue.ontology.*;

import java.awt.Component;
import java.awt.datatransfer.*;
import java.awt.geom.Rectangle2D;
import java.awt.dnd.*;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
//import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;

import tufts.vue.*;
import tufts.vue.gui.*;

/*
 * TypeList.java
 *
 * modelled on tufts.vue.ui.ResourceList, this List will show
 * mapping of styles to ontologies and allow easy transfer
 * to LWMap - editing of individual  styles should also be possible
 * through the VUE gui.
 *
 * Created on March 7, 2007, 1:05 PM
 *
 * @author dhelle01
 */
public class TypeList extends JList {
    
    public static int count = 0;
    
    //private static Ontology fedoraOntology;
    
    //private DefaultListModel mDataModel;
    private ListModel mDataModel;
    
    private LWComponent comp;

    private LWSelection selection;
    
    private Ontology ontology;
    
    public TypeList() {
        
        //this.ontology = ontology;
        
        //mDataModel = new DefaultListModel();
        /*mDataModel = new ListModel()
        {
            public Object getElementAt(int index)
            {
                if(ontology!=null)
                  return ontology.getOntTypes().get(index);
                else
                  return null;
            }
            
            public int getSize()
            {
                if(ontology!=null)
                  return ontology.getOntTypes().size();
                else
                  return 0;
            }
            
            public void addListDataListener(ListDataListener listener)
            {

            }
            
            public void removeListDataListener(ListDataListener listener)
            {
                
            }
        };*/
        //setModel(mDataModel);
        setCellRenderer(new TypeCellRenderer());
        
        javax.swing.DefaultListSelectionModel selectionModel = new javax.swing.DefaultListSelectionModel();
        selectionModel.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        setSelectionModel(selectionModel);
        selectionModel.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e)
            {
                //comp = ((LWComponent)getSelectedValue()).duplicate();
                comp = createLWComponent(getSelectedValue());
                //comp.setParentStyle(comp);
                //VUE.getSelection().setSource(TypeList.this);
                //VUE.getSelection().setTo(comp);
            }
        });
        
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {    
                public void mouseDragged(java.awt.event.MouseEvent me) {
                    System.out.println("TypeList: mouse dragged");
                    //System.out.println("TypeList: selected label " + ((LWComponent)(getSelectedValue())).getLabel());
                    
                    /*if(comp instanceof LWNode)
                    {
                      VueToolbarController vtc = VueToolbarController.getController();
                      VueTool rTool = vtc.getTool("nodeTool");
                      //rTool.setSelectedSubTool(vtc.getTool("rect"));
                      VueToolbarController.getController().setSelectedTool(rTool);
                    }*/
;                    
                    GUI.startLWCDrag(TypeList.this,
                                     me,
                                     comp,
                                      VUE.getActiveViewer().getTransferableHelper(comp));
                 
                }
         });
    
    }
    
    public static LWComponent createLWComponent(Object type)
    {
        LWComponent compFor = null;
        OntType ontType = null;
        if(type instanceof OntType)
        {
          ontType = (OntType)type;
          if(ontType !=null)
            if(isNode(ontType))  
            {
              compFor = new LWNode(ontType.getLabel());
            }
            else
            {
              LWLink r = new LWLink();
              r.setLabel(ontType.getLabel());
              compFor = r;
            }
          else
            compFor = new LWNode("null ont type");
        }
        else
        {
          compFor = new LWNode("error");    
        }
        
        if(ontType.getStyle()!=null)
            compFor.applyCSS(ontType.getStyle());
        
        return compFor;
    }
    
    /*public void addType(LWComponent typeComponent)
    {
        mDataModel.addElement(typeComponent);
    }*/
    
    class TypeCellRenderer implements ListCellRenderer
    { 
        public java.awt.Component getListCellRendererComponent(JList jList, Object value, int i, boolean isSelected, boolean hasFocus) 
        {
            JPanel p = new JPanel();
            java.awt.GridLayout grid = new java.awt.GridLayout(1,2);
            p.setLayout(grid);
           // p.setLayout(new java.awt.BorderLayout());
            
            p.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200,200,200)));
                    
            if(value instanceof OntType)
            {
                OntType t = (OntType)value;
                LWComponent noLabel = createLWComponent(t).duplicate();
                noLabel.setLabel("");
                noLabel.setAutoSized(false);
                noLabel.setSize(40,25);
                //p.add(new JLabel(new javax.swing.ImageIcon(createLWComponent(value).getAsImage())));
                p.add(new JLabel(new javax.swing.ImageIcon(noLabel.getAsImage())));
                p.add(new JLabel(t.getLabel()));
                Style s = t.getStyle();
                String icon = s.getAttribute("background-image");
                System.out.println("icon " + icon);
                if(icon != null)
                {
                    javax.swing.ImageIcon ii = new javax.swing.ImageIcon(icon);
                    System.out.println("image icon: " + ii);
                    p.add(new JLabel(ii));
                }
            }
            
            /*if(value instanceof LWComponent)
            {
                LWComponent comp = (LWComponent)value;
                LWComponent noLabelComp = (LWComponent)comp.duplicate();
                if(comp instanceof LWNode)
                  noLabelComp.setLabel("   ");
                else
                  noLabelComp.setLabel(" ");
                String truncatedLabel = comp.getLabel();
                if(truncatedLabel.length() > 15)
                {
                    truncatedLabel = truncatedLabel.substring(0,15) + "...";
                }
                JLabel label = new JLabel(truncatedLabel);
                //java.awt.Image im = comp.getAsImage();
                java.awt.Image im = noLabelComp.getAsImage();
                JLabel imageLabel = new JLabel(new javax.swing.ImageIcon(im));
                p.add(imageLabel);
                p.add(label);
            }*/
            
            if(value == getSelectedValue())
            {
                p.setBackground(new java.awt.Color(230,230,230));
            }
            else
                p.setBackground(new java.awt.Color(255,255,255));
            return p;
        }
    }
    
    public void loadOntology(URL ontologyURL,URL cssURL,org.osid.shared.Type ontType,boolean fromFile)
    {
        ontology = OntManager.getOntManager().readOntologyWithStyle(ontologyURL,
                                                      cssURL,
                                                      ontType);
        
        //OntManager.getOntManager().getOntList().add(ontology);
        //System.out.println("TypeList: ontology list size: " + OntManager.getOntManager().getOntList().size());
        
        setModel(new OntologyTypeListModel(ontology));
        
        //fillList(ontology);
        
    }
    
    public void loadOntology(URL ontologyURL,URL cssURL,org.osid.shared.Type ontType)
    {
       ontology = OntManager.getOntManager().readOntologyWithStyle(ontologyURL,
                                                      cssURL,
                                                      ontType);
        
       setModel(new OntologyTypeListModel(ontology));
       // fillList(ontology);
        
    }
    
    
    public void loadOntology(String ontologyLocation,String cssLocation,org.osid.shared.Type ontType,boolean fromFile)
    {
        ontology = OntManager.getOntManager().readOntologyWithStyle(VueResources.getURL(ontologyLocation),
                                                      VueResources.getURL(cssLocation),
                                                      ontType);
        
        //OntManager.getOntManager().getOntList().add(ontology);
        
        //System.out.println("TypeList: ontology list size: " + OntManager.getOntManager().getOntList().size());
        
        //fillList(ontology);
        
        setModel(new OntologyTypeListModel(ontology));
        
    }
    
    /**
     *
     * May be handled by LWComponent in future.
     *
     * Right now OntManager appears to read only link.localname 
     * styles, so check for node.localname and let default
     * style get applied for now.
     *
     **/
    private static boolean isNode(OntType type)
    {        
        //return type.getType().equals(edu.tufts.vue.style.SelectorType.getNodeType());
        
        System.out.println("tl: type.getStyle.getClass() " + type.getStyle().getClass());
               
        return (type.getStyle() instanceof NodeStyle);    
    }
    
    private void fillList(Ontology ontology)
    {
        List<OntType> types = ontology.getOntTypes();
        
        Iterator<OntType> iter = types.iterator();
        while(iter.hasNext())
        {
            OntType ot = iter.next();
            
            System.out.println("TypeList: " + ot.getComment());
            System.out.println("tl: isNode: " + ot.getId() + " isNode?:" + isNode(ot));
            
            Style style = ot.getStyle();
            if(isNode(ot))
            {
                addNode(ot,ontology,style);
            }
            else
            {   
              addLink(ot,ontology,style);  
            }
        }        
    }
    
    private void addNode(OntType ontType,Ontology ontology,Style style)
    {         
                LWNode node = new LWNode(ontType.getLabel());
                  
                //node.setLabel(ot.getLabel());

                node.setAutoSized(false);
                
                node.setAbsoluteSize(25,25);
                

                NodeTool.SubTool st = NodeTool.getActiveSubTool();
                node.setShape(st.getShape());
                node.applyCSS(style);
                //addType(node);
          
    }
    
    private void applyDefaultNodeStyle(LWNode node)
    {
        
    }
    
    private void addLink(OntType ontType,Ontology ontology, Style style)
    {
        
              LWLink link = new LWLink();
              link.setAutoSized(false);
              link.setLabel(ontType.getLabel());
              link.setHeadPoint(10,25);
              link.setTailPoint(60,30);
              link.setAbsoluteSize(100,50);
              link.setStrokeWidth(4.0f);
              link.setStrokeColor(java.awt.Color.RED);
              //applyDefaultLinkStyle(link);
              link.applyCSS(ontType.getStyle());
              //addType(link);
        
    }
    
    private void applyStyleSheetLinkStyle(LWLink link,Style style)
    {
              if(style.getAttribute("stroke-width") != null)
                link.setStrokeWidth(Float.parseFloat(style.getAttribute("stroke-width")));
    }
    
    private void applyDefaultLinkStyle(LWLink link)
    {
              link.setArrowState(LWLink.ARROW_TAIL);
              link.setWeight(1);
    }
    
    public class OntologyTypeListModel implements ListModel
    {
        private Ontology ontology;
             
        public OntologyTypeListModel(Ontology ontology)
        {
          this.ontology = ontology;   
        }
        
        public Object getElementAt(int index)
            {
                if(ontology!=null)
                  return ontology.getOntTypes().get(index);
                else
                  return null;
            }
            
            public int getSize()
            {
                if(ontology!=null)
                  return ontology.getOntTypes().size();
                else
                  return 0;
            }
            
            public void addListDataListener(ListDataListener listener)
            {

            }
            
            public void removeListDataListener(ListDataListener listener)
            {
                
            }
        
    }
    
    
    /**
     *
     * just for testing/demo purposes
     *
     */
    /*public static void main(String[] args)
    {
        
        // add a mouselistener to bring up a color chooser (and possibly
        // the text formatter or a generic one?).
        // attempt to edit an LWComponent directly on the List
        // if this works, try it all in VUE (and add ability to drag 
        // types to LWMap to create instances)
        
        //TypeList tlist = new TypeList();
        javax.swing.JPanel panel = createTestPanel(tlist);//new javax.swing.JFrame();
        javax.swing.JFrame f = new javax.swing.JFrame();
        f.getContentPane().add(panel);
        
        //show the frame
        f.setBounds(100,100,150,300);
        f.setVisible(true);
    }*/
    
    /*public static javax.swing.JPanel createTestPanel(TypeList tlist)
    {
        javax.swing.JPanel testPanel = new javax.swing.JPanel();
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(tlist);
        testPanel.add(scroll);
        
        //add some demo data to the List
        /*LWNode rectangle = new LWNode("Test Node");
        rectangle.setShape( new java.awt.Rectangle(5,5,50,20));
        LWLink link = new LWLink();
        link.setStartPoint(10,10);
        link.setEndPoint(90,10);
        link.setAbsoluteSize(100,15);
        link.setArrowState(LWLink.ARROW_BOTH);
        link.setWeight(2);
        link.setLabel("contains");
        LWNode oval = new LWNode("Test Node 2");
        oval.setShape(new java.awt.geom.Ellipse2D.Double(5,5,50,20));

        tlist.addType(rectangle);
        tlist.addType(link);
        tlist.addType(oval);*/
        
        //add Fedora types and an extra node
        

        
        //Ontology ontology = OntManager.getFedoraOntologyWithStyles();
        
        /*Ontology ontology = null;
        
        
        try
        {  
        
        System.out.println("fedora url: " + VueResources.getURL("fedora.ontology.url"));
        System.out.println("fedora url: " + VueResources.getString("fedora.ontology.url"));
        
         ontology = OntManager.getOntManager().readOntologyWithStyle(VueResources.getURL("fedora.ontology.rdf"),
                                                      //new java.net.URL("http://www.fedora.info/definitions/1/0/fedora-relsext-ontology.rdfs"),
                                                      VueResources.getURL("fedora.ontology.css"),
                                                      OntologyType.RDFS_TYPE);
          
         /*ontology = OntManager.getOntManager().readOntologyWithStyle(//VueResources.getURL("fedora.ontology.rdf"),
                                                      new java.net.URL("http://www.atl.lmco.com/projects/ontology/ontologies/animals/animalsA.owl"),
                                                      new java.net.URL("http://vue-dev.uit.tufts.edu/ontology/css/animalsAStyle.css"),
                                                      OntManager.OWL);*/
         
         
          /*ontology = OntManager.getOntManager().readOntologyWithStyle(//VueResources.getURL("fedora.ontology.rdf"),
                                                      new java.net.URL("http://vue-dev.uit.tufts.edu/ontology/season.rdfs.xml"),
                                                      new java.net.URL("http://vue-dev.uit.tufts.edu/ontology/css/season.css"),
                                                      OntManager.OWL);*/

          
        //}
        //catch(java.net.MalformedURLException urlException)
        /*catch(Exception urlException)
        {
            System.out.println("Ontology Manager: Malformed URL:" + urlException);
            VueUtil.alert("Ontology Load Failed - improper URL","Ontology Load Failed - improper URL");
            //ontology = OntManager.getFedoraOntologyWithStyles();
        }
        
        List<OntType> types = ontology.getOntTypes();
        
        System.out.println("TypeList: types size " + types.size());
        
        Iterator<OntType> iter = types.iterator();
        while(iter.hasNext())
        {
            OntType ot = iter.next();
            Style style = ot.getStyle();
            
            if(isNode(ot))
            {
                LWNode node = new LWNode();
                node.applyCSS(style);
                tlist.addType(node);
            }
            else
            {    
              LWLink link = new LWLink();
              link.setLabel(ot.getLabel() + "-->" + count);
              link.setHeadPoint(10,25);
              link.setTailPoint(140,25);
              link.setAbsoluteSize(150,50);
              //link.setArrowState(LWLink.ARROW_HEAD);
              //link.setWeight(Integer.parseInt(style.getAttribute("weight")));
              
              System.out.println("t1: test panel about to apply style for: " + ot.getLabel());
              System.out.println("tl: test panel applying style: " + ot.getStyle());
              
              link.applyCSS(style);
              tlist.addType(link);
            }
            
            
            //System.out.println("tl: ot: " + ot + " style: " + style.getName());
            /*System.out.println("tl: isNode: " + ot.getId() + " isNode?:" + isNode(ot));
            System.out.println("tl: description: " + ot.getComment());
            LWLink link = new LWLink();
            link.setLabel(ot.getLabel());
            link.setHeadPoint(10,25);
            link.setTailPoint(140,25);
            link.setAbsoluteSize(150,50);
            //link.setArrowState(LWLink.ARROW_HEAD);
            //link.setWeight(Integer.parseInt(style.getAttribute("weight")));
            link.applyCSS(style);
            tlist.addType(link);*/
        //}
        
        /*LWNode node = new LWNode("Fedora Object");
        //node.setLabel("Fedora Object");
        node.setAbsoluteSize(150,50);
        //node.setShape( new java.awt.Rectangle(5,5,135,45));
        NodeTool.SubTool st = NodeTool.getActiveSubTool();
        node.setShape(st.getShape());
        tlist.addType(node);
        
        return testPanel;
    }*/
    
}

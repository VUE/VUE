/*
 * PathwayTableModel.java
 *
 * Created on December 3, 2003, 1:15 PM
 */

package tufts.vue;

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 *
 * @author  Jay Briedis
 */
public class PathwayTableModel extends DefaultTableModel{
    
    private PathwayTab tab = null;
    private LWPathwayManager manager = VUE.getActiveMap().getPathwayManager();
    //private ArrayList tableList = null;
   
    
    public PathwayTableModel(PathwayTab tab){
       this.tab = tab;
       //tableList = new ArrayList();
       this.addTableModelListener(tab);
    }

    private LWPathwayManager getManager(){
        if(manager == null)
            this.manager = VUE.getActiveMap().getPathwayManager();
        return manager;
    }
    
    public void addPathway(LWPathway pathway){
        if(this.getManager() != null){  
            manager.addPathway(pathway);         
            this.fireTableDataChanged(); 
        }
        tab.updateControlPanel();
    }
    
    public LWPathway getCurrentPathway(){
        return this.manager.getCurrentPathway();
    }
    
    public int getCurrentIndex(){
        return this.manager.getCurrentIndex();
    }
    
    public Object getElement(int row){
        if(this.manager != null)
            return manager.getPathwaysElement(row);
        return null;
    }
    
    public void addElement(LWComponent comp){
        this.manager.addPathwayElement(comp, this.getCurrentPathway());
    }
    
    public void addElements(LWComponent[] array){
        this.manager.addPathwayElements(array, this.getCurrentPathway());
    }
    
    public void removeElement(LWComponent comp){
        this.manager.removeElement(comp);
    }
    
    public void removePathway(LWPathway pathway){
        if(this.getManager() != null){
            
            manager.removePathway(pathway);
            
            if (manager.getCurrentPathway() == null){
                tab.removeElement.setEnabled(false);
                tab.addElement.setEnabled(false);
            }   
            
            this.fireTableDataChanged();
        }
        tab.updateControlPanel();
    }
    
    public synchronized int getRowCount(){
        if(this.getManager() != null)
            return manager.length();
        return 0;
    }

    public int getColumnCount(){
        return 5;
    }
    
    public String getColumnName(int col){
        try{
            switch(col){
                case 0:
                    return "A";
                case 1:
                    return "B";
                case 2:
                    return "C";
                case 3:
                    return "D";
                case 4:
                    return "E";
            }
        }catch (Exception e){
            System.err.println("exception in the table model:" + e);
        }
        return "";
    }

    public Class getColumnClass(int col){
        if(col == 1)
            return Color.class;
        else if(col == 0 || col == 2 || col == 4)
            return ImageIcon.class;
        else
            return String.class;
    }
    
    public void setCurrentPathway(LWPathway path){
        manager.setCurrentPathway(path);
    }
    
    public boolean isCellEditable(int row, int col){
        Class colClass = this.getColumnClass(col);
        return colClass == Boolean.class || colClass == String.class;
    }
    
    /*
    public void setCurrentOpen(boolean val){
        manager.setCurrentOpen(val);
    }
    
    public void setCurrentOpen(){
        manager.setCurrentOpen();
    }*/
    
    public boolean isRepeat(String name){
        boolean isRep = false;
        Iterator iter = manager.getPathwayIterator();
        while(iter.hasNext()){
            Object obj = iter.next();
            if(obj instanceof LWPathway){
                LWPathway path = (LWPathway)obj;
                if(path.getLabel().equals(name))
                    isRep = true;
            }
        }
        return isRep;
    }
    
    public synchronized Object getValueAt(int row, int col){
        
        Object elem = manager.getPathwaysElement(row);
        if(elem instanceof LWPathway){
            LWPathway pathway = (LWPathway)elem;
            
            boolean hasNotes = true;
            if(pathway.getNotes().equals(null) || pathway.getNotes().equals(""))
                hasNotes = false;
                
            boolean isDisplayed = false;
            if(pathway.getShowing())
                isDisplayed = true;
            
            boolean isOpen = false;
            if(pathway.getOpen())
                isOpen = true;
                
            try{
                switch(col){
                    case 0:
                        return new Boolean(isDisplayed);
                    case 1:
                        return pathway.getBorderColor();
                    case 2:
                        return new Boolean(isOpen);
                    case 3:
                        return pathway.getLabel();
                    case 4:
                        return new Boolean(hasNotes);
                }
            }catch (Exception e){
                System.err.println("exception in the table model, setting pathway cell:" + e);
            } 
        }else if (elem instanceof LWComponent){
            LWComponent comp = (LWComponent)elem;
            
            //LWPathway pathway = manager.getPathwayforElementAt(row);
            //int elemIndex = pathway.getElementIndex(comp);

            /*boolean isCurrent = false;
            if(pathway.getCurrentIndex() == elemIndex)
                isCurrent = true;
                */
            try{
                switch(col){
                    case 3:
                        return comp.getLabel();
                }
            }catch (Exception e){
                System.err.println("exception in the table model, setting pathway element cell:" + e);
            }  
        }
        return null; 
    }
    
    public void setValueAt(Object aValue, int row, int col){
        
        Object elem = manager.getPathwaysElement(row);
        if(elem instanceof LWPathway){
            
            LWPathway path = (LWPathway)elem;
            
            if(path != null){
                if(col == 0){
                    path.setShowing();
                }
                else if(col == 1){
                    //path.setBorderColor((Color)aValue);
                    //manager.setCurrentPathway(path);
                }
                else if(col == 2){
                    manager.setPathOpen(path);
                    //manager.setCurrentPathway(path);                   
                }
                else if(col == 3){
                    path.setLabel((String)aValue);
                    //manager.setCurrentPathway(path);
                }
                else if(col == 4){
                    path.setNotes((String)aValue);
                    //manager.setCurrentPathway(path);
                }
            }
        }
        else if (elem instanceof LWComponent){
            LWComponent comp = (LWComponent)elem;
            if(col == 3){
                comp.setLabel((String)aValue);
            }
        }
        this.fireTableDataChanged();
        System.out.println("just fired table data changed in SetValueAt");
    }   
}


/*
    //switches a row to a new location
    public void switchRow(int oldRow, int newRow){
        //pathway.moveElement(oldRow, newRow);
        fireTableDataChanged();

        //update the pathway control panel
        tab.updateControlPanel();
    }
    */
    /*
    //adds a row at the designated location
    public synchronized void addRow(LWComponent element, int row){
        //pathway.addElement(element, row);
        fireTableRowsInserted(row, row);

        //update the pathway control panel
        tab.updateControlPanel();
    }

    //deletes the given row from the table
    public synchronized void deleteRow(int row){
        //pathway.removeElement(row);
        fireTableRowsDeleted(row, row); 

        //update the pathway control panel
        tab.updateControlPanel();
        this.removePathway(pathway);
    }
*/
/*if(-1 < row && row < tableList.size()){
            Object element = this.tableList.get(row);
            if(element instanceof LWPathway){
                LWPathway rowPath = (LWPathway)element; 

                boolean hasNotes = true;
                if(rowPath.getNotes().equals(null) || rowPath.getNotes().equals(""))
                    hasNotes = false;
                
                boolean isCurrent = false;
                if( ((LWPathway)manager.getCurrentPathway()) != null
                    &&((LWPathway)manager.getCurrentPathway()).equals(rowPath))
                    isCurrent = true;

                try{
                    switch(column){
                        case 0:
                            return new Boolean(true); //return new Boolean(isCurrent);
                        case 1:
                            return rowPath.getBorderColor();
                        case 2:
                            return new Boolean(isCurrent);
                        case 3:
                            return rowPath.getLabel();
                        case 4:
                            return new Boolean(hasNotes);
                    }
                }catch (Exception e){
                    System.err.println("exception in the table model, setting pathway cell:" + e);
                }    
            }
            else if(element instanceof Object[]){
                Object[] storage = (Object[])element;
                LWPathway rowPath = (LWPathway)storage[0];
                Integer stor = (Integer)storage[1];
                int elemIndex = stor.intValue();

                boolean isCurrent = false;
                if(rowPath.getCurrentIndex() == elemIndex){
                    isCurrent = true;
                }

                try{
                    switch(column){
                        case 0:
                            return null;
                        case 1:
                            return null;
                        case 2:
                            return new Boolean(isCurrent);
                        case 3:
                            return rowPath.getElement(elemIndex).getLabel();
                        case 4:
                            return null;
                    }
                }catch (Exception e){
                    System.err.println("exception in the table model, setting pathway element cell:" + e);
                }  
            }
        }
        */ 

/*
        Iterator iter = this.tableList.iterator();
        int j = 0;
        while(iter.hasNext()){
            Object object = iter.next();
            if(object instanceof Object[])
                this.tableList.remove(j);
            j++;
        }*/
    /*int endRange = tableIndex+1;
            if(endRange < tableList.size()){
                boolean found = false;
                while(!found && endRange < tableList.size()){
                    if(tableList.get(endRange) instanceof LWPathway){
                        found = true;
                    }
                    else{
                        endRange++;
                        
                    }
                }
                System.out.println("tableIndex, endRange: "+tableIndex+","+endRange);
                    
                for(int range = tableIndex+1; range < endRange-1; range++){
                    tableList.remove(range);
                    System.out.println("removing element from tableList at index: "+range);
                }
            }*/
        
    
/*
 * NodeFilter.java
 *
 * Created on February 14, 2004, 2:55 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */

import java.util.*;
import javax.swing.table.*;
import javax.swing.event.*;
public class NodeFilter extends AbstractTableModel  implements MapFilterModel.Listener {
    public static final int KEY_COL = 0;
    public static final int OPERATOR_COL = 1;
    public static final int VALUE_COL = 2;
    public static final int TYPE_COL = 3;
    /** Creates a new instance of NodeFilter */
    private Vector statementVector;
    
    boolean editable;
    public NodeFilter(boolean editable) {
        this.editable = editable;
        statementVector = new Vector();
    }
    public NodeFilter() {
        this(false);
    }
    
    public void mapFilterModelChanged(MapFilterModelEvent e) {
        Vector removeStatements = new Vector();
        Iterator i = statementVector.iterator();
        while(i.hasNext()) {
            Statement statement = (Statement)i.next();
            if(statement.getKey() == e.getKey() && e.getAction() == MapFilterModelEvent.KEY_DELETED) {
                removeStatements.add(statement);
            }
        }
        removeAll(removeStatements);
        // setNodeFilter(nodeFilter);
    }
    public synchronized void add(Statement statement) {
        statementVector.add(statement);
    }
    
    public  synchronized void remove(Statement statement) {
        remove(statementVector.indexOf(statement));
    }
    
    public  synchronized void remove(int i) {
        statementVector.remove(i);
        fireTableRowsDeleted(i,i);
        fireTableDataChanged();
        fireTableStructureChanged();
    }
    
    public  synchronized  void addAll(NodeFilter statements) {
        statementVector.addAll(statements.getStatementVector());
    }
    /** nned to fire tableRowsDeleted **/
    public synchronized  void removeAll(NodeFilter statements) {
        removeAll(statements.getStatementVector());
    }
    public synchronized  void removeAll(Vector statements) {
        Iterator i = statements.iterator();
        while(i.hasNext()) {
            Statement s = (Statement)i.next();
            statementVector.remove(s);
        }
    }
    public  synchronized void removeAllElements() {
        statementVector.removeAllElements();
        fireTableRowsDeleted(0, size()-1);
        fireTableDataChanged();
    }
    public int size() {
        return statementVector.size();
    }
    
    public Statement get(int i) {
        return (Statement)statementVector.get(i);
    }
    public void setStatementVector(Vector statementVector) {
        this.statementVector = statementVector;
    }
    
    public Vector getStatementVector() {
        return this.statementVector;
    }
    
    public boolean add(Object o) {
        throw new RuntimeException(this + " can't add " + o.getClass() + ": " + o);
    }
    
    /* compares statement with all the statement and returns true only if the statement is true. */
    public boolean compare(Statement statement) {
        Iterator i = statementVector.iterator();
        while(i.hasNext()) {
            Statement nodeStatement = (Statement) i.next();
            if(nodeStatement.compare(statement))
                return true;
        }
        return false;
    }
    
    
    
    
    public void addStatement(Statement statement) {
        add(statement);
    }
    
    public boolean isEditable() {
        return editable;
    }
    
    public String getColumnName(int col) {
        if (col==0) {
            return "Field";
        } else if(col == 1) {
            return "Criteria";
        } else
            return "Search";
        
        
    }
    
    public int getRowCount() {
        return size();
    }
    
    public int getColumnCount() {
        return 3;
    }
    /**
     * public Class getColumnClass(int c) {
     * if(getValueAt(0,c) != null)
     * return getValueAt(0, c).getClass();
     * return Object.class;
     * }
     */
    public Object getValueAt(int row, int col) {
        Statement statement = (Statement) get(row);
        if (col== KEY_COL)
            return statement.getKey().toString();
        else if(col == OPERATOR_COL)
            return statement.getOperator();
        else
            return statement.getValue();
        
    }
    public void setValueAt(Object value, int row, int col) {
        Statement statement = (Statement) get(row);
        Key key = statement.getKey();
        if(col == VALUE_COL) {
            if(statement.getKey().getType().getDisplayName().equals(Type.INTEGER_TYPE)) {
                statement.setValue(new Integer(value.toString()));
            } else {
                statement.setValue(value);
            }
        } else if(col == KEY_COL)  {
            statement.setKey((Key)value);
            setValueAt(((Key)value).getType().getDefaultOperator(),row,OPERATOR_COL);
            setValueAt(((Key)value).getDefaultValue(), row,VALUE_COL);
            //statement.setOperator(((Key)value).getType().getDefaultOperator());
            //statement.setValue(((Key)value).getDefaultValue());
            // fireTableRowsUpdated(row,row);
            
        }else if(col == OPERATOR_COL)  {
            if(value instanceof Operator)
                statement.setOperator((Operator)value);
        }
        
        // row = -1 adds new condions else replace the existing one.
        
        fireTableCellUpdated(row, col);
        fireTableDataChanged();
    }
    
    
    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        //  return editable;
        return true;
    }
    
}

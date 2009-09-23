package tufts.vue.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import tufts.vue.DRBrowser;
import tufts.vue.MouseAdapter;
import tufts.vue.VueResources;
import tufts.vue.EventHandler;
import tufts.vue.gui.GUI;
import tufts.vue.gui.Widget;

import tufts.vue.LWComponent;
import tufts.vue.ds.Association;
import tufts.vue.ds.Field;

public class AssociationsPane extends Widget
{
	static final long		serialVersionUID = 1;
	private static final org.apache.log4j.Logger
							Log = org.apache.log4j.Logger.getLogger(AssociationsPane.class);
	static final int		BUTTON_WIDTH = 20;
	static final String		CHOOSE_FIELD = VueResources.getString("associationsPane.chooseField");
//	static AbstractAction	addAssociationAction = null;
	AbstractAction			deleteAssociationAction = null;
	JTable					associationsTable = null;
	BlankRenderer			blankRenderer = new BlankRenderer();
	GrayRenderer			grayRenderer = new GrayRenderer();

	public AssociationsPane() {
		this(VueResources.getString("associationsPane.name"));
	}

	public AssociationsPane(String name) {
		super(name);

		try {
// 			addAssociationAction = new AbstractAction(VueResources.getString("associationsPane.addassociation")) {
// 				public void actionPerformed(ActionEvent e) {
// 					addAssociation();
// 				}
// 			 };

			deleteAssociationAction = new AbstractAction(VueResources.getString("associationsPane.deleteassociation")) {
				static final long		serialVersionUID = 1;
				public void actionPerformed(ActionEvent e) {
					deleteAssociation();
				}
			};

			associationsTable = new JTable(new AssociationsTableModel()){
				static final long		serialVersionUID = 1;

				public TableCellRenderer getCellRenderer(int row, int column) {
					int	lastRow = associationsTable.getRowCount() - 1;

					return (row != lastRow) ? super.getCellRenderer(row, column) :
						(column != 0) ? grayRenderer : blankRenderer;
				}
			};

			TableColumnModel	colModel = associationsTable.getColumnModel();
			TableColumn			column = colModel.getColumn(0);

			column.setPreferredWidth(BUTTON_WIDTH);
			column.setMaxWidth(BUTTON_WIDTH);
			column.setMinWidth(BUTTON_WIDTH);

			column = colModel.getColumn(2);
			column.setPreferredWidth(BUTTON_WIDTH);
			column.setMaxWidth(BUTTON_WIDTH);
			column.setMinWidth(BUTTON_WIDTH);

			associationsTable.setDropTarget(new AssociationsDropTarget());
			associationsTable.addMouseListener(new AssociationsMouseListener());
			associationsTable.getSelectionModel().addListSelectionListener(new AssociationsListSelectionListener());

			Font	tableFont = GUI.LabelFace;

			associationsTable.setFont(tableFont);
			associationsTable.setRowHeight((int)(1.5 * associationsTable.getFontMetrics(tableFont).getHeight()));
			associationsTable.setShowGrid(false);

			setLayout(new BorderLayout());
			add(associationsTable);
		} catch (Exception ex) {
			Log.error(ex);
		}
	}

	public void finalize() {
//		addAssociationAction = null;
		deleteAssociationAction = null;
		associationsTable = null;
	}

	public void setActions() {
//		setMiscAction(this, new AddAssociationListener(), "dockWindow.addButton");
		setHelpAction(this, VueResources.getString("dockWindow.Datasources.associationsPane.helpText"));;

		enableMenuActions();
	}

	public void enableMenuActions() {
		int		deleteCount = associationsTable.getSelectedRowCount(),
				deleteRow = associationsTable.getSelectedRow();

		// Enable delete for multiple rows or for a single row other than the last row
		// (the last row is the the "drop here" row, which can't be deleted).
		deleteAssociationAction.setEnabled(deleteCount > 1 || (deleteCount == 1 && deleteRow != Association.getCount()));

		setMenuActions(this,
				new Action[] {
//			addAssociationAction,
			deleteAssociationAction
		});
	}

// 	public void addAssociation() {
// 		AssociationsTableModel	model = ((AssociationsTableModel)associationsTable.getModel());
// 		ListSelectionModel		selModel = associationsTable.getSelectionModel();
// 		int						insertAt = associationsTable.getSelectedRow();

// 		// Add the new row above the first selected row;
// 		// if no row is selected, add it at the end.
// 		if (insertAt == -1) {
// 			insertAt = model.getRowCount();
// 		}

// 		model.addEmptySlot(insertAt);
// 		selModel.clearSelection();
// 		selModel.setSelectionInterval(insertAt, insertAt);
// 	}

	public void deleteAssociation() {
		AssociationsTableModel	model = ((AssociationsTableModel)associationsTable.getModel());
		int toDelete[] = associationsTable.getSelectedRows(),
			deleteCount = toDelete.length,
			lastRow = associationsTable.getRowCount() - 1;

		while (deleteCount > 0) {
			deleteCount--;

			int		deleteRow = toDelete[deleteCount];

			if (deleteRow < lastRow) {
				model.deleteAssociation(deleteRow);
			}
		}
	}

	public boolean dropAssociation(Transferable transfer, int row, int column) {
		boolean			result = false;

		if (column == 1 || column == 3) {
			try {
				final LWComponent dragNode = tufts.vue.MapDropTarget.extractData
					(transfer,
					 LWComponent.DataFlavor,
					 LWComponent.class);

				final Field field = dragNode.getClientData(tufts.vue.ds.Field.class);

				associationsTable.setValueAt(field, row, column);

				result = true;
			} catch (Throwable t) {
				Log.error("exception processing drop " + transfer + " at " + row + "," + column, t);
			}
		}

		return result;
	}

	public void toggleAssociation() {
		if (associationsTable.getSelectedRowCount() == 1 &&
				associationsTable.getSelectedColumnCount() == 1 &&
				associationsTable.getSelectedColumn() == 0) {
			int						selectedRow = associationsTable.getSelectedRow();
			AssociationsTableModel	model = ((AssociationsTableModel)associationsTable.getModel());

			model.toggleAssociation(selectedRow);
		}
	}

	protected class AssociationsTableModel extends AbstractTableModel implements Association.Listener
	{
		static final long			serialVersionUID = 1;
		private static final int	COL_ENABLED = 0;
		private static final int	COL_FIELD_LEFT = 1;
		private static final int	COL_EQUALS = 2;
		private static final int	COL_FIELD_RIGHT = 3;

		private Field				tmpField0;
		private Field				tmpField1;

		private AssociationsTableModel() {
			EventHandler.addListener(Association.Event.class, this);
		}

		public void eventRaised(Association.Event e) {
			fireTableDataChanged();
		}

		public int getRowCount() {
			return Association.getCount() + 1;
		}

		public int getColumnCount() {
			return 4;
		}

		public boolean isCellEditable(int row, int column) {
			return (column == 0);
		}

		public Class getColumnClass(int column) {
			if (getRowCount() > 0)
				return getValueAt(0, column).getClass();
			else
				return null;
		}

		public Object getValueAt(int row, int column) {
			try {
				return fetchValue(row, column);
			} catch (Throwable t) {
				Log.error("failed to fetch value at row=" + row + ", col=" + column, t);
				return null;
			}
		}

		private Object fetchValue(final int row, final int column) {
			Object result = null;

			final int	lastRow = Association.getCount();

			switch (column) {
			case COL_ENABLED:
				result = (row == lastRow ? Boolean.FALSE : (Association.get(row).isEnabled() ? Boolean.TRUE : Boolean.FALSE));
				break;
			case COL_FIELD_LEFT:
				result = (row == lastRow ? tmpField0 : Association.get(row).getLeft());
				break;
			case COL_EQUALS:
				result = "=";
				break;
			case COL_FIELD_RIGHT:
				result = (row == lastRow ? tmpField1 : Association.get(row).getRight());
				break;
			}

			if (result == null) {
				result = CHOOSE_FIELD;
			}

			return result;
		}

		public void setValueAt(Object obj, int row, int column) {
			// note: this ignores row for now -- we only pay attention to column
			// the only row that can be updated this way is the last row

			switch (column) {
			case COL_FIELD_LEFT:
				if (tmpField1 != obj) {
					Log.debug("set f0 to " + obj);
					tmpField0 = (Field) obj;
				}
				break;
			case COL_FIELD_RIGHT:
				if (tmpField0 != obj) {
					Log.debug("set f1 to " + obj);
					tmpField1 = (Field) obj;
				}
				break;
			default:
				return;
			}

			Log.debug("f0=" + tmpField0 + "; f1=" + tmpField1);
			if (tmpField0 != null && tmpField1 != null && tmpField0 != tmpField1) {
				// construct a new field
				final Field fLeft = tmpField0;
				final Field fRight = tmpField1;
				// we'll get an Assocation.Event callback from the add, so make sure these are null first
				tmpField0 = tmpField1 = null; 
				Association.add(fLeft, fRight);
			} else {
				fireTableRowsUpdated(row, row);
			}
		}

		public void deleteAssociation(int index) {
			Association.remove(Association.get(index));
			// we'll get an Assocation.Event callback for the table update
		}

		public void toggleAssociation(int index) {

			final Association a = Association.get(index);

			if (a != null) {
				a.setEnabled(!a.isEnabled());
				Log.debug("toggled " + a);
			}

			fireTableRowsUpdated(index, index);
		}
	}

//===================================================================================================
// 	protected class AssociationsTableModel extends AbstractTableModel {
// 		static final long					serialVersionUID = 1;

// 		protected Vector<Vector<Object>>	associations = new Vector<Vector<Object>>();

// 		public int getRowCount() {
// 			return associations.size();
// 		}

// 		public int getColumnCount() {
// 			return 4;
// 		}

// 		public boolean isCellEditable(int row, int column) {
// 			return (column == 0);
// 		}

// 		public Class getColumnClass(int column) {
// 			return (associations.size() > 0 ? getValueAt(0, column).getClass() : null);
// 		}

// 		public Object getValueAt(int row, int column) {
// 			Object	result = null;

// 			switch (column){
// 			case 0:		// column 0 contains the Boolean stored in the Vector's 0th element (displays as a checkbox)
// 			case 1:		// column 1 contains the String stored in the Vector's 1st element
// 				result = associations.elementAt(row).elementAt(column);
// 				break;

// 			case 2:		// column 2 contains an equals sign
// 				result = "=";
// 				break;

// 			case 3:		// column 3 contains the String stored in the Vector's 2nd element
// 				result = associations.elementAt(row).elementAt(2);
// 				break;
// 			}

// 			if (result == null) {
// 				result = VueResources.getString("associationsPane.chooseField");
// 			}

// 			return result;
// 		}

// 		public void setValueAt(Object obj, int row, int column) {
// 			switch (column){
// 			case 0:		// column 0 contains the Boolean stored in the Vector's 0th element (displays as a checkbox)
// 			case 2:		// column 2 contains an equals sign
// 				break;

// 			case 1:		// column 1 contains the String stored in the Vector's 1st element
// 			case 3:		// column 3 contains the String stored in the Vector's 2nd element
// 				Vector<Object>	association = associations.elementAt(row);

// 				association.setElementAt(obj, column == 3 ? 2 : column);

// 				if (association.elementAt(1) != null && association.elementAt(2) != null) {
//									 association.setElementAt(new Boolean(true), 0);
// 				}


// 				fireTableRowsUpdated(row, row);
// 				break;
// 			}
// 		}

// 		public void addAssociation(int index) {
// 			Vector<Object>	association = new Vector<Object>();

// 			association.add(new Boolean(false));
// 			association.add(null);
// 			association.add(null);

// 			associations.add(index, association);

// 			fireTableRowsInserted(index, index);
// 		}

// 		public void deleteAssociation(int index) {
// 			associations.remove(index);

// 			fireTableRowsDeleted(index, index);
// 		}

// 		public void toggleAssociation(int index) {
// 			Vector<Object>	association = associations.elementAt(index);

// 			if (association.elementAt(1) != null && association.elementAt(2) != null) {
// 				boolean		currentState = ((Boolean)(association.elementAt(0))).booleanValue();

// 				association.setElementAt(new Boolean(!currentState), 0);
// 			}

// 			fireTableRowsUpdated(index, index);
// 		}
//	}

	protected class BlankRenderer extends DefaultTableCellRenderer {
		static final long	serialVersionUID = 1;
		JLabel				emptyLabel = new JLabel("");

		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, 
				int row, int column) {
			return emptyLabel;
		}
	}

	protected class GrayRenderer extends DefaultTableCellRenderer {
		static final long	serialVersionUID = 1;
		JLabel				grayLabel = new JLabel("");

		{
			grayLabel.setFont(GUI.LabelFace);
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
			String	stringValue = value.toString();

			grayLabel.setText(stringValue);
			grayLabel.setForeground(stringValue.equals(CHOOSE_FIELD) ? Color.GRAY : Color.BLACK);

			return grayLabel;
		}
	}

	protected class AssociationsDropTarget extends DropTarget {
		static final long					serialVersionUID = 1;

		public void dragEnter(DropTargetDragEvent event) {
			Point	dropLocation = event.getLocation();
			int		row = associationsTable.rowAtPoint(dropLocation);

			if (row == Association.getCount()) {
				event.acceptDrag(DnDConstants.ACTION_COPY);
			}
		}

		public void dragExit(DropTargetEvent event) {}

		public void dragOver(DropTargetDragEvent event) {
			Point	dropLocation = event.getLocation();
			int		row = associationsTable.rowAtPoint(dropLocation);

			if (row == Association.getCount()) {
				event.acceptDrag(DnDConstants.ACTION_COPY);
			}
		}

		public void drop(DropTargetDropEvent event) {
			Point	dropLocation = event.getLocation();
			int		row = associationsTable.rowAtPoint(dropLocation),
					column = associationsTable.columnAtPoint(dropLocation);

			if (row == Association.getCount()) {
				event.acceptDrop(DnDConstants.ACTION_COPY);
				event.dropComplete(dropAssociation(event.getTransferable(), row, column));
			}
		}

		public void dropActionChanged(DropTargetDragEvent event) {
			event.acceptDrag(DnDConstants.ACTION_COPY);
		}
	}

	protected class AssociationsListSelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			enableMenuActions();
		}
	}

	protected class AssociationsMouseListener extends MouseAdapter {
		public void mouseReleased(MouseEvent event) {
			toggleAssociation();
		}
	}

// 	protected class AddAssociationListener extends MouseAdapter {
// 		public void mouseClicked(MouseEvent event) {
// 			addAssociationAction.actionPerformed(null);
// 		}
// 	}
}

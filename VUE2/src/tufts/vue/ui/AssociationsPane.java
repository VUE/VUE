package tufts.vue.ui;

import java.awt.BorderLayout;
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
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import tufts.vue.DRBrowser;
import tufts.vue.MouseAdapter;
import tufts.vue.VueResources;
import tufts.vue.gui.Widget;

import tufts.vue.LWComponent;
import tufts.vue.ds.Field;

public class AssociationsPane extends Widget {
	static final long		serialVersionUID = 1;
	private static final org.apache.log4j.Logger
							Log = org.apache.log4j.Logger.getLogger(DRBrowser.class);
	static final int		BUTTON_WIDTH = 20;
	static AbstractAction	addAssociationAction = null;
	AbstractAction			deleteAssociationAction = null;
	JTable	   				associationsTable = null;

	public AssociationsPane() {
		this(VueResources.getString("associationsPane.name"));
	}

	public AssociationsPane(String name) {
		super(name);

		try {
			addAssociationAction = new AbstractAction(VueResources.getString("associationsPane.addassociation")) {
				public void actionPerformed(ActionEvent e) {
					addAssociation();
				}
			 };

			deleteAssociationAction = new AbstractAction(VueResources.getString("associationsPane.deleteassociation")) {
				public void actionPerformed(ActionEvent e) {
					deleteAssociation();
				}
			};

			associationsTable = new JTable(new AssociationsTableModel());

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
			
			setLayout(new BorderLayout());
			add(associationsTable);
		} catch (Exception ex) {
			Log.error(ex);
		}
	}

	public void finalize() {
		addAssociationAction = null;
		deleteAssociationAction = null;
		associationsTable = null;
	}

	public void setActions() {
		setMiscAction(this, new AddAssociationListener(), "dockWindow.addButton");
		setHelpAction(this, VueResources.getString("dockWindow.Resources.associationsPane.helpText"));;

		enableMenuActions();
	}

	public void enableMenuActions() {
		deleteAssociationAction.setEnabled(associationsTable.getSelectedRow() != -1);

		setMenuActions(this,
				new Action[] {
			addAssociationAction,
			deleteAssociationAction
		});
	}

	public void addAssociation() {
		AssociationsTableModel	model = ((AssociationsTableModel)associationsTable.getModel());
		ListSelectionModel		selModel = associationsTable.getSelectionModel();
		int						insertAt = associationsTable.getSelectedRow();

		// Add the new row above the first selected row;
		// if no row is selected, add it at the end.
		if (insertAt == -1) {
			insertAt = model.getRowCount();
		}

		model.addAssociation(insertAt);
		selModel.clearSelection();
		selModel.setSelectionInterval(insertAt, insertAt);
	}

	public void deleteAssociation() {
		AssociationsTableModel	model = ((AssociationsTableModel)associationsTable.getModel());
		int						deleteAt;

		while ((deleteAt = associationsTable.getSelectedRow()) != -1) {
			model.deleteAssociation(deleteAt);
		}
	}

	public boolean dropAssociation(Transferable transfer, int row, int column) {
		boolean			result = false;

/* 		DataFlavor[]	flavors = transfer.getTransferDataFlavors();
		int				flavorCount = flavors.length,
						index;
		for (index = 0; index < flavorCount; index++) {
			System.out.println("!!!!!!!!!! flavor " + index + " is " + flavors[index].getHumanPresentableName());
		} */

		if (column == 1 || column == 3) {
                    try {
                        LWComponent dragNode = tufts.vue.MapDropTarget.extractData(transfer,
                                                                                   LWComponent.DataFlavor,
                                                                                   LWComponent.class);
                        tufts.vue.ds.Field field = dragNode.getClientData(tufts.vue.ds.Field.class);

                        //associationsTable.setValueAt(field.getSchema().getName() + "." + field.getName(), row, column);
                        associationsTable.setValueAt(field, row, column);

                        tufts.vue.ds.Schema.addAssociation(field,
                                                           (Field) associationsTable.getValueAt(row, column == 1 ? 3 : 1));
                        
                        result = true;
                    } catch (Exception ex) {
                        Log.error("exception processing drop " + transfer + " at " + row + "," + column, ex);
                    }
		}
// 		if (column == 1 || column == 3) {
// 			try {
// 				String	data = (String)transfer.getTransferData(DataFlavor.stringFlavor);

// 				associationsTable.setValueAt(data.substring(data.indexOf('\'') + 1, data.lastIndexOf('\'')), row, column);

// 				result = true;
// 			} catch (Exception ex) {
// 				Log.error(ex);
// 			}
// 		}

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

	protected class AssociationsTableModel extends AbstractTableModel {
		static final long					serialVersionUID = 1;

		protected Vector<Vector<Object>>	associations = new Vector<Vector<Object>>();

		public int getRowCount() {
			return associations.size();
		}

		public int getColumnCount() {
			return 4;
		}

		public boolean isCellEditable(int row, int column) {
			return (column == 0);
		}

		public Class getColumnClass(int column) {
			return (associations.size() > 0 ? getValueAt(0, column).getClass() : null);
		}

		public Object getValueAt(int row, int column) {
			Object	result = null;

			switch (column){
			case 0:		// column 0 contains the Boolean stored in the Vector's 0th element (displays as a checkbox)
			case 1:		// column 1 contains the String stored in the Vector's 1st element
				result = associations.elementAt(row).elementAt(column);
				break;

			case 2:		// column 2 contains an equals sign
				result = "=";
				break;

			case 3:		// column 3 contains the String stored in the Vector's 2nd element
				result = associations.elementAt(row).elementAt(2);
				break;
			}

			if (result == null) {
				result = VueResources.getString("associationsPane.chooseField");
			}

			return result;
		}

		public void setValueAt(Object obj, int row, int column) {
			switch (column){
			case 0:		// column 0 contains the Boolean stored in the Vector's 0th element (displays as a checkbox)
			case 2:		// column 2 contains an equals sign
				break;

			case 1:		// column 1 contains the String stored in the Vector's 1st element
			case 3:		// column 3 contains the String stored in the Vector's 2nd element
				Vector<Object>	association = associations.elementAt(row);

				association.setElementAt(obj, column == 3 ? 2 : column);

				if (association.elementAt(1) != null && association.elementAt(2) != null) {
                                    association.setElementAt(new Boolean(true), 0);
				}


				fireTableRowsUpdated(row, row);
				break;
			}
		}

		public void addAssociation(int index) {
			Vector<Object>	association = new Vector<Object>();

			association.add(new Boolean(false));
			association.add(null);
			association.add(null);

			associations.add(index, association);

			fireTableRowsInserted(index, index);
		}

		public void deleteAssociation(int index) {
			associations.remove(index);

			fireTableRowsDeleted(index, index);
		}

		public void toggleAssociation(int index) {
			Vector<Object>	association = associations.elementAt(index);

			if (association.elementAt(1) != null && association.elementAt(2) != null) {
				boolean		currentState = ((Boolean)(association.elementAt(0))).booleanValue();

				association.setElementAt(new Boolean(!currentState), 0);
			}

			fireTableRowsUpdated(index, index);
		}
	}

	protected class AssociationsDropTarget extends DropTarget {
		static final long					serialVersionUID = 1;

		public void dragEnter(DropTargetDragEvent event) {
			event.acceptDrag(DnDConstants.ACTION_COPY);
		}

		public void dragExit(DropTargetEvent event) {}

		public void dragOver(DropTargetDragEvent event) {
			event.acceptDrag(DnDConstants.ACTION_COPY);
		}

		public void drop(DropTargetDropEvent event) {
			event.acceptDrop(DnDConstants.ACTION_COPY);

			Point	dropLocation = event.getLocation();
			int		row = associationsTable.rowAtPoint(dropLocation),
					column = associationsTable.columnAtPoint(dropLocation);

			event.dropComplete(dropAssociation(event.getTransferable(), row, column));
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

	protected class AddAssociationListener extends MouseAdapter {
		public void mouseClicked(MouseEvent event) {
			addAssociationAction.actionPerformed(null);
		}
	}
}

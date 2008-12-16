package tufts.vue.gui.renderer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import tufts.vue.SearchData;

public class SearchResultTableModel extends AbstractTableModel {

	private java.util.List<SearchData> searchResultArrLst = new java.util.ArrayList<SearchData>();
	private int columns = 2;
	private boolean editFlg;
	
	public SearchResultTableModel(){
		
	}
	public int getRowCount() {
		return searchResultArrLst.size();
	}

	public void addRow(SearchData data) {			
		searchResultArrLst.add(data);		
		fireTableStructureChanged();
	}

	public void removeRow(int rowIndex) {
		if (searchResultArrLst != null) {
			searchResultArrLst.remove(rowIndex);
		}
		fireTableStructureChanged();
	}

	public void setData(ArrayList dataLst) {		
		searchResultArrLst = dataLst;
		fireTableStructureChanged();
	}

	public int getColumnCount() {
		return columns;
	}

	public void setColumns(int columns) {
		this.columns = columns;
		fireTableStructureChanged();
	}

	public boolean isCellEditable(int row, int col) {
		if (editFlg && col == 0) {
			return true;
		}
		return false;
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		SearchData data = searchResultArrLst.get(rowIndex);
		if (aValue != null) {
			data.setSearchSaveName(aValue.toString());
		}
	}

	public SearchData getData(int row) {		
		SearchData data = searchResultArrLst.get(row);			
		return data;
	}
	public SearchData getSearchData(int row) {		
		SearchData data = searchResultArrLst.get(row);		
		return data;
	}
	public List getData(){
		return searchResultArrLst;
	}
	
	public Object getValueAt(int row, int col) {
		SearchData data = searchResultArrLst.get(row);
		return data.getSearchSaveName();
	}

	public void refresh() {
		fireTableDataChanged();
	}

	public void setEditableFlag(boolean b) {
		// TODO Auto-generated method stub
		editFlg = b;
	}
}

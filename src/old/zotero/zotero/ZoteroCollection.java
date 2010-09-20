package edu.tufts.vue.zotero;

import java.util.List;

public class ZoteroCollection {

	private String name;
	private List<ZoteroItem> itemList;
	
	public ZoteroCollection(String name)
	{
		this.name = name;
	}
	
	public ZoteroCollection(String name, List<ZoteroItem>itemList)
	{
		this.name = name;
		this.itemList = itemList;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setItemList(List<ZoteroItem> itemList) {
		this.itemList = itemList;
	}

	public List<ZoteroItem> getItemList() {
		return itemList;
	}
	
}

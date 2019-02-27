package edu.tufts.vue.fsm.event;

public interface SearchListener extends java.util.EventListener
{
	public void searchPerformed(SearchEvent evt);
}
package edu.tufts.vue.fsm.event;

public interface SearchListener extends java.util.EventListener
{
	void searchPerformed(SearchEvent evt);
}
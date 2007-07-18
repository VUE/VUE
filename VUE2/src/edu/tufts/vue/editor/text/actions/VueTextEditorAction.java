package edu.tufts.vue.editor.text.actions;

import javax.swing.Action;

public interface VueTextEditorAction extends Action {

  public void update();
  public void getProperties();

}
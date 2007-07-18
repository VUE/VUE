package edu.tufts.vue.editor.text.actions;

import javax.swing.text.AttributeSet;

public interface AttributeSetModifier {

  public boolean setValue(AttributeSet a);

  public AttributeSet getValue();

}
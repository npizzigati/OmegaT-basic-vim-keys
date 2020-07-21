package org.omegat.gui.editor;

import javax.swing.text.DefaultCaret;


public class EditorController implements IEditor {
  // Note that "editor" in the declaration below is referring to
  // the pane inside the fake editor; OmegaT instantiates both as
  // "editor," so in the OmegaT script environment, editor is the
  // instance of EditorController and editor.editor is the pane
  EditorTextArea3 editor;

  EditorController() {
    editor = new EditorTextArea3();
  }

  public int getCurrentPositionInEntryTranslation() {
    return editor.getCaretPosition();
  }

  public String getCurrentTranslation() {
    return editor.getText();
  }

  public void replaceEditText(String replacement) {
    editor.setText(replacement);
  }

  public void replacePartOfText(String replacement,
                                int startIndex,
                                int endIndex) {
    String currentText = editor.getText();
    editor.setText(currentText.substring(0, startIndex) + replacement + currentText.substring(endIndex));
  }

  // Need to have another method signature for 2 inputs
  // (to mark selection)
  public void setCaretPosition(CaretPosition pos) {
    // Original OmegaT implementation
    // if (pos.position != null) {
    //   editor.setCaretPosition(pos.position);
    // } else if (pos.selectionStart != null
    //            && pos.selectionEnd != null) {
    //   editor.select(off + pos.selectionStart,
    //                 off + pos.selectionEnd);
    // }

    // Fake implementation for testing

    DefaultCaret caret = editor.getCaret();
    caret.setDot(pos.getPosition());
  }

  public EditorSettings getSettings() {
    new EditorSettings();  
  }
}

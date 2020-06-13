package org.omegat.gui.editor;

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
    return editor.getCaretPosition;
  }

  public void setCaretPosition(CaretPosition pos) {
    if (pos.position != null) {
      editor.setCaretPosition(pos.position);
    } else if (pos.selectionStart != null
               && pos.selectionEnd != null) {
      editor.select(off + pos.selectionStart,
                    off + pos.selectionEnd);
    }
  }
}

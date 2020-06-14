package org.omegat.gui.editor;

public interface IEditor {
  public class CaretPosition {
    Integer position;
    Integer selectionStart, selectionEnd;

    public CaretPosition(int position) {
      this.position = position;
      this.selectionStart = null;
      this.selectionEnd = null;
    }

    public CaretPosition(int selectionStart, int selectionEnd) {
      this.position = null;
      this.selectionStart = selectionStart;
      this.selectionEnd = selectionEnd;
    }

    /** This comment it from the OmegaT source
     * We can't define it once since 'position' can be changed later.
     */
    public static CaretPosition startOfEntry() {
      return new CaretPosition(0);
    }

    // Metnods in fake implemetation (only available to tests)
    public int getPosition() {
      return position; 
    }

  }
}

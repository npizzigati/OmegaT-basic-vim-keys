/* :name=Basic Vim Keys :description=Basic Vim keys for OmegaT editor pane
 *
 * @author: Nicholas Pizzigati
 */

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import org.omegat.gui.editor.IEditor;
import org.omegat.gui.editor.EditorTextArea3;
import org.omegat.gui.editor.EditorController;

class InterruptException extends Exception {
  InterruptException(){
    super();
  }

  InterruptException(String message){
    super(message);
  }
}

class Listener implements KeyListener {

  char keyChar;
  EditorTextArea3 pane;
  KeyEvent lastKeyPressed;
  KeyEvent lastKeyTyped;
  KeyManager manager;
  Stroke stroke;

  Listener(EditorController editor,
           EditorTextArea3 pane) {
    this.pane = pane;
    println 'Listener initialized';
    lastKeyPressed = null;
    lastKeyTyped = null;
    manager = new KeyManager(editor, pane);
    startListening();
  }

  void startListening() {
    pane.addKeyListener(this);
    println 'Listening now';
  }

  void stopListening () {
    pane.removeKeyListener(this);
  }

  void keyPressed(KeyEvent event) {
    if(isRedispatchedEvent(event)) {
      println 'Redispatched event';
      return; //This will pass on event to pane
    }

    lastKeyPressed = event;
    println "Key press event: "
    print event.getKeyCode();

    event.consume();
  }

  void keyTyped(KeyEvent event) {
    if(isRedispatchedEvent(event)) {
    return;
    }
    lastKeyTyped = event;
    // Since the keyTyped event comes after the keyPressed event
    // both of the respective instance variables are available
    // when stroke is instantiated
    stroke = new Stroke(lastKeyPressed, lastKeyTyped)
    try {
      manager.route(stroke);
    } catch (InterruptException e) {
      stopListening()
      println e.message
    }
    event.consume();
  }

  void keyReleased(KeyEvent releasedEvent) {
    releasedEvent.consume();
  }

  boolean isRedispatchedEvent(KeyEvent event) {
    KeyEvent lastConsumedEvent = (event.getID() == KeyEvent.KEY_PRESSED) ? lastKeyPressed : lastKeyTyped;
    ((lastConsumedEvent != null) && (lastConsumedEvent.getWhen() == event.getWhen()))
  }
}

class Stroke {
  KeyEvent keyPressed;
  KeyEvent keyTyped;

  Stroke(keyPressed, keyTyped) {
    this.keyPressed = keyPressed;
    this.keyTyped = keyTyped;
  }

}

class Mode {

  KeyManager manager;

  enum ModeID {
    NORMAL, INSERT, VISUAL;
  }

  Mode(KeyManager manager) {
    this.manager = manager;
  }


  void process(Stroke stroke) {
  }
}

class NormalMode extends Mode {

  NormalMode(KeyManager manager) {
    super(manager);
  }

  void process(Stroke stroke) {
    char keyChar = stroke.keyTyped.keyChar;
    switch (keyChar) {
      case 'i':
        manager.switchTo(ModeID.INSERT);
        break;
      case 'h':
        manager.moveCaret(-1)
        break;
    }
  }
}

class InsertMode extends Mode {

  InsertMode(KeyManager manager) {
    super(manager);
  }

  void process(Stroke stroke) {
    char keyChar = stroke.keyTyped.keyChar;
    if ((int)keyChar == 27) {
      manager.switchTo(ModeID.NORMAL)
    } else {
      manager.redispatchEvent(stroke.keyTyped);
    }
  }
}

class VisualMode extends Mode {
  VisualMode(KeyManager manager) {
    super(manager);
  }
}

class KeyManager {

  Mode normalMode;
  Mode insertMode;
  Mode visualMode;
  Mode currentMode;
  char keyChar;
  EditorController editor;
  EditorTextArea3 pane;

  KeyManager(EditorController editor, EditorTextArea3 pane) {
    this.editor = editor;
    this.pane = pane;
    normalMode = new NormalMode(this);
    insertMode = new InsertMode(this);
    visualMode = new VisualMode(this);
    currentMode = normalMode;
  }

  void switchTo(modeID) {
    switch(modeID) {
      case modeID.NORMAL:
        currentMode = normalMode;
        println('Switching to normal mode')
        break;
      case modeID.INSERT:
        currentMode = insertMode;
        println('Switching to insert mode')
        break;
      case modeID.VISUAL:
        currentMode = visualMode;
        println('Switching to visual mode')
        break;
    }
  }

  void route(Stroke stroke) {
    // put stroke in some kind of history?


    // Temporary debugging stuff
    keyChar = stroke.keyTyped.getKeyChar();
    int keyWhen = stroke.keyTyped.getWhen();
    int modifiers = stroke.keyTyped.getModifiers();
    int keyCode = stroke.keyTyped.getKeyCode();
    int id = stroke.keyTyped.getID();

    println "KeyTyped info";
    println "keyChar: $keyChar";
    println "When: $keyWhen";
    println "Modifiers: $modifiers";
    println "keyCode: $keyCode";
    println "ID: $id";
    println "" 

    int pKeyChar = stroke.keyPressed.getKeyChar();
    int pKeyWhen = stroke.keyPressed.getWhen();
    int pModifiers = stroke.keyPressed.getModifiers();
    int pKeyCode = stroke.keyPressed.getKeyCode();
    int pId = stroke.keyPressed.getID();
    println "KeyPressed info";
    println "keyChar: $pKeyChar";
    println "When: $pKeyWhen";
    println "Modifiers: $pModifiers";
    println "keyCode: $pKeyCode";
    println "ID: $pId";
    println "" 

    // End temporary debugging stuff


    if (keyChar == 'q') {
      throw new InterruptException('Listening interrupted');
    }

    if (isNotVimKey(keyChar)) {
      redispatchEvent(stroke.keyPressed);
    } else {
      currentMode.process(stroke);
    }
    // } else if((int)keyChar == 27) {
    //   enterNormalMode();
    //   println "Entering normal mode";
    // } else if(keyChar == 't') {
    //   testSelection();
    //   println "testing selection";
    // } else {
    //   redispatchEvent(stroke.keyTyped);
    // }
  }

  // The delete key doesn't seem to show up as CHAR_UNDEFINED, so
  // we check for it separately
  boolean isNotVimKey(char keyChar) {
    (keyChar == KeyEvent.CHAR_UNDEFINED) || isDelete(keyChar);
  }

  boolean isDelete(char keyChar) {
    (int)keyChar == 127;
  }

  void testSelection() {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    int positionChange = 2;
    IEditor.CaretPosition caret = new IEditor.CaretPosition(currentPos,
                                            currentPos + positionChange);
    editor.setCaretPosition(caret);
  }

  void moveCaret(int positionChange) {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    println "currentPos = $currentPos"
    IEditor.CaretPosition caretPosition = new IEditor.CaretPosition(currentPos +
                                                            positionChange);
    editor.setCaretPosition(caretPosition);
  }

  void redispatchEvent(KeyEvent event) {
    pane.dispatchEvent(new KeyEvent(pane, event.getID(),
                                    event.getWhen(),
                                    event.getModifiers(),
                                    event.getKeyCode(),
                                    event.getKeyChar()));
  }
}

if (binding.hasVariable('testing')) { 
  editor = new EditorController();
  testWindow.setup(editor.editor);
}

new Listener(editor, editor.editor);

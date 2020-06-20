/* :name=Basic Vim Keys :description=Basic Vim keys for OmegaT editor pane
 *
 * @author: Nicholas Pizzigati
 */

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JEditorPane;
import javax.swing.PopupFactory;
import javax.swing.Popup;
import javax.swing.SwingUtilities;
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
    println "Key press event: $event.keyChar"
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

  void execute(Stroke stroke) {
  }
}

class NormalMode extends Mode {

  Map<String, String> remaps
  Map<String, String> remapCandidates

  NormalMode(KeyManager manager) {
    super(manager);

    // Temporary: put remaps into remaps map
    // These will eventually be entered by the user
    remaps = [a: 'b', b: 'c']
  }

  void process(Stroke stroke) {
    char keyChar = stroke.keyTyped.keyChar;

    // Handle remaps
    // remapCandidates = remaps.findAll { k, v ->
    //   // key.startsWith(keyChar)
    // }

    // remaps.each { k, v ->
    //   if (keyChar == k) {
    //     keyChar = v
    //   }
    // }

    switch (keyChar) {
      case 'i':
        manager.switchTo(ModeID.INSERT);
        break;
      case 'h':
        manager.moveCaret(-1)
        break;
      case 'l':
        manager.moveCaret(1)
        break;
    }
  }
}

class InsertMode extends Mode {

  Map<String, String> remaps;
  Map<String, String> remapCandidates;
  List<Character> accumulatedKeys;
  String remapMatch;
  List<Character> redispatchQueue;
  Long lastKeypressTime;
  Thread remapTimeoutThread;
  static final int REMAP_TIMEOUT = 900;

  InsertMode(KeyManager manager) {
    super(manager);
    resetAccumulatedKeys();

    // Temporary: put remaps into remaps map
    // These will eventually be entered by the user
    remaps = [ab: 'ba', bc: 'cb'];
    resetRemapCandidates();
    redispatchQueue = [];
  }

  class RemapTimeoutThread extends Thread {
    long cutoff = lastKeypressTime + REMAP_TIMEOUT;

    public void run() {
      while (System.currentTimeMillis() < cutoff) {
        if (Thread.interrupted()) return;
      }

      println 'Timeout'
      redispatchQueue = accumulatedKeys.toList();
      invokeLaterRedispatch(redispatchQueue.clone());
      resetAccumulatedKeys();
    }
  }

  void process(Stroke stroke) {
    char keyChar = stroke.keyTyped.keyChar;

    if (redispatchQueue) {
      execute(stroke);
      redispatchQueue.pop();
      return;
    }

    if (isRemapTimeoutThreadRunning())
      remapTimeoutThread.interrupt();

    accumulatedKeys = accumulatedKeys << keyChar;
    remapCandidates = findRemapCandidates();

    if (remapCandidates) {
      handlePossibleRemapMatch();
    } else {
      refireNonRemappedKeys();
    }
  }

  void runRemapTimeoutThread() {
    lastKeypressTime = System.currentTimeMillis();
    remapTimeoutThread = new RemapTimeoutThread();
    remapTimeoutThread.start();
  }

  void invokeLaterRedispatch(List queue) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        manager.batchRedispatchKeys(queue);
      }
    });
  }

  void handlePossibleRemapMatch() {
      runRemapTimeoutThread();
      remapMatch = remaps[accumulatedKeys.join()];

      if (remapMatch) {
        resetAccumulatedKeys();
        redispatchQueue = remapMatch.split('').toList();
        manager.batchRedispatchKeys(redispatchQueue.clone());
      }
  }

  void refireNonRemappedKeys() {
    redispatchQueue = accumulatedKeys;
    manager.batchRedispatchKeys(redispatchQueue.clone());
    resetAccumulatedKeys();
  }

  void resetRemapCandidates() {
    remapCandidates = [:];
  }

  void resetAccumulatedKeys() {
    accumulatedKeys = [];
  }

  void execute(Stroke stroke) {
    char keyChar = stroke.keyTyped.keyChar;

    if ((int)keyChar == KeyEvent.VK_ESCAPE) {
      manager.switchTo(ModeID.NORMAL)
    } else {
      manager.redispatchEvent(stroke.keyTyped);
    }
  }

  Map<String, String> findRemapCandidates() {
    String joinedAccumulatedKeys = accumulatedKeys.join();
    remaps.findAll { k, v ->
      k.startsWith(joinedAccumulatedKeys);
    }
  }

  boolean isRemapTimeoutThreadRunning() {
    remapTimeoutThread && remapTimeoutThread.isAlive()
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

  void route(Stroke stroke) throws InterruptException {
    // put stroke in some kind of history?

    keyChar = stroke.keyTyped.keyChar;

    if (keyChar == 'q') {
      throw new InterruptException();
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

  void batchRedispatchKeys(List queue) {
    queue.each {
      KeyEvent event = createEvent(it as char);
      Thread.sleep(1); // Prevent KeyEvents being created with the same time
      redispatchEvent(event);
    }
  }

  KeyEvent createEvent(char keyChar) {
    int id = KeyEvent.KEY_TYPED;
    long when = System.currentTimeMillis();
    int modifiers = 0;
    int keyCode = 0;
    return new KeyEvent(pane, id, when, modifiers,
                        keyCode, keyChar);
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

println binding.variables
new Listener(editor, editor.editor);

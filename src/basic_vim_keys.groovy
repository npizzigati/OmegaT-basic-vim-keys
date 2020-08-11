/* :name=Basic Vim Keys :description=Basic Vim keys for OmegaT editor pane
 *
 * @author: Nicholas Pizzigati
 */

// Implement yank -- have the yank be reflected in the clipboard (but have this be an option)

// Don't do weird stuff if non-implemented keys are pressed in normal mode

// Cursor shape

// GoForwardTo and GoForwardTill are the same method. GoForwardTo
// with Operator.DELETE should delete up to and including the
// specified letter, etc.

// Can I make normal-mode cursor a block?

// Need to implement registerInsertAfter method
// Need to make fake editor.insertText method

// Related to the above, probably, delete word doesn't delete the
// final character in a segment.

// Convert java regex to groovy regex in normal mode w method

// Too much casting in mode functions. Should I have a hash with
// the different char values, e.g. Letters['a'] instead of (int)'a' 

// When action keys don't match in normal mode, they just keep on
// accumulating. This needs to reset somehow


import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.JFrame;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import org.omegat.gui.editor.IEditor;
import org.omegat.gui.editor.EditorTextArea3;
import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.EditorSettings;

class InterruptException extends Exception {
  InterruptException(){
    super();
  }

  InterruptException(String message){
    super(message);
  }
}

enum ModeID {
  NORMAL, INSERT, VISUAL, OPERATOR_PENDING;
}

enum Operator {
  DELETE, CHANGE, YANK, NONE;
}

class Listener implements KeyListener {
  // char keyChar;
  EditorTextArea3 pane;
  EditorSettings editorSettings;
  KeyEvent lastKeyPressed;
  KeyEvent lastKeyTyped;
  KeyEvent lastConsumedKeyPressed;
  KeyEvent lastConsumedKeyTyped;
  KeyManager keyManager;
  Stroke stroke;
  
  static final FAKE_REDISPATCH_DELAY = 50;

  Listener(EditorController editor,
           EditorTextArea3 pane) {
    this.pane = pane;
    lastKeyPressed = null;
    lastKeyTyped = null;
    keyManager = new KeyManager(editor, pane);
    editorSettings = editor.getSettings();
    startListening();
  }

  void startListening() {
    pane.addKeyListener(this);
    println 'Listening now';
  }

  void stopListening () {
    pane.removeKeyListener(this);
  }

  void storeLastConsumed() {
    lastConsumedKeyPressed = lastKeyPressed;
    lastConsumedKeyTyped = lastKeyTyped;
  }

  void resetKeyEvents() {
    lastKeyPressed = null
    lastKeyTyped = null
  }

  // OmegaT consumes the KeyPressed event when the
  // "Advance on tab" option is selected, but check for it
  // below (isIgnoredTab) in case that implementation is changed
  // in the future
  void keyPressed(KeyEvent event) {
    if(isRedispatchedEvent(event) || event.isActionKey() || isIgnoredTab(event)) {
      println "\nLetting keyPressed event ${event.getKeyCode()} pass through"
      println "keyPressed time: ${event.getWhen()}";
      return; //This will allow event to pass on to pane
    }

    lastKeyPressed = event;
    println "\nkeyPressed time: ${event.getWhen()}";

    if(lastKeyTyped) {
      stroke = new Stroke(lastKeyPressed, lastKeyTyped);
      storeLastConsumed();
      resetKeyEvents();
      try {
        keyManager.route(stroke);
      } catch (InterruptException e) {
        stopListening();
        println e.message;
      }
    }
    event.consume();
  }

  void keyTyped(KeyEvent event) {
    if(isRedispatchedEvent(event) || isIgnoredTab(event)) {
      println "Letting keyTyped event pass through"
      println "keyTyped time: ${event.getWhen()}";
      return;
    }

    lastKeyTyped = event;
    println "keyTyped time: ${event.getWhen()}";

    // If remap dispatch underway, there will be no keyPressed event
    if(lastKeyPressed || keyManager.isRemapDispatchUnderway) {
      stroke = new Stroke(lastKeyPressed, lastKeyTyped);
      storeLastConsumed();
      resetKeyEvents();
      try {
        keyManager.route(stroke);
      } catch (InterruptException e) {
        stopListening();
        println e.message;
      }
    }
    event.consume();
  }

  void keyReleased(KeyEvent releasedEvent) {
    releasedEvent.consume();
  }

  boolean isRedispatchedEvent(KeyEvent event) {
    KeyEvent lastConsumedEvent = (event.getID() == KeyEvent.KEY_PRESSED) ? lastConsumedKeyPressed
                                                                         : lastConsumedKeyTyped;
    (lastConsumedEvent != null) && isSameEvent(event, lastConsumedEvent);
  }

  boolean isIgnoredTab(KeyEvent event) {
    ((int)event.getKeyChar() == 9) && editorSettings.isUseTabForAdvance()
  } 

  boolean isSameEvent(event, lastConsumedEvent) {
    // Redispatched event is set to a fraction of a second earlier than
    // original event to avoid repeating keys (like when holding
    // key down) from being considered redispatched events
    (lastConsumedEvent.getWhen() == (event.getWhen() + FAKE_REDISPATCH_DELAY)
     && (lastConsumedEvent.getKeyChar() == event.getKeyChar()));
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

abstract class Mode {
  KeyManager keyManager;
  // userEnteredRemaps is temporary -- these will be retrieved
  // from user or configuration file
  Map userEnteredRemaps;
  Map remaps;
  Map remapCandidates;
  Map accumulatedStrokes;
  List remapDispatchQueue;
  List strokeDispatchQueue;
  Long lastKeypressTime;
  Thread remapTimeoutThread;
  static final VK_KEYS = ['<esc>': KeyEvent.VK_ESCAPE, '<bs>': KeyEvent.VK_BACK_SPACE];


  Mode(KeyManager keyManager) {
    this.keyManager = keyManager;
    resetAccumulatedStrokes();
    resetRemapCandidates();
    remapDispatchQueue = [];
    strokeDispatchQueue = [];
  }

  class RemapTimeoutThread extends Thread {
    long cutoff = lastKeypressTime + REMAP_TIMEOUT;

    public void run() {
      while (System.currentTimeMillis() < cutoff) {
        if (Thread.interrupted()) return;
      }

      strokeDispatchQueue = accumulatedStrokes.values() as List;
      invokeLaterDispatch(strokeDispatchQueue.clone());
      resetAccumulatedStrokes();
    }
  }

  void process(Stroke stroke) {
    if (strokeDispatchQueue) {
      println "dispatching from strokeDispatchQueue";
      execute(stroke);
      strokeDispatchQueue.pop();
      return;
    }

    if (isRemapTimeoutThreadRunning())
      remapTimeoutThread.interrupt();

    int key = stroke.keyTyped.getKeyChar();

    accumulatedStrokes << [(key): stroke];
    remapCandidates = findRemapCandidates();

    if (remapCandidates) {
      handlePossibleRemapMatch();
    } else {
      println "refiring non-remapped stroke";
      refireNonRemappedStrokes();
    }
  }

  void startRemapTimeoutThread() {
    lastKeypressTime = System.currentTimeMillis();
    remapTimeoutThread = new RemapTimeoutThread();
    remapTimeoutThread.start();
  }

  void invokeLaterDispatch(List queue) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        keyManager.batchRedispatchStrokes(queue);
      }
    });
  }

  void handlePossibleRemapMatch() {
      startRemapTimeoutThread();

      def remapMatch = remaps[accumulatedStrokes.keySet() as List];
      if (remapMatch) {
        resetAccumulatedStrokes();
        resetRemapCandidates();
        remapTimeoutThread.interrupt();
        keyManager.dispatchRemapMatchKeys(remapMatch.clone());
      }
  }

  List tokenizeRemapString(String remapString) {
    Pattern pattern = Pattern.compile("(<\\w+>|\\w)");
    Matcher matcher = pattern.matcher(remapString);
    List<String> results = [];
    while (matcher.find()) {
      results << translateKey(matcher.group(1));
    }
    return results;
  }

  Map tokenizeUserEnteredRemaps() {
    Map tokenizedRemaps = [:];
    userEnteredRemaps.each { k, v ->
      tokenizedRemaps[tokenizeRemapString(k)] = tokenizeRemapString(v);
    }
    return tokenizedRemaps;
  }

  int translateKey(key) {
    return VK_KEYS[key.toLowerCase()] ?: (int)key;
  }

  void refireNonRemappedStrokes() {
    strokeDispatchQueue = (strokeDispatchQueue << accumulatedStrokes.values()).flatten();
    keyManager.batchRedispatchStrokes(strokeDispatchQueue.clone());
    resetAccumulatedStrokes();
  }

  void resetRemapCandidates() {
    remapCandidates = [:];
  }

  void resetAccumulatedStrokes() {
    accumulatedStrokes = [:];
  }

  Map findRemapCandidates() {
    remaps.findAll { k, v ->
      int candidate_size = accumulatedStrokes.size();
      int end_idx = candidate_size - 1;
      println "acc strokes keyset: ${accumulatedStrokes.keySet()}";
      println "remapped keys matching size: ${k[0..end_idx]}";
      k[0..end_idx] == accumulatedStrokes.keySet() as List;
    }
  }

  boolean isRemapTimeoutThreadRunning() {
    remapTimeoutThread && remapTimeoutThread.isAlive()
  }

  abstract void execute(Stroke stroke);
}

class NormalMode extends Mode {
  static final int REMAP_TIMEOUT = 1000;
  int keyChar;
  int previousChar;

  NormalMode(KeyManager keyManager) {
    super(keyManager);
    userEnteredRemaps = [:];
    remaps = tokenizeUserEnteredRemaps();
  }

  void execute(Stroke stroke) {
    keyChar = (int)stroke.keyTyped.getKeyChar();
    if (isToOrTill()) {
      keyManager.registerActionKey((char)keyChar);
    } else if (keyChar == (int)'i') {
      keyManager.switchTo(ModeID.INSERT);
    } else if (keyChar == (int)'d') {
      keyManager.switchTo(ModeID.OPERATOR_PENDING);
      keyManager.setOperator(Operator.DELETE);
    } else if (keyChar == (int)'c') {
      keyManager.switchTo(ModeID.OPERATOR_PENDING);
      keyManager.setOperator(Operator.CHANGE);
    } else if (keyChar == (int)'y') {
      keyManager.switchTo(ModeID.OPERATOR_PENDING);
      keyManager.setOperator(Operator.YANK);
    } else if (/[0-9wlhPpftx]/ =~ (char)keyChar) {
      println "pass to action keyChar: $keyChar"
      keyManager.registerActionKey((char)keyChar);
    }
    previousChar = keyChar;
  }

  boolean isToOrTill() {
    [(int)'f', (int)'F', (int)'t', (int)'T'].contains(previousChar);
  }
}

class OperatorPendingMode extends Mode {
  static final int REMAP_TIMEOUT = 900; // In milliseconds
  Operator operator;
  int keyChar;
  int previousChar;

  OperatorPendingMode(KeyManager keyManager) {
    super(keyManager);
    userEnteredRemaps = [:];
    remaps = tokenizeUserEnteredRemaps();
  }

  void execute(Stroke stroke) {
    keyChar = (int)stroke.keyTyped.getKeyChar();

    if (isToOrTill()) {
      keyManager.registerActionKey((char)keyChar);
    } else {
      keyManager.registerActionKey((char)keyChar);
    }
    previousChar = keyChar;
  }

  boolean isToOrTill() {
    [(int)'f', (int)'F', (int)'t', (int)'T'].contains(previousChar);
  }
}

class InsertMode extends Mode {
  static final int REMAP_TIMEOUT = 20; // In milliseconds

  InsertMode(KeyManager keyManager) {
    super(keyManager);
    userEnteredRemaps = ['ei': '<Esc>', 'ie': '<Esc>'];
    remaps = tokenizeUserEnteredRemaps();
  }

  void execute(Stroke stroke) {
    if ((int)stroke.keyTyped.getKeyChar() == KeyEvent.VK_ESCAPE) {
      keyManager.switchTo(ModeID.NORMAL)
    } else {
      keyManager.redispatchStrokeToPane(stroke);
    }
  }
}

class VisualMode extends Mode {
  VisualMode(KeyManager keyManager) {
    super(keyManager);
    userEnteredRemaps = [:];
    remaps = tokenizeUserEnteredRemaps();
  }

  void execute(Stroke stroke) {
    switch (keyChar) {
      case 'i':
        keyManager.switchTo(ModeID.INSERT);
        break;
      case 'h':
        keyManager.moveCaret(-1)
        break;
      case 'l':
        keyManager.moveCaret(1)
        break;
    }
  }
}

class KeyManager {
  Mode normalMode;
  Mode insertMode;
  Mode visualMode;
  Mode operatorPendingMode;
  Mode currentMode;
  Operator operator;
  ActionManager actionManager;
  boolean isRemapDispatchUnderway;
  EditorController editor;
  static EditorTextArea3 pane;

  KeyManager(EditorController editor, EditorTextArea3 editorPane) {
    this.editor = editor;
    pane = editorPane;
    normalMode = new NormalMode(this);
    insertMode = new InsertMode(this);
    visualMode = new VisualMode(this);
    operatorPendingMode = new OperatorPendingMode(this);
    actionManager = new ActionManager(this, editor);
    currentMode = normalMode;
  }

  void switchTo(ModeID modeID) {
    // If swiching from operator pending mode, reset operator
    // to none
    if (operator != Operator.NONE) {
        operator = Operator.NONE;
    }
    switch(modeID) {
      case ModeID.NORMAL:
        currentMode = normalMode;
        println('Switching to normal mode')
        break;
      case ModeID.INSERT:
        currentMode = insertMode;
        println('Switching to insert mode')
        break;
      case ModeID.VISUAL:
        currentMode = visualMode;
        println('Switching to visual mode')
        break;
      case ModeID.OPERATOR_PENDING:
        currentMode = operatorPendingMode;
        println('Switching to operator pending mode')
        break;
    }
  }

  ModeID getCurrentModeID() {
    switch(currentMode) {
      case normalMode:
        return ModeID.NORMAL;
      case insertMode:
        return ModeID.INSERT;
      case visualMode:
        return ModeID.VISUAL;
      case operatorPendingMode:
        return ModeID.OPERATOR_PENDING;
    }
  }

  void route(Stroke stroke) throws InterruptException {
    int key = stroke.keyTyped.getKeyChar();

    if ((char)key == 'q') {
      throw new InterruptException();
    }

    // Immediately redispatch all keys with ctrl modifier
    // as well as delete (not used in vim)
    // May want to change this to be able to use ctrl key in vim
    if ((isDelete(key)) || ctrlPressed(stroke)) {
      redispatchStrokeToPane(stroke);
    } else {
      currentMode.process(stroke);
    }
  }

  void registerActionKey(char actionKey) {
    actionManager.processActionKey(actionKey);
  }

  void setOperator(Operator operator) {
    this.operator = operator 
  }

  Operator getOperator() {
    operator
  }

  boolean ctrlPressed(Stroke stroke) {
    if (!!(stroke.keyPressed)) {
      return ((stroke.keyPressed.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0);
    } else {
      return false
    }
  }

  boolean isDelete(int key) {
    key == 127;
  }

  void batchRedispatchStrokes(List queue) {
    queue.each {
      // For some reason, keyPressed events are not dispatched for
      // delete and escape if I don't create new event

      // If remap dispatch underway, there will be no keyPressed
      // so no KeyPressed to redispatch
      redispatchStrokeForProcessing(it);
      // if (it.keyPressed) {
      //   redispatchEventForProcessing(it.keyPressed);
      // }
      // redispatchEventForProcessing(it.keyTyped);
    }
  }

  void redispatchStrokeForProcessing(Stroke stroke) {
    if (stroke.keyPressed) {
      redispatchEventForProcessing(stroke.keyPressed);
    }
    redispatchEventForProcessing(stroke.keyTyped);
  }

  void dispatchRemapMatchKeys(List remapMatch) {
    isRemapDispatchUnderway = true;
    remapMatch.each {
      KeyEvent keyTypedEvent = createKeyTypedEvent(it);
      pane.dispatchEvent(keyTypedEvent);
      Thread.sleep(10); // Is this necessary? Prevent KeyEvents being created with the same time
    }
    isRemapDispatchUnderway = false;
  }
  
  KeyEvent createKeyTypedEvent(int key) { // this creates a typed event from a key
    int id = KeyEvent.KEY_TYPED;
    int keyCode = 0;
    char keyChar = key;
    long when = System.currentTimeMillis();
    int modifiers = 0;
    return new KeyEvent(pane, id, when, modifiers,
                        keyCode, keyChar);
  }

  void redispatchEventToPane(KeyEvent event) {
    // Redispatched event is set to a fraction of a second earlier than
    // original event to avoid repeating keys (like when holding
    // key down) from being considered redispatched events
    pane.dispatchEvent(new KeyEvent(pane, event.getID(),
                                    event.getWhen() - Listener.FAKE_REDISPATCH_DELAY,
                                    event.getModifiers(),
                                    event.getKeyCode(),
                                    event.getKeyChar()));
  }

  
  void redispatchEventForProcessing(KeyEvent event) {
    pane.dispatchEvent(new KeyEvent(pane, event.getID(),
                                    event.getWhen(),
                                    event.getModifiers(),
                                    event.getKeyCode(),
                                    event.getKeyChar()));
  }

  void redispatchStrokeToPane(Stroke stroke) {
    println "Redispatching stroke: (keyTyped: ${(int)(stroke.keyTyped.getKeyChar())})\n";
    redispatchEventToPane(stroke.getKeyPressed());
    redispatchEventToPane(stroke.getKeyTyped());
  }

}

class ActionManager {
  String actionKeys;
  KeyManager keyManager;
  EditorController editor;
  Register register;

  ActionManager(KeyManager keyManager, EditorController editor) {
    this.keyManager = keyManager;
    this.editor = editor;
    this.register = new Register();
  }

  void processActionKey(char actionKey) {
    actionKeys = (actionKeys != null) ? actionKeys += actionKey : actionKey;
    println "actionKeys: $actionKeys"
    String nonCountKeys = removeCountKeys(actionKeys);
    println "nonCountKeys: $nonCountKeys"

    Map actions = [(/^w$/):    { cnt -> moveByWord(cnt) },
                   (/^l$/):    { cnt -> moveCaret(cnt) },
                   (/^h$/):    { cnt -> moveCaret(-cnt) },
                   (/^P$/):    { cnt -> registerInsertBefore(cnt) },
                   (/^p$/):    { cnt -> registerInsertAfter(cnt) },
                   (/^0$/):    { moveToLineStart() },
                   (/^\$$/):   { moveToLineEnd() },
                   (/^f.$/):   { cnt, key -> goForwardToChar(cnt, key) },
                   (/^t.$/):   { cnt, key -> goForwardTillChar(cnt, key) },
                   (/^x$/):    { cnt -> deleteChars(cnt) }]

    String match = actionMatch(actions, nonCountKeys);
    if (match) {
      int count = calculateCount(actionKeys);
      trigger(actions[match], count, nonCountKeys);
      actionKeys = ''
    }
  }

  String removeCountKeys(String actionKeys) {
    return actionKeys.replaceAll(/[1-9]|(?<=[1-9])0/, '')
  }

  int calculateCount(actionKeys) {
    int count = 1
    def matcher = actionKeys =~ /[0-9]+/
    int numberOfMatches = matcher.size()
    if (numberOfMatches == 1 && matcher[0] != '0')  {
      count = matcher[0].toInteger();
    } else if (numberOfMatches == 2) {
      count = matcher[0].toInteger() * matcher[1].toInteger();
    }
    return count
  }

  def actionMatch(Map actions, String nonCountKeys) {
    actions.keySet().find { mapKey -> nonCountKeys =~ mapKey }
  }

  void trigger(Closure action, int count, String nonCountKeys) {
    String targetKey = (nonCountKeys =~ /[tfTF]/) ? nonCountKeys[-1]
                                                  : null
    (targetKey) ? action.call(count, targetKey) : action.call(count);
    if (keyManager.getCurrentModeID() == ModeID.OPERATOR_PENDING) {
      keyManager.switchTo(ModeID.NORMAL)
    }
  }

  void testSelection() {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    int positionChange = 2;
    IEditor.CaretPosition caret = new IEditor.CaretPosition(currentPos,
                                            currentPos + positionChange);
    editor.setCaretPosition(caret);
  }

  void registerInsertBefore(int count) {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    String pullContent = register.getContent();
    editor.insertText(pullContent);
  }

  void registerInsertAfter(int count) {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    placeCaret(currentPos + 1);
    String pullContent = register.getContent();
    editor.insertText(pullContent);
  }

  void deleteChars(int number) {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    String text = editor.getCurrentTranslation();
    int length = text.length();

    int deleteStart = currentPos;
    int deleteEnd = currentPos + number

    if (deleteEnd > length) {
      deleteEnd = length
    }

    editor.replacePartOfText('', deleteStart, deleteEnd);
  }

  void deleteToPos(int currentPos, int newPos) {
    editor.replacePartOfText('', currentPos, newPos);
  }
  
  void changeToPos(int currentPos, int newPos) {
    deleteToPos(currentPos, newPos)
    keyManager.switchTo(ModeID.INSERT)
  }

  void selectToPos(int currentPos, int newPos) {
    IEditor.CaretPosition caretPosition = new IEditor.CaretPosition(currentPos,
                                                                    newPos);
    editor.setCaretPosition(caretPosition);
  }

  void placeCaret(int newPos) {
    IEditor.CaretPosition caretPosition = new IEditor.CaretPosition(newPos);
    editor.setCaretPosition(caretPosition);
  }

  void moveByWord(int number) {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    String text = editor.getCurrentTranslation();
    int length = text.length();

    String candidateRegex = '(?=[^\\p{L}\\d])(?=\\S)|((?<=[^\\p{L}\\d])[\\p{L}\\d])'
    Pattern pattern = Pattern.compile(candidateRegex);
    Matcher matcher = pattern.matcher(text);
    List matches = getMatches(text, candidateRegex);

    List candidates = matches.findAll { it > currentPos };
    int endIndex = (!!candidates) ? candidates[-1] : length
    int newPos = (candidates[number - 1]) ?: endIndex;

    executeGoForwardToOperation(currentPos, newPos, text)

  }

  void executeGoForwardToOperation(int currentPos, int newPos,
                                   String text) {
    if (keyManager.getOperator() == Operator.DELETE) {
      register.push(text[currentPos..(newPos - 1)]);
      deleteToPos(currentPos, newPos);
    } else if (keyManager.getOperator() == Operator.CHANGE) {
      register.push(text[currentPos..(newPos - 1)]);
      changeToPos(currentPos, newPos);
    } else if (keyManager.getOperator() == Operator.YANK) {
      register.push(text[currentPos..(newPos - 1)]);
    } else if (keyManager.getCurrentModeID() == ModeID.VISUAL) {
      selectToPos(currentPos, newPos);
    } else {
      placeCaret(newPos);
    }
  }

  boolean stopPositionIsSpace(String text, int stopPos, int length) {
    int index = (stopPos == length) ? stopPos - 1 : stopPos;
    (text[index] ==~ /\s/);
  }

  void goForwardToChar(int number, String key) {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    String text = editor.getCurrentTranslation();
    int length = text.length();
    String candidateRegex = key
    List matches = getMatches(text, candidateRegex);
    List candidates = matches.findAll { it > currentPos };

    int newPos = (candidates[number - 1]) ?: currentPos;

    executeGoForwardToOperation(currentPos, newPos, text)
  }

  void goForwardTillChar(int number, String key) {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    String text = editor.getCurrentTranslation();
    int length = text.length();
    String candidateRegex = key
    List matches = getMatches(text, candidateRegex);
    List candidates = matches.findAll { it > currentPos };

    int newPos = (candidates[number - 1]) ?: currentPos;

    executeGoForwardToOperation(currentPos, newPos)
  }

  List getMatches(String text, String candidateRegex) {
    Pattern pattern = Pattern.compile(candidateRegex);
    Matcher matcher = pattern.matcher(text);
    List matches = [];

    while (matcher.find()) {
      matches << matcher.start();
    }
    return matches;
  }

  void moveCaret(int positionChange) {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    int newPos = currentPos + positionChange;
    int lineLength = editor.getCurrentTranslation().length();

    if (newPos < 0) {
      newPos = 0;
    } else if (newPos > lineLength) {
      newPos = lineLength;
    }

    placeCaret(newPos);
  }

  void moveToLineStart() {
    IEditor.CaretPosition caretPosition = new IEditor.CaretPosition(0);
    editor.setCaretPosition(caretPosition);
  }

  void moveToLineEnd() {
    int endCaretPosition = editor.getCurrentTranslation().length();

    placeCaret(endCaretPosition);
  }
}

class Register {
  String content;

  void push(String yankContent) {
    content = yankContent
  }

  String getContent() {
    content;
  } 
}

if (binding.hasVariable('testing')) {
  editor = new EditorController();
  testWindow.setup(editor.editor);
}

new Listener(editor, editor.editor);

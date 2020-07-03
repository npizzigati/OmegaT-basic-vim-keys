/* :name=Basic Vim Keys :description=Basic Vim keys for OmegaT editor pane
 *
 * @author: Nicholas Pizzigati
 */

// TODO: When holding down h in normal mode, some h's  
// appear not to be processed -- they are output to the
// OmegaT editor pane. Update: this actually happens while
// holding down any key in normal mode
// FIXED: I fixed this by giving these immediately redispatched keys
// a fake time a fraction of a second earlier than the original event...
// Minor problems with the fix: architectural: pane is now a static variable
// in KeyManager but an instance variable in Listener -- can I make it a
// static variable in Listener, too, maybe along with editor?
// Maybe get rid of the createEventWithFakeDelay function altogether and
// Simply create another method in KeyManager for immediately redispatched
// Keys, and include the fake delay there. This would also enable me to
// customize the function I'm using for other (inoremap) events, to avoid
// creating an event twice

// Second potential problem: Are there more places where I need to redispatch
// keys with fake delay?

// Shift-F3 for change case does not get passed through

// Compose key characters seem not to work -- do I need to let all modifiers
// (except shift) through untouched, like ctrl?

// TODO: Sometimes when in normal mode, I have to press i
// twice to switch to normal mode.


 
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
  static final FAKE_REDISPATCH_DELAY = 50;

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
      println "redispatching event (keyChar: ${event.getKeyChar()})";
      return; //This will allow event to pass on to pane
    }

    lastKeyPressed = event;

    event.consume();
  }

  void keyTyped(KeyEvent event) {
    if(isRedispatchedEvent(event)) {
      println "redispatched event (keyChar: ${event.getKeyChar()}, time: ${event.getWhen()})";
      return;
    }
    println "new event (keyChar: ${event.getKeyChar()}, time: ${event.getWhen()})";
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
    ((lastConsumedEvent != null) && isSameEvent(event, lastConsumedEvent));
  }

  boolean isSameEvent(event, lastConsumedEvent) {
    // Redispatched event is set to a fraction of a second less than
    // original event to avoid repeating keys from being considered
    // redispatched events
    // (lastConsumedEvent.getWhen() == (event.getWhen() + FAKE_REDISPATCH_DELAY) && (lastConsumedEvent.getKeyChar() == event.getKeyChar()));
    (lastConsumedEvent.getWhen() == (event.getWhen() + FAKE_REDISPATCH_DELAY) && (lastConsumedEvent.getKeyChar() == event.getKeyChar()));
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
  KeyManager manager;
  // userEnteredRemaps is temporarily -- these will be retrieved
  // from user or configuration file
  Map userEnteredRemaps;
  Map remaps;
  Map remapCandidates;
  List accumulatedKeys;
  List<Character> redispatchQueue;
  Long lastKeypressTime;
  Thread remapTimeoutThread;
  static final VK_KEYS = ['<esc>': KeyEvent.VK_ESCAPE, '<bs>': KeyEvent.VK_BACK_SPACE];

  enum ModeID {
    NORMAL, INSERT, VISUAL;
  }

  Mode(KeyManager manager) {
    this.manager = manager;
    resetAccumulatedKeys();
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
    int key;
    int keyChar;

    if (redispatchQueue) {
      println "executing stroke ${stroke.keyTyped.keyChar}"
      execute(stroke);
      redispatchQueue.pop();
      return;
    }

    if (isRemapTimeoutThreadRunning())
      remapTimeoutThread.interrupt();

    keyChar = stroke.keyTyped.keyChar;
    if ((32..126).contains(keyChar)) {
      key = keyChar;
    } else {
      key = stroke.keyPressed.keyCode;
    }

    accumulatedKeys = accumulatedKeys << key;
    remapCandidates = findRemapCandidates();

    if (remapCandidates) {
      handlePossibleRemapMatch();
    } else {
      refireNonRemappedKeys();
    }
  }

  void startRemapTimeoutThread() {
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
      startRemapTimeoutThread();

      def remapMatch = remaps[accumulatedKeys];
      if (remapMatch) {
        resetAccumulatedKeys();
        resetRemapCandidates();
        remapTimeoutThread.interrupt();
        redispatchQueue = remapMatch.clone();
        manager.batchRedispatchKeys(redispatchQueue.clone());
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
    // List results = [];
    // remapMatch.each {
      // int translatedKey = VK_KEYS[it.toLowerCase()] ?: (int)it;
      // results << translatedKey;
    // }
    // return results;
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

  Map findRemapCandidates() {
    remaps.findAll { k, v ->
      // k.startsWith(accumulatedKeys);
      int candidate_size = accumulatedKeys.size();
      int end_idx = candidate_size - 1;
      k[0..end_idx] == accumulatedKeys;
    }
  }

  boolean isRemapTimeoutThreadRunning() {
    remapTimeoutThread && remapTimeoutThread.isAlive()
  }

  abstract void execute(Stroke stroke);
}

class NormalMode extends Mode {
  static final int REMAP_TIMEOUT = 1000;
  static final int CNT_RANGE_START = (int)'1';
  static final int CNT_RANGE_END = (int)'9';
  OperatorPendingMode operatorPendingMode;
  String count;
  int keyChar;
  enum ToOrTill {
    NONE, TO, TILL, TO_BACK, TILL_BACK;
  }
  ToOrTill toOrTill;


  NormalMode(KeyManager manager) {
    super(manager);
    userEnteredRemaps = [:];
    remaps = tokenizeUserEnteredRemaps();
    resetCount();
    operatorPendingMode = new OperatorPendingMode();
    toOrTill = ToOrTill.NONE;
  }

  boolean toOrTillPending() {
    toOrTill != ToOrTill.NONE;
  }

  void executeToOrTill(keyChar) {
    int number = count ? count.toInteger() : 1;
    switch (toOrTill) {
      case ToOrTill.TO:
        manager.goForwardToChar(keyChar, number);
        break;
    }
    toOrTill = ToOrTill.NONE;
    resetCount();
  }

  void execute(Stroke stroke) {
    keyChar = stroke.keyTyped.keyChar
    if (toOrTillPending()) {
      executeToOrTill(keyChar);
      return;
    }
    switch (keyChar) {
      case (int)'i':
        manager.switchTo(Mode.ModeID.INSERT);
        resetCount();
        break;
      case (int)'t':
        toOrTill = ToOrTill.TILL
        break;
      case (int)'f':
        toOrTill = ToOrTill.TO
        break;
      case (int)'h':
        int positionChange = count ? -(count.toInteger()) : -1;
        manager.moveCaret(positionChange);
        resetCount();
        break;
      case (int)'l':
        int positionChange = count ? count.toInteger() : 1;
        manager.moveCaret(positionChange);
        resetCount();
        break;
      case (int)'w':
        int number = count ? count.toInteger() : 1;
        manager.moveByWord(number);
        resetCount();
        break;
      case (int)'x':
        int number = count ? count.toInteger() : 1;
        manager.deleteChars(number);
        resetCount();
        break;
      case (int)'0':
        if (count == "") {
          manager.moveToLineStart();
        } else {
          count = count + (char)keyChar;
        }
        break;
      case (int)'$':
        manager.moveToLineEnd();
        break;
      case (CNT_RANGE_START..CNT_RANGE_END):
        println "countchar: $keyChar"
        count = count + (char)keyChar;
        println "count: $count";
        break;
    }
  }

  void resetCount() {
    count = "";
  }

  class OperatorPendingMode {
    void execute(Stroke stroke) {
      if((1..9).contains(stroke.keyTyped.keyChar)) {
        // Do something;
      }
    }
  }

}

class InsertMode extends Mode {
  static final int REMAP_TIMEOUT = 20; // In milliseconds

  InsertMode(KeyManager manager) {
    super(manager);
    userEnteredRemaps = ['ei': '<Esc>', 'ie': '<Esc>'];
    remaps = tokenizeUserEnteredRemaps();
  }

  void execute(Stroke stroke) {
    if ((int)stroke.keyTyped.keyChar == KeyEvent.VK_ESCAPE) {
      manager.switchTo(ModeID.NORMAL)
    } else if ((int)stroke.keyPressed.keyChar == KeyEvent.VK_BACK_SPACE) {
      KeyEvent eventWithFakeDelay = KeyManager.createEventWithFakeDelay(stroke.keyPressed);
      manager.redispatchEvent(eventWithFakeDelay);
    } else {
      KeyEvent eventWithFakeDelay = KeyManager.createEventWithFakeDelay(stroke.keyTyped);
      manager.redispatchEvent(eventWithFakeDelay);
    }
  }
}

class VisualMode extends Mode {

  VisualMode(KeyManager manager) {
    super(manager);
    userEnteredRemaps = [:];
    remaps = tokenizeUserEnteredRemaps();
  }

  void execute(Stroke stroke) {
    switch (keyChar) {
      case 'i':
        manager.switchTo(Mode.ModeID.INSERT);
        break;
      case 'h':
        manager.moveCaret(-1)
        break;
      case 'l':
        manager.moveCaret(1)
        break;
      case ('1'..'9'):
        // Store count in some sort of instance variable to
        // be used in operator mode
        break;
    }
  }
}


class KeyManager {
  Mode normalMode;
  Mode insertMode;
  Mode visualMode;
  Mode currentMode;
  char keyChar;
  EditorController editor;
  static EditorTextArea3 pane;

  KeyManager(EditorController editor, EditorTextArea3 editorPane) {
    this.editor = editor;
    pane = editorPane;
    normalMode = new NormalMode(this);
    insertMode = new InsertMode(this);
    visualMode = new VisualMode(this);
    currentMode = normalMode;
  }

  static KeyEvent createEventWithFakeDelay(event) {
    // Fake event is set to a fraction of a second less than
    // original event to avoid repeating keys from being considered
    // redispatched events
    KeyEvent eventWithFakeDelay = new KeyEvent(pane, event.getID(),
                                               event.getWhen() - Listener.FAKE_REDISPATCH_DELAY,
                                               event.getModifiers(),
                                               event.getKeyCode(),
                                               event.getKeyChar())
    return eventWithFakeDelay;
  }

  void switchTo(modeID) {
    switch(modeID) {
      case modeID.NORMAL: // Can this just be NORMAL?
        currentMode = normalMode;
        println('Switching to normal mode')
        break;
      case modeID.INSERT: // can this just be INSERT?
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

    // Immediately redispatch all keys with ctrl modifier
    // as well as all keys that are not used in vim
    // I may want to change this to be able to use ctrl key in vim
    if ((isNotVimKey(keyChar)) || ctrlPressed(stroke)) {
      KeyEvent eventWithFakeDelay = KeyManager.createEventWithFakeDelay(stroke.keyPressed);
      redispatchEvent(eventWithFakeDelay);
    } else {
      currentMode.process(stroke);
    }
  }


  boolean ctrlPressed(Stroke stroke) {
    ((stroke.keyPressed.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0);
  }

  // The delete key doesn't show up as CHAR_UNDEFINED, so
  // we check for it separately
  boolean isNotVimKey(char keyChar) {
    isUndefined(keyChar) || isDelete(keyChar);
  }

  boolean isDelete(char keyChar) {
    (int)keyChar == 127;
  }

  boolean isUndefined(char keyChar) {
    keyChar == KeyEvent.CHAR_UNDEFINED;
  }

  void testSelection() {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    int positionChange = 2;
    IEditor.CaretPosition caret = new IEditor.CaretPosition(currentPos,
                                            currentPos + positionChange);
    editor.setCaretPosition(caret);
  }

  void deleteChars(int number) {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    int deleteStart = currentPos;
    int deleteEnd = currentPos + number
    editor.replacePartOfText('', deleteStart, deleteEnd);
  }

  void moveByWord(int number) {
    // TODO: Fix stop on accented characters
    //       Should stop on puntuation (right?)
    //       When number of words goes beyond end, caret should
    //       go to end
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    String text = editor.getCurrentTranslation();
    int length = text.length();

    String candidateRegex = '(?=[^\\p{L}])(?=\\S)|((?<=[^\\p{L}])\\p{L})'
    Pattern pattern = Pattern.compile(candidateRegex);
    Matcher matcher = pattern.matcher(text);
    List matches = getMatches(text, candidateRegex);

    List candidates = matches.findAll { it > currentPos };
    int endIndex = (!!candidates) ? candidates[-1] : currentPos
    int newPos = (candidates[number - 1]) ?: endIndex;


    // This repeats in several methods --> extract to another method
    // Include logic to see if final index if within bounds
    IEditor.CaretPosition caretPosition = new IEditor.CaretPosition(newPos);
    editor.setCaretPosition(caretPosition);
  }

  boolean stopPositionIsSpace(String text, int stopPos, int length) {
    int index = (stopPos == length) ? stopPos - 1 : stopPos;
    (text[index] ==~ /\s/);
  }

  void goForwardToChar(int keyChar, int number) {
    int currentPos = editor.getCurrentPositionInEntryTranslation();
    String text = editor.getCurrentTranslation();
    int length = text.length();
    String candidateRegex = (char)keyChar
    List matches = getMatches(text, candidateRegex);
    List candidates = matches.findAll { it > currentPos };

    int newPos = (candidates[number - 1]) ?: currentPos;
    // This repeats in several methods --> extract to another method
    // Include logic to see if final index if within bounds
    IEditor.CaretPosition caretPosition = new IEditor.CaretPosition(newPos);
    editor.setCaretPosition(caretPosition);
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

    println "\nnewPos: $newPos\n";
    if (newPos < 0) {
      newPos = 0;
    } else if (newPos > lineLength) {
      newPos = lineLength;
    }

    IEditor.CaretPosition caretPosition = new IEditor.CaretPosition(newPos);
    editor.setCaretPosition(caretPosition);
  }

  void moveToLineStart() {
    IEditor.CaretPosition caretPosition = new IEditor.CaretPosition(0);
    editor.setCaretPosition(caretPosition);
  }

  void moveToLineEnd() {
    int endCaretPosition = editor.getCurrentTranslation().length();
    println "\nsegment length: $endCaretPosition\n";

    IEditor.CaretPosition caretPosition = new IEditor.CaretPosition(endCaretPosition);
    editor.setCaretPosition(caretPosition);
  }

  void batchRedispatchKeys(List queue) {
    queue.each {
      // KeyEvent event = createEvent(it as char);
      KeyEvent event = createEvent(it);
      Thread.sleep(1); // Prevent KeyEvents being created with the same time
      redispatchEvent(event);
    }
  }

  // KeyEvent createEvent(char keyChar) {
  KeyEvent createEvent(key) {
    int id;
    int keyCode;
    char keyChar;
    if ((32..126).contains(key) || [8, 27, 127].contains(key)) {
      keyChar = (char)key;
      keyCode = 0;
      id = KeyEvent.KEY_TYPED;
    } else {
      keyChar = KeyEvent.CHAR_UNDEFINED;
      keyCode = key;
      id = KeyEvent.KEY_PRESSED;
    }
    long when = System.currentTimeMillis();
    int modifiers = 0;
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

new Listener(editor, editor.editor);

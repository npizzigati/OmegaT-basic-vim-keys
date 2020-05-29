// :name=OmegaT Vim Keys :description=Vim Keys for OmegaT
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import org.omegat.gui.editor.IEditor.CaretPosition;

class MyNewException extends RuntimeException {

    def MyNewException(){
        super();
    }

    def MyNewException(String message){
        super(message);
    }
}

class VirtualSegment {
    String content;

    VirtualSegment(segment) {
            content = segment; 
    }

    String toString() {
            return content;
    }

}

class Listener {
    final String ENTER_INSERT_KEY = 'i' 
    final int ENTER_NORMAL_KEYCODE = 27

    enum Mode {
            NORMAL, INSERT
    }

    KeyEventDispatcher keyDispatcher;
    def console;
    def editor;
    def caret;
    String currentTrans;
    String insertion;
    String keyChar;
    int keyCode;
    int currentPos;
    int positionChange;
    int eventID;
    def newPos;
    Mode mode;
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

    Listener(console, editor) {
        this.console = console
        this.editor = editor
        mode = Mode.NORMAL
    }

    def listen() {
        keyDispatcher = new KeyEventDispatcher() {
            boolean dispatchKeyEvent(KeyEvent e) {
                try {
                    eventID = e.getID();
                    if(isKeyReleasedEvent(eventID)) {
                        console.println 'key released'
                        return true; // Stop key event processing
                    }
                    keyChar = e.getKeyChar();
                    // Pass thru keypresses from backspace and arrow keys
                    if(isKeyPressedEvent(eventID)) {
                        if(isNonEnglishKey(keyChar)) {
                            console.println 'pass through key pressed';
                            keyCode = e.getKeyCode();
                            console.println "pressed_code: $keyCode";
                            if(keyCode == ENTER_NORMAL_KEYCODE) {
                                enterNormalMode();
                            }
                        }
                        return false; // Pass on key
                    }
                    //check what's going on
                    if(isKeyPressedEvent(eventID)) {
                        console.print 'key pressed event: '
                    }
                    if(isKeyTypedEvent(eventID)) {
                        console.print 'key typed event: '
                    }

                    console.println "typed_char: $keyChar";

                    if(keyChar == 'q') {
                        throw new MyNewException('Interrupt');
                    }
                    currentPos = editor.getCurrentPositionInEntryTranslation();
                    // Need to pass keycode through to get backspace to work
                    if(mode == Mode.NORMAL) {
                        if(keyChar == ENTER_INSERT_KEY) {
                            enterInsertMode();
                            return true;
                        } 

                        positionChange = 0;
                        switch(keyChar) {
                            case 'h': 
                                positionChange = -1;
                                break;
                            case 'l': 
                                positionChange = 1;
                                break;
                            default:
                                break;
                        }
                        caret = new CaretPosition(currentPos + positionChange);		
                        editor.setCaretPosition(caret);	
                        return true;				
                    }

                    println "This will print ${e.getKeyChar()}"
                    return false;

                } catch (Exception exc) {
                    removeDispatcher();
                    console.println exc.getMessage();
                } finally {
                }
            }
        };
        manager.addKeyEventDispatcher(keyDispatcher);
    }

    def removeDispatcher() {
        manager.removeKeyEventDispatcher(keyDispatcher)
    }

    def isKeyPressedEvent(eventID) {
        return eventID == KeyEvent.KEY_PRESSED
    }

    def isKeyReleasedEvent(eventID) {
        return eventID == KeyEvent.KEY_RELEASED
    }

    def isKeyTypedEvent(eventID) {
        return eventID == KeyEvent.KEY_TYPED
    }

    def isNonEnglishKey(keyChar) {
        !('A'..'z').contains(keyChar)
    }

    def enterNormalMode() {
        mode = Mode.NORMAL
    }

    def enterInsertMode() {
        mode = Mode.INSERT
    }
}

def myListener = new Listener(console, editor);
myListener.listen();

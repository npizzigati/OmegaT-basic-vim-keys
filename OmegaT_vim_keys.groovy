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
    final ENTER_INSERT = (int)'i' 
    final ENTER_NORMAL = 27 //escape

    enum Mode {
            NORMAL, INSERT
    }

    KeyEventDispatcher keyDispatcher;
    def console;
    def editor;
    def caret;
    String currentTrans;
    String insertion;
    int asciiCode;
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
                eventID = e.getID();
                if isKeyReleasedEvent(eventID) {
                    console.println 'key released'
                    return true;
                }
                // Pass thru keypresses from backspace and arrow keys
                if isKeyPressedEvent(eventID)
                try {
                    asciiCode = e.getKeyChar();
                    console.println "char: $asciiCode";

                    if(asciiCode == (int)'q') {
                        throw new MyNewException('Interrupt');
                    } else if(asciiCode == ENTER_NORMAL) {
                        enterNormalMode();
                    } else if(asciiCode == ENTER_INSERT) {
                        enterInsertMode();
                        console.println 'Enter insert mode'
                        return true;
                    }
                    currentPos = editor.getCurrentPositionInEntryTranslation();
                    // Need to pass keycode through to get backspace to work
                    if(mode == Mode.NORMAL) {
                        positionChange = 0;
                        switch(asciiCode) {
                            case (int)'h': 
                                positionChange = -1;
                                    break;
                            case (int)'l': 
                                positionChange = 1;
                                    break;
                            default:
                                    break;
                        }
                        caret = new CaretPosition(currentPos + positionChange);		
                        editor.setCaretPosition(caret);	
                        return true;				
                    }

                    console.println "currentPos: $currentPos";
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

    def isNonTypedKey(e) {
        e.getKeyCode = 
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
	





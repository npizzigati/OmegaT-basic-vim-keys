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
    final ENTER_INSERT = 'i' 
    final ENTER_NORMAL = 27 

    enum Mode {
            NORMAL, INSERT
    }

    KeyEventDispatcher keyDispatcher;
    def console;
    def editor;
    def caret;
    String currentTrans;
    String insertion;
    int keyCode;
    String keyChar;
    int modifierCode;
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
                if(eventID == KeyEvent.KEY_RELEASED) {
                    console.println 'key released'
                    // e.consume();
                    return true; // Adding true here makes it so no characters are passed on to the screen
                }
                if(eventID == KeyEvent.KEY_TYPED) {
                    // console.println "key typed: e.keyChar "
                    // e.consume();
                    return (mode == Mode.NORMAL)
               }
                try {
                    keyCode = e.getKeyCode();
                    keyChar = e.getKeyChar();
                    modifierCode = e.getModifiersEx();
                    console.println "code: $keyCode";
                    console.println "modifier code: $modifierCode";
                    console.println "char: $keyChar";

                    if(modifierCode == 128) {
                        throw new MyNewException('Interrupt');
                    } else if(keyChar == ENTER_NORMAL) {
                        enterNormalMode();
                    } else if(keyChar == ENTER_INSERT) {
                        enterInsertMode();
                        console.println 'Enter insert mode'
                        // TODO: Need to not print the "i" when entering insert mode
                        return true;
                    }
                    currentPos = editor.getCurrentPositionInEntryTranslation();
                    if(mode == Mode.NORMAL) {
                        positionChange = 0;
                        switch(keyCode) {
                            case 72: 
                                positionChange = -1;
                                    break;
                            case 76: 
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

    def enterNormalMode() {
        mode = Mode.NORMAL
    }

    def enterInsertMode() {
        mode = Mode.INSERT
    }
}

def myListener = new Listener(console, editor);
myListener.listen();
	





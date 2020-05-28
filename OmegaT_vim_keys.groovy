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
                    console.println 'key_typed'
                    // e.consume();
                    return (mode == Mode.NORMAL)
               }
                try {
                    keyCode = e.getKeyCode();
                    modifierCode = e.getModifiersEx();
                    console.println keyCode;
                    console.println modifierCode;
                    console.println e.keyChar;

                    if(modifierCode == 128) {
                        throw new MyNewException('Interrupt');
                    } else if(keyCode == 27) {
                        enterNormalMode();
                    } else if(keyCode == 73) {
                        enterInsertMode();
                        console.println 'Enter insert mode'
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

                    // currentTrans = editor.getCurrentTranslation();
                    console.println "currentPos: $currentPos";
                    //editor.insertText('d');
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
	





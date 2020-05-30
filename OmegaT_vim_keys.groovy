 :name=OmegaT Vim Keys :description=Vim Keys for OmegaT

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import org.omegat.gui.editor.IEditor.CaretPosition;
import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.EditorTextArea3;

class MyNewException extends RuntimeException {

    MyNewException(){
        super();
    }

    MyNewException(String message){
        super(message);
    }
}

class Listener implements KeyListener {

    int keyCharValue;
    EditorController editor;
    EditorTextArea3 pane;
    KeyEvent lastConsumedEvent;

    Listener(EditorController editor, EditorTextArea3 pane) {
        pane.addKeyListener(this)
        this.editor = editor;
        this.pane = pane;  
        println 'Listener initialized'
        lastConsumedEvent = null;
        // this.editor.addKeyListener(this);
    }

    void keyPressed(KeyEvent e) {
        if(isRedispatchedEvent(e)) {
            return;
        }
        e.consume();
    }

    void keyTyped(KeyEvent e) {
        try {
            if(isRedispatchedEvent(e)) {
                return;
            }
            lastConsumedEvent = e
            keyCharValue = (int)e.getKeyChar();
            println keyCharValue
            if(e.getKeyChar() == 'q') {
                throw new MyNewException('Interrupt');
            }
            e.consume();
            pane.dispatchEvent(new KeyEvent(pane, e.getID(), e.getWhen(), e.getModifiers(),
                                            e.getKeyCode(), e.getKeyChar()));
        } catch (Exception exc) {
            println exc.getMessage();
            pane.removeKeyListener(this);
        } finally {
        }
    }


    /** Handle the key-released event from the text field. */
    void keyReleased(KeyEvent e) {
        e.consume();
    }

    boolean isRedispatchedEvent(e) {
      ((lastConsumedEvent != null) && (lastConsumedEvent.getWhen() == e.getWhen()))
    }
}

new Listener(editor, editor.editor);


    // private void displayInfo(KeyEvent e, String keyStatus){
        
    //     //You should only rely on the key char if the event
    //     //is a key typed event.
    //     int id = e.getID();
    //     String keyString;
    //     if (id == KeyEvent.KEY_TYPED) {
    //         char c = e.getKeyChar();
    //         keyString = "key character = '" + c + "'";
    //     } else {
    //         int keyCode = e.getKeyCode();
    //         keyString = "key code = " + keyCode
    //                 + " ("
    //                 + KeyEvent.getKeyText(keyCode)
    //                 + ")";
    //     }
        
    //     int modifiersEx = e.getModifiersEx();
    //     String modString = "extended modifiers = " + modifiersEx;
    //     String tmpString = KeyEvent.getModifiersExText(modifiersEx);
    //     if (tmpString.length() > 0) {
    //         modString += " (" + tmpString + ")";
    //     } else {
    //         modString += " (no extended modifiers)";
    //     }
    // }
// editor.editor.addKeyListener(myListener);
// myListener.listen();

// class MyNewException extends RuntimeException {

//     def MyNewException(){
//         super();
//     }

//     def MyNewException(String message){
//         super(message);
//     }
// }

// class VirtualSegment {
//     String content;

//     VirtualSegment(segment) {
//             content = segment; 
//     }

//     String toString() {
//             return content;
//     }

// }

// class Listener {
//     final String ENTER_INSERT_KEY = 'i' 
//     final int ENTER_NORMAL_KEYCODE = 27

//     enum Mode {
//             NORMAL, INSERT
//     }

//     KeyEventDispatcher keyDispatcher;
//     def console;
//     def editor;
//     def caret;
//     String currentTrans;
//     String insertion;
//     String keyChar;
//     int keyCode;
//     int currentPos;
//     int positionChange;
//     int eventID;
//     def newPos;
//     String key;
//     Mode mode;
//     KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

//     Listener(console, editor) {
//         this.console = console
//         this.editor = editor
//         mode = Mode.NORMAL
//     }

//     def listen() {
//         keyDispatcher = new KeyEventDispatcher() {
//             boolean dispatchKeyEvent(KeyEvent e) {
//                 try {
//                     eventID = e.getID();

//                     // Is the following block necessary or can we
//                     // just ignore the KEY_RELEASED event?
//                     if(isKeyReleasedEvent(eventID)) {
//                         return true;
//                     } else if(isKeyPressedEvent(eventID)) {
//                         keyCode = e.getKeyCode();
//                         // return true;
//                     }

//                     keyChar = e.getKeyChar();
//                     if(keyChar == KeyEvent.CHAR_UNDEFINED) {
//                         console.println 'Not a Unicode char'
//                         key = KeyEvent.getKeyText(keyCode);
//                     } else {
//                         key = keyChar
//                     }

//                     if(keyChar == 'q') {
//                         throw new MyNewException('Interrupt');
//                     }

//                     console.println "keyCode: $keyCode"
//                     console.println key
                    // Pass thru keypresses from backspace and arrow keys
                    // if(isKeyPressedEvent(eventID)) {
                    //     if(isNonEnglishKey(keyChar)) {
                    //         console.println 'pass through key pressed';
                    //         keyCode = e.getKeyCode();
                    //         console.println "pressed_code: $keyCode";
                    //         if(keyCode == ENTER_NORMAL_KEYCODE) {
                    //             enterNormalMode();
                    //         }
                    //     }
                    //     return false; // Pass on key
                    // }
                    // //check what's going on
                    // if(isKeyPressedEvent(eventID)) {
                    //     console.print 'key pressed event: '
                    // }
                    // if(isKeyTypedEvent(eventID)) {
                    //     console.print 'key typed event: '
                    // }

                    // console.println "typed_char: $keyChar";

                    // currentPos = editor.getCurrentPositionInEntryTranslation();
                    // // Need to pass keycode through to get backspace to work
                    // if(mode == Mode.NORMAL) {
                    //     if(keyChar == ENTER_INSERT_KEY) {
                    //         enterInsertMode();
                    //         return true;
                    //     } 

                    //     positionChange = 0;
                    //     switch(keyChar) {
                    //         case 'h': 
                    //             positionChange = -1;
                    //             break;
                    //         case 'l': 
                    //             positionChange = 1;
                    //             break;
                    //         default:
                    //             break;
                    //     }
                    //     caret = new CaretPosition(currentPos + positionChange);		
                    //     editor.setCaretPosition(caret);	
                    //     return true;				
                    // }

                    // println "This will print ${e.getKeyChar()}"
                    // return false;

                // } catch (Exception exc) {
                //     removeDispatcher();
                //     console.println exc.getMessage();
                // } finally {
                // }
//             }
//         };
//         manager.addKeyEventDispatcher(keyDispatcher);
//     }

//     def removeDispatcher() {
//         manager.removeKeyEventDispatcher(keyDispatcher)
//     }

//     def isKeyPressedEvent(eventID) {
//         return eventID == KeyEvent.KEY_PRESSED
//     }

//     def isKeyReleasedEvent(eventID) {
//         return eventID == KeyEvent.KEY_RELEASED
//     }

//     def isKeyTypedEvent(eventID) {
//         return eventID == KeyEvent.KEY_TYPED
//     }

//     def isNonEnglishKey(keyChar) {
//         !('A'..'z').contains(keyChar)
//     }

//     def enterNormalMode() {
//         mode = Mode.NORMAL
//     }

//     def enterInsertMode() {
//         mode = Mode.INSERT
//     }
// }


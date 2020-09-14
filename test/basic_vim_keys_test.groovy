import java.awt.event.KeyEvent;
import java.awt.Robot;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.DefaultCaret;

class VimKeysTest extends GroovyTestCase {
  Binding binding;
  GroovyShell shell;
  JFrame frame;


  class TestWindow {
    void setup(pane) {
      frame = new JFrame();
      frame.setSize(300, 400);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setVisible(true);

      pane.setContentType("text/plain");
      pane.setBackground(Color.BLACK);
      pane.setForeground(Color.WHITE);
      frame.setContentPane(pane);
      pane.setEditable(true);
      pane.setFocusable(true);
      pane.setVisible(true);
      pane.requestFocusInWindow();

      DefaultCaret caret = pane.getCaret();
      caret.setVisible(true);
      caret.setSelectionVisible(true);
    }
  }

  class TestRobot {

    static void enterKeys(String robotKeys) {
      boolean hasShift
      Robot robot = new Robot();
      robot.setAutoDelay(20);
      robot.setAutoWaitForIdle(true);
      // Create KeyEvent instance to be used in reflection to produce VK constant
      KeyEvent dummyKeyEvent = createDummyKeyEvent();
      int vkShiftCode = KeyEvent.getDeclaredField("VK_SHIFT").get(dummyKeyEvent);

      String[] robotKeysArray = robotKeys.split();
      robotKeysArray.each {
        if (it ==~ /[!@#$%\^&*()_+<>?~|{}]/) {
          hasShift = true;
        } else {
          hasShift = false;
        }
        String upcaseKey = it.toUpperCase();
        upcaseKey = (upcaseKey == '$') ? 'DOLLAR' : upcaseKey;
        int vkKey = KeyEvent.getDeclaredField("VK_$upcaseKey").get(dummyKeyEvent);
        executeKeypress(robot, vkKey, hasShift, vkShiftCode);
      }
    }

    static void executeKeypress(robot, vkKey, hasShift, vkShiftCode) {
      if (hasShift) {
        robot.keyPress(vkShiftCode);
        robot.keyPress(vkKey);
        robot.keyRelease(vkKey);
        robot.keyRelease(vkShiftCode);
      } else {
        robot.keyPress(vkKey);
        robot.keyRelease(vkKey);
      }
    }

    static void createDummyKeyEvent() {
      new KeyEvent(new JPanel(), KeyEvent.KEY_TYPED,
                   System.currentTimeMillis(), 0, 0, (char)'a');
    }
  }

  void setupShell() {
    binding = new Binding();
    shell = new GroovyShell(binding);
    binding.testWindow = new TestWindow();
    binding.testing = true;
  }

  // In robotKeys test string, each character should be separated by a
  // space and named as they are in the VK constants in the java KeyEvent
  // class

  void setUp() {
    setupShell();
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    Thread.sleep(300);
  }

  void tearDown() {
    frame.setVisible(false);
    frame.dispose();
  }

  void testDoesNotInsertTextInNormalMode() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);

    String robotKeys = 'b'
    TestRobot.enterKeys(robotKeys);

    String expected = 'This is a test'
    String actual = binding.editor.getCurrentTranslation();
    assertEquals(expected, actual);
  }

  void testhMovesOneSpaceBackInNormalMode() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(2);

    String robotKeys = 'h'
    TestRobot.enterKeys(robotKeys);

    int expected = 1
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testhhMovesTwoSpacesBackInNormalMode() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(2);

    String robotKeys = 'h h'
    TestRobot.enterKeys(robotKeys);

    int expected = 0
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testllMovesTwoSpacesForwardInNormalMode() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(0);

    String robotKeys = 'l l'
    TestRobot.enterKeys(robotKeys);

    int expected = 2
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void test0MovesToBeginningOfSegment() {
    String text = 'This is a test';
    int endPosition = text.length() - 1
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(endPosition);

    String robotKeys = '0' 
    TestRobot.enterKeys(robotKeys);

    int expected = 0
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testMoveCaretOneWord() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(0);

    String robotKeys = 'w'
    TestRobot.enterKeys(robotKeys);

    int expected = 5
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testMoveCaretMultipleWords() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(0);

    String robotKeys = '3 w'
    TestRobot.enterKeys(robotKeys);

    int expected = 10
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testMoveCaretMultipleWordstoFinalPosition() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(0);

    String robotKeys = '4 w'
    TestRobot.enterKeys(robotKeys);

    int expected = 14
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testGoToChar() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(0);

    String robotKeys = 'f s'
    TestRobot.enterKeys(robotKeys);

    int expected = 3
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testGoToSecondChar() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(0);

    String robotKeys = '2 f i'
    TestRobot.enterKeys(robotKeys);

    int expected = 5
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testGoToEnd() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(0);

    String robotKeys = '$'
    TestRobot.enterKeys(robotKeys);

    int expected = 14;
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testDeleteChar() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(0);

    String robotKeys = 'x'
    TestRobot.enterKeys(robotKeys);

    String expected = 'his is a test'
    String actual = binding.editor.getCurrentTranslation();
    assertEquals(expected, actual);
  }

  void testDeletetoEndwithLowercaseDAndDollarSign() {
    String text = 'This is a test';
    binding.editor.editor.setText(text);
    binding.editor.editor.setCaretPosition(2);

    String robotKeys = 'd $'
    TestRobot.enterKeys(robotKeys);

    String expected = 'Th'
    String actual = binding.editor.getCurrentTranslation();
    assertEquals(expected, actual);
  }

}

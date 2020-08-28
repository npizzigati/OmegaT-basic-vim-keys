import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.DefaultCaret;
import java.awt.Robot;
import java.awt.Color;

class VimKeysTest extends GroovyTestCase {
  Binding binding
  GroovyShell shell

  class TestWindow {
    def frame;
    def caret;

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
      Robot robot = new Robot();
      robot.setAutoWaitForIdle(true);
      // Create KeyEvent instance to be used in reflection to produce VK constant
      KeyEvent dummyKeyEvent = createDummyKeyEvent();

      String[] robotKeysArray = robotKeys.split();
      println "robotKeysArray = $robotKeysArray"
      robotKeysArray.each {
        String upcaseKey = it.toUpperCase();
        int vkKey = KeyEvent.getDeclaredField("VK_$upcaseKey").get(dummyKeyEvent);
        robot.delay(30);
        robot.keyPress(vkKey);
        robot.delay(10);
        robot.keyRelease(vkKey);
        robot.delay(20);
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

  void testDoesNotInsertTextInNormalMode() {
    Thread.sleep(50);
    setupShell();
    String robotKeys = 'i a ESCAPE b'
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    assertEquals('a', binding.editor.editor.getText());
  }

  void testhMovesOneSpaceBackInNormalMode() {
    Thread.sleep(50);
    setupShell();
    String robotKeys = 'i a ESCAPE h'
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    int expected = 0
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testhhMovesTwoSpacesBackInNormalMode() {
    Thread.sleep(50);
    setupShell();
    String robotKeys = 'i a a ESCAPE h h i b'
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    int expected = 1
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testllMovesTwoSpacesForwardInNormalMode() {
    Thread.sleep(50);
    setupShell();
    String robotKeys = 'i t h i s space i s space a space t e s t escape';
    robotKeys += ' 0' 
    robotKeys += ' l l'
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    int expected = 2
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void test0MovesToBegginingOfSegment() {
    Thread.sleep(50);
    setupShell();
    String robotKeys = 'i t h i s space i s space a space t e s t escape';
    robotKeys += ' 0' 
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    int expected = 0
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testMoveCaretOneWord() {
    Thread.sleep(50);
    setupShell();
    String robotKeys = 'i t h i s space i s space a space t e s t escape';
    robotKeys += ' 0 w' 
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    int expected = 5
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testMoveCaretMultipleWords() {
    Thread.sleep(50);
    setupShell();
    String robotKeys = 'i t h i s space i s space a space t e s t escape';
    robotKeys += ' 0 3 w' 
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    int expected = 10
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testMoveCaretMultipleWordsatEnd() {
    // Caret should move to end if number beyond last candidate
    Thread.sleep(50);
    setupShell();
    String robotKeys = 'i t h i s space i s space a space t e s t escape';
    robotKeys += ' 0 4 w' 
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    int expected = 13
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testGoToChar() {
    Thread.sleep(50);
    setupShell();
    String robotKeys = 'i t h i s ESCAPE 0 f s';
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    int expected = 3
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testGoToSecondChar() {
    Thread.sleep(50);
    setupShell();
    String robotKeys = 'i t h i s SPACE i s ESCAPE 0 2 f i';
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    int expected = 5
    int actual = binding.editor.editor.getCaretPosition();
    assertEquals(expected, actual);
  }

  void testDeleteChar() {
    Thread.sleep(100);
    setupShell();
    String robotKeys = 'i t h i s ESCAPE 0 x';
    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    String expected = 'his'
    String actual = binding.editor.getCurrentTranslation();
    assertEquals(expected, actual);
  }
}

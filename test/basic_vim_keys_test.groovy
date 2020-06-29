import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.DefaultCaret;
import java.awt.Robot;

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
        String upcaseChar = it.toUpperCase();
        int vkKey = KeyEvent.getDeclaredField("VK_$upcaseChar").get(dummyKeyEvent);
        Thread.sleep(30);
        robot.keyPress(vkKey);
        Thread.sleep(30);
        robot.keyRelease(vkKey);
        Thread.sleep(30);
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
    setupShell();
    String robotKeys = 'i a ESCAPE b'

    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);
    assertEquals('a', binding.editor.editor.getText());
  }

  void testhMovesOneSpaceBackInNormalMode() {
    setupShell();

    String robotKeys = 'i a ESCAPE h'

    shell.evaluate(new File('../src/basic_vim_keys.groovy'));

    TestRobot.enterKeys(robotKeys);

    int expected = 0
    int actual = binding.editor.editor.getCaretPosition();

    assertEquals(expected, actual);
  }

  void testhhMovesTwoSpacesBackInNormalMode() {
    setupShell();

    String robotKeys = 'i a a ESCAPE h h i b'

    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);

    int expected = 1
    int actual = binding.editor.editor.getCaretPosition();

    assertEquals(expected, actual);
  }

  void testllMovesTwoSpacesForwardInNormalMode() {
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

  void testMovesToBegginingOfSegment() {
    setupShell();

    String robotKeys = 'i t h i s space i s space a space t e s t escape';
    robotKeys += ' 0' 

    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);

    int expected = 0
    int actual = binding.editor.editor.getCaretPosition();

    assertEquals(expected, actual);
  }

  void testMovesToEndOfSegment() {
    setupShell();

    String robotKeys = 'i t h i s space i s space a space t e s t escape';
    robotKeys += ' 0 l dollar dollar l' 

    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.enterKeys(robotKeys);

    int expected = 14;
    int actual = binding.editor.editor.getCaretPosition();

    assertEquals(expected, actual);
  }
}

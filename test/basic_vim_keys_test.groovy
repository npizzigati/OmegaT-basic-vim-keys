import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.text.DefaultCaret;
import java.awt.Robot;

class VimKeysTest extends GroovyTestCase {
  Binding binding
  GroovyShell shell

  class TestWindow {
    def frame;
    def caret;
    def binding;
    def shell;

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

  // Can I make the loading of the window something that
  // happens only once with every test suite run?
  // void testInsertsTextInNormalMode() {
  //   Binding binding = new Binding();
  //   GroovyShell shell = new GroovyShell(binding);
  //   binding.testWindow = new TestWindow();

  //   binding.testing = true;
  //   binding.robotKeyPresses = [KeyEvent.VK_J, KeyEvent.VK_B]

  //   shell.evaluate(new File('../src/basic_vim_keys.groovy'));
  //   assertEquals('jb', binding.editor.editor.getText());
  // }

  class TestRobot {
    
    static void run(robotKeyPresses) {
      Robot robot = new Robot();
      robot.setAutoDelay(40);
      robot.setAutoWaitForIdle(true);
      robot.delay(500);
      robotKeyPresses.each {
        robot.keyPress(it);
        robot.keyRelease(it);
        robot.delay(500);
      }
    }
  }

  void setupShell() {
    binding = new Binding();
    shell = new GroovyShell(binding);
    binding.testWindow = new TestWindow();
    binding.testing = true;
  }

  void testDoesNotInsertTextInNormalMode() {
    setupShell();
    def robotKeyPresses = [KeyEvent.VK_I,
                           KeyEvent.VK_A,
                           KeyEvent.VK_ESCAPE,
                           KeyEvent.VK_B]

    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.run(robotKeyPresses);
    assertEquals('a', binding.editor.editor.getText());
  }

  void testlKeyMovesOneSpaceForwardInNormalMode() {
    setupShell();
    def robotKeyPresses = [KeyEvent.VK_L];

    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    TestRobot.run(robotKeyPresses);
    assertEquals('a', binding.editor.editor.getText());
  }
}

import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.text.DefaultCaret;

class VimKeysTest extends GroovyTestCase {
  // EditorController editor = new EditorController();

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
      // pane.setFocusable(true);
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

  void testDoesNotInsertTextInNormalMode() {
    Binding binding = new Binding();
    GroovyShell shell = new GroovyShell(binding);
    binding.testWindow = new TestWindow();

    binding.testing = true;
    binding.robotKeyPresses = [KeyEvent.VK_A,
                               KeyEvent.VK_ESCAPE,
                               KeyEvent.VK_B]

    shell.evaluate(new File('../src/basic_vim_keys.groovy'));
    assertEquals('a', binding.editor.editor.getText());
  }
}

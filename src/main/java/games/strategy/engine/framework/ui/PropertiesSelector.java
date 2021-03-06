package games.strategy.engine.framework.ui;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.ui.SwingAction;

/**
 * Wrapper for properties selection window.
 */
public class PropertiesSelector {
  /**
   * @param parent
   *        parent component
   * @param properties
   *        properties that will get displayed
   * @param buttonOptions
   *        button options. They will be displayed in a row on the bottom
   * @return pressed button
   */
  public static Object getButton(final JComponent parent, final String title,
      final List<IEditableProperty> properties, final Object... buttonOptions) {
    if (!SwingUtilities.isEventDispatchThread()) {
      // throw new IllegalStateException("Must run from EventDispatchThread");
      final AtomicReference<Object> rVal = new AtomicReference<>();
      SwingAction.invokeAndWait(() -> rVal.set(showDialog(parent, title, properties, buttonOptions)));
      return rVal.get();
    } else {
      return showDialog(parent, title, properties, buttonOptions);
    }
  }

  private static Object showDialog(final JComponent parent, final String title,
      final List<IEditableProperty> properties, final Object... buttonOptions) {
    final PropertiesUI panel = new PropertiesUI(properties, true);
    final JScrollPane scroll = new JScrollPane(panel);
    scroll.setBorder(null);
    scroll.getViewport().setBorder(null);
    final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
    pane.setOptions(buttonOptions);
    final JDialog window = pane.createDialog(JOptionPane.getFrameForComponent(parent), title);
    window.setVisible(true);
    return pane.getValue();
  }
}

package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.util.ThreadUtil;

/**
 * A panel for showing the battle steps in a display.
 * Contains code for walking from the current step, to a given step
 * there is a delay while we walk so that the user can see the steps progression.
 * Users of this class should deactive it after they are done.
 */
class BattleStepsPanel extends JPanel implements Active {
  private static final long serialVersionUID = 911638924664810435L;
  private static final Logger log = Logger.getLogger(BattleStepsPanel.class.getName());
  // if this is the target step, we want to walk to the last step
  private static final String LAST_STEP = "NULL MARKER FOR LAST STEP";
  private final DefaultListModel<String> m_listModel = new DefaultListModel<>();
  private final JList<String> m_list = new JList<>(m_listModel);
  private final MyListSelectionModel m_listSelectionModel = new MyListSelectionModel();
  // the step we want to reach
  private String m_targetStep = null;
  // all changes to state should be done while locked on this object.
  // when we reach the target step, or when we want to walk the step
  // notifyAll on this object
  private final Object m_mutex = new Object();
  private final List<CountDownLatch> m_waiters = new ArrayList<>();
  private boolean m_hasWalkThread = false;

  BattleStepsPanel() {
    setLayout(new BorderLayout());
    add(m_list, BorderLayout.CENTER);
    m_list.setBackground(this.getBackground());
    m_list.setSelectionModel(m_listSelectionModel);
  }

  @Override
  public void deactivate() {
    wakeAll();
  }

  private void wakeAll() {
    synchronized (m_mutex) {
      for (final CountDownLatch l : m_waiters) {
        l.countDown();
      }
      m_waiters.clear();
    }
  }

  /**
   * Set the steps given, setting the selected step to the first step.
   */
  public void listBattle(final List<String> steps) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Not in dispatch thread");
    }
    synchronized (m_mutex) {
      m_listModel.removeAllElements();
      final Iterator<String> iter = steps.iterator();
      while (iter.hasNext()) {
        m_listModel.addElement(iter.next());
      }
      m_listSelectionModel.hiddenSetSelectionInterval(0);
      if (!steps.contains(m_targetStep)) {
        m_targetStep = null;
      }
    }
    validate();
  }

  private void clearTargetStep() {
    synchronized (m_mutex) {
      m_targetStep = null;
    }
    wakeAll();
  }

  private boolean doneWalkingSteps() {
    synchronized (m_mutex) {
      // not looking for anything
      if (m_targetStep == null) {
        return true;
      }
      // we cant find it, something is wrong
      if (!m_targetStep.equals(LAST_STEP) && m_listModel.lastIndexOf(m_targetStep) == -1) {
        new IllegalStateException("Step not found:" + m_targetStep + " in:" + m_listModel).printStackTrace();
        clearTargetStep();
        return true;
      }
      // at end, we are done
      if (m_targetStep.equals(LAST_STEP) && m_list.getSelectedIndex() == m_listModel.getSize() - 1) {
        return true;
      }
      // we found it, we are done
      if (m_targetStep.equals(m_list.getSelectedValue())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Walks through and pause at each list item until we find our target.
   */
  private void walkStep() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    if (doneWalkingSteps()) {
      wakeAll();
      return;
    }
    int index = m_list.getSelectedIndex() + 1;
    if (index >= m_list.getModel().getSize()) {
      index = 0;
    }
    m_listSelectionModel.hiddenSetSelectionInterval(index);
    waitThenWalk();
  }

  private void waitThenWalk() {
    new Thread(() -> {
      synchronized (m_mutex) {
        if (m_hasWalkThread) {
          return;
        }
        m_hasWalkThread = true;
      }
      try {
        if (ThreadUtil.sleep(330)) {
          SwingUtilities.invokeLater(this::walkStep);
        }
      } finally {
        synchronized (m_mutex) {
          m_hasWalkThread = false;
        }
      }
    }).start();
  }

  /**
   * This method blocks until the last step is reached, unless
   * this method is called from the swing event thread.
   */
  public void walkToLastStep() {
    synchronized (m_mutex) {
      m_targetStep = LAST_STEP;
    }
    goToTarget();
  }

  /**
   * Set the target step for this panel
   * This method returns immediatly, and must be called from the swing event thread.
   */
  public void setStep(final String step) {
    synchronized (m_mutex) {
      if (m_listModel.indexOf(step) != -1) {
        m_targetStep = step;
      } else {
        log.info("Could not find step name:" + step);
      }
    }
    goToTarget();
  }

  private void goToTarget() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Not swing event thread");
    }
    waitThenWalk();
  }
}


/**
 * Doesnt allow the user to change the selection, must be done through
 * hiddenSetSelectionInterval.
 */
class MyListSelectionModel extends DefaultListSelectionModel {
  private static final long serialVersionUID = -4359950441657840015L;

  @Override
  public void setSelectionInterval(final int index0, final int index1) {}

  public void hiddenSetSelectionInterval(final int index) {
    super.setSelectionInterval(index, index);
  }
}

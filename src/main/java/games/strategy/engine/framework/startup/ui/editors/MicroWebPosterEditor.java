package games.strategy.engine.framework.startup.ui.editors;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;

import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.pbem.IWebPoster;
import games.strategy.engine.pbem.TripleAWebPoster;
import games.strategy.ui.ProgressWindow;
import games.strategy.util.UrlStreams;

/**
 * A class for displaying settings for the micro web site poster.
 */
public class MicroWebPosterEditor extends EditorPanel {
  private static final long serialVersionUID = -6069315084412575053L;
  public static final String HTTP_BLANK = "http://";
  private final JButton m_viewSite = new JButton("View Web Site");
  private final JButton m_testSite = new JButton("Test Web Site");
  private final JButton m_initGame = new JButton("Initialize Game");
  // private final JLabel m_idLabel = new JLabel("Site ID:");
  private final JTextField m_id = new JTextField();
  private final JLabel m_hostLabel = new JLabel("Host:");
  private final JComboBox<String> m_hosts;
  private final JCheckBox m_includeSaveGame = new JCheckBox("Send emails");
  private final IWebPoster m_bean;
  private final String[] m_parties;
  private final JLabel m_gameNameLabel = new JLabel("Game Name:");
  private final JTextField m_gameName = new JTextField();

  public MicroWebPosterEditor(final IWebPoster bean, final String[] parties) {
    m_bean = bean;
    m_parties = parties;
    final int bottomSpace = 1;
    final int labelSpace = 2;
    int row = 0;
    add(m_hostLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    m_bean.addToAllHosts(m_bean.getHost());
    m_hosts = new JComboBox<>(m_bean.getAllHosts());
    m_hosts.setEditable(true);
    m_hosts.setMaximumRowCount(6);
    add(m_hosts, new GridBagConstraints(1, row, 1, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    add(m_viewSite, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 2, bottomSpace, 0), 0, 0));
    row++;
    add(m_gameNameLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(m_gameName, new GridBagConstraints(1, row, 1, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    m_gameName.setText(m_bean.getGameName());
    add(m_initGame, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 2, bottomSpace, 0), 0, 0));
    if ((m_parties == null) || (m_parties.length == 0)) {
      m_initGame.setEnabled(false);
    }
    row++;
    // add(m_idLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new
    // Insets(0, 0,
    // bottomSpace, labelSpace), 0, 0));
    // add(m_id, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
    // new Insets(0, 0,
    // bottomSpace, 0), 0, 0));
    // m_id.setText(m_bean.getSiteId());
    // row++;
    add(m_includeSaveGame, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
    m_includeSaveGame.setSelected(m_bean.getMailSaveGame());
    add(m_testSite, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 2, bottomSpace, 0), 0, 0));
    setupListeners();
  }

  /**
   * Configures the listeners for the gui components.
   */
  private void setupListeners() {
    m_viewSite.addActionListener(e -> ((IWebPoster) getBean()).viewSite());
    m_testSite.addActionListener(e -> testSite());
    m_initGame.addActionListener(e -> initGame());
    m_hosts.addActionListener(e -> fireEditorChanged());
    // add a document listener which will validate input when the content of any input field is changed
    final DocumentListener docListener = new EditorChangedFiringDocumentListener();
    // m_hosts.getDocument().addDocumentListener(docListener);
    m_id.getDocument().addDocumentListener(docListener);
    m_gameName.getDocument().addDocumentListener(docListener);
  }

  private void initGame() {
    if (m_parties == null) {
      return;
    }
    final String hostUrl;
    if (!((String) m_hosts.getSelectedItem()).endsWith("/")) {
      hostUrl = (String) m_hosts.getSelectedItem();
    } else {
      hostUrl = m_hosts.getSelectedItem() + "/";
    }
    final ArrayList<String> players = new ArrayList<>();
    try {
      final URL url = new URL(hostUrl + "getplayers.php");
      final Optional<InputStream> inputStream = UrlStreams.openStream(url);
      if (inputStream.isPresent()) {
        try (InputStream stream = inputStream.get()) {
          try (InputStreamReader reader = new InputStreamReader(stream)) {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
              String inputLine;
              while ((inputLine = bufferedReader.readLine()) != null) {
                players.add(inputLine);
              }
            }
          }

        }
      }

      for (int i = 0; i < players.size(); i++) {
        players.set(i, players.get(i).substring(0, players.get(i).indexOf("\t")));
      }
    } catch (final Exception ex) {
      JOptionPane.showMessageDialog(MainFrame.getInstance(),
          "Retrieving players from " + hostUrl + " failed:\n" + ex.toString(), "Error",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    final JFrame window = new JFrame("Select Players");
    window.setLayout(new GridBagLayout());
    window.getContentPane().add(new JLabel("Select Players For Each Nation:"), new GridBagConstraints(0, 0, 2, 1, 0, 0,
        GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(20, 20, 20, 20), 0, 0));
    final List<JComboBox<String>> comboBoxes = new ArrayList<>(m_parties.length);
    for (int i = 0; i < m_parties.length; i++) {
      final JLabel label = new JLabel(m_parties[i] + ": ");
      comboBoxes.add(i, new JComboBox<>());
      for (int p = 0; p < players.size(); p++) {
        comboBoxes.get(i).addItem(players.get((p)));
      }
      comboBoxes.get(i).setSelectedIndex(i % players.size());
      window.getContentPane().add(label, new GridBagConstraints(0, i + 1, 1, 1, 0, 0, GridBagConstraints.EAST,
          GridBagConstraints.NONE, new Insets(5, 20, 5, 5), 0, 0));
      window.getContentPane().add(comboBoxes.get(i),
          new GridBagConstraints(1, i + 1, 1, 1, 0, 0, GridBagConstraints.WEST,
              GridBagConstraints.NONE, new Insets(5, 5, 5, 20), 0, 0));
    }
    final JButton btnClose = new JButton("Cancel");
    btnClose.addActionListener(e -> {
      window.setVisible(false);
      window.dispose();
    });
    final JButton btnOk = new JButton("Initialize");
    btnOk.addActionListener(e -> {
      window.setVisible(false);
      window.dispose();
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < comboBoxes.size(); i++) {
        sb.append(m_parties[i]);
        sb.append(": ");
        sb.append(comboBoxes.get(i).getSelectedItem());
        sb.append("\n");
      }
      HttpEntity entity = MultipartEntityBuilder.create()
          .addTextBody("siteid", m_id.getText())
          .addTextBody("players", sb.toString())
          .addTextBody("gamename", m_gameName.getText())
          .build();
      try {
        final String response = TripleAWebPoster.executePost(hostUrl, "create.php", entity);
        if (response.toLowerCase().contains("success")) {
          JOptionPane.showMessageDialog(MainFrame.getInstance(), response, "Game initialized",
              JOptionPane.INFORMATION_MESSAGE);
        } else {
          JOptionPane.showMessageDialog(MainFrame.getInstance(), "Game initialization failed:\n" + response, "Error",
              JOptionPane.INFORMATION_MESSAGE);
        }
      } catch (final Exception ex) {
        JOptionPane.showMessageDialog(MainFrame.getInstance(), "Game initialization failed:\n" + ex.toString(),
            "Error", JOptionPane.INFORMATION_MESSAGE);
      }
    });
    window.getContentPane().add(btnOk, new GridBagConstraints(0, m_parties.length + 1, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(30, 20, 20, 10), 0, 0));
    window.getContentPane().add(btnClose, new GridBagConstraints(1, m_parties.length + 1, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(30, 10, 20, 20), 0, 0));
    window.pack();
    window.setLocationRelativeTo(null);
    window.setVisible(true);
  }

  /**
   * Tests the Forum poster.
   */
  void testSite() {
    final IWebPoster poster = (IWebPoster) getBean();
    final ProgressWindow progressWindow = new ProgressWindow(MainFrame.getInstance(), poster.getTestMessage());
    progressWindow.setVisible(true);
    final Runnable runnable = () -> {
      Exception tmpException = null;
      try {
        final File f = File.createTempFile("123", "test");
        f.deleteOnExit();
        // For .jpg use this:
        final BufferedImage image = new BufferedImage(130, 40, BufferedImage.TYPE_INT_RGB);
        final Graphics g = image.getGraphics();
        g.drawString("Testing file upload", 10, 20);
        ImageIO.write(image, "jpg", f);
        poster.addSaveGame(f, "Test.jpg");
        poster.postTurnSummary(null, "Test Turn Summary.", "TestPlayer", 1);
      } catch (final Exception ex) {
        tmpException = ex;
      } finally {
        progressWindow.setVisible(false);
      }
      final Exception exception = tmpException;
      // now that we have a result, marshall it back unto the swing thread
      SwingUtilities.invokeLater(() -> {
        try {
          final String message = (exception != null) ? exception.toString() : m_bean.getServerMessage();
          JOptionPane.showMessageDialog(MainFrame.getInstance(), message, "Test Turn Summary Post",
              JOptionPane.INFORMATION_MESSAGE);
        } catch (final HeadlessException e) {
          // should never happen in a GUI app
        }
      });
    };
    // start a background thread
    final Thread t = new Thread(runnable);
    t.start();
  }

  @Override
  public boolean isBeanValid() {
    final boolean hostValid = validateText((String) m_hosts.getSelectedItem(), m_hostLabel,
        text -> text != null && text.length() > 0 && !text.equalsIgnoreCase(HTTP_BLANK));
    final boolean idValid = validateTextFieldNotEmpty(m_gameName, m_gameNameLabel);
    final boolean allValid = hostValid && idValid;
    m_testSite.setEnabled(allValid);
    m_initGame.setEnabled(allValid);
    m_viewSite.setEnabled(hostValid);
    return allValid;
  }

  @Override
  public IBean getBean() {
    m_bean.setHost((String) m_hosts.getSelectedItem());
    m_bean.addToAllHosts((String) m_hosts.getSelectedItem());
    m_bean.getAllHosts().remove(HTTP_BLANK);
    m_bean.setSiteId(m_id.getText());
    m_bean.setMailSaveGame(m_includeSaveGame.isSelected());
    m_bean.setGameName(m_gameName.getText());
    return m_bean;
  }
}

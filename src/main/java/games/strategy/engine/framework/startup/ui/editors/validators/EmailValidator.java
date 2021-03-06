package games.strategy.engine.framework.startup.ui.editors.validators;

import games.strategy.util.Util;

/**
 * A validator which validates that a text string is an email.
 */
public class EmailValidator implements IValidator {
  private final boolean m_validIfEmpty;

  /**
   * create a new instance.
   *
   * @param validIfEmpty
   *        is the text valid if empty
   */
  public EmailValidator(final boolean validIfEmpty) {
    m_validIfEmpty = validIfEmpty;
  }

  @Override
  public boolean isValid(final String text) {
    if (text.length() == 0) {
      return m_validIfEmpty;
    }
    return Util.isMailValid(text);
  }
}

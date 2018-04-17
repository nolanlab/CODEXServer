package org.nolanlab.CODEX.utils;

import java.awt.*;

/**
 * @author Vishal
 */
public interface OptionPane {

    int showConfirmDialog(Component parentComponent,
                          Object message, String title, int optionType);
}
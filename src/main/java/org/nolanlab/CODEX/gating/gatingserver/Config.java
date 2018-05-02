package org.nolanlab.CODEX.gating.gatingserver;

import java.awt.*;

public abstract class Config {
    public static final int BIAXIAL_PLOT_SIZE = 256;

    public static final int MAX_DENSITY_BIN_CLIP = 10;

    public static final int X_MIN = 0;
    public static final int Y_MIN = 0;


    public static final Color BIAXIAL_PLOT_BACKGROUND = Color.WHITE;
    public static final ColorMapper biaxialPlotColorMapper = new ColorMapper();


}


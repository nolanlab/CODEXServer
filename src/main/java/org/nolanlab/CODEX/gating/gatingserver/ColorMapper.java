package org.nolanlab.CODEX.gating.gatingserver;

import java.awt.*;

public class ColorMapper {

    public int getColor(float value){
        return Color.HSBtoRGB((1.0f - value)*0.60f, 1, 1f);
    }
}


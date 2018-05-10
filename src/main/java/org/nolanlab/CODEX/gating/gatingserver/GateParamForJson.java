package org.nolanlab.CODEX.gating.gatingserver;

import java.awt.*;

public class GateParamForJson {

    private String gateName;
    private Polygon polygon;
    private String x;
    private String y;

    public String getGateName() {
        return gateName;
    }

    public void setGateName(String gateName) {
        this.gateName = gateName;
    }

    public Shape getPolygon() {
        return polygon;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }
}

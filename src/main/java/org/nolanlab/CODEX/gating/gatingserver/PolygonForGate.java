package org.nolanlab.CODEX.gating.gatingserver;

import com.google.gson.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class PolygonForGate {

    public Polygon createPolygon(String coordinates) {

        coordinates = coordinates.replace("[{", "{");
        coordinates = coordinates.replace("}]", "}");

        Gson gson = new Gson();
        JsonElement element = gson.fromJson(coordinates, JsonElement.class);
        JsonObject jsonOb = element.getAsJsonObject();
        JsonArray arrXJson=jsonOb.getAsJsonArray("x");
        JsonArray arrYJson=jsonOb.getAsJsonArray("y");

        int[] xPoints = new int[arrXJson.size()];
        int[] yPoints = new int[arrYJson.size()];

        for(int i=0; i<arrXJson.size(); i++) {
            xPoints[i] = Integer.parseInt(arrXJson.get(i).toString());
        }

        for(int i=0; i<arrYJson.size(); i++) {
            yPoints[i] = Integer.parseInt(arrYJson.get(i).toString());
        }

        Polygon p =  new Polygon(xPoints, yPoints, xPoints.length);
        return p;
    }


}

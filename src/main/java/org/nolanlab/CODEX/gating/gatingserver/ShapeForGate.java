package org.nolanlab.CODEX.gating.gatingserver;

import com.google.gson.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class ShapeForGate {

    public Shape createShape(String coordinates) {

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

        Shape s =  new Polygon(xPoints, yPoints, xPoints.length);
        return s;
    }

    public void saveShapeAsJson(Shape s, String user, String exp, String tstamp, String fcs) throws IOException {
        String js = toJSON(s);
        String serverConfig = System.getProperty("user.dir");
        File gatesDir = new File(serverConfig + File.separator + "data" + File.separator + user +
                File.separator + exp + File.separator + "processed" + File.separator + "segm" +
                File.separator + tstamp + File.separator + "FCS" +
                File.separator + fcs + File.separator + "gates");

        if(!gatesDir.exists()) {
            gatesDir.mkdir();
        }

        File sJson = new File(gatesDir + File.separator + "shapeForGate.json");
        BufferedWriter bw = new BufferedWriter(new FileWriter(sJson));
        bw.write(js);
        bw.flush();
        bw.close();
    }

    public String toJSON(Shape s) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT) // include static
                .create();
        String js = gson.toJson(s).replaceAll(",", ",\n");
        return js;
    }

}

package org.nolanlab.CODEX.utils.codexhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.nolanlab.CODEX.gating.gatingserver.GateParamForJson;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class GatingHelper {

    public void saveGateAsJson(String user, String exp, String tstamp, String fcs, GateParamForJson gateParamForJson) throws IOException {
        String js = toJSON(gateParamForJson);
        String serverConfig = System.getProperty("user.dir");
        File gatesDir = new File(serverConfig + File.separator + "data" + File.separator + user +
                File.separator + exp + File.separator + "processed" + File.separator + "segm" +
                File.separator + tstamp + File.separator + "FCS" +
                File.separator + fcs + File.separator + "gates");

        if(!gatesDir.exists()) {
            gatesDir.mkdir();
        }

        File sJson = new File(gatesDir + File.separator + gateParamForJson.getGateName() + ".json");
        BufferedWriter bw = new BufferedWriter(new FileWriter(sJson));
        bw.write(js);
        bw.flush();
        bw.close();
    }

    public String toJSON(GateParamForJson g) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT) // include static
                .create();
        String js = gson.toJson(g).replaceAll(",", ",\n");
        return js;
    }

}

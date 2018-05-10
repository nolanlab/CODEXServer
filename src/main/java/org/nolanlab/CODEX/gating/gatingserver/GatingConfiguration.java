package org.nolanlab.CODEX.gating.gatingserver;

import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import dataIO.DatasetStub;
import org.apache.commons.io.FilenameUtils;
import org.nolanlab.CODEX.uploader.uplclient.Experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GatingConfiguration {

    private String user;
    private String exp;
    private String tStamp;
    private String fcs;
    private List<String> tStamps = new ArrayList<>();
    private List<String> combinedXYNames = new ArrayList<>();
    private String selectedGate;
    private String selectedX;
    private String selectedY;
    private List<String> gates = new ArrayList<>();


    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getExp() {
        return exp;
    }

    public void setExp(String exp) {
        this.exp = exp;
    }

    public String getTStamp() {
        return tStamp;
    }

    public void setTStamp(String tStamp) {
        this.tStamp = tStamp;
    }

    public String getFcs() {
        return fcs;
    }

    public void setFcs(String fcs) {
        this.fcs = fcs;
    }

    public String getSelectedGate() {
        return selectedGate;
    }

    public void setSelectedGate(String selectedGate) {
        this.selectedGate = selectedGate;
    }

    public String getSelectedX() {
        return selectedX;
    }

    public void setSelectedX(String selectedX) {
        this.selectedX = selectedX;
    }

    public String getSelectedY() {
        return selectedY;
    }

    public void setSelectedY(String selectedY) {
        this.selectedY = selectedY;
    }

    public List<String> listSegmTimestamps() {
        String serverConfig = System.getProperty("user.dir");
        File tStampsDir = new File(serverConfig + File.separator + "data" + File.separator + user + File.separator + exp + File.separator + "processed" + File.separator + "segm");
        tStamps = Arrays.asList(tStampsDir.list());
        return tStamps;
    }

    public List<String> listCombinedNames() {
        String serverConfig = System.getProperty("user.dir");
        File fcsDir = new File(serverConfig + File.separator + "data" + File.separator + user + File.separator + exp + File.separator + "processed" + File.separator + "segm" + File.separator + tStamp
        + File.separator + "FCS" + File.separator + "compensated");
        File[] fcsFile = fcsDir.listFiles(f -> !f.isDirectory() && f.getName().endsWith(".fcs"));
        if(fcsFile != null && fcsFile.length != 0) {
            DatasetStub s = DatasetStub.createFromFCS(fcsFile[0]);
            combinedXYNames = Streams.zip(Arrays.asList(s.getLongColumnNames()).stream(), Arrays.asList(s.getShortColumnNames()).stream(), (a, b) -> a + " (" + b + ")").collect(Collectors.toList());
        }
        return combinedXYNames;
    }

    public List<String> listGates() {
        String serverConfig = System.getProperty("user.dir");
        File gatesDir = new File(serverConfig + File.separator + "data" + File.separator + user + File.separator + exp + File.separator + "processed" + File.separator + "segm" + File.separator + tStamp
                + File.separator + "FCS" + File.separator + fcs + File.separator + "gates");
        if(gatesDir.exists()) {
            gates = Arrays.asList(gatesDir.list());
        }
        for(int i=0; i<gates.size(); i++) {
            gates.set(i, FilenameUtils.removeExtension(gates.get(i)));
        }
        return gates;
    }

    public GateParamForJson getGateJson() {
        String serverConfig = System.getProperty("user.dir");
        File gatesDir = new File(serverConfig + File.separator + "data" + File.separator + user + File.separator + exp + File.separator + "processed" + File.separator + "segm" + File.separator + tStamp
                + File.separator + "FCS" + File.separator + fcs + File.separator + "gates");
        if(this.selectedGate != null) {
            File gateJson = new File(gatesDir + File.separator + selectedGate + ".json");
            try {
                return loadFromJSON(gateJson);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if(this.selectedX != null && this.selectedY != null) {
            if(gatesDir.exists()) {
                File[] listGatesJson = gatesDir.listFiles(j -> j.getName().endsWith(".json"));
                for (File aGateJson : listGatesJson) {
                    try {
                        GateParamForJson gp = loadFromJSON(aGateJson);
                        if (gp.getX().equalsIgnoreCase(this.selectedX) && gp.getY().equalsIgnoreCase(this.selectedY)) {
                            return gp;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public GateParamForJson loadFromJSON(File f) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(f));
        GateParamForJson gateParam = gson.fromJson(reader, GateParamForJson.class);
        return gateParam;
    }

}

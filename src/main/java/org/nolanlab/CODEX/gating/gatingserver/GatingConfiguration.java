package org.nolanlab.CODEX.gating.gatingserver;

import com.google.common.collect.Streams;
import dataIO.DatasetStub;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GatingConfiguration {

    private String user;
    private String exp;
    private String tStamp;
    private List<String> tStamps = new ArrayList<>();
    private List<String> combinedXYNames = new ArrayList<>();


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

}

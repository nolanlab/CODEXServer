package org.nolanlab.CODEX.uploader.uplserver;

import org.nolanlab.CODEX.uploader.uplserver.driffta.*;
import java.io.*;
import java.util.ArrayList;

import static spark.Spark.post;

public class UploaderController {
    private static ArrayList<Process> allProcess = new ArrayList<>();

    public static void main(String[] args) {

        post("/makeMontage", "application/octet-stream", (request, response) -> {
            String user = request.queryParams("user");
            String expName = request.queryParams("exp");
            String factor = request.queryParams("fc");

            try {
                MakeMontage.createMontages(user, expName, factor);
            }
            catch (Exception e) {
                return e.getMessage();
            }

            return "Montages created at: /" + user + "/" + expName + "/processed/stitched";
        });

        post("/runDriffta", "application/octet-stream", (request, response) -> {
            String user = request.queryParams("user");
            String expName = request.queryParams("exp");
            String reg = request.queryParams("reg");
            String tile = request.queryParams("tile");

            try {
                Driffta.drifftaProcessing(user, expName, reg, tile);
//                //ProcessBuilder pb = new ProcessBuilder("cmd", "/C", "java -Xms5G -Xmx64G -Xmn50m -cp \".\\*\" org.nolanlab.CODEX.uploader.uplserver.driffta.Driffta \"" + inputExp + " " + user + " " + reg + " " + tile);
//                //File libDir = new File("C:\\Users\\Nikolay\\IdeaProjects\\codexnetviewer\\target");
//                ProcessBuilder pb = new ProcessBuilder("java", "-cp", System.getProperty("user.dir"), "org.nolanlab.CODEX.uploader.uplserver.driffta.Driffta", drifftaArgs[0], drifftaArgs[1], drifftaArgs[2], drifftaArgs[3]);
//                //pb.directory(libDir);
//                pb.redirectErrorStream(true);
//                ExperimentHelper expHelper = new ExperimentHelper();
//                expHelper.log("Starting process: " + pb.command().toString());
//                Process proc = pb.start();
//                allProcess.add(proc);
//                expHelper.waitAndPrint(proc);
            }
            catch(Exception e) {
                return e.getMessage();
            }

            return "Processed files uploaded at: /" + user + "/" + expName + "/processed";
        });

//        post("/runBestFocus", "application/octet-stream", (request, response) -> {
//            String user = request.queryParams("user");
//            String expName = request.queryParams("exp");
//
//            try {
//                BestFocus.makeBestFocus(user, expName);
//            }
//            catch(Exception e) {
//                return e.getMessage();
//            }
//
//            return "Best focus files uploaded at: /" + user + "/" + expName + "/processed";
//        });
    }
}

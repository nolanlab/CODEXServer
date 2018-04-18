package org.nolanlab.CODEX.segm.segmserver;

import com.google.gson.Gson;
import org.nolanlab.CODEX.segm.segmclient.SegConfigParam;
import org.nolanlab.CODEX.utils.logger;
import org.nolanlab.CODEX.viewer.viewerserver.CodexImageServer;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class SegmController {

    private static RunSegm rs = new RunSegm();

    public static String dataHomeDir = null;

    public static void initServer(String dataHomeDir){
        SegmController.dataHomeDir = dataHomeDir;
    }

    public static void main(String[] args) {
        staticFiles.location("/public");
        staticFiles.externalLocation("C:/CODEX_server_home/data");
        staticFiles.expireTime(60);

        init();
        initServer(args[0]+File.separator+"data");

        Gson gson = new Gson();
        get("/getUserList", "application/json", (request, response) -> {
            List<String> users = Arrays.asList(new File(dataHomeDir).listFiles(f->f.isDirectory())).stream().map(f->f.getName()).collect(Collectors.toList());
            return users;
        }, gson::toJson);

        get("/getExperimentList", "application/json",(request, response) -> {
            String user = (request.queryParams("user"));
            List<String> exp = Arrays.asList(new File(dataHomeDir+"/"+user).listFiles(f->f.isDirectory())).stream().map(f->f.getName()).collect(Collectors.toList());
            return exp;
        }, gson::toJson);

        get("/getProgress", "application/json", (request, response) -> String.valueOf(rs.getProgress()));

        post("/runSegm", "application/json", (request, response) -> {
            String user = request.queryParams("user");
            String exp = request.queryParams("exp");
            String radius = request.queryParams("radius");
            String maxCutOff = request.queryParams("maxCutOff");
            String minCutOff = request.queryParams("minCutOff");
            String relativeCutOff = request.queryParams("relativeCutOff");
            String nucStainChannel = request.queryParams("nucStainChannel");
            String nucStainCycle = request.queryParams("nucStainCycle");
            String membStainChannel = request.queryParams("membStainChannel");
            String membStainCycle = request.queryParams("membStainCycle");

            boolean showImage = false;
            boolean use_membrane = false;

            SegConfigParam segParam = new SegConfigParam();

            String serverConfig = System.getProperty("user.dir");
            File rootDir = new File(serverConfig + File.separator + "data" + File.separator + user + File.separator + exp + File.separator + "processed");

            segParam.setRootDir(rootDir);
            segParam.setShowImage(showImage);
            segParam.setRadius(Integer.parseInt(radius));
            segParam.setUse_membrane(use_membrane);
            segParam.setMaxCutoff(Double.parseDouble(maxCutOff));
            segParam.setMinCutoff(Double.parseDouble(minCutOff));
            segParam.setRelativeCutoff(Double.parseDouble(relativeCutOff));
            segParam.setNuclearStainChannel(Integer.parseInt(nucStainChannel));
            segParam.setNuclearStainCycle(Integer.parseInt(nucStainCycle));
            segParam.setMembraneStainChannel(Integer.parseInt(membStainChannel));
            segParam.setMembraneStainCycle(Integer.parseInt(membStainCycle));
            segParam.setInner_ring_size(1.0);
            segParam.setCount_puncta(false);
            segParam.setDont_inverse_memb(false);
            segParam.setConcentricCircles(0);
            segParam.setDelaunay_graph(false);

            logger.print("Parameters as seen from the browser: ");
            logger.print("Input dir: " + segParam.getRootDir().getPath());
            logger.print("showImage: "+ segParam.isShowImage());
            logger.print("radius: " + segParam.getRadius());
            logger.print("use_Membrane: " + segParam.isUse_membrane());
            logger.print("maxCutOff: " + segParam.getMaxCutoff());
            logger.print("minCutOff: " + segParam.getMinCutoff());
            logger.print("relativeCutOff: " + segParam.getRelativeCutoff());
            logger.print("nucStainChannel: " + segParam.getNuclearStainChannel());
            logger.print("nucStainCycle: " + segParam.getNuclearStainCycle());
            logger.print("membStainChannel: " + segParam.getMembraneStainChannel());
            logger.print("membStainCycle: " + segParam.getMembraneStainCycle());
            logger.print("inner ring size: " + segParam.getInner_ring_size());
            logger.print("count puncta: " + segParam.isCount_puncta());
            logger.print("dont_inverse_memb: " + segParam.isDont_inverse_memb());
            logger.print("concentric circles: " +segParam.getConcentricCircles());
            logger.print("delaunay graph: "+ segParam.isDelaunay_graph());


            String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Timestamp(System.currentTimeMillis()));
            logger.print("Starting Main Segmentation...");

            try {
                rs.runSeg(segParam, timestamp);
            }
            catch (Exception e) {
                return e.getMessage();
            }
            logger.print("Main segmentation done");

            logger.print("Starting Concatenate results");
            try {
                ConcatenateResults.callConcatenateResults(new File(segParam.getRootDir() + File.separator +"segm" + File.separator + "segm_" + timestamp));
            }
            catch (Exception e) {
                return e.getMessage();
            }
            logger.print("ConcatenateResults done");

            logger.print("Starting MakeFCS");
            try {
                MakeFCS.callMakeFcs(segParam, timestamp);
            }
            catch (Exception e) {
                return e.getMessage();
            }
            logger.print("MakeFCS done");

            File checkOut = new File(segParam.getRootDir() + File.separator + "segm" + File.separator + "segm_" + timestamp);
            File[] txtFiles = checkOut.listFiles(t -> checkOut.getName().endsWith(".txt"));
            File[] fcsFiles = checkOut.listFiles(t -> checkOut.getName().endsWith(".fcs"));
            if(txtFiles.length == 0 || fcsFiles.length == 0) {
                return "Segmentation didn't run properly. Either fcs files/txt files were not created!";
            }
            return "Segmentation Ran Successfully! Check the processed folder at: " + user+File.separator+exp+File.separator;
        });
    }
}

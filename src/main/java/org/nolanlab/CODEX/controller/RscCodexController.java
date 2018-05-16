package org.nolanlab.CODEX.controller;

import com.google.gson.Gson;
import com.sun.javafx.binding.StringFormatter;
import ij.IJ;
import org.nolanlab.CODEX.clustering.clsserver.ClusterConfig;
import org.nolanlab.CODEX.gating.gatingserver.*;
import org.nolanlab.CODEX.segm.segmclient.SegConfigParam;
import org.nolanlab.CODEX.segm.segmserver.ConcatenateResults;
import org.nolanlab.CODEX.segm.segmserver.MakeFCS;
import org.nolanlab.CODEX.segm.segmserver.RunSegm;
import org.nolanlab.CODEX.uploader.uplserver.driffta.Driffta;
import org.nolanlab.CODEX.uploader.uplserver.driffta.MakeMontage;
import org.nolanlab.CODEX.utils.codexhelper.GatingHelper;
import org.nolanlab.CODEX.utils.logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class RscCodexController {

    public static String dataHomeDir = null;
    private static RunSegm rs = new RunSegm();
    public static List<String> XYnames = new ArrayList<>();

    public static void initServer(String dataHomeDir){
        RscCodexController.dataHomeDir = dataHomeDir;
    }

    public static void main(String[] args) {



        initServer(args[0] + File.separator + "data");

        staticFiles.location("/public");
        staticFiles.externalLocation(args[0] + File.separator + "cache");
        staticFiles.externalLocation(dataHomeDir);
        staticFiles.expireTime(60);
        init();
        Gson gson = new Gson();

        //Common
        get("/getUserList", "application/json", (request, response) -> {
            List<String> users = Arrays.asList(new File(dataHomeDir).listFiles(f->f.isDirectory())).stream().map(f->f.getName()).collect(Collectors.toList());
            return users;
        }, gson::toJson);

        get("/getExperimentList", "application/json",(request, response) -> {
            String user = (request.queryParams("user"));
            List<String> exp = Arrays.asList(new File(dataHomeDir+"/"+user).listFiles(f->f.isDirectory())).stream().map(f->f.getName()).collect(Collectors.toList());
            return exp;
        }, gson::toJson);

        get("/getExperiment","application/json", (request, response) -> {
            String user = (request.queryParams("user"));
            String experiment = request.queryParams("exp");
            try{
                String content = new Scanner(new File(dataHomeDir+"/"+user+"/"+experiment+"/Experiment.json")).useDelimiter("\\Z").next();
                return content;
            }catch (Exception e){
                return e.fillInStackTrace().toString();
            }
        });

        //Uploader
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
            }
            catch(Exception e) {
                return e.getMessage();
            }

            return "Processed files uploaded at: /" + user + "/" + expName + "/processed";
        });


        //Segmentation
        get("/getProgress", "application/json", (request, response) -> String.valueOf(rs.getProgress()));

        get("/runSegm", "application/json", (request, response) -> {
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
            String segmName = request.queryParams("segmName");

            boolean showImage = false;
            boolean use_membrane = false;

            SegConfigParam segParam = new SegConfigParam();

            String serverConfig = System.getProperty("user.dir");
            File rootDir = new File(serverConfig + File.separator + "data" + File.separator + user + File.separator + exp + File.separator + "processed");

            segParam.setSegmName(segmName);
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

            logger.print("Starting Main Segmentation...");

            try {
                rs.runSeg(segParam);
            }
            catch (Exception e) {
                return e.getMessage();
            }
            logger.print("Main segmentation done");

            logger.print("Starting Concatenate results");
            try {
                ConcatenateResults.callConcatenateResults(new File(segParam.getRootDir() + File.separator +"segm" + File.separator + segmName));
            }
            catch (Exception e) {
                return e.getMessage();
            }
            logger.print("ConcatenateResults done");

            try {
                MakeFCS.callMakeFcs(segParam);
            }
            catch (Exception e) {
                return e.getMessage();
            }

            File checkOut = new File(segParam.getRootDir() + File.separator + "segm" + File.separator + segmName + File.separator + "FCS");
            for(File compUncomp : checkOut.listFiles()) {
                if(compUncomp.isDirectory()) {
                    File[] txtFiles = compUncomp.listFiles(t -> t.getName().endsWith(".txt"));
                    File[] fcsFiles = compUncomp.listFiles(f -> f.getName().endsWith(".fcs"));
                    if (txtFiles.length == 0 || fcsFiles.length == 0) {
                        return "Segmentation didn't run properly. Either fcs files/txt files were not created!";
                    }
                }
            }
            //response.redirect("html");
            String res = "Segmentation Ran Successfully! Check the processed folder at: " + user+File.separator+exp+File.separator;
            return res;
        }, gson::toJson);



        //Gating
        get("/getSegTimestampsForGate", "application/json", (request, response) -> {
            GatingConfiguration gatingConfiguration = new GatingConfiguration();
            gatingConfiguration.setUser(request.queryParams("user"));
            gatingConfiguration.setExp(request.queryParams("exp"));
            return gatingConfiguration.listSegmTimestamps();
        }, gson::toJson);

        get("/getXYListForGate", "application/json", (request, response) -> {
            GatingConfiguration gatingConfiguration = new GatingConfiguration();
            gatingConfiguration.setUser(request.queryParams("user"));
            gatingConfiguration.setExp(request.queryParams("exp"));
            gatingConfiguration.setTStamp(request.queryParams("tstamp"));
            XYnames = gatingConfiguration.listCombinedNames();
            return XYnames;
        }, gson::toJson);

        get("/getBiaxialPlot", (request, response) -> {

            String user = request.queryParams("user");
            String exp = request.queryParams("exp");
            String fcs = request.queryParams("FCS");
            String X = request.queryParams("X");
            String Y = request.queryParams("Y");
            String tstamp = request.queryParams("tstamp");

            int x = Integer.parseInt(X);
            int y = Integer.parseInt(Y);

            if(x == -1 || y == -1) {
                throw new IllegalStateException("X or Y cant be -1!");
            }

            System.out.println("drawing the biaxial: X=" +x +  " Y=" + y);

            String serverConfig = System.getProperty("user.dir") + File.separator + "data";

            File fcsDir = new File(serverConfig + File.separator + user + File.separator + exp + File.separator + "processed" + File.separator + "segm" + File.separator + tstamp + File.separator
                    + "FCS" + File.separator + fcs);

            File[] fcsFile = fcsDir.listFiles(file -> file.getName().endsWith(".fcs"));

            if(fcsFile == null && fcsFile.length!=1){
                throw new IllegalStateException("invalid number of files for region:" + "reg001" + " numFiles"+fcsFile.length);
            }

            GateFilter gf = new GateFilter(fcsFile[0], Config.BIAXIAL_PLOT_SIZE);

            BufferedImage bi = gf.getPlot(x,y, Config.biaxialPlotColorMapper);

            File _imagesDir = new File(new File(serverConfig).getParent() + File.separator + "cache"+ File.separator + "_images/");
            if(!_imagesDir.exists()) {
                _imagesDir.mkdir();
            }
            File tempFile = File.createTempFile("biaxial", ".png", _imagesDir);

            ImageIO.write(bi,"PNG", tempFile);

            return "/_images/"+tempFile.getName();
        }, gson::toJson);


        get("/saveGate", "application/json",(request, response) -> {
            //[{"x":[78,172,172,78],"y":[52,52,138,138]}]
            String gateName = request.queryParams("gateName");
            String user = request.queryParams("user");
            String exp = request.queryParams("exp");
            String tstamp = request.queryParams("tstamp");
            String X = request.queryParams("X");
            String Y = request.queryParams("Y");
            String fcs = request.queryParams("FCS");
            String coordinates = request.queryParams("coordinates");

            PolygonForGate polygonForGate = new PolygonForGate();
            Polygon p = polygonForGate.createPolygon(coordinates);

            GateParamForJson gateParamForJson = new GateParamForJson();
            gateParamForJson.setGateName(gateName);
            gateParamForJson.setPolygon(p);


            gateParamForJson.setX(XYnames.get(Integer.parseInt(X)));
            gateParamForJson.setY(XYnames.get(Integer.parseInt(Y)));

            GatingHelper gatingHelper = new GatingHelper();
            gatingHelper.saveGateAsJson(user, exp, tstamp, fcs, gateParamForJson);

            String serverConfig = System.getProperty("user.dir") + File.separator + "data";

            File fcsDir = new File(serverConfig + File.separator + user + File.separator + exp + File.separator + "processed" + File.separator + "segm" + File.separator + tstamp + File.separator
                    + "FCS" + File.separator + fcs);

            File[] fcsFile = fcsDir.listFiles(file -> file.getName().endsWith(".fcs"));

            if(fcsFile == null && fcsFile.length!=1){
                throw new IllegalStateException("invalid number of files for region:" + "reg001" + " numFiles"+fcsFile.length);
            }

            GateFilter gf = new GateFilter(fcsFile[0], Config.BIAXIAL_PLOT_SIZE);

            int xInd = Integer.parseInt(X);
            int yInd = Integer.parseInt(Y);

            gf.setGate(xInd,yInd,p, gateName);

            return gateParamForJson;
        }, gson::toJson);

        get("/getGates", "application/json", (request, response) -> {
            GatingConfiguration gatingConfiguration = new GatingConfiguration();
            gatingConfiguration.setUser(request.queryParams("user"));
            gatingConfiguration.setExp(request.queryParams("exp"));
            gatingConfiguration.setTStamp(request.queryParams("tstamp"));
            gatingConfiguration.setFcs(request.queryParams("FCS"));

            return gatingConfiguration.listGates();
        }, gson::toJson);

        get("/loadGateConfig", "application/json", (request, response) -> {
            GatingConfiguration gatingConfiguration = new GatingConfiguration();
            gatingConfiguration.setUser(request.queryParams("user"));
            gatingConfiguration.setExp(request.queryParams("exp"));
            gatingConfiguration.setTStamp(request.queryParams("tstamp"));
            gatingConfiguration.setFcs(request.queryParams("FCS"));
            String X = request.queryParams("X");
            String Y = request.queryParams("Y");
            if(X != null && Y != null) {
                gatingConfiguration.setSelectedX(XYnames.get(Integer.parseInt(X)));
                gatingConfiguration.setSelectedY(XYnames.get(Integer.parseInt(Y)));
            }
            gatingConfiguration.setSelectedGate(request.queryParams("gate"));

            //change parameter here
            return gatingConfiguration.getGateJson();
        }, gson::toJson);



        //Clustering
        get("/getClusterCols", "application/json", (request, response) -> {
            ClusterConfig clusterConfig = new ClusterConfig();
            clusterConfig.setUser(request.queryParams("user"));
            clusterConfig.setExp(request.queryParams("exp"));
            clusterConfig.setTStamp(request.queryParams("tstamp"));
            return clusterConfig.listCombinedNames();
        }, gson::toJson);



        //Viewer
        get("/getStitchedImageList", "application/octet-stream", (request, response) -> {
            String user = request.queryParams("user");
            String experiment = request.queryParams("exp");
            int region = Integer.parseInt(request.queryParams("reg"));

            final String path = dataHomeDir+"/"+user+"/"+experiment+"/stitched/"+"reg"+StringFormatter.format("%03d",region).getValue();

            List<String> exp = Arrays.asList(new File(path).listFiles(f->f.getName().endsWith(".tif"))).stream().map(f->f.getAbsolutePath().substring(dataHomeDir.length()).replaceAll("\\\\","/")).collect(Collectors.toList());

            return  exp;
        }, gson::toJson);

        get("/getImage", "application/octet-stream", (request, response) -> {
            String user = request.queryParams("user");
            String experiment = request.queryParams("exp");
            int region = Integer.parseInt(request.queryParams("reg"));
            int tileX = Integer.parseInt(request.queryParams("tileX"));
            int tileY = Integer.parseInt(request.queryParams("tileY"));
            int channel = Integer.parseInt(request.queryParams("ch"));
            int z = Integer.parseInt(request.queryParams("z"));

            String folderName = "reg"+StringFormatter.format("%03d",region).getValue()+"_X"+StringFormatter.format("%02d",tileX).getValue()+"_Y"+StringFormatter.format("%02d",tileY).getValue();

            String path = dataHomeDir+"/"+user+"/"+experiment+"/"+folderName+"/"+folderName+"_z"+StringFormatter.format("%03d",z).getValue()+"_c"+StringFormatter.format("%03d",channel).getValue()+".tif";

            System.out.println(path);
            return ImageIO.write((RenderedImage)IJ.openImage(path).getProcessor().createImage(), "PNG",response.raw().getOutputStream());
        });

    }

}

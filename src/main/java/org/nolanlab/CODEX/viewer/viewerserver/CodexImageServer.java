package org.nolanlab.CODEX.viewer.viewerserver;

/**
 * Hello world!
 *
 */
import com.google.gson.Gson;
import com.sun.javafx.binding.StringFormatter;
import ij.IJ;


import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class CodexImageServer {

    public static HashMap<String, List<String>> userExperimentMap = new HashMap<String, List<String>>();

    public static String dataHomeDir = null;

    public static void initServer(String dataHomeDir){
        CodexImageServer.dataHomeDir = dataHomeDir;
        userExperimentMap.put("Darci", new ArrayList<>());
        userExperimentMap.put("Christian", new ArrayList<>());
        userExperimentMap.put("Nikolay", Arrays.asList(new String[] {"exp1"}));
    }


    public static void main(String[] args) throws Exception {

        staticFiles.location("/public");
        staticFiles.externalLocation("C:/CODEX_server_home/data");
        staticFiles.expireTime(60);

        init();
        initServer(args[0]);


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

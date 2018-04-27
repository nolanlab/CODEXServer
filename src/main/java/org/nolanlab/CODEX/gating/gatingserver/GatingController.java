package org.nolanlab.CODEX.gating.gatingserver;

import com.google.gson.Gson;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class GatingController {
    public static String dataHomeDir = null;

    public static void initServer(String dataHomeDir){
        GatingController.dataHomeDir = dataHomeDir;
    }

    public static void main(String[] args) {
        staticFiles.location("/public");
        staticFiles.externalLocation("C:/CODEX_server_home/data");
        staticFiles.expireTime(60);

        init();
        initServer(args[0] + File.separator + "data");

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
            return gatingConfiguration.listCombinedNames();
        }, gson::toJson);

        get("/getImage", "application/json",(request, response) -> {
            String user = (request.queryParams("user"));
            String exp = (request.queryParams("exp"));
            String fcs = (request.queryParams("fcs"));
            String x = (request.queryParams("x"));
            String y = (request.queryParams("y"));
            return "Image";
        }, gson::toJson);

        get("/setGate", "application/json",(request, response) -> {
            //[{"x":[78,172,172,78],"y":[52,52,138,138]}]
            String user = request.queryParams("user");
            String exp = request.queryParams("exp");
            String tstamp = request.queryParams("tstamp");
            String x = request.queryParams("x");
            String y = request.queryParams("y");
            String fcs = request.queryParams("fcs");
            String coordinates = request.queryParams("coordinates");

            ShapeForGate sGate = new ShapeForGate();
            Shape s = sGate.createShape(coordinates);
            sGate.saveShapeAsJson(s, user, exp, tstamp, fcs);
            return s;
        }, gson::toJson);
    }
}

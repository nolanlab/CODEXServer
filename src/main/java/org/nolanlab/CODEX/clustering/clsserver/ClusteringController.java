package org.nolanlab.CODEX.clustering.clsserver;

import com.google.gson.Gson;
import org.nolanlab.CODEX.gating.gatingserver.GatingConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static spark.Spark.get;
import static spark.Spark.init;
import static spark.Spark.staticFiles;

public class ClusteringController {
    public static String dataHomeDir = null;

    public static void initServer(String dataHomeDir){
        ClusteringController.dataHomeDir = dataHomeDir;
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
            ClusterConfig clusterConfig = new ClusterConfig();
            clusterConfig.setUser(request.queryParams("user"));
            clusterConfig.setExp(request.queryParams("exp"));
            return clusterConfig.listSegmTimestamps();
        }, gson::toJson);

        get("/getClusterCols", "application/json", (request, response) -> {
            ClusterConfig clusterConfig = new ClusterConfig();
            clusterConfig.setUser(request.queryParams("user"));
            clusterConfig.setExp(request.queryParams("exp"));
            clusterConfig.setTStamp(request.queryParams("tstamp"));
            return clusterConfig.listCombinedNames();
        }, gson::toJson);

    }
}

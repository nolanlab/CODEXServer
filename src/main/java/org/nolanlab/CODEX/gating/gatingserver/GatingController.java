package org.nolanlab.CODEX.gating.gatingserver;

import com.google.gson.Gson;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class GatingController {
    public static String dataHomeDir = null;
    public static List<String> XYnames = new ArrayList<>();

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
            List<String> users = Arrays.asList(new File(dataHomeDir).listFiles(f->f.isDirectory()&&!f.getName().startsWith("_"))).stream().map(f->f.getName()).collect(Collectors.toList());
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
            XYnames = gatingConfiguration.listCombinedNames();
            return XYnames;
        }, gson::toJson);

//        get("/getImage", "application/json",(request, response) -> {
//
//            return "Image";
//        }, gson::toJson);


        get("/getImage", (request, response) -> {

            String user = request.queryParams("user");
            String exp = request.queryParams("exp");
            String fcs = request.queryParams("FCS");
            //String reg = request.queryParams("reg");
            String X = request.queryParams("X");
            String Y = request.queryParams("Y");
            String tstamp = request.queryParams("tstamp");

            List<String> colNames = XYnames;

            int x=0;
            int y=0;

            for(int i=0; i<colNames.size(); i++){
                if(colNames.get(i).contains(X)) {
                    x = i;
                }
                if(colNames.get(i).contains(Y)) {
                    y = i;
                }
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

            File tempFile = File.createTempFile("biaxial", ".png", new File(serverConfig+"/_images/"));


            ImageIO.write(bi,"PNG", tempFile);


            /*




            byte[] data = null;
            try {
                data = Files.readAllBytes(Paths.get(tempFile.getAbsolutePath()));
                tempFile.delete();
            } catch (Exception e1) {
                e1.printStackTrace();
            }*/

            /*HttpServletResponse raw = response.raw();
            response.type("image/png");
            response.status(200);
            try {
                ImageIO.write(bi,"PNG", raw.getOutputStream());
                raw.getOutputStream().flush();
                raw.getOutputStream().close();
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            return "/_images/"+tempFile.getName();
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

        get("/saveGate", "application/json", (request, response) -> {
            String gateName = request.queryParams("gateName");
            String user = request.queryParams("userVal");
            String exp = request.queryParams("expVal");
            String tstamp = request.queryParams("tstampVal");
            String fcs = request.queryParams("fcsVal");
            String x = request.queryParams("xVal");
            String y = request.queryParams("yVal");

            String serverConfig = System.getProperty("user.dir");
            File gatesDir = new File(serverConfig + File.separator + "data" + File.separator + user +
                    File.separator + exp + File.separator + "processed" + File.separator + "segm" +
                    File.separator + tstamp + File.separator + "FCS" +
                    File.separator + fcs + File.separator + "gates");

            response.redirect("/html/clustering.html");
            return "";
        });
    }
}

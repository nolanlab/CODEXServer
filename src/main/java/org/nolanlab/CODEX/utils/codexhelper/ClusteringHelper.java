package org.nolanlab.CODEX.utils.codexhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.nolanlab.CODEX.clustering.clsclient.ClusteringConfigParam;

import java.io.*;
import java.lang.reflect.Modifier;

public class ClusteringHelper {

    public void saveToFile(ClusteringConfigParam clusteringConfigParam, File f) throws IOException {
        String js = toJSON(clusteringConfigParam);
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write(js);
        bw.flush();
        bw.close();
    }

    public String toJSON(ClusteringConfigParam clusteringConfigParam) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT) // include static
                .create();
        String js = gson.toJson(clusteringConfigParam).replaceAll(",", ",\n");
        return js;
    }

    public ClusteringConfigParam loadFromJSON(File f) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(f));
        ClusteringConfigParam clusteringConfigParam = gson.fromJson(reader, ClusteringConfigParam.class);
        return clusteringConfigParam;
    }
}

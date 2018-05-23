package org.nolanlab.CODEX.utils.codexhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.nolanlab.CODEX.segm.segmclient.SegConfigParam;

import java.io.*;
import java.lang.reflect.Modifier;

public class SegmHelper {

    public void saveToFile(SegConfigParam segConfigParam, File f) throws IOException {
        String js = toJSON(segConfigParam);
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write(js);
        bw.flush();
        bw.close();
    }

    public String toJSON(SegConfigParam segConfigParam) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT) // include static
                .create();
        String js = gson.toJson(segConfigParam).replaceAll(",", ",\n");
        return js;
    }

    public SegConfigParam loadFromJSON(File f) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(f));
        SegConfigParam segConfigParam = gson.fromJson(reader, SegConfigParam.class);
        return segConfigParam;
    }
}

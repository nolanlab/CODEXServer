package org.nolanlab.CODEX.utils.codexhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.nolanlab.CODEX.segm.segmclient.SegConfigParam;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
}

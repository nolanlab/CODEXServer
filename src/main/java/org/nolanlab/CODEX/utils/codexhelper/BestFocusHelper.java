package org.nolanlab.CODEX.utils.codexhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class BestFocusHelper {
    public void saveToFile(HashMap<String, Integer> tb, File f) throws IOException {
        String js = toJSON(tb);
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write(js);
        bw.flush();
        bw.close();
    }

    public String toJSON(HashMap<String, Integer> tb) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT) // include static
                .create();
        String js = gson.toJson(tb).replaceAll(",", ",\n");
        return js;
    }

    public Map<String, Integer> load(File f) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(f));
        Type type = new TypeToken<Map<String, Integer>>(){}.getType();
        Map<String, Integer> map = gson.fromJson(reader, type);
        return map;
    }
}

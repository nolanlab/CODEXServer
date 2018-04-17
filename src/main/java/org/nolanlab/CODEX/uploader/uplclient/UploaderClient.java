package org.nolanlab.CODEX.uploader.uplclient;

import org.nolanlab.CODEX.utils.RequestHandler;

import java.io.IOException;

public class UploaderClient {

    private String path;
    private String host;
    private String response;

    public UploaderClient(String host, String pathToProcess) throws IOException {
        path = pathToProcess;
        this.host = host;
        this.response = new String(RequestHandler.post(host + "/" + pathToProcess, pathToProcess));
    }

    public String getResponse() {
        return response;
    }

    public String getPath() {
        return path;
    }

    public String getHost() {
        return host;
    }
}

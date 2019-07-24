package com.db.copycat.server;

import io.netty.handler.codec.http.FullHttpRequest;

public interface ServerActionHandler {

    /**
     * handle the http request and return the response string
     * @param req
     * @return
     */
    String handle(FullHttpRequest req) throws Exception;

}

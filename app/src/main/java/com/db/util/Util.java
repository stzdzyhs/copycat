package com.db.util;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;

public class Util {

    public static void assertTrue(String msg, boolean t) {
        if(!t) {
            throw new RuntimeException(msg);
        }
    }

    public static void decodePost(String post, Map<String,String> m) throws IOException {
        String[] ss = post.split("&");
        int idx;
        String k,v;
        for(int i=0;i<ss.length;i++) {
            idx = ss[i].indexOf("=");
            if(idx==-1) {
                throw new IOException("invalid post");
            }

            k = ss[i].substring(0,idx);
            v = ss[i].substring(idx+1);
            v = URLDecoder.decode(v, "UTF-8");
            m.put(k,v);
        }
    }

}

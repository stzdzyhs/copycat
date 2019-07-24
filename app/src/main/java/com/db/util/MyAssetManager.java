package com.db.util;

import java.io.IOException;
import java.io.InputStream;

public interface MyAssetManager {

    public InputStream open(String name) throws IOException;

}

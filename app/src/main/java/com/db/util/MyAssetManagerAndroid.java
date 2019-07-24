package com.db.util;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * AssetManager for Android platform
 */
public class MyAssetManagerAndroid implements MyAssetManager {

    AssetManager assetManager;

    public MyAssetManagerAndroid(AssetManager am) {
        this.assetManager = am;
    }

    @Override
    public InputStream open(String name) throws IOException {
        if(name==null) {
            throw new NullPointerException("null name");
        }

        // AssetManager: without leading /
        if(name.startsWith("/")) {
            name = name.substring(1);
        }

        InputStream is = this.assetManager.open(name);
        return is;
    }

}

package com.edwoollard.weathermap.Utils;

import android.support.v4.util.LruCache;

public class Cache {

    private static Cache instance;
    private LruCache<Object, Object> lru;
    private final static int CACHE_MAX_SIZE = 1024;

    private Cache() {
        lru = new LruCache<Object, Object>(CACHE_MAX_SIZE);
    }

    public static Cache getInstance() {
        if (instance == null) {
            instance = new Cache();
        }

        return instance;
    }

    public LruCache<Object, Object> getLru() {
        return lru;
    }
}
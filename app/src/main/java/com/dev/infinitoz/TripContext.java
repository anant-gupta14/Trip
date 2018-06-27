package com.dev.infinitoz;

import java.util.HashMap;
import java.util.Map;

public class TripContext {

    private static Map<String, Object> map = new HashMap<>();


    private TripContext() {

    }

    /*private static TripContext tripContext;

    public static TripContext getInstance(){
        if(tripContext==null){
            synchronized (TripContext.class){
                if(tripContext == null){
                    tripContext = new TripContext();
                }
            }
        }
        return tripContext;
    }*/
    public static Object getValue(String key) {
        return map.get(key);
    }

    public static void addValue(String key, Object value) {
        map.put(key, value);
    }

    public static Object removeValue(String key) {
        return map.remove(key);
    }

}

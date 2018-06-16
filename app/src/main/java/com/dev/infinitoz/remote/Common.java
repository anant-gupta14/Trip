package com.dev.infinitoz.remote;

public class Common {

    public static final String baseURL = "https://googleapis.com";

    public static IGoogleApi getGoogleApi() {
        return RetrofitClient.getClient(baseURL).create(IGoogleApi.class);
    }
}

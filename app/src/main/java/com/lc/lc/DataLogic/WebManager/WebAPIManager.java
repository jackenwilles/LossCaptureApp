package com.lc.lc.DataLogic.WebManager;

import android.location.Location;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hare on 1/31/2015.
 */

public class WebAPIManager {
    private static final String ANDROID_FLAG = "1";

    private static final String HOST_ADDRESS = "http://losscapture.com/v1";
    public String m_url;
    OkHttpClient m_client = new OkHttpClient();

    public static Gson JSONParser = new Gson();
    public static WebAPIManager APIs = new WebAPIManager();

    enum MethodType{
        GET, POST, PUT, DELETE
    }

    private WebAPIManager(){
    }

    private String Process(MethodType type, Object param) throws IOException {
        RequestBody body = null;
        if (param != null) {
            FormEncodingBuilder form = new FormEncodingBuilder();
            for (String key : ((Map<String, String>) param).keySet())
                form.add(key, ((Map<String, String>) param).get(key));
            body = form.build();
        }

        Request.Builder builder = null;
        switch (type){
            case GET:
                builder = new Request.Builder().url(m_url);
                break;
            case POST:
                builder = new Request.Builder().url(m_url).post(body);
                break;
            case PUT:
                builder = new Request.Builder().url(m_url).put(body);
                break;
            case DELETE:
                builder = new Request.Builder().url(m_url).delete();
                break;
        }

        Response response = m_client.newCall(builder.build()).execute();
        String strResponse = response.body().string();
        Log.e("Web API Response", strResponse);
        return strResponse;
    }

    public JsonObject loadClaim(int claim_id, String lastName) throws IOException

    {
        m_url = HOST_ADDRESS + "/data/loadclaim";
        JsonObject postBody = new JsonObject();
        postBody.addProperty("code", 200);
        JsonObject customInfo = new JsonObject();
        postBody.add("data", customInfo);
        customInfo.addProperty("ClaimID", claim_id);
        customInfo.addProperty("Customer_LastName", lastName);

        Map<String, String> param = new HashMap<String, String>();
        param.put("data", WebAPIManager.JSONParser.toJson(postBody));
        Log.d("m_url", m_url + "\n" + param.toString());
        return WebAPIManager.JSONParser.fromJson(Process(MethodType.POST, param), JsonObject.class);
    }

    public JsonObject getToken() throws IOException
    {
        m_url = HOST_ADDRESS + "/media/connect";

        return WebAPIManager.JSONParser.fromJson(Process(MethodType.POST, null), JsonObject.class);
    }

    public JsonObject uploadFile(int claim_id, int userId, File file)
    {
        m_url = HOST_ADDRESS + "/data/photoupload";
        JsonObject postBody = new JsonObject();

        JsonObject customInfo = new JsonObject();
        postBody.add("data", customInfo);
        customInfo.addProperty("ClaimID", claim_id);
        customInfo.addProperty("UserID", userId);

        HttpPost httpost = new HttpPost(m_url);
        MultipartEntity entity = new MultipartEntity();

        try
        {
            entity.addPart("data", new StringBody(WebAPIManager.JSONParser.toJson(postBody)));
            entity.addPart("file", new FileBody(file));
            httpost.setEntity(entity);

            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(httpost);
            HttpEntity httpEntity = response.getEntity();
            String result = EntityUtils.toString(httpEntity);
            return WebAPIManager.JSONParser.fromJson(result, JsonObject.class);

        }catch(Exception e) {

        }


        return null;
    }
}

package com.lc.lc.DataLogic;

import android.content.Context;
import android.location.Location;

import com.google.gson.JsonObject;
import com.lc.lc.DataLogic.Model.CustomerInfo;
import com.lc.lc.DataLogic.WebManager.WebAPIManager;

import java.io.File;

/**
 * Created by SkyZero on 6/28/2016.
 */
public class SharedManager {
    private static SharedManager mInstance = null;
    public static Context m_context = null;

    public static CustomerInfo m_CustomerInfo = null;

    public String mToken = null;
    private SharedManager(){

    }

    public void init(Context context) {
        m_context = context;
    }
    public static SharedManager getInstance() {

        if(mInstance == null) {
            mInstance = new SharedManager();
        }

        return mInstance;
    }

    public String fileUpload(int claim_id, int userId, File uploadFile){
        String strErr = null;
        try {
            JsonObject json = WebAPIManager.APIs.uploadFile(claim_id, userId, uploadFile);
            if (json.get("code").getAsInt() == 200 ) {

            }
        } catch (Exception ex) {
            strErr = ex.getMessage();
        }
        return strErr;
    }

    public String getToken(){
        String strErr = null;
        try {
            JsonObject json = WebAPIManager.APIs.getToken();
            if (json.get("code").getAsInt() == 200 ) {
                JsonObject data = json.get("data").getAsJsonObject();
                mToken = data.get("token").getAsString();
            }
            else{
                mToken = null;
            }
        } catch (Exception ex) {
            mToken = null;
            strErr = ex.getMessage();
        }
        return strErr;
    }
}

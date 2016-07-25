package com.lc.lc.DataLogic.Model;

import android.util.Log;

import com.google.gson.JsonObject;
import com.lc.lc.DataLogic.SharedManager;
import com.lc.lc.DataLogic.WebManager.WebAPIManager;

/**
 * Created by SkyZero on 6/28/2016.
 */
public class CustomerInfo {
    public int mClaimID;
    public String mSlug;
    public String mCustomerFirstName;
    public String mCustomerLastName;
    public String mAutoVin;

    public static String loadClaimInfo(int claim_id, String lastName)
    {
        Log.d("save", "loadClaimInfo");
        String strErr = null;
        try{
            JsonObject json = WebAPIManager.APIs.loadClaim(claim_id, lastName);
            Log.d("save", claim_id +" "+lastName);

            Log.d("save", Integer.toString(json.get("code").getAsInt())+ "\n"+ json.toString());
            if(json.get("code").getAsInt() == 200)
            {//Success
                Log.d("save", "json success");
                JsonObject data = json.get("data").getAsJsonObject();
                SharedManager.getInstance().m_CustomerInfo = new CustomerInfo();
                SharedManager.getInstance().m_CustomerInfo.mClaimID = claim_id;
                SharedManager.getInstance().m_CustomerInfo.mSlug = data.get("slug").getAsString();
                SharedManager.getInstance().m_CustomerInfo.mCustomerFirstName = data.get("Customer_FirstName").getAsString();
                SharedManager.getInstance().m_CustomerInfo.mCustomerLastName = data.get("Customer_LastName").getAsString();
                SharedManager.getInstance().m_CustomerInfo.mAutoVin = data.get("AutoVIN").getAsString();
            }
        }
        catch(Exception e)
        {
            strErr = e.getMessage();
        }
        return strErr;
    }
}

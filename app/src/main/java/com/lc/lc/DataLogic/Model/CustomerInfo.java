package com.lc.lc.DataLogic.Model;

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
        String strErr = null;
        try{
            JsonObject json = WebAPIManager.APIs.loadClaim(claim_id, lastName);

            if(json.get("code").getAsInt() == 200)
            {//Success
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

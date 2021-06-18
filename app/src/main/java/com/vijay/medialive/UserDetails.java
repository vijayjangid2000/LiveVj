package com.vijay.medialive;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class UserDetails {

    private transient final String USER_DATA_SP = "userDataSP1";
    private transient final String USER_DATA_JSON = "userDataJson1";
    private transient SharedPreferences sharedPref;
    private transient SharedPreferences.Editor sPrefEditor;
    private static UserDetails userDetails;

    private String str_streamUrlForClient,
            str_rtmpHostUrl, str_rtmpPassword, str_streamKey, str_userName;

    public UserDetails(Context context) {
        sharedPref = context.getSharedPreferences(USER_DATA_SP, Context.MODE_PRIVATE);
        sPrefEditor = sharedPref.edit();
        sPrefEditor.apply();

        initializeAllStrings(); // to prevent null exceptions

        String jsonString = sharedPref.getString(USER_DATA_JSON, "");

        // if there is no data then do nothing
        assert jsonString != null;
        if (!jsonString.equals("")) {
            Gson gson = new Gson();
            UserDetails userDetails = gson.fromJson(jsonString, UserDetails.class);
            setUserDataFromJson(userDetails);
            // now we have assigned previous (saved) data to this object
        }
    }


    public static UserDetails getInstance(Context context) {
        if (userDetails == null) userDetails =
                new UserDetails(context.getApplicationContext());
        return userDetails;
    }

    public static UserDetails getInstance() {
        return userDetails; // WARNING: only when you are sure that it has been created already
    }

    void initializeAllStrings() {
        str_streamUrlForClient = "https://multiplatform-f.akamaihd.net/i/multi/will/bunny/big_buck_bunny_,640x360_400,640x360_700,640x360_1000,950x540_1500,.f4v.csmil/master.m3u8";
        str_rtmpHostUrl = "rtmp://eea70250-c032-3de3-4ff4-63fe0d3de5bc.dacastmmd.pri.lldns.net/dacastmmd";
        str_streamKey = "c2f950e77be6482fb7bfae3304640db3_4500";
        str_userName = "4287844889";
        str_rtmpPassword = "07d9dec9ecaf4f9e";
    }

    public void applyUpdate(Context context) {

        /* TO UPDATE DATA FOLLOW THESE STEPS
         * 1. Create the object (created by old json automatically)
         * 2. Set the data you want to change
         * 3. Now call updateData from the object in which data changed
         * 4. We will save the data from that object in SharedPref
         * 5. HOW? updated strings gets replaced and others will be there as it is
         *  */

        sharedPref = context.getSharedPreferences(USER_DATA_SP,
                Context.MODE_PRIVATE);
        sPrefEditor = sharedPref.edit();
        sPrefEditor.putString(USER_DATA_JSON, new Gson().toJson(this));
        sPrefEditor.apply();
    }

    private void setUserDataFromJson(UserDetails userDetails) {

        this.str_rtmpPassword = userDetails.str_rtmpPassword;
        this.str_rtmpHostUrl = userDetails.str_rtmpHostUrl;
        this.str_streamUrlForClient = userDetails.str_streamUrlForClient;
        this.str_streamKey = userDetails.str_streamKey;
        this.str_userName = userDetails.str_userName;

    }

    // GETTERS AND SETTERS

    public String getStr_rtmpHostUrl() {
        return str_rtmpHostUrl;
    }

    public void setStr_rtmpHostUrl(String str_rtmpHostUrl) {
        this.str_rtmpHostUrl = str_rtmpHostUrl;
    }

    public String getStr_streamUrlForClient() {
        return str_streamUrlForClient;
    }

    public void setStr_streamUrlForClient(String str_streamUrlForClient) {
        this.str_streamUrlForClient = str_streamUrlForClient;
    }

    public String getStr_rtmpPassword() {
        return str_rtmpPassword;
    }

    public void setStr_rtmpPassword(String str_rtmpPassword) {
        this.str_rtmpPassword = str_rtmpPassword;
    }

    public String getStr_streamKey() {
        return str_streamKey;
    }

    public void setStr_streamKey(String str_streamKey) {
        this.str_streamKey = str_streamKey;
    }

    public String getStr_userName() {
        return str_userName;
    }

    public void setStr_userName(String str_userName) {
        this.str_userName = str_userName;
    }
}

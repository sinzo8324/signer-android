package com.gruutnetworks.gruutsigner.gruut;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UnpackMsgRequestSignature extends MsgUnpacker {
    @Expose
    @SerializedName("time")
    private String time;
    @Expose
    @SerializedName("mID")
    private String mID;
    @Expose
    @SerializedName("cID")
    private String chainId;
    @Expose
    @SerializedName("hgt")
    private String blockHeight;
    @Expose
    @SerializedName("txrt")
    private String transaction;

    public UnpackMsgRequestSignature(byte[] bytes) {
        parse(bytes); // parse the whole message
        bodyFromJson(body);
    }

    public String getTime() {
        return time;
    }

    public String getmID() {
        return mID;
    }

    public String getChainId() {
        return chainId;
    }

    public String getBlockHeight() {
        return blockHeight;
    }

    public String getTransaction() {
        return transaction;
    }

    @Override
    void bodyFromJson(byte[] bodyBytes) {
        Gson gson = new Gson();
        UnpackMsgRequestSignature msgRequestSignature = gson.fromJson(new String(bodyBytes), UnpackMsgRequestSignature.class);

        this.time = msgRequestSignature.time;
        this.mID = msgRequestSignature.mID;
        this.chainId = msgRequestSignature.chainId;
        this.blockHeight = msgRequestSignature.blockHeight;
        this.transaction = msgRequestSignature.transaction;
    }
}

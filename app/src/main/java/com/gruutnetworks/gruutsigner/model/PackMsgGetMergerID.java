package com.gruutnetworks.gruutsigner.model;

import android.util.Base64;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import static com.gruutnetworks.gruutsigner.model.MsgHeader.MSG_HEADER_LEN;

/**
 * Title: Get Merger ID List
 * Description: Request the merger ID list to a merger manager which are connected with the merger manager
 * Message Type: 0x5C
 */
public class PackMsgGetMergerID extends MsgPacker {
    @Expose
    @SerializedName("sID")
    private String sID;  // BASE64 encoded 8 byte data
    @Expose
    @SerializedName("cID")
    private String localChainId;  // BASE64 encoded 8 byte data

    public PackMsgGetMergerID(String sID, String localChainId) {
        this.sID = sID;
        this.localChainId = localChainId;
        setHeader();
    }

    @Override
    void setHeader() {
        this.header = new MsgHeader.Builder()
                .setMsgType(TypeMsg.MSG_GET_MERGER_ID.getType())
                .setCompressionType(TypeComp.NONE.getType())
                .setTotalLen(MSG_HEADER_LEN + getCompressedJsonLen())
                .setLocalChainId(Base64.decode(localChainId, Base64.NO_WRAP)) // Base64 decoding
                .setSender(Base64.decode(sID, Base64.NO_WRAP)) // Base64 decoding
                .build();
    }

    @Override
    public void setDestinationId(String id) {
        this.destinationId = id;
    }

    @Override
    public byte[] bodyToJson() {
        // Super class는 제외하고 serialize
        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getDeclaringClass().equals(MsgPacker.class);
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).create();
        String tmp = gson.toJson(PackMsgGetMergerID.this);
        return tmp.getBytes();
    }
}

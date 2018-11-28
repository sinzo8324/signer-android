package com.gruutnetworks.gruutsigner.gruut;

import android.util.Base64;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.gruutnetworks.gruutsigner.model.TypeComp;
import com.gruutnetworks.gruutsigner.model.TypeMac;
import com.gruutnetworks.gruutsigner.model.TypeMsg;

import static com.gruutnetworks.gruutsigner.gruut.MessageHeader.MSG_HEADER_LEN;

public class PackMsgSuccess extends MsgPacker {
    @Expose(serialize = false)
    private String headerLocalChainId;

    @Expose
    @SerializedName("sender")
    private String sender;
    @Expose
    @SerializedName("time")
    private String time;
    @Expose
    @SerializedName("val")
    private boolean val;

    public PackMsgSuccess(String sender, String time, boolean val) {
        this.sender = sender;
        this.time = time;
        this.val = val;

        setHeader();
    }

    public PackMsgSuccess(String headerLocalChainId, String sender, String time, boolean val) {
        this.headerLocalChainId = headerLocalChainId;
        this.sender = sender;
        this.time = time;
        this.val = val;

        setHeader();
    }

    @Override
    void setHeader() {
        if (headerLocalChainId != null) {
            this.header = new MessageHeader.Builder()
                    .setMsgType(TypeMsg.MSG_SUCCESS.getType())
                    .setMacType(TypeMac.HMAC_SHA256.getType())
                    .setCompressionType(TypeComp.LZ4.getType())
                    .setTotalLen(MSG_HEADER_LEN + getCompressedJsonLen())
                    .setSender(Base64.decode(sender, Base64.NO_WRAP)) // Base64 decoding
                    .setLocalChainId(Base64.decode(headerLocalChainId, Base64.NO_WRAP))
                    .build();
        } else {
            this.header = new MessageHeader.Builder()
                    .setMsgType(TypeMsg.MSG_SUCCESS.getType())
                    .setMacType(TypeMac.HMAC_SHA256.getType())
                    .setCompressionType(TypeComp.LZ4.getType())
                    .setTotalLen(MSG_HEADER_LEN + getCompressedJsonLen())
                    .setSender(Base64.decode(sender, Base64.NO_WRAP)) // Base64 decoding
                    .build();
        }
    }

    @Override
    byte[] bodyToJson() {
        Gson gson = new Gson();
        return gson.toJson(PackMsgSuccess.this).getBytes();
    }
}

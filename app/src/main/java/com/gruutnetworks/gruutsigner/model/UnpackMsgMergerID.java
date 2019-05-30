package com.gruutnetworks.gruutsigner.model;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class UnpackMsgMergerID {
    String[] id;
    public UnpackMsgMergerID(String input) {
        bodyFromJson(input);
    }

    void bodyFromJson(String input) {
        Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getDeclaringClass().equals(MsgUnpacker.class);
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).create();

        UnpackMsgMergerID msgMergerIDs = gson.fromJson(input, UnpackMsgMergerID.class);

        this.id = msgMergerIDs.id;

    }

    public String[] getMIDs(){
        return id;
    }
}

package com.nbplus.push.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by basagee on 2015. 6. 30..
 */
public class PushBaseData implements Parcelable {
    public static final int MAX_APP_ID_LENGTH = 100;
    public static final int MAX_REPEAT_KEY_LENGTH = 10;
    public static final int MAX_ALERT_LENGTH = 300;

    private char messageType;
    private int messageId;
    private int bodyLength;

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public char getMessageType() {
        return messageType;
    }

    public void setMessageType(char messageType) {
        this.messageType = messageType;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(messageType);
        dest.writeInt(this.messageId);
        dest.writeInt(this.bodyLength);
    }

    public PushBaseData() {
    }

    protected PushBaseData(Parcel in) {
        this.messageType = (char) in.readInt();
        this.messageId = in.readInt();
        this.bodyLength = in.readInt();
    }

    public static final Parcelable.Creator<PushBaseData> CREATOR = new Parcelable.Creator<PushBaseData>() {
        public PushBaseData createFromParcel(Parcel source) {
            return new PushBaseData(source);
        }

        public PushBaseData[] newArray(int size) {
            return new PushBaseData[size];
        }
    };
}

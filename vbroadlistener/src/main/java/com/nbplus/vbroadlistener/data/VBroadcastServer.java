package com.nbplus.vbroadlistener.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 5. 18..
 */
public class VBroadcastServer implements Parcelable {
    @SerializedName("api_server")
    private String apiServer;
    @SerializedName("doc_server")
    private String docServer;
    @SerializedName("push_if_server")
    private String pushInterfaceServer;

    // 접속할 때마다 달라진다. push gateway server 에서 받아온다.
    private String pushConnServer;

    public String getApiServer() {
        return apiServer;
    }

    public void setApiServer(String apiServer) {
        this.apiServer = apiServer;
    }

    public String getDocServer() {
        return docServer;
    }

    public void setDocServer(String docServer) {
        this.docServer = docServer;
    }

    public String getPushInterfaceServer() {
        return pushInterfaceServer;
    }

    public void setPushInterfaceServer(String pushGateway) {
        this.pushInterfaceServer = pushGateway;
    }

    public String getPushConnServer() {

        return pushConnServer;
    }

    public void setPushConnServer(String pushConnServer) {
        this.pushConnServer = pushConnServer;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.apiServer);
        dest.writeString(this.docServer);
        dest.writeString(this.pushInterfaceServer);
        dest.writeString(this.pushConnServer);
    }

    public VBroadcastServer() {
    }

    private VBroadcastServer(Parcel in) {
        this.apiServer = in.readString();
        this.docServer = in.readString();
        this.pushInterfaceServer = in.readString();
        this.pushConnServer = in.readString();
    }

    public static final Creator<VBroadcastServer> CREATOR = new Creator<VBroadcastServer>() {
        public VBroadcastServer createFromParcel(Parcel source) {
            return new VBroadcastServer(source);
        }

        public VBroadcastServer[] newArray(int size) {
            return new VBroadcastServer[size];
        }
    };
}

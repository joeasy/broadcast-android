package com.nbplus.iotgateway.btcharacteristics;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/**
 * Created by basagee on 2015. 9. 14..
 *
 * Service : Glucose	org.bluetooth.service.glucose	0x1808
 * see https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.glucose.xml
 */
public class RecordAccessControlPoint extends BaseCharacteristicValue {

    private int opCode;                     // uint8
    private int operator;            // uint8
    private byte[] operand;             //  variable (0 ~ 2 bytes)

    public int getOpCode() {
        return opCode;
    }

    public void setOpCode(int opCode) {
        this.opCode = opCode;
    }

    public int getOperator() {
        return operator;
    }

    public void setOperator(int operator) {
        this.operator = operator;
    }

    public byte[] getOperand() {
        return operand;
    }

    public void setOperand(byte[] operand) {
        this.operand = operand;
    }

    public static RecordAccessControlPoint parseRecordAccessControlPoint(byte[] value) {
        RecordAccessControlPoint measurement;
        if (value == null || value.length == 0) {
            return null;
        }

        measurement = new RecordAccessControlPoint();
        measurement.setOpCode(value[0] & 0xff);
        measurement.setOperator(value[1] & 0xff);

        if (value.length > 2) {
            measurement.setOperand(Arrays.copyOfRange(value, 2, value.length + 1));
        }

        return measurement;
    }

    public RecordAccessControlPoint() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.opCode);
        dest.writeInt(this.operator);
        dest.writeByteArray(this.operand);
    }

    protected RecordAccessControlPoint(Parcel in) {
        super(in);
        this.opCode = in.readInt();
        this.operator = in.readInt();
        this.operand = in.createByteArray();
    }

    public static final Parcelable.Creator<RecordAccessControlPoint> CREATOR = new Parcelable.Creator<RecordAccessControlPoint>() {
        public RecordAccessControlPoint createFromParcel(Parcel source) {
            return new RecordAccessControlPoint(source);
        }

        public RecordAccessControlPoint[] newArray(int size) {
            return new RecordAccessControlPoint[size];
        }
    };
}

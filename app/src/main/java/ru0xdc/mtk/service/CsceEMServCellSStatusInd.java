package ru0xdc.mtk.service;


import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * [RR 3G CsceEMServCellSStatusInd]
 */
public class CsceEMServCellSStatusInd implements Parcelable {

    public final int refCount;

    public final int msgLen;

    public final int celIdx;

    public final long uarfacnDl;

    public final int psc;

    public final boolean isCriteriaSatisfied;

    public final int qQualMin;

    public final int qRxLevMin;

    public final long srxlev;

    public final long spual;

    public final float rscp;

    public final float ecno;

    public final int cycleLen;

    public final int qualityMeasure;

    public final int band;

    public final int rssi;

    public final long cellIdentity;

    short get1ub(byte buf[], int pos) {
        return (short)(buf[pos] & 0xff);
    }


    public CsceEMServCellSStatusInd(String hexResponse) {
        this(hexResponse, false);
    }

    public CsceEMServCellSStatusInd(String hexResponse, boolean isTdd) {
        // E7F80B000E03D929E00001EE8D00000000500100004400000020FAFF0024FFFF06000001B0FFFFFFF2014B05
        // E7 F8 0B00 0E 03 D929 E000 01 EE 8D 000000 00500100 00440000 0020FAFF 0024FFFF 0600 00 01 B0FFFFFF F2014B05
        // ref_count: 231
        // msg_len: 11
        // cell_idx: 14
        // uarfacnDl: 10713
        // psc: 224
        // isCriteriaSatisfied: 1
        // qQualMin: -18
        // qRxLevMin: -115
        // srxlev: 86016
        // spual: 17408
        // rscp: -94
        // ecno: -13.75
        // cyclelen: 6
        // quality_measure: 0
        // band: 1
        // rssi: -80 (0xffffffb0)
        // cell_ident: 88801778 (0x054b01f2)
        ByteBuffer bb = ByteBuffer.wrap(hexStringToByteArray(hexResponse)).order(ByteOrder.LITTLE_ENDIAN);

        refCount = bb.get() & 0xff;
        bb.get();  // alignment
        msgLen = bb.getShort() & 0xffff;
        celIdx = bb.get() & 0xff;
        bb.get();  // alignment
        uarfacnDl = bb.getShort() & 0xffff;
        psc = bb.getShort() & 0xffff;
        isCriteriaSatisfied = bb.get() != 0;
        qQualMin = bb.get();
        qRxLevMin = bb.get();
        bb.position(bb.position() + 3); // alignment
        srxlev = bb.getInt();
        spual = bb.getInt();
        rscp = bb.getInt() / 4096f;
        if (!isTdd) {
            ecno = bb.getInt() / 4096f;
        } else {
            ecno = 0;
        }
        cycleLen = bb.getShort() & 0xffff;
        if (!isTdd)
            qualityMeasure = bb.get() & 0xff;
        else
            qualityMeasure = 0;
        band = bb.get() & 0xff;
        if (isTdd) bb.get(); // alignment
        rssi = bb.getInt();
        cellIdentity = bb.getInt() & 0xffffffffl;
    }

    public CsceEMServCellSStatusInd(Parcel in) {
        refCount = in.readInt();
        msgLen = in.readInt();
        celIdx = in.readInt();
        uarfacnDl = in.readLong();
        psc = in.readInt();
        isCriteriaSatisfied = in.readInt() != 0;
        qQualMin = in.readInt();
        qRxLevMin = in.readInt();
        srxlev = in.readLong();
        spual = in.readLong();
        rscp = in.readFloat();
        ecno = in.readFloat();
        cycleLen = in.readInt();
        qualityMeasure = in.readInt();
        band = in.readInt();
        rssi = in.readInt();
        cellIdentity = in.readLong();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @Override
    public String toString() {
        return "ref_count: " + refCount
                + " msg_len: " + msgLen
                + " cell_idx: " + celIdx
                + " uarfacnDl: " + uarfacnDl
                + " psc: " + psc
                + " isCriteriaSatisfied: " + isCriteriaSatisfied
                + " qQualMin: " + qQualMin
                + " qRxLevMin: " + qRxLevMin
                + " srxlev: " + srxlev
                + " spual: " + spual
                + " rscp: " + rscp
                + " ecno: "  + ecno
                + " cyclelen: " + cycleLen
                + " quality_measure: " + qualityMeasure
                + " band: " + band
                + " rssi: " + rssi
                + " cell_ident: "  + cellIdentity;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(refCount);
        dest.writeInt(msgLen);
        dest.writeInt(celIdx);
        dest.writeLong(uarfacnDl);
        dest.writeInt(psc);
        dest.writeInt(isCriteriaSatisfied ? 1 : 0);
        dest.writeInt(qQualMin);
        dest.writeInt(qRxLevMin);
        dest.writeLong(srxlev);
        dest.writeLong(spual);
        dest.writeFloat(rscp);
        dest.writeFloat(ecno);
        dest.writeInt(cycleLen);
        dest.writeInt(qualityMeasure);
        dest.writeInt(band);
        dest.writeInt(rssi);
        dest.writeLong(cellIdentity);
    }

    public static final Parcelable.Creator<CsceEMServCellSStatusInd> CREATOR = new
            Parcelable.Creator<CsceEMServCellSStatusInd>() {
                public CsceEMServCellSStatusInd createFromParcel(Parcel in) {
                    return new CsceEMServCellSStatusInd(in);
                }

                public CsceEMServCellSStatusInd[] newArray(int size) {
                    return new CsceEMServCellSStatusInd[size];
                }
            };

}

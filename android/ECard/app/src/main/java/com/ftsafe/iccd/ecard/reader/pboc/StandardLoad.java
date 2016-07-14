package com.ftsafe.iccd.ecard.reader.pboc;

import com.ftsafe.iccd.ecard.bean.Card;
import com.ftsafe.iccd.ecard.reader.pboc.*;
import com.ftsafe.iccd.ecard.reader.pboc.StandardPboc;

import java.io.IOException;
import java.util.ArrayList;

import ftsafe.reader.tech.Iso7816;

/**
 * Created by qingyuan on 16-7-14.
 */
public class StandardLoad extends com.ftsafe.iccd.ecard.reader.pboc.StandardPboc {

    @Override
    protected Object getApplicationId() {
        return null;
    }

    @Override
    protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException {
        final ArrayList<Iso7816.ID> aids = getApplicationIds(tag);

        for (Iso7816.ID aid:aids) {
            /*--------------------------------------------------------------*/
            // select application
            /*--------------------------------------------------------------*/
            Iso7816.Response rsp = tag.selectByName(aid.getBytes());
            if (rsp.isOkey() == false)
                continue;

            final Iso7816.BerHouse subTLVs = new Iso7816.BerHouse();

        }
        return card.isUnknownCard() ? HINT.RESETANDGONEXT : HINT.STOP;
    }

    private final Iso7816.BerHouse topTLVs = new Iso7816.BerHouse();

    private ArrayList<Iso7816.ID> getApplicationIds(Iso7816.StdTag tag) throws IOException {

        final ArrayList<Iso7816.ID> ret = new ArrayList<Iso7816.ID>();

        // try to read DDF
        Iso7816.BerTLV sfi = topTLVs.findFirst(Iso7816.BerT.CLASS_SFI);
        if (sfi != null && sfi.length() == 1) {
            final int SFI = sfi.v.toInt();
            Iso7816.Response r = tag.readRecord(SFI, 1);
            for (int p = 2; r.isOkey(); ++p) {
                Iso7816.BerTLV.extractPrimitives(topTLVs, r);
                r = tag.readRecord(SFI, p);
            }
        }

        // add extracted
        ArrayList<Iso7816.BerTLV> aids = topTLVs.findAll(Iso7816.BerT.CLASS_AID);
        if (aids != null) {
            for (Iso7816.BerTLV aid : aids)
                ret.add(new Iso7816.ID(aid.v.getBytes()));
        }

        // use default list
        if (ret.isEmpty()) {
            ret.add(new Iso7816.ID(StandardECash.AID_DEBIT));
            ret.add(new Iso7816.ID(StandardECash.AID_CREDIT));
            ret.add(new Iso7816.ID(StandardECash.AID_QUASI_CREDIT));
        }

        return ret;
    }
}

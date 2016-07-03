package com.ftsafe.iccd.ecard.pojo;

/**
 * Created by takakiyo on 15/8/10.
 */
public class TransactionLog {

    private String terminalName = null, type = null, money = null, date = null;

    public TransactionLog(String terminalName, String type, String money, String date) {
        this.terminalName = terminalName;
        this.type = type;
        this.money = money;
        this.date = date;
    }

    public String getTerminalName() {
        return terminalName;
    }

    public String getType() {
        return type;
    }

    public String getMoney() {
        return money;
    }

    public String getDate() {
        return date;
    }
}

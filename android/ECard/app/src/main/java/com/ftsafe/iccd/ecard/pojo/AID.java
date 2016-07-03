package com.ftsafe.iccd.ecard.pojo;

public enum AID {

	PBOC_DEBIT("A000000333010101"),
	PBOC_CREDIT("A000000333010102"),
	PBOC_QUASICREDIT("A000000333010103"),
	PBOC_ELECTRONIC_CASH("A000000333010106"),
	PBOC_DEBIT_CREDIT("A0000003330101"),
	VISA_DEBIT("A0000000032010"),
	VISA_CREDIT_DEBIT("A0000000031010"),
	ZJJT("FF485A4A54010106");
	
	private String aid;
	
	private AID(String aid)
	{
		this.aid = aid;
	}
	
	public String getAid(){
		return aid;
	}
}
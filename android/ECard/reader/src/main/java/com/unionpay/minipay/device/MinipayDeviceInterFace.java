package com.unionpay.minipay.device;

public interface MinipayDeviceInterFace {
	  public String SCardStatus();
	  public void ScardDisconnect();
	  public int ScardConnect(Object paramObject) throws ReaderException;
	  public String ScardTransmit(byte[] paramArrayOfByte, int paramInt) throws ReaderException;
	  public byte []ScardTransmit1(byte[] paramArrayOfByte, int paramInt) throws ReaderException ;
}

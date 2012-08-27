package com.sodao.dubbo.thrift.common;

import org.apache.thrift.protocol.TMessageType;

public class ThriftConstants {
	public static final String INNER_PROCESSOR = "$Processor";
	public static final int SIZE = 1024;
    public static final int VERSION_1 = 0x80010000;
	public static final int VERSION = VERSION_1 | TMessageType.CALL;//TBinaryprotocol.VERSION_1=0x80010000
	public static final String UTF8 = "UTF-8";
	public static final String _RESULT = "_result";
	public static final String _ARGS = "_args";
	public static final int VERSION_MASK = 0xffff0000;
	public static final String SEQ_ID = "seqId";
	public static final String POISON_METHOD = "poisonMethod";//毒丸方法，用于服务器端异常时，构造异常的TMessage
}

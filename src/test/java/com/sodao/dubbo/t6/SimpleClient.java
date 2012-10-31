package com.sodao.dubbo.t6;

import java.util.concurrent.TimeUnit;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;


public class SimpleClient {
	public static void main(String[] args) throws Exception {
		
		TTransport transport = new TSocket("localhost", 28088);//28088
		
//		TProtocol protocol = new TBinaryProtocol(transport);
		
		TProtocol protocol = new TBinaryProtocol(new TFramedTransport((transport)));
		
		transport.open();
		
		HelloService.Client client = new HelloService.Client(protocol);
		for (int i = 0; i <Integer.MAX_VALUE; i++) {
			try {
				String str = client.getString("hello" + i);
				if (Integer.parseInt(str.substring(5)) != i + 1) {
					throw new IllegalStateException("result is error!");
				}
				System.out.println(str);
				TimeUnit.SECONDS.sleep(1);
			} catch(Exception e) {
//				System.out.println("--------------------" + i);
//				System.out.println();
				e.printStackTrace();
			}
		}
//		client.sayHello("hello");

		transport.close();
		
		System.out.println("end");
	}
}

package com.sodao.dubbo.t6;

import org.apache.thrift.TException;


public class HelloServiceImpl implements HelloService.Iface {
	
	@Override
	public void sayHello(String str) throws TException {
		System.out.println("sayHello:" + str);
//		throw new NullPointerException("test nullpointer");
	}

	@Override
	public String getString(String str) throws TException {
		String sub = str.substring(0, 5);
		int i = Integer.parseInt(str.substring(5));
		i = i + 1;
//		try {
//			Thread.sleep(10000);
//		} catch (Exception e) {
//			
//		}
		return sub + i;
//		throw new NullPointerException("test nullpointer");
	}

	@Override
	public User getUser(int id, String name, int age) throws Xception,
			TException {
		return new User(id, name, age);
	}
}

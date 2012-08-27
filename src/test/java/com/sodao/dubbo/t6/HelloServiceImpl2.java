package com.sodao.dubbo.t6;

import org.apache.thrift.TException;


public class HelloServiceImpl2 implements HelloService2.Iface {
	
	@Override
	public void sayHello(String str) throws TException {
		System.out.println("sayHello:" + str);
//		throw new NullPointerException("test nullpointer");
	}

	@Override
	public User getUser(int id, String name, int age) throws Xception,
			TException {
		return new User(id, "HelloServiceImpl2." + name, age);
	}

	@Override
	public String getString(String str) throws TException {
		String sub = str.substring(0, 5);
		int i = Integer.parseInt(str.substring(5));
		i = i + 1;
		return "HelloServiceImpl2." + sub + i;
	}
}

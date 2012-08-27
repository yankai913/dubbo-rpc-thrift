package com.sodao.dubbo.t6;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DubboProvider {
	public static void main(String[] args) throws Exception{
		String path = 
				DubboProvider.class.getPackage().getName().replace(".", "/")
				+ "/demo-provider.xml";
		
		ClassPathXmlApplicationContext ctx = 
				new ClassPathXmlApplicationContext(path);
		ctx.start();
		System.in.read();
	}
}

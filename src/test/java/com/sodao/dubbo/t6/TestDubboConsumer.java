package com.sodao.dubbo.t6;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestDubboConsumer {
	
	public static void main(String[] args) throws Exception{
		String path = 
				DubboConsumer.class.getPackage().getName().replace(".", "/")
				+ "/demo-consumer.xml";
		
		ClassPathXmlApplicationContext ctx = 
				new ClassPathXmlApplicationContext(path);
		
		final HelloService.Iface service = (HelloService.Iface) ctx.getBean("helloService");
		
		service.getUser(1, "tom", 24);
	}
}

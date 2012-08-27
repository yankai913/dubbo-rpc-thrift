package com.sodao.dubbo.t6;

import java.util.concurrent.Future;

import org.apache.thrift.TException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.rpc.RpcContext;

public class DubboConsumer {
	static class Task extends Thread {
		HelloService.Iface service;
		public Task(HelloService.Iface service) {
			this.service = service;
		}
		
		@Override
		public void run() {
			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				try {
					String str = service.getString("hello" + i);
					if (Integer.parseInt(str.substring(5)) != i + 1) {
					throw new IllegalStateException("result is error!");
				}
				} catch (TException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		
			
		String path = 
				DubboConsumer.class.getPackage().getName().replace(".", "/")
				+ "/demo-consumer.xml";
		
		ClassPathXmlApplicationContext ctx = 
				new ClassPathXmlApplicationContext(path);
		
		final HelloService.Iface service = (HelloService.Iface) ctx.getBean("helloService");
		for (int i = 0; i < 10; i++) {
			new Task(service).start();
		}
			
			
//		HelloService2.Iface service2 = (HelloService2.Iface) ctx.getBean("helloService2");
//		EchoService echoService = (EchoService) service; // 强制转型为EchoService
//		String status = (String) echoService.$echo("OK"); // 回声测试可用性
//		try {
//		for (int i = 0; i < Integer.MAX_VALUE; i++) {
//			try {
//			
//			service.sayHello("hello");
//			Future<Object> void_future = RpcContext.getContext().getFuture();
			
//			String str = service.getString("hello" + i);
//			String str = service.getString("hello" + i);
			
//			Future<String> string_future = RpcContext.getContext().getFuture();
			
//			String str2 = service2.getString("hello" + i);
//			Future<String> string2_future = RpcContext.getContext().getFuture();
			
//			service.getUser(i, "aa", i);
//			Future<User> user_future = RpcContext.getContext().getFuture();
			
//			System.out.println(void_future.get());
//			System.out.println(str);
//			String str = string_future.get();
//			if (Integer.parseInt(str.substring(5)) != i + 1) {
//				throw new IllegalStateException("result is error!");
//			}
//			System.out.println(str);
//			String str2 = string2_future.get();
//			if (str2.split("[.]")[0].equals("HelloServiceImpl2")
//					&& Integer.parseInt(str2.split("[.]")[1].substring(5)) != i + 1
//					 ) {
//				throw new IllegalStateException("result is error!");
//			}
//			System.out.println(str2);
//			
////			User user = service.getUser(i, "aa", i);
//			User user = user_future.get();
//			if (user == null) {
//				throw new IllegalStateException("user is error!");
//			}
//			System.out.println(user);
				
//			} catch (Throwable t) {
//				t.printStackTrace();
//				if (t instanceof IllegalStateException) {
//					System.exit(0);
//				}
//			}
//			break;
//		}
//		service.sayWorld("world");
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//		}
//		System.out.println(RpcContext.getContext().getFuture()); 
		System.in.read();
		
		
	}
}

package com.sodao.dubbo.t6;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;


public class SimpleServer {
	public static void main(String[] argss) throws Exception {
		
		HelloService.Iface impl = new HelloServiceImpl();
		
		TProtocolFactory protocolFactory = new  TBinaryProtocol.Factory();
		
		TServerTransport serverSocket = new TServerSocket(7911);
		
		TSimpleServer.Args args = new TSimpleServer.Args(serverSocket);
		
		args.processor(new HelloService.Processor<HelloService.Iface>(impl));
		
		args.protocolFactory(protocolFactory);
		
		TServer server = new TSimpleServer(args); 
		
		server.serve();
	}
}

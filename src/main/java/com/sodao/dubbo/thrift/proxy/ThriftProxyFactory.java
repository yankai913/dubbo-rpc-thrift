package com.sodao.dubbo.thrift.proxy;

import static com.sodao.dubbo.thrift.common.ThriftConstants.INNER_PROCESSOR;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.Proxy;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyFactory;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker;
import com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler;
import com.sodao.dubbo.thrift.codec.ThriftExchangeCodec;
import com.sodao.dubbo.thrift.codec.ThriftExchangeCodec.BufferWrapper;
import com.sodao.dubbo.thrift.common.TBaseTools;
import com.sodao.dubbo.thrift.common.ThriftBuffer;
import com.sodao.dubbo.thrift.transport.ThriftTransport;
/**
 * 
 * @author yankai
 * @date 2012-3-28
 */
public class ThriftProxyFactory extends AbstractProxyFactory{

	private static final Logger logger = LoggerFactory.getLogger(ThriftProxyFactory.class);
			
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName, 
                                      Class<?>[] parameterTypes, 
                                      Object[] arguments) throws Throwable {
            	
        		ThriftBuffer outputBuffer = new ThriftBuffer();
        		Long bufferId = (Long) arguments[arguments.length - 1];
        		Class<?> iface = proxy.getClass().getInterfaces()[0];
        		ThriftBuffer inputBuffer = null;
        		TTransport transport = null;
        		BufferWrapper bufferWrapper = null;
        		try {
        			bufferWrapper = ThriftExchangeCodec.getBuffer(bufferId);
        			inputBuffer = bufferWrapper.getBuffer();
        			inputBuffer.resetReaderIndex();
        			transport = new ThriftTransport(inputBuffer.getBuffer(), outputBuffer.getBuffer());
        			TBinaryProtocol protocol = new TBinaryProtocol(transport);
        			getTProcessor(iface, proxy).process(protocol, protocol);
        		} catch (Throwable e) {
        			/**
        			 * 1.在方法定义的时候没有定义抛出异常，并且在方法执行时又抛出异常，这个时候异常信息就会在这里捕获。
        			 * 在thrift内部方法执行的过程中，没有对异常信息进行捕获，所以异常会被抛到这里。
        			 * 2.在方法执行的外部，也就是说，程序还没有跑到具体执行方法处，就抛出了异常，在这里也可以捕获，并返回给客户端，
        			 * 但是有个前提，TMessage信息必须拥有。
        			 */
        			logger.error(e);
        			
        			if (outputBuffer.getBuffer().writerIndex() > 0) {
        				outputBuffer.resetWriterIndex();
        			}
        			int seqId = 1;
        			if (bufferWrapper != null) {
        				inputBuffer.resetReaderIndex();
            			inputBuffer.readInt();//过version
            			inputBuffer.readUTF();//跳过方法名
            			seqId = inputBuffer.readInt();//获取seqId
        			}
        			TBaseTools.createErrorTMessage(outputBuffer, methodName, seqId, e.toString());
        		} finally {
        			ThriftExchangeCodec.removeBuffer(bufferId);
        		}
        		return outputBuffer.array();
            }
        };
    }

    private static TProcessor getTProcessor(Class<?> serviceIface, Object serviceImpl) {
		try {
			if (serviceImpl == null) {
				throw new IllegalStateException("serviceImpl is null, can not create TProcessor");
			}
			TProcessor processor = serviceClazz2Processor.get(serviceIface);
			if (processor == null) {
				String iface = serviceIface.getName();
				String iface_prefix = iface.substring(0, iface.lastIndexOf("$"));
				Class<?> proServiceClazz = Class.forName(iface_prefix + INNER_PROCESSOR);
				processor = (TProcessor) proServiceClazz.getConstructor(serviceIface).newInstance(serviceImpl);
				serviceClazz2Processor.putIfAbsent(serviceIface, processor);
			}
			return processor;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}
	
	private static ConcurrentHashMap<Class<?>, TProcessor> serviceClazz2Processor = new ConcurrentHashMap<Class<?>, TProcessor>();
}

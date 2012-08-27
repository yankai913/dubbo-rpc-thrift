package com.sodao.dubbo.thrift.codec;

import static com.sodao.dubbo.thrift.common.ThriftConstants.SEQ_ID;
import static com.sodao.dubbo.thrift.common.ThriftConstants.VERSION;
import static com.sodao.dubbo.thrift.common.ThriftConstants.VERSION_1;
import static com.sodao.dubbo.thrift.common.ThriftConstants.VERSION_MASK;
import static com.sodao.dubbo.thrift.common.ThriftConstants._ARGS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.transport.codec.TransportCodec;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.sodao.dubbo.thrift.common.TBaseTools;
import com.sodao.dubbo.thrift.common.ThriftBuffer;
import com.sodao.dubbo.thrift.common.ThriftConstants;
/**
 * 创建自己的exchangeCodec， 在这里不需要继承telnetCodec，
 * 目前只支持一个端口上暴露一个service，(threadSafe, prototype)
 * @author yankai
 * @date 2012-4-5
 */
public class ThriftExchangeCodec extends TransportCodec {
	
	public static final String NAME = "thrift2";
	
	private static final Logger logger = LoggerFactory.getLogger(ThriftExchangeCodec.class);
	
	/**用于服务器端，设为静态，作为请求buffer的全局id*/
	private static final AtomicLong BUFFER_ID = new AtomicLong();
	
	/**
	 * 用于服务器端，设为静态，作为全局变量<br>
	 * 1. 缓存客户端请求服务端的buffer，原因是在方法具体执行时用的buffer，而不是方法名，参数列表
	 * 这样在方法具体执行时，减少一步序列化操作，用在decodeRequest。<br>
	 * 2. 有多个thrift-client时，seqId可能相同，所以必须用buffer_id来标明唯一性。
	 */
	private static final ConcurrentHashMap<Long, BufferWrapper> BUFFERS = new ConcurrentHashMap<Long, BufferWrapper>();
	
	/**
	 * 客户端变量，解决reqId强转为int型后，在DefaultFuture里找不到自己的future,
	 * 用于记录当requestId大于int类型的最大值时强转，然后关联<seqId, requestId>*/
	private final ConcurrentHashMap<Integer, Long> numLinks = new ConcurrentHashMap<Integer, Long>();
	
	/**
	 * 1. 属于客户端变量<br>
	 * 2. 用在客户端的encodeRequest, decodeResponse，区别客户端调用方法时，方法名相同而引起的找不到自己的接口类问题
	 * key为方法名+seqId，seqId是唯一的<br>
	 * 3. 一个ExchangeClient，一个Codec
	 */
	private final ConcurrentHashMap<String, String> requestClientIfaceMap = new ConcurrentHashMap<String, String>();
	
	/**
	 * 1. nettyServer2实例化时装入URL，目的是区分哪个port对应处理哪个服务，基本原因还是方法名重复问题导致<br>
	 * 2. nettyClient2是可以不用装入URL，目前没有装入，所以暂时只用于服务端<br>
	 */
	private URL url;

	public ThriftExchangeCodec() {
		
	}
	
	public ThriftExchangeCodec(URL url) {
		if (url == null) {
			throw new NullPointerException("url is null");
		}
		this.url = url;
	}

	public static BufferWrapper getBuffer(Long key) {
		return BUFFERS.get(key);
	}
	
	public static BufferWrapper removeBuffer(Long key) {
		return BUFFERS.remove(key);
	}
	
	private String removeRequestClientIface(String key) {
		return requestClientIfaceMap.remove(key);
	}
	
	/**
	 * 用在encodeRequest，解决在用dubbo客户端调方法时，如果方法名相同，在decodeResponse时，保证找到方法所属的
	 * 正确的iface$methodname_result对象，这个缓存缓存维持在客户端本地
	 */
	private void putRequestClientIface(String key, String value) {
		requestClientIfaceMap.putIfAbsent(key, value);
	}
	
	@Override
	public void encode(Channel channel, OutputStream output, Object message) throws IOException {
		if (message instanceof Request) {
			encodeRequest(output, (Request) message);
		} else if (message instanceof Response) { 
			encodeResponse(channel, output, (Response) message);
		} else if (message instanceof byte[]) { 
			output.write((byte[])message);
		} else {
			super.encode(channel, output, message);
		}
	}

	protected void encodeRequest(OutputStream os, Request req) throws IOException { 
		RpcInvocation inv = (RpcInvocation) req.getData();
		/**
		 * 用thrift原生协议，seqId是用来做消息号的，但它是int型的，但是RequestId是long型的，
		 * 不可以强转，如果把int强转为long后，当执行到DefaultFuture时，Id发生变化，找不到对应的future。
		 * 所以只可以关联，可以用静态的map存储关联关系，因为request的id也是全局的。
		 * 记住强转前的long值，便于在decodeResponse里恢复以前的long型的requestId，
		 * 用在dubbo客户端里。
		 */
		long requestId = req.getId();
		int seqId = (int)requestId;
		if (requestId > Integer.MAX_VALUE || requestId < Integer.MIN_VALUE) {
			numLinks.put(seqId, requestId);
		}

		String iface = inv.getAttachment(Constants.PATH_KEY);
        String methodName = inv.getMethodName();
        Class<?>[] paramTypes = inv.getParameterTypes();
        Object[] args = inv.getArguments();
        
        ThriftBuffer out = new ThriftBuffer(); 
        //write TMessage
        out.writeInt(VERSION); out.writeUTF(methodName); out.writeInt(seqId);
        //请求方法的接口名称缓存起来，缓存在客户端本地
        putRequestClientIface(methodName + seqId, iface);
		try {
			TBase<?,?> _args = TBaseTools.getTBaseObj(iface, methodName, paramTypes, args, _ARGS);
			byte[] argsArr = TBaseTools.tBase2buffer(_args);
			out.writeBytes(argsArr);
		} catch (Exception e) {
			throw new RpcException(e);
		}
		os.write(out.array());
	}
	
	protected void encodeResponse(Channel channel, OutputStream os, Response res) throws IOException {
		int status = res.getStatus();
		//这里强转是安全的，在客户端有int型的seqId和reqId的缓存关联
		int resId = (int)res.getId();
		try {
			String error = null;
			if (status == Response.OK) {
				Result result = (Result) res.getResult();
				Throwable th = result.getException();
				if (th == null) {
					os.write((byte[])result.getValue());
					return ;
				} 
				error = th.getMessage();
			} 
			if (error == null) {
				error = res.getErrorMessage();
			}
			logger.error(error);
			
			ThriftBuffer buffer = new ThriftBuffer();
			TBaseTools.createErrorTMessage(buffer, ThriftConstants.POISON_METHOD, resId, error);
			os.write(buffer.array());
		} catch (Throwable t) {
			logger.error(t);
			ThriftBuffer buffer = new ThriftBuffer();
			TBaseTools.createErrorTMessage(buffer, ThriftConstants.POISON_METHOD, resId, t.toString());
			os.write(buffer.array());
		}
	}
	
	@Override
	public Object decode(Channel channel, InputStream input) throws IOException {
		ThriftBuffer in = new ThriftBuffer(input);
		//read TMessage
		int version = in.readInt();
		String methodName = in.readUTF();
		int seqId = in.readInt();
		
		if (version == VERSION) {//decode request
			return decodeRequest(methodName, seqId, in);
		} else {//decode response
			return decodeResponse(version, methodName, seqId, in);
		}
	}
	
	
	protected Object decodeRequest(String methodName, int seqId, ThriftBuffer in) throws IOException { 
		long bufferId = BUFFER_ID.incrementAndGet();
		BUFFERS.put(bufferId, new BufferWrapper(bufferId, in, url.getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT)));
		
		Request req = new Request(seqId);
		RpcInvocation inv = new RpcInvocation();
		try {
			String iface = url.getPath();
			byte[] buf = in.array();//_args
			//根据类名和buffer反序列化出_args对象
	    	TBase<?, ?> argsObj = TBaseTools.buffer2TBase_args(iface, methodName, buf);
	    	//获取参数类型
	    	Class<?>[] paramTypes = TBaseTools.getParamterType(argsObj.getClass());
	    	//获取参数值
	    	Object[] args = TBaseTools.getArgs(argsObj);
	    	//封装invocation对象
	    	inv.setMethodName(methodName);
	    	inv.setArguments(args);
	    	inv.setParameterTypes(paramTypes);
	    	inv.setAttachment(Constants.PATH_KEY, iface);
	    	inv.setAttachment(SEQ_ID, String.valueOf(seqId));
			Object[] dest = new Object[inv.getArguments().length + 1];
			System.arraycopy(inv.getArguments(), 0, dest, 0, inv.getArguments().length);
			dest[dest.length - 1] = bufferId;
			inv.setArguments(dest);

			req.setData(inv);
		} catch (Throwable t) {// bad request
            req.setBroken(true);
            req.setData(t);
		}
		return req;
	}
	
	protected Object decodeResponse(int version, String methodName, int seqId, ThriftBuffer in) throws IOException {
		//首先去seqId和requestId关联的numLinks里判断是否有对应的requestId
		long requestId = seqId;
		if (numLinks.get(seqId) != null) {
			requestId = numLinks.remove(seqId);
		}
		Response res = new Response(requestId);
		try {
			byte type = (byte)(version & 0x000000ff);
			if (type == TMessageType.EXCEPTION) {
				TProtocol ipro = new TBinaryProtocol(new TMemoryInputTransport(in.array()));
				TApplicationException x = TApplicationException.read(ipro);
				throw new RpcException(x);
			}
			if (version > 0 || (version & VERSION_MASK) != VERSION_1) {
				throw new RpcException("Bad version in readMessageBegin");
			}
			String iface = removeRequestClientIface(methodName + seqId);
        	Object result = TBaseTools.getResult(in.array(), iface, methodName, seqId);
        	//服务器端的服务实体类的方法执行失败，失败信息封装在response中，在外部看来response是OK的，这样做目的是把错误消息返回给非java的客户端
        	if (result instanceof Exception) {
				throw (Throwable)result;
			}
			RpcResult obj = new RpcResult();
			obj.setValue(result);
			res.setResult(obj);
		} catch (Throwable t) {
			res.setStatus(Response.SERVER_ERROR);
            res.setErrorMessage(StringUtils.toString(t));
		}
		return res;
	}

	public static class BufferWrapper {
		private final Long id;
		private final ThriftBuffer buffer;
		private final int timeout;
		private final long startTimestamp = System.currentTimeMillis();
		
		public BufferWrapper(Long id, ThriftBuffer buffer, int timeout) {
			this.id = id;
			this.buffer = buffer;
			this.timeout = timeout;
		}

		public ThriftBuffer getBuffer() {
			return buffer;
		}

		public long getStartTimestamp() {
			return startTimestamp;
		}

		public Long getId() {
			return id;
		}

		public int getTimeout() {
			return timeout;
		}
	}
	
	private static class ThriftRequestBufferTimeoutScan implements Runnable {
        public void run() {
            while (true) {
                try {
                    for (BufferWrapper wrapper : BUFFERS.values()) {
                        if (wrapper == null ) {
                            continue;
                        }
                        if (System.currentTimeMillis() - wrapper.getStartTimestamp() 
                        		> wrapper.getTimeout()) {
                        	BUFFERS.remove(wrapper.getId());
                        }
                    }
                } catch (Throwable e) {
                    logger.error("Exception when scan the timeout request buffer of remoting.", e);
                } finally {
                	try {
                		//清理异常的buffer，一小时清理一次，因为异常的概率很小，同时减少线程上下文切换消耗。
						Thread.sleep(30);
					} catch (InterruptedException e) {
						logger.error(e);
					}
                }
            }
        }
    }

    static {
        Thread th = new Thread(new ThriftRequestBufferTimeoutScan(), "ThriftRequestBufferTimeoutScanTimer");
        th.setDaemon(true);
        th.start();
    }
}

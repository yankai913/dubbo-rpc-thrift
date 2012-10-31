package com.sodao.dubbo.thrift.exchange;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.remoting.exchange.support.DefaultFuture;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.sodao.dubbo.thrift.common.TBaseTools;

/**
 * 
 * @author yankai
 * @date 2012-8-27
 */
public class HeaderExchangeChannel2 implements ExchangeChannel {

    private static final Logger logger      = LoggerFactory.getLogger(HeaderExchangeChannel2.class);

    private static final String CHANNEL_KEY = HeaderExchangeChannel2.class.getName() + ".CHANNEL";

    private final Channel       channel;

    private volatile boolean    closed = false;

	protected static final AtomicInteger SEQUENCE = new AtomicInteger(0);// seqId generator

	public static final String SEQ_ID = "seqId";
	
    HeaderExchangeChannel2(Channel channel){
        if (channel == null) {
            throw new IllegalArgumentException("channel == null");
        }
        this.channel = channel;
    }

    static HeaderExchangeChannel2 getOrAddChannel(Channel ch) {
        if (ch == null) {
            return null;
        }
        HeaderExchangeChannel2 ret = (HeaderExchangeChannel2) ch.getAttribute(CHANNEL_KEY);
        if (ret == null) {
            ret = new HeaderExchangeChannel2(ch);
            if (ch.isConnected()) {
                ch.setAttribute(CHANNEL_KEY, ret);
            }
        }
        return ret;
    }
    
    static void removeChannelIfDisconnected(Channel ch) {
        if (ch != null && ! ch.isConnected()) {
            ch.removeAttribute(CHANNEL_KEY);
        }
    }
    
    public void send(Object message) throws RemotingException {
        send(message, getUrl().getParameter(Constants.SENT_KEY, false));
    }
    
    ChannelBuffer createRequestBuffer(int id, RpcInvocation inv) throws RemotingException {
    	 //serialize request
         ChannelBuffer output = ChannelBuffers.dynamicBuffer(512);
         TProtocol oprot = TBaseTools.newProtocol(null, output);
         
         String methodName = inv.getMethodName();
         String serviceName = inv.getAttachment(Constants.PATH_KEY);
         Class<?>[] parameterTypes = inv.getParameterTypes();
         Object[] arguments = inv.getArguments();
         
         try {
         	oprot.writeMessageBegin(new TMessage(methodName, TMessageType.CALL, id));
 			String argsServiceName = TBaseTools.getArgsClassName(serviceName, methodName, "_args");
            Class<?> clazz = TBaseTools.getTBaseClass(argsServiceName);
 			TBase<?, ?> _args = TBaseTools.getTBaseObject(clazz, parameterTypes, arguments);
 			_args.write(oprot);
 		} catch (Exception e) {
 			throw new RemotingException(channel, e);
 		}
        return output;
    }
    
    public void send(Object message, boolean sent) throws RemotingException {
        if (closed) {
            throw new RemotingException(this.getLocalAddress(), null, "Failed to send message " + message + ", cause: The channel " + this + " is closed!");
        }
        
        RpcInvocation inv = (RpcInvocation)message;
        int id = SEQUENCE.incrementAndGet();
        ChannelBuffer output = createRequestBuffer(id, inv);
        
        channel.send(output, sent);
    }

    public ResponseFuture request(Object request) throws RemotingException {
        return request(request, channel.getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
    }

    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        if (closed) {
            throw new RemotingException(this.getLocalAddress(), null, "Failed to send request " + request + ", cause: The channel " + this + " is closed!");
        }
        
        RpcInvocation inv = (RpcInvocation)request;
        int id = SEQUENCE.incrementAndGet();
        ChannelBuffer output = createRequestBuffer(id, inv);
     
        DefaultFuture2 future = new DefaultFuture2(id, channel, timeout, inv.getAttachment(Constants.PATH_KEY), inv.getMethodName());
        channel.send(output);
        return future;
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        try {
            channel.close();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    // graceful close
    public void close(int timeout) {
        if (closed) {
            return;
        }
        closed = true;
        if (timeout > 0) {
            long start = System.currentTimeMillis();
            while (DefaultFuture.hasFuture(HeaderExchangeChannel2.this) 
                    && System.currentTimeMillis() - start < timeout) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        close();
    }

    public InetSocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    public InetSocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    public URL getUrl() {
        return channel.getUrl();
    }

    public boolean isConnected() {
        return channel.isConnected();
    }

    public ChannelHandler getChannelHandler() {
        return channel.getChannelHandler();
    }

    public ExchangeHandler getExchangeHandler() {
        return (ExchangeHandler) channel.getChannelHandler();
    }
    
    public Object getAttribute(String key) {
        return channel.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        channel.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        channel.removeAttribute(key);
    }

    public boolean hasAttribute(String key) {
        return channel.hasAttribute(key);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        HeaderExchangeChannel2 other = (HeaderExchangeChannel2) obj;
        if (channel == null) {
            if (other.channel != null) return false;
        } else if (!channel.equals(other.channel)) return false;
        return true;
    }

    @Override
    public String toString() {
        return channel.toString();
    }
  
}

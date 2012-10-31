package com.sodao.dubbo.thrift.exchange;

import java.net.InetSocketAddress;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
/**
 * 
 * @author yankai
 * @date 2012-8-27
 */
public class HeaderExchangeClient2 implements ExchangeClient {

    private final Client client;

    private final ExchangeChannel channel;

    public HeaderExchangeClient2(Client client){
        if (client == null) {
            throw new IllegalArgumentException("client == null");
        }
        this.client = client;
        this.channel = new HeaderExchangeChannel2(client);
    }

    public ResponseFuture request(Object request) throws RemotingException {
       return channel.request(request);
    }

    public URL getUrl() {
        return channel.getUrl();
    }

    public InetSocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        return channel.request(request, timeout);
    }

    public ChannelHandler getChannelHandler() {
        return channel.getChannelHandler();
    }

    public boolean isConnected() {
        return channel.isConnected();
    }

    public InetSocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    public ExchangeHandler getExchangeHandler() {
        return channel.getExchangeHandler();
    }
    
    public void send(Object message) throws RemotingException {
        channel.send(message);
    }
    
    public void send(Object message, boolean sent) throws RemotingException {
        channel.send(message, sent);
    }

    public boolean isClosed() {
        return channel.isClosed();
    }

    public void close() {
        channel.close();
    }

    public void close(int timeout) {
        channel.close(timeout);
    }

    public void reset(URL url) {
        client.reset(url);
    }
    
    @Deprecated
    public void reset(com.alibaba.dubbo.common.Parameters parameters){
        reset(getUrl().addParameters(parameters.getParameters()));
    }

    public void reconnect() throws RemotingException {
        client.reconnect();
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

}

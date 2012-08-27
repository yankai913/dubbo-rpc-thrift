/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sodao.dubbo.thrift.netty;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.exchange.Response;

/**
 * NettyCodecAdapter.
 * 
 * @author qian.lei
 */
final class NettyCodecAdapter2 {

    private final ChannelHandler encoder = new InternalEncoder();
    
    private final ChannelHandler decoder = new InternalDecoder();

    private final Codec          upstreamCodec;
    private final Codec          downstreamCodec;
    
    private final URL            url;
    
    private final com.alibaba.dubbo.remoting.ChannelHandler handler;

    public NettyCodecAdapter2(Codec codec, URL url, com.alibaba.dubbo.remoting.ChannelHandler handler){
        this(codec, codec, url, handler);
    }
    
    /**
     * server 端如果有消息发送需要分开codec，默认的上行code是dubbo1兼容的
     */
    public NettyCodecAdapter2(Codec upstreamCodec, Codec downstreamCodec, URL url, com.alibaba.dubbo.remoting.ChannelHandler handler){
        this.downstreamCodec = downstreamCodec;
        this.upstreamCodec = upstreamCodec;
        this.url = url;
        this.handler = handler;
    }

    public ChannelHandler getEncoder() {
        return encoder;
    }

    public ChannelHandler getDecoder() {
        return decoder;
    }

    @Sharable
    private class InternalEncoder extends OneToOneEncoder {

        @Override
        protected Object encode(ChannelHandlerContext ctx, Channel ch, Object msg) throws Exception {
            UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(1024); // 不需要关闭
            NettyChannel2 channel = NettyChannel2.getOrAddChannel(ch, url, handler);
            try {
                if(!(msg instanceof Response)){
                    downstreamCodec.encode(channel, os, msg);
                }else {
                    upstreamCodec.encode(channel, os, msg);
                }
            } finally {
                NettyChannel2.removeChannelIfDisconnected(ch);
            }
            return ChannelBuffers.wrappedBuffer(os.toByteBuffer());
        }
    }

    private class InternalDecoder extends SimpleChannelUpstreamHandler {

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
            Object o = event.getMessage();
            if (! (o instanceof ChannelBuffer)) {
                ctx.sendUpstream(event);
                return;
            }

            ChannelBuffer input = (ChannelBuffer) o;
            int readable = input.readableBytes();
            if (readable <= 0) {
                return;
            }
            
            NettyChannel2 channel = NettyChannel2.getOrAddChannel(ctx.getChannel(), url, handler);
            byte[] arr = new byte[readable];
            input.readBytes(arr);
            
            UnsafeByteArrayInputStream bis = new UnsafeByteArrayInputStream(arr); // 不需要关闭
            try {
            	Object msg = upstreamCodec.decode(channel, bis);
                Channels.fireMessageReceived(ctx, msg, event.getRemoteAddress());
                NettyChannel2.removeChannelIfDisconnected(ctx.getChannel());
            }
            catch (IOException e)
            {
            	e.printStackTrace();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            ctx.sendUpstream(e);
        }
    }
}
package com.sodao.dubbo.thrift.exchange;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Transporters;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchanger;
/**
 * 
 * @author yankai
 * @date 2012-8-27
 */
public class HeaderExchanger2 implements Exchanger {
    
    public static final String NAME = "header2";

    public ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException {
        return new HeaderExchangeClient2(Transporters.connect(url, new HeaderExchangeClientHandler(handler)));
    }

    public ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
        return new HeaderExchangeServer2(Transporters.bind(url, new HeaderExchangeServerHandler(handler)));
    }

}

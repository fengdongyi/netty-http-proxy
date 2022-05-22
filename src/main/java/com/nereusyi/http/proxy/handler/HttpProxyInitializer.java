package com.nereusyi.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

public class HttpProxyInitializer extends ChannelInitializer<Channel> {

    private final Channel inboundChannel;

    public HttpProxyInitializer(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    protected void initChannel(Channel ch) {
        ch.pipeline().addLast(new HttpClientCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(65535));
        ch.pipeline().addLast(new HttpProxyHandler(inboundChannel));
    }
}

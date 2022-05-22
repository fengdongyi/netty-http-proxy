package com.nereusyi.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class HttpProxyHandler extends ChannelInboundHandlerAdapter {

    private final Channel inboundChannel;

    public HttpProxyHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        inboundChannel.writeAndFlush(msg);
    }
}

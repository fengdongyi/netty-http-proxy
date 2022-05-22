package com.nereusyi.http.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class HttpsProxyHandler extends ChannelInboundHandlerAdapter {

    private final Channel outboundChannel;

    public HttpsProxyHandler(Channel outboundChannel) {
        this.outboundChannel = outboundChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        outboundChannel.writeAndFlush(msg);
    }
}

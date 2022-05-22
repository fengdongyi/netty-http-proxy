package com.nereusyi.http.proxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;


public class HttpProxyHandshakeHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger();

    private static final HttpResponseStatus CONNECTION_ESTABLISHED = HttpResponseStatus.valueOf(200, "Connection Established");

    @Override
    public void channelRead(ChannelHandlerContext inboundCtx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest request)) {
            log.error("unsupported request...");
            return;
        }
        URI uri = new URI(request.uri());

        // HTTPS代理
        if (HttpMethod.CONNECT.equals(request.method())) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(inboundCtx.channel().eventLoop())
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new ReplyInboundHandler(inboundCtx.channel()));
                        }
                    });
            ChannelFuture cf = bootstrap.connect(uri.getScheme(), Integer.parseInt(uri.getSchemeSpecificPart()));
            cf.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, CONNECTION_ESTABLISHED);
                    inboundCtx.writeAndFlush(response).addListener((ChannelFutureListener) f1 -> {
                        if (f1.isSuccess()) {
                            ChannelPipeline pipeline = inboundCtx.pipeline();
                            pipeline.remove(HttpServerCodec.class);
                            pipeline.remove(HttpObjectAggregator.class);
                            pipeline.remove(HttpProxyHandshakeHandler.this);
                            pipeline.addLast(new HttpsProxyHandler(future.channel()));
                        }else{
                            log.error("reply to client failed,uri={}", uri);
                            inboundCtx.channel().close();
                        }
                    });
                } else {
                    log.error("connect to server failed,uri={}", uri);
                    inboundCtx.channel().close();
                }
            });
        } else {
            // HTTP代理
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(inboundCtx.channel().eventLoop())
                    .channel(NioSocketChannel.class)
                    .handler(new HttpProxyInitializer(inboundCtx.channel()));

            ChannelFuture cf = bootstrap.connect(uri.getHost(), uri.getPort() <= 0 ? HttpScheme.HTTP.port() : uri.getPort());
            cf.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    future.channel().writeAndFlush(msg);
                } else {
                    log.error("connect to server failed,uri={}", uri);
                    inboundCtx.channel().close();
                }
            });
        }
    }

    static class ReplyInboundHandler extends ChannelInboundHandlerAdapter{

        private final Channel inboundChannel;

        public ReplyInboundHandler(Channel inboundChannel) {
            this.inboundChannel = inboundChannel;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx0, Object msg) {
            inboundChannel.writeAndFlush(msg);
        }
    }
}

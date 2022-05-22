package com.nereusyi.http.proxy;

import com.nereusyi.http.proxy.handler.HttpProxyHandshakeHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpProxyApplication {
    private static final Logger log = LogManager.getLogger();

    static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));

    public static void main(String[] args) {
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            ChannelFuture channelFuture = b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG))
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(65535))
                                    .addLast(new HttpProxyHandshakeHandler());
                        }
                    })
                    .bind(PORT).sync();
            log.info("start success at port {}" , PORT);
            channelFuture
                    .channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("unexpected exception", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

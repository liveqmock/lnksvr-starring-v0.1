package org.fbi.linking.server.starring.tcpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.fbi.linking.server.starring.util.ProjectConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: zr
 * Date: 13-4-13
 */
public class NettyServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final int port;

    public NettyServer() {
        this.port = Integer.valueOf((String) ProjectConfigManager.getInstance().getProperty("server.starring.port"));
    }

    public NettyServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        logger.info("服务器开始启动......");

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new NettyServerInitializer());

            b.bind(port).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void stop(){
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new NettyServer(port).run();
    }
}

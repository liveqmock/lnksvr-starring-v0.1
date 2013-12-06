package org.fbi.linking.server.starring.tcpserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * User: zhanrui
 * Date: 13-8-21
 * Time: 下午1:14
 */
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("decoder", new MessageDecoder());
        pipeline.addLast("encoder", new MessageEncoder());

        pipeline.addLast("handler", new MessageServerHandler());
    }


}

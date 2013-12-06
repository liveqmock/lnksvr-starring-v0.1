package org.fbi.linking.server.starring.tcpserver;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: zhanrui
 * Date: 13-4-13
 */

public class MessageEncoder  extends MessageToByteEncoder<String> {
    private static final Logger logger = LoggerFactory.getLogger(MessageEncoder.class);
    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
        String res = msg;
        //logger.info(res);

        byte[] data = res.getBytes();
        out.writeBytes(data);
    }
}

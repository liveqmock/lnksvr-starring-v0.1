package org.fbi.linking.server.starring.tcpserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang.StringUtils;
import org.fbi.linking.connector.Request;
import org.fbi.linking.connector.Response;
import org.fbi.linking.processor.Processor;
import org.fbi.linking.processor.ProcessorManagerService;
import org.fbi.linking.processor.standprotocol10.Stdp10ProcessorRequest;
import org.fbi.linking.processor.standprotocol10.Stdp10ProcessorResponse;
import org.fbi.linking.server.starring.bootstrap.ServerActivator;
import org.fbi.linking.server.starring.util.MD5Helper;
import org.fbi.linking.server.starring.util.ProjectConfigManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * User: zhanrui
 * Date: 13-4-13
 */
public class MessageServerHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(MessageServerHandler.class);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String requestBuffer) throws Exception {
        String responseBuffer = "";
        logger.info("服务器收到报文：" + requestBuffer);

        Stdp10ProcessorResponse response = new Response();

        try {
            Stdp10ProcessorRequest request = new Request(requestBuffer);
            //ProcessorRequest processorRequest = new Stdp10ProcessorRequestWrapper(request) ;


            //1.MAC校验  实时获取是否校验标志，方便更新
            String macFlag = (String) ProjectConfigManager.getInstance().getProperty("posserver_mac_flag");
            if (macFlag != null && "1".equals(macFlag)) {//需校验
                //TODO
            }

            //2.获取交易码
            String txnCode = request.getHeader("txnCode");
            logger.info("服务器收到报文，交易号:" + txnCode);

            //3.调用业务逻辑处理程序
            //Class clazz = Class.forName("org.fbi.linking.server.posprize.processor.T" + txnCode + "processor");
            //TxnProcessor processor = (TxnProcessor)clazz.newInstance();


            Processor processor = getTxnprocessor(txnCode);

            //ProcessorResponse response = new ProcessorResponse();
            processor.service(request, response);

            //
            response.addHeader("version", request.getHeader("version"));
            response.addHeader("serialNo", request.getHeader("serialNo"));
            response.addHeader("txnCode", request.getHeader("txnCode"));
            response.addHeader("branchID", request.getHeader("branchID"));
            response.addHeader("tellerID", request.getHeader("tellerID"));
            response.addHeader("ueserID", request.getHeader("ueserID"));
            response.addHeader("appID", request.getHeader("appID"));
            response.addHeader("txnTime", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            String mac = "";
            if (response.getResponseBody().length == 0) {
                mac = MD5Helper.getMD5String(response.getHeader("txnTime").substring(0, 8)
                        + response.getHeader("ueserID").trim());
            } else {
                mac = MD5Helper.getMD5String(new String(response.getResponseBody())
                        + response.getHeader("txnTime").substring(0, 8)
                        + response.getHeader("ueserID").trim());
            }
            response.addHeader("mac", mac);

        } catch (Exception ex) {
            logger.error("Get txn processor instance error.", ex);
            responseBuffer = getErrResponse("1000");
        }

        //TODO 直接发送字节数组？
        responseBuffer = getResponseMessage(response);
        byte[] buf = responseBuffer.getBytes(response.getCharacterEncoding());
        String strLen = "" + (buf.length + 6);
        for (int i = strLen.length(); i < 6; i++) {
            strLen += " ";
        }

        //ctx.write(strLen.getBytes());
        //ctx.writeAndFlush(buf);
        ctx.writeAndFlush(strLen+responseBuffer);

        logger.info("服务器返回报文：" + responseBuffer);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("ChannelInactived.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception from downstream.", cause);
        ctx.close();
    }


    private Processor getTxnprocessor(String txnCode) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        BundleContext context = ServerActivator.getBundleContext();
        ServiceReference reference = null;
        ServiceReference[] references = new ServiceReference[0];
        try {
            //String filter = "(objectclass=" + ProcessorManagerService.class.getName() + ")";
            String filter = "(APPID=" + "AIC-QDE" + ")";
            references = context.getServiceReferences(ProcessorManagerService.class.getName(), filter);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        if (references.length == 0) {
            System.out.println("服务名称未找到" + ProcessorManagerService.class.getName());
            throw new RuntimeException("此交易的应用处理程序未找到：" + txnCode);
        } else {
            //TODO
            System.out.println("找到的服务个数：" + references.length);
        }
        ProcessorManagerService service = (ProcessorManagerService) context.getService(references[0]);

        //TODO 统一交易号后四位处理逻辑！
        return service.getProcessor(txnCode.substring(3));
    }

    private String getErrResponse(String errCode) {
        return "121212";
    }

    private String getResponseMessage(Stdp10ProcessorResponse response) throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        sb.append(StringUtils.rightPad(response.getHeader("version"), 3, " "));
        sb.append(StringUtils.rightPad(response.getHeader("serialNo"), 18, " "));
        sb.append(StringUtils.rightPad(response.getHeader("rtnCode"), 4, " "));
        sb.append(StringUtils.rightPad(response.getHeader("txnCode"), 7, " "));
        sb.append(StringUtils.rightPad(response.getHeader("branchID"), 9, " "));
        sb.append(StringUtils.rightPad(response.getHeader("tellerID"), 12, " "));
        sb.append(StringUtils.rightPad(response.getHeader("ueserID"), 6, " "));
        sb.append(StringUtils.rightPad(response.getHeader("appID"), 6, " "));
        sb.append(StringUtils.rightPad(response.getHeader("txnTime"), 14, " "));
        sb.append(StringUtils.rightPad(response.getHeader("mac"), 32, " "));
        sb.append(new String(response.getResponseBody(), response.getCharacterEncoding()));

        return sb.toString();
    }
}

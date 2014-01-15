package org.fbi.linking.server.starring.tcpserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang.StringUtils;
import org.fbi.linking.connector.cbs10.Cbs10Request;
import org.fbi.linking.connector.cbs10.Cbs10Response;
import org.fbi.linking.processor.Processor;
import org.fbi.linking.processor.ProcessorManagerService;
import org.fbi.linking.processor.standprotocol10.Stdp10ProcessorRequest;
import org.fbi.linking.processor.standprotocol10.Stdp10ProcessorResponse;
import org.fbi.linking.server.starring.bootstrap.ServerActivator;
import org.fbi.linking.server.starring.util.MD5Helper;
import org.fbi.linking.server.starring.util.ProjectConfigManager;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * User: zhanrui
 * Date: 13-4-13
 */
public class MessageServerHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(MessageServerHandler.class);
    private static Map<String,Object> contextsMap = new ConcurrentHashMap<String,Object>();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String requestBuffer) throws Exception {
        String responseBuffer = "";
        logger.info("服务器收到报文：" + requestBuffer);

        Stdp10ProcessorRequest request = new Cbs10Request(requestBuffer);
        Stdp10ProcessorResponse response = new Cbs10Response();

        ServiceReference reference = null;
        try {
            //1.MAC校验  实时获取是否校验标志，方便更新
            String macFlag = (String) ProjectConfigManager.getInstance().getProperty("posserver_mac_flag");
            if (macFlag != null && "1".equals(macFlag)) {//需校验
                //TODO
            }

            //2.获取交易码
            String txnCode = request.getHeader("txnCode");
            logger.info("服务器收到报文，交易号:" + txnCode);

            //3.调用业务逻辑处理程序
            String appId = request.getHeader("appId");
            ApplicationContext context = (ApplicationContext) contextsMap.get(appId);
            if (context == null) {
                context = new ApplicationContext();
                contextsMap.put(appId, context);
            }
            request.setProcessorContext(context);

            reference = getServiceReference(appId);
            Processor processor = getTxnprocessor(reference, txnCode);
            processor.service(request, response);
        } catch (Throwable ex) {
            logger.error("报文处理失败.", ex);
            response.addHeader("rtnCode", "9999"); //TODO
        } finally {
            if (reference != null) {
                ServerActivator.getBundleContext().ungetService(reference);
            }
        }

        assembleResponseInfo(request, response);

        //TODO 直接发送字节数组？
        responseBuffer = getResponseMessage(response);
        byte[] buf = responseBuffer.getBytes(response.getCharacterEncoding());
        String strLen = "" + (buf.length + 6);
        for (int i = strLen.length(); i < 6; i++) {
            strLen += " ";
        }

        //ctx.write(strLen.getBytes());
        //ctx.writeAndFlush(buf);
        ctx.writeAndFlush(strLen + responseBuffer);

        logger.info("服务器返回报文：" + responseBuffer);
        ctx.close();
    }

    private void assembleResponseInfo(Stdp10ProcessorRequest request, Stdp10ProcessorResponse response) {
        response.addHeader("version", request.getHeader("version"));
        response.addHeader("serialNo", request.getHeader("serialNo"));
        response.addHeader("txnCode", request.getHeader("txnCode"));
        response.addHeader("branchId", request.getHeader("branchId"));
        response.addHeader("tellerId", request.getHeader("tellerId"));
        response.addHeader("ueserId", request.getHeader("ueserId"));
        response.addHeader("appId", request.getHeader("appId"));
        response.addHeader("txnTime", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        String mac = "";
        byte[] responseBody = response.getResponseBody();
        if (responseBody == null || responseBody.length == 0) {
            mac = MD5Helper.getMD5String(response.getHeader("txnTime").substring(0, 8)
                    + response.getHeader("ueserId").trim());
        } else {
            mac = MD5Helper.getMD5String(new String(responseBody)
                    + response.getHeader("txnTime").substring(0, 8)
                    + response.getHeader("ueserId").trim());
        }
        response.addHeader("mac", mac);
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


    //获取OSGI app 服务引用。
    private ServiceReference getServiceReference(String appId)  {
        ServiceReference[] references;
        try {
            //TODO APPID配置表
            String filter = "(APPID=" + appId + ")";
            references = ServerActivator.getBundleContext().getServiceReferences(ProcessorManagerService.class.getName(), filter);
            if (references == null) {
                throw  new RuntimeException("应用服务APP未找到：APPID=" + appId);
            }
        } catch (InvalidSyntaxException e) {
            throw  new RuntimeException("获取交易处理程序错误。", e);
        }

        if (references.length == 0) {
            throw new RuntimeException("服务名称未找到:" + ProcessorManagerService.class.getName());
        } else if (references.length > 1) {
            //TODO
            throw new RuntimeException("找到的服务超过一个:" + ProcessorManagerService.class.getName());
        } else {
            return references[0];
        }
    }
    private Processor getTxnprocessor(ServiceReference reference, String txnCode)  {
        ProcessorManagerService service = (ProcessorManagerService) ServerActivator.getBundleContext().getService(reference);

        //TODO 统一交易号后四位处理逻辑！
        try {
            return service.getProcessor(txnCode.substring(3));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("应用处理Processor不存在,交易码:" + txnCode.substring(3));
        } catch (Exception ex) {
            throw new RuntimeException("查找应用处理Processor失败,交易码:" + txnCode.substring(3));
        }
    }


    private String getResponseMessage(Stdp10ProcessorResponse response) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.rightPad(response.getHeader("version"), 3, " "));
        sb.append(StringUtils.rightPad(response.getHeader("serialNo"), 18, " "));
        sb.append(StringUtils.rightPad(response.getHeader("rtnCode"), 4, " "));
        sb.append(StringUtils.rightPad(response.getHeader("txnCode"), 7, " "));
        sb.append(StringUtils.rightPad(response.getHeader("branchId"), 9, " "));
        sb.append(StringUtils.rightPad(response.getHeader("tellerId"), 12, " "));
        sb.append(StringUtils.rightPad(response.getHeader("ueserId"), 6, " "));
        sb.append(StringUtils.rightPad(response.getHeader("appId"), 6, " "));
        sb.append(StringUtils.rightPad(response.getHeader("txnTime"), 14, " "));
        sb.append(StringUtils.rightPad(response.getHeader("mac"), 32, " "));
        byte[] responseBody = response.getResponseBody();
        if (responseBody != null && responseBody.length != 0) {
            sb.append(new String(responseBody, response.getCharacterEncoding()));
            sb.append("|");
        }

        return sb.toString();
    }
}

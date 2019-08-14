package com.dt.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.dt.channel.ChannelSupervise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

/**
 * @Auther: dengtao
 * @Date: 2019/8/13 14:59
 * @Description:
 */
@Slf4j
public class NioWebSocketHandler extends SimpleChannelInboundHandler<Object> {


    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("收到消息");

        //文本消息用文本处理
        if (msg instanceof TextWebSocketFrame){
            System.out.println("TextWebSocketFrame"+msg);
            textdoMessage(ctx,(TextWebSocketFrame)msg);
        }
        //图片消息用图片处理
        else if (msg instanceof WebSocketFrame){
            System.out.println("WebSocketFrame"+msg);
            webdoMessage(ctx,(WebSocketFrame)msg);
        }
        //http消息用http请求处理，一般第一次创建websocket会发起http请求
        else if (msg instanceof FullHttpRequest){
            System.out.println("FullHttpRequest"+msg);
            handleHttpRequest(ctx,(FullHttpRequest)msg);
        }
        else if (msg instanceof CloseWebSocketFrame){
            System.out.println("CloseWebSocketFrame"+msg);
            handlerWebSocketFrame(ctx,(CloseWebSocketFrame)msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //添加连接
        log.info("客户端加入连接");
        ChannelSupervise.addChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //断开连接
        log.info("客户端断开连接");
        ChannelSupervise.removeChannel(ctx.channel());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }


    /**
     * 唯一的一次http请求，用于创建websocket
     * */
    private void handleHttpRequest(ChannelHandlerContext ctx,
                                   FullHttpRequest req) {
        //要求Upgrade为websocket，过滤掉get/Post
        if (!req.decoderResult().isSuccess()
                || (!"websocket".equals(req.headers().get("Upgrade")))) {
            //若不是websocket方式，则创建BAD_REQUEST的req，返回给客户端
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8081/websocket", null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory
                    .sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }
    /**
     * 拒绝不合法的请求，并返回错误信息
     * */
    private static void sendHttpResponse(ChannelHandlerContext ctx,
                                         FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(),
                    CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        // 如果是非Keep-Alive，关闭连接
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    //处理二进制信息，例如图片
    protected void webdoMessage(ChannelHandlerContext ctx, WebSocketFrame msg) {

    }

    protected void textdoMessage(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        // 接收到的消息
        String request = frame.text();
        Map<String,String> requestMap =  JSON.parseObject(request, new TypeReference<Map<String,String>>() {});
        String userId = requestMap.get("userId");
        String msg = requestMap.get("msg");
        String type = requestMap.get("type");
        //log.info("服务端收到：" + request);
        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString()
                + ctx.channel().id() + "：" + request);
        // 群发
        ChannelSupervise.send2All(tws);
        // 返回【谁发的发给谁】
        //ctx.channel().writeAndFlush(tws);
    }

    private void handlerWebSocketFrame(ChannelHandlerContext ctx, CloseWebSocketFrame frame){
        //关闭链路的指令
        handshaker.close(ctx.channel(),frame.retain());
    }
}

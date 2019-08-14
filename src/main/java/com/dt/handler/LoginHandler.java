package com.dt.handler;

import com.dt.session.Session;
import com.dt.util.SessionUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @Auther: dengtao
 * @Date: 2019/8/14 17:33
 * @Description:
 */
@Slf4j
public class LoginHandler extends SimpleChannelInboundHandler<Map<String,String>> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Map<String,String> map) throws Exception {
        log.info("开始登陆");
        String userId = map.get("userId");
        String type = map.get("type");
        if ("login".equals(type)&&SessionUtil.getChannel(userId) == null){
            log.info("开始登陆");
            SessionUtil.bindSession(new Session(userId, "dt"), ctx.channel());
        }
    }

}

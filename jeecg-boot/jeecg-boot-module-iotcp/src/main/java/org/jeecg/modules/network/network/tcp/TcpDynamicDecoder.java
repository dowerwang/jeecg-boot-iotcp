package org.jeecg.modules.network.network.tcp;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zhouwr.protocol.DataProtocolProvider;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.network.network.NetworkConnect;
import org.jeecg.modules.network.network.NetworkConnectStore;
import org.jeecg.modules.protocol.entity.ProtocolData;
import org.jeecg.modules.protocol.service.IProtocolDataService;
import org.jeecg.utils.HexConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * @program: 物联网平台系统
 * @description: TCP动态解码器
 * @author: zhouwr
 * @create: 2020-05-23 11:29
 * @version：1.0
 **/
@Slf4j
@Component
public class TcpDynamicDecoder extends MessageToMessageDecoder<ByteBuf> {

    private static TcpDynamicDecoder tcpDynamicDecoder;
    @Autowired
    private NetworkConnectStore connectStore;
    @Autowired
    private IProtocolDataService protocolDataService;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        log.info("decode 接收数据：{}", HexConvertUtil.BinaryToHexString(ByteBufUtil.getBytes(msg)));
        // 获取channel
        Channel channel = ctx.channel();
        // 客户端地址
        String addr = channel.remoteAddress().toString().substring(1);

        // 根据tcp地址(host:port)拿到连接信息
        Map<String, NetworkConnect> connectMap = NetworkConnectStore.getNetworkConnectMap();
        NetworkConnect connect = connectMap.get(addr);
        // 连接设备模型
        String deviceModelId = connect.getDeviceInstance().getModelBy();
        String deviceInstanceId = connect.getDeviceInstance().getId();

        ProtocolData protocolData = tcpDynamicDecoder.protocolDataService.getDeviceDataProtocol(deviceModelId);

        // 验证数据协议
        if (!"private".equals(protocolData.getCode())) {
            // 自定义协议
            DataProtocolProvider protocolProvider = tcpDynamicDecoder.protocolDataService.loadDataProtocolProvider(protocolData);
            JSONArray array = protocolProvider.decoder(msg);
            log.info("接收到数据：{}", array.toJSONString());


            array.stream().forEach(o -> {
                out.add((JSONObject) o);
            });

        } else {
            out.add(ByteBufUtil.getBytes(msg));
        }
    }

    @PostConstruct
    public void init() {
        tcpDynamicDecoder = this;
        // this.publisher = this.publisher;
        tcpDynamicDecoder.protocolDataService = this.protocolDataService;
    }

}

package com.DDT.loadbalancer.impl;

import com.DDT.RpcBootstrap;
import com.DDT.loadbalancer.AbstractLoadBalancer;
import com.DDT.loadbalancer.Selector;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 最小响应时间负载均衡算法，选择响应时间最短的服务节点
 * 通过维护一个服务节点响应时间的缓存，选择响应时间最短的节点
 * 这种算法适用于服务节点性能差异较大的情况，可以有效地提高系统的整体性能和响应速度
 */
public class MinimumResponseTimeLoadBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new MinimumResponseTimeSelector(serviceList);
    }

    private static class MinimumResponseTimeSelector implements Selector {

        public MinimumResponseTimeSelector(List<InetSocketAddress> serviceList) {

        }

        @Override
        public InetSocketAddress getNext() {
            Map.Entry<Long, Channel> entry = RpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry();
            if (entry != null) {
                return (InetSocketAddress) entry.getValue().remoteAddress();
            }

            // 直接从缓存中获取一个可用的就行了
            System.out.println("----->"+ Arrays.toString(RpcBootstrap.CHANNEL_CACHE.values().toArray()));
            Channel channel = (Channel)RpcBootstrap.CHANNEL_CACHE.values().toArray()[0];
            return (InetSocketAddress)channel.remoteAddress();
        }



    }
}
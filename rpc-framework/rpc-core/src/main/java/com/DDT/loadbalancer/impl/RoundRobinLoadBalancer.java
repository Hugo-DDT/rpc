package com.DDT.loadbalancer.impl;

import com.DDT.exceptions.LoadBalancerException;
import com.DDT.loadbalancer.AbstractLoadBalancer;
import com.DDT.loadbalancer.Selector;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new RoundRobinSelector(serviceList);
    }

    private static class RoundRobinSelector implements Selector {
        private List<InetSocketAddress> serviceList;
        private AtomicInteger index;

        public RoundRobinSelector(List<InetSocketAddress> serviceList) {
            this.serviceList = serviceList;
            index = new AtomicInteger(0);
        }

        @Override
        public InetSocketAddress getNext() {
            if(serviceList == null || serviceList.isEmpty()){
                log.error("进行负载均衡选取节点时发现服务列表为空.");
                throw new LoadBalancerException();
            }

            InetSocketAddress address = serviceList.get(index.get());

            // 如果他到了最后的一个位置，重置
            if (index.incrementAndGet() >= serviceList.size()) {
                index.set(0);
            }

            return address;
        }

    }
}

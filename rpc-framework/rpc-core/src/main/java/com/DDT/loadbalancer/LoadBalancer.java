package com.DDT.loadbalancer;

import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.util.List;

public interface LoadBalancer {

    /**
     * 根据服务名获取一个可用的服务地址
     * @param serviceName 服务名
     * @return 服务地址
     */
    InetSocketAddress selectServiceAddress(String serviceName, String group);


    /**
     * 当感知节点发生了动态上下线，需要重新进行负载均衡
     * @param serviceName 服务的名称
     */
    void reLoadBalance(String serviceName, List<InetSocketAddress> addresses);
}

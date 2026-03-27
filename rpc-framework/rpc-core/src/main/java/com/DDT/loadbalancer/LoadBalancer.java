package com.DDT.loadbalancer;

import java.net.InetSocketAddress;
import java.net.InterfaceAddress;

public interface LoadBalancer {

    /**
     * 根据服务名获取一个可用的服务地址
     * @param serviceName 服务名
     * @return 服务地址
     */
    InetSocketAddress selectServiceAddress(String serviceName);
}

package com.DDT.discovery;

import com.DDT.ServiceConfig;

import java.net.InetSocketAddress;
import java.util.List;

public interface Registry {
    /**
     * 注册服务
     * @param serviceConfig 服务的配置内容
     */
    void register(ServiceConfig<?> serviceConfig);


    /**
     * 发现服务
     * @param serviceName 服务名称
     * @return
     */
    InetSocketAddress lookup(String serviceName);
}

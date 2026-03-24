package com.DDT.discovery;

import com.DDT.discovery.impl.ZookeeperRegistry;

import com.DDT.Constant;
public class RegistryConfig {

    // 定义连接的 url zookeeper://127.0.0.1:2181
    //              redis://192.168.12.125:3306
    private String connectString;

    public RegistryConfig(String connectString) {
        this.connectString = connectString;
    }

    /**
     * 可以使用简单工厂来完成
     * @return 具体的注册中心实例
     */
    public Registry getRegistry() {
        // 1、获取注册中心的类型
        String registryType = getRegistryType(connectString,true).toLowerCase().trim();
        // 2、通过类型获取具体注册中心
        if( registryType.equals("zookeeper") ){
            String host = getRegistryType(connectString, false);
            return new ZookeeperRegistry(host, Constant.TIME_OUT);
        }
//        else if (registryType.equals("nacos")){
//            String host = getRegistryType(connectString, false);
//            return new NacosRegistry(host, Constant.TIME_OUT);
//        }
        throw new RuntimeException("未发现合适的注册中心。");
    }

    /**
     * 获取注册中心类型或host
     * @param connectString
     * @param ifType
     * @return
     */
    private String getRegistryType(String connectString,boolean ifType){
        String[] typeAndHost = connectString.split("://");
        if(typeAndHost.length != 2){
            throw new RuntimeException("给定的注册中心连接url不合法");
        }
        if(ifType){
            return typeAndHost[0];
        } else {
            return typeAndHost[1];
        }
    }
}

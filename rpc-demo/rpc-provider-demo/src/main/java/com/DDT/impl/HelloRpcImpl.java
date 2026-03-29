package com.DDT.impl;

import com.DDT.annotation.RpcApi;

@RpcApi(group = "default")
public class HelloRpcImpl implements com.DDT.HelloRpc {

    @Override
    public String sayHi(String msg) {
        return "hi consumer: " + msg;
    }
}

package com.DDT.impl;

public class HelloRpc implements com.DDT.HelloRpc {

    @Override
    public String sayHi(String msg) {
        return "hi consumer: " + msg;
    }
}

package com.DDT.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 这个类用来保存一些全局的状态，主要是为了在服务关闭的时候，能够有一个标记，来标记当前服务已经关闭了，
 */
public class ShutDownHolder {

    // 用来标记请求挡板
    public static AtomicBoolean BAFFLE = new AtomicBoolean(false);

    // 用于请求的计数器
    public static LongAdder REQUEST_COUNTER = new LongAdder();
}
package com.DDT;

import com.DDT.annotation.TryTimes;

public interface HelloRpc {
    @TryTimes
    String sayHi(String msg);
}

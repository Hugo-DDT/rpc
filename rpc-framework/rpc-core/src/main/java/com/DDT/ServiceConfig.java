package com.DDT;

import lombok.Getter;
import lombok.Setter;

public class ServiceConfig<T> {
    private Class<?> interfaceProvider;
    @Setter
    @Getter
    private Object ref;

    public Class<?> getInterface() {
        return interfaceProvider;
    }

    public void setInterface(Class<?> interfaceProvider) {
        this.interfaceProvider = interfaceProvider;
    }

}

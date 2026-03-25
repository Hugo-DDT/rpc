package com.DDT;

import lombok.Getter;
import lombok.Setter;

public class ServiceConfig<T> {
    private Class<T> interfaceProvider;
    @Setter
    @Getter
    private Object ref;

    public Class<T> getInterface() {
        return interfaceProvider;
    }

    public void setInterface(Class<T> interfaceProvider) {
        this.interfaceProvider = interfaceProvider;
    }

}

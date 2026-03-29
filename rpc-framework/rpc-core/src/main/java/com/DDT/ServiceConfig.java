package com.DDT;

import lombok.Getter;
import lombok.Setter;

public class ServiceConfig<T> {
    private Class<?> interfaceProvider;
    @Setter
    @Getter
    private Object ref;

    @Setter
    @Getter
    private String group = "default";

    public Class<?> getInterface() {
        return interfaceProvider;
    }

    public void setInterface(Class<?> interfaceProvider) {
        this.interfaceProvider = interfaceProvider;
    }

}

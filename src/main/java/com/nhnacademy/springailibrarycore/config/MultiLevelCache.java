package com.nhnacademy.springailibrarycore.config;

import java.util.concurrent.Callable;
import org.springframework.cache.Cache;

public class MultiLevelCache implements Cache {

    private final Cache l1Cache;
    private final Cache l2Cache;

    public MultiLevelCache(Cache l1Cache, Cache l2Cache) {
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
    }

    @Override
    public String getName() {
        return l1Cache.getName();
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper wrapper = l1Cache.get(key);
        if (wrapper != null) {
            return wrapper;
        }
        wrapper = l2Cache.get(key);
        if (wrapper != null) {
            l1Cache.put(key, wrapper.get());
        }
        return wrapper;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = l1Cache.get(key, type);
        if (value != null) {
            return value;
        }
        value = l2Cache.get(key, type);
        if (value != null) {
            l1Cache.put(key, value);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return l1Cache.get(key, () -> {
            return l2Cache.get(key, valueLoader);
        });
    }

    @Override
    public void put(Object key, Object value) {
        l1Cache.put(key, value);
        l2Cache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        l1Cache.evict(key);
        l2Cache.evict(key);
    }

    @Override
    public void clear() {
        l1Cache.clear();
        l2Cache.clear();
    }
}

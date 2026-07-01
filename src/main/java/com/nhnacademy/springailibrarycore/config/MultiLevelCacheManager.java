package com.nhnacademy.springailibrarycore.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class MultiLevelCacheManager implements CacheManager {

    private final CacheManager l1CacheManager;
    private final CacheManager l2CacheManager;
    private final Set<String> cacheNames = ConcurrentHashMap.newKeySet();

    public MultiLevelCacheManager(CacheManager l1CacheManager, CacheManager l2CacheManager) {
        this.l1CacheManager = l1CacheManager;
        this.l2CacheManager = l2CacheManager;
    }

    @Override
    public Cache getCache(String name) {
        cacheNames.add(name);
        Cache l1Cache = l1CacheManager.getCache(name);
        Cache l2Cache = l2CacheManager.getCache(name);
        
        if (l1Cache == null && l2Cache == null) {
            return null;
        }
        
        return new MultiLevelCache(l1Cache, l2Cache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(cacheNames);
    }
}

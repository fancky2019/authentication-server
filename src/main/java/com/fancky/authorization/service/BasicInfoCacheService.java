package com.fancky.authorization.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface BasicInfoCacheService {

    void initBasicInfoCache();

    void initUser();

    void initRole();

    void initUserRole();

    void initUserPermission();

    void initUserRolePermission();





    boolean getSbpEnable();

    void setSbpEnable();

    void setKeyVal(String keyVal, Object val);

    void setKeyValExpire(String keyVal, Object val, long timeout, TimeUnit unit);

    Object getStringKey(String key);



}

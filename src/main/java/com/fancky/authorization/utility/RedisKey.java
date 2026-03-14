package com.fancky.authorization.utility;

public class RedisKey {
    public static final String UPDATE_INVENTORY_INFO = "redisson:updateInventoryInfo";

    public static final Integer INIT_INVENTORY_INFO_FROM_DB_WAIT_TIME=300;
    public static final Integer INIT_INVENTORY_INFO_FROM_DB_LEASE_TIME=1500;
    public static final String INIT_INVENTORY_INFO_FROM_DB = "redisson:initInventoryInfoFromDb";


    public static final String SHIP_ORDER_COMPLETE= "shipOrderComplete:";

    public static final String CREATE_WORKING_DIRECTORY= "createWorkingDirectory:";


    //region 表锁
    public static final String UPDATE_MQ_MESSAGE_INFO = "redisson:updateMqMessage";
    public static final String UPDATE_TRUCK_ORDER_INFO = "redisson:updateTruckOrder";
    public static final String UPDATE_TRUCK_ORDER_ITEM = "redisson:updateTruckOrderItem";

    //endregion


    public static final String INVENTORY_DELETED= "inventoryDeleted:";
    public static final String SBP_ENABLE= "Sbp:Enable";

    public static final String DEBIT= "redisson:debit";
    public static final String SYNCHRONIZE_TRUCK_ORDER_STATUS= "redisson:synchronizeTruckOrderStatus";


    public static  final  String KEY_LOCK_SUFFIX=":operation";
    public static  final  String MQ_FAIL_HANDLER="mqFailHandler:mqOperation";
    public static  final  String MQ_FAIL_HANDLER_TIME="mqFailHandler:latestExecutingTime";


    public  static Integer TIME_OUT_HALF_MINUTE=60;
    public  static Integer TIME_OUT_ONE_MINUTE=60;
    //__NULL__
    public static final String EMPTY_VALUE = "__NULL__";

    public static final String REDISSON_PREFIX = "Redisson:";
    //region sys
    public static final String USER_KEY = "BasicInfo:User";
    public static final String USER_CODE_KEY = "BasicInfo:User:Code";
    public static final String USER_NULL_PREFIX = "BasicInfo:User:Null:";
    public static final String USER_CODE_NULL_PREFIX = "BasicInfo:User:Code:Null:";


    public static final String ROLE_KEY = "BasicInfo:Role";
    public static final String ROLE_NULL_KEY = "BasicInfo:Role:Null:";

    public static final String USER_ROLE_KEY = "BasicInfo:UserRole";
    public static final String USER_ROLE_USER_KEY = "BasicInfo:UserRole:User";
    public static final String USER_ROLE_USER_NULL_KEY = "BasicInfo:UserRole:User:Null:";

    public static final String PERMISSION_KEY = "BasicInfo:Permission";
    public static final String PERMISSION_NULL_KEY = "BasicInfo:Permission:Null:";

    public static final String ROLE_PERMISSION_KEY = "BasicInfo:RolePermission";
    public static final String ROLE_PERMISSION_ROLE_KEY = "BasicInfo:RolePermission:Role";
    public static final String ROLE_PERMISSION_ROLE_NULL_KEY = "BasicInfo:RolePermission:Role:Null:";


    //endregion











}

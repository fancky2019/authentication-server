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
}

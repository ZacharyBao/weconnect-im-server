package SocketServer.util;

import SocketServer.entity.MessageEntity;
import SocketServer.entity.People;
import redis.clients.jedis.Jedis;


import java.util.*;

/**
 * Created by BaoDong on 2018/4/5.
 */
public class RedisHelper {
    private List<People> myList1;

    public Jedis initializeJedis() {
        Jedis jedis = new Jedis("localhost", 6379);
        //jedis.auth("123456");
        return jedis;
    }

    public void closeJedis(Jedis jedis) {
        jedis.disconnect();
        System.out.println("close jedis successfully!");
    }

    public Queue<MessageEntity> getOfflineMsgFromRedis(String key, Jedis jedis) {
        byte[] serializedString = jedis.get(key.getBytes());
        Queue<MessageEntity> offlineMsgQueue = (Queue<MessageEntity>) SerializeUtil.unserialize(serializedString);
        return offlineMsgQueue;
    }

    public void saveOfflineMsgInRedis(Queue<MessageEntity> offlineMsgQueue, Jedis jedis, String friendId) {
        jedis.set(friendId.getBytes(), SerializeUtil.serialize(offlineMsgQueue));
    }

    public HashMap<String, String> getOfflineUserIdMapFromRedis(Jedis jedis) {
        byte[] serializedString = jedis.get("offlineUserIdMap".getBytes());
        HashMap<String, String> offlineUserIdMap = (HashMap<String, String>) SerializeUtil.unserialize(serializedString);
        return offlineUserIdMap;
    }

    public void saveOfflineUserIdMapInRedis(HashMap<String, String> offlineUserIdMap, Jedis jedis) {
        jedis.set("offlineUserIdMap".getBytes(), SerializeUtil.serialize(offlineUserIdMap));
    }

}

package SocketServer.servercode;

/**
 * Created by BaoDong on 2017/1/26.
 */


import SocketServer.dto.Group;
import SocketServer.dto.User;
import SocketServer.entity.MessageEntity;
import SocketServer.util.DbHelper;
import SocketServer.util.RedisHelper;
import redis.clients.jedis.Jedis;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

//服务器类
public class WeServer {
    public static void main(String[] args) throws Exception {

        // 实例化一个hashmap,用于保存所有的在线的User，相当于在线列表
        HashMap<String,User> onlineUser=new HashMap<>();
        //离线缓存，用来存储由于离线而未能接收到消息的用户id
        HashMap<String,String> offlineUserIdMap;
        //存放所有数据库中群组的信息
        HashMap<String,List<String>> allGroupInfo;
        //创建redisHelper对象
        RedisHelper redisHelper=new RedisHelper();

        //加载群组信息
        DbHelper dbHelper=new DbHelper();
        allGroupInfo=dbHelper.getAllGroupInfoTwo();
        dbHelper.weClear();

        //获取有离线消息的用户Id的map是否存在
        Jedis jedis=redisHelper.initializeJedis();
        offlineUserIdMap=redisHelper.getOfflineUserIdMapFromRedis(jedis);
        if(offlineUserIdMap==null){
            offlineUserIdMap=new HashMap<>();
            redisHelper.saveOfflineUserIdMapInRedis(offlineUserIdMap,jedis);
        }else{
            //什么都不做
        }
        redisHelper.closeJedis(jedis);

        // 创建绑定到特定端口的服务器套接字
        @SuppressWarnings("resource")
        ServerSocket serverSocket = new ServerSocket(9999);
        System.out.println("服务端正在开始~");
        // 循环监听客户端连接
        while (true) {
            Socket socket = serverSocket.accept();
            // 每接受一个线程，就随机生成一个一个新用户
            User user = new User(socket);
            System.out.println(user.getName() + "正在登录。。。");
            //list.add(user);
            onlineUser.put(user.getName(),user);
            // 创建一个新的线程，接收信息并转发
            ServerThread thread = new ServerThread(user, onlineUser,allGroupInfo,redisHelper);
            thread.start();
        }
    }

}

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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 *   服务器线程的作用主要是:
 *   1.接收来自客户端的信息
 *   2.将接收到的信息解析，并转发给目标客户端
 * */
public class ServerThread extends Thread {

    private User user;
    //private List<User> list;
    private HashMap<String, User> onlineUser;
    private HashMap<String, String> offlineUserIdMap;
    private HashMap<String, List<String>> allGroupInfo;
    private Queue<MessageEntity> offlineMsgQueue;
    private RedisHelper redisHelper;

    public ServerThread() {

    }

    public ServerThread(User user, HashMap<String, User> onlineUser, HashMap<String, List<String>> allGroupInfo, RedisHelper redisHelper) {
        this.user = user;
        this.onlineUser = onlineUser;
        this.allGroupInfo = allGroupInfo;
        this.redisHelper = redisHelper;
    }

    public void run() {
        int flag = 0;
        try {
            sendOfflineMsg();//在进行发消息给别人之前，得从后台获取自己的离线消息，并发给自己

            while (true) {//这样的线程每次接收消息，
                // 信息的格式：(login||logout||say),发送人,收发人,信息体
                //不断地读取客户端发过来的信息
                String msg = user.getBr().readLine();
                System.out.println(msg);
                String[] str = msg.split(",");
                switch (str[0]) {
                    case "logout":
                        remove(user);// 移除用户
                        flag = 1;
                        break;
                    case "say":
                        sendToClient(str[1], msg); // 转发信息给特定的用户
                        break;
                    case "group":
                        sendToGroupMembers(str[1], str[2], msg); // 转发信息给特定的用户
                        break;
                    case "add":
                        updateGroupMember(str[1], str[2], msg, 1);//这种情况包含在已有群组当中邀请新成员和创建新的群组
                        break;
                    case "quit":
                        updateGroupMember(str[1], str[2], msg, 2);
                        break;
                    case "create":
                        updateGroupMember(str[1], str[2], msg, 3);//这种情况包含在已有群组当中邀请新成员和创建新的群组
                        break;
                    default:
                        break;
                }
                if (flag == 1) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("异常");
        } finally {
            System.out.println("为" + user.getName() + "而启动的转发线程终止了");
        }
    }

    private void sendToClient(String username, String msg) {
        if (onlineUser.containsKey(username)) {//包含这个用户名说明对方在线
            try {
                PrintWriter pw = onlineUser.get(username).getPw();
                pw.println(msg);
                pw.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            MessageEntity messageEntity = new MessageEntity();
            messageEntity.setmSenderId(user.getName());
            messageEntity.setmReceiverId(username);
            messageEntity.setmMsgText(msg);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm");
            Date date = new Date();
            String sendtime = sdf.format(date);
            messageEntity.setmSendTime(sendtime);
            //将离线消息存储到redis当中
            Jedis jedis = redisHelper.initializeJedis();
            offlineMsgQueue = redisHelper.getOfflineMsgFromRedis(username, jedis);
            if (offlineMsgQueue != null) {//最开始有可能redis里面什么都没有
                offlineMsgQueue.offer(messageEntity);
                redisHelper.saveOfflineMsgInRedis(offlineMsgQueue, jedis, username);
            } else {
                offlineMsgQueue = new LinkedList<>();
                offlineMsgQueue.offer(messageEntity);
                redisHelper.saveOfflineMsgInRedis(offlineMsgQueue, jedis, username);
            }
            //将好友Id添加到有离线消息的好友列表
            offlineUserIdMap = redisHelper.getOfflineUserIdMapFromRedis(jedis);
            offlineUserIdMap.put(username, username);
            redisHelper.saveOfflineUserIdMapInRedis(offlineUserIdMap, jedis);
            System.out.println("add " + username + " to " + offlineUserIdMap + " successfully!");
            redisHelper.closeJedis(jedis);
        }
    }

    public void sendNoticeMag(String username, String msg) {
        if (onlineUser.containsKey(username)) {//包含这个用户名说明对方在线
            try {
                PrintWriter pw = onlineUser.get(username).getPw();
                pw.println(msg);
                pw.flush();
                System.out.println("发给" + username + " " + msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

        }
    }

    private void sendToGroupMembers(String groupId, String fromUserId, String msg) {
        Boolean flag = false;
        while (!flag) {
            if (allGroupInfo.containsKey(groupId)) {
                flag = true;
                for (String userId : allGroupInfo.get(groupId)) {//广播发送群消息
                    if (!userId.equals(fromUserId)) {
                        sendToClient(userId, msg);
                        System.out.println(fromUserId + "发给" + userId);
                    }
                }
            } else {
                DbHelper dbHelper = new DbHelper();
                Group mygroup = dbHelper.getGroupInfoById(groupId);
                dbHelper.weClear();
                allGroupInfo.put(mygroup.getGroupId(), mygroup.getGroupUserIdList());
            }
        }
    }

    public void updateGroupMember(String userId, String groupId, String msg, int type) {
        List<String> groupUserIdList;
        boolean flag = false;
        while (!flag) {
            if (allGroupInfo.containsKey(groupId)) {
                flag = true;
                if (type == 1) {//添加新成员
                    groupUserIdList = allGroupInfo.get(groupId);
                    groupUserIdList.add(userId);
                    allGroupInfo.put(groupId, groupUserIdList);//更新值
                    System.out.println("成功添加");
                    sendNoticeMag(userId, msg);
                } else if (type == 2) {//删除退出群组的成员
                    groupUserIdList = allGroupInfo.get(groupId);
                    for (int i = groupUserIdList.size() - 1; i >= 0; i--) {
                        if (groupUserIdList.get(i).equals(userId)) {
                            String item = groupUserIdList.get(i);
                            groupUserIdList.remove(item);
                            allGroupInfo.put(groupId, groupUserIdList);
                            System.out.println("成功删除");
                            break;
                        }
                    }
                } else if (type == 3) {
                    sendNoticeMag(userId, msg);
                }
            } else {//如果不包含Key就说明这个群组时新创建的
                DbHelper dbHelper = new DbHelper();
                Group mygroup = dbHelper.getGroupInfoById(groupId);
                dbHelper.weClear();
                System.out.println("新创建的群组的成员" + mygroup.getGroupUserIdList());
                allGroupInfo.put(mygroup.getGroupId(), mygroup.getGroupUserIdList());
            }
        }
    }

    private void remove(User user2) {
        onlineUser.remove(user2.getName());//关闭资源
        try {
            user2.getBr().close();
            user2.getPw().close();
            user2.getSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("已经释放" + user2.getName());
        }

    }

    private void sendOfflineMsg() {
        Jedis jedis = redisHelper.initializeJedis();
        offlineUserIdMap = redisHelper.getOfflineUserIdMapFromRedis(jedis);
        MessageEntity messageEntity;
        if (offlineUserIdMap.containsKey(user.getName())) {
            Queue<MessageEntity> myOfflineMsgQueue = redisHelper.getOfflineMsgFromRedis(user.getName(), jedis);
            while (myOfflineMsgQueue.peek() != null) {
                messageEntity = myOfflineMsgQueue.poll();
                sendToClient(user.getName(), "offline," + messageEntity.getmSendTime() + "," + messageEntity.getmMsgText());
            }
            redisHelper.saveOfflineMsgInRedis(myOfflineMsgQueue, jedis, user.getName());

            offlineUserIdMap.remove(user.getName());
            redisHelper.saveOfflineUserIdMapInRedis(offlineUserIdMap, jedis);
            redisHelper.closeJedis(jedis);
        } else {
            System.out.println("No offline message.");
        }
    }

}

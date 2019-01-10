package SocketServer.util;

import SocketServer.dto.Group;
import SocketServer.entity.MessageEntity;
import SocketServer.entity.UserEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DbHelper {
    private SessionFactory sessionFactory;
    private Session session;
    private Transaction transaction;
    public DbHelper(){
        Configuration configuration = new Configuration().configure();//创建配置对象
        sessionFactory = configuration.buildSessionFactory();//创建会话工厂
        session = sessionFactory.openSession();//开启会话
        transaction = session.beginTransaction();//开启事务
    }
    public void weClear(){
        transaction.commit();
        session.close();
        sessionFactory .close();
    }
    //获取用户信息实例
    public UserEntity getUserEntityByUserId(String propertyName,Object value){
        String hql = "from " + "UserEntity" + " as model where model."
                + propertyName + " = ?";
        return (UserEntity) session.createQuery(hql).setParameter(0, value)
                .uniqueResult();
    }
    //获取离线消息
    public List<MessageEntity> getMessageEntityListByReceiverId(String propertyName, Object value){
        System.out.println("sql===================================================" );
        String hql = "from " + "MessageEntity" + " as model where model."
                + propertyName + " = ?";
        System.out.println("sql===================================================" + hql);
        return session.createQuery(hql).setParameter(0, value).list();
    }
    //存储离线消息
    public Integer saveMessageEntity(MessageEntity messageEntity){
        return (Integer)session.save(messageEntity);
    }
    //删除离线消息
    public void deleteMessageEntity(MessageEntity messageEntity){
        session.delete(messageEntity);
    }
    //获取群组Id
    public List<String> getAllGroupId(){
        System.out.println("sql===================================================" );
        String hql = "select "+"model.groupId "+"from " + "MygroupEntity as model";
        System.out.println("sql===================================================" + hql);
        return session.createQuery(hql).list();
    }
    //获取群组信息
    public List<Group> getAllGroupInfo(){
        List<String> groupIdList=getAllGroupId();
        List<Group> groupList=new ArrayList<>();
        for(int i=0;i<groupIdList.size();i++){
            String hql = "select "+"model.userId "+"from " + "GrouprelationshipmappingEntity"+ " as model where model.groupId"+"=?";
            Query query = session.createQuery(hql).setParameter(0,groupIdList.get(i));
            List<String> userIdList=query.list();
            Group group=new Group();
            group.setGroupId(groupIdList.get(i));
            group.setGroupUserIdList(userIdList);
            groupList.add(group);
        }
        return groupList;
    }
    //获取群组信息
    public HashMap<String,List<String>> getAllGroupInfoTwo(){
        List<String> groupIdList=getAllGroupId();
        HashMap<String,List<String>> groupInfo=new HashMap<>();
        for(int i=0;i<groupIdList.size();i++){
            String hql = "select "+"model.userId "+"from " + "GrouprelationshipmappingEntity"+ " as model where model.groupId"+"=?";
            Query query = session.createQuery(hql).setParameter(0,groupIdList.get(i));
            List<String> userIdList=query.list();
            groupInfo.put(groupIdList.get(i),userIdList);
        }
        return groupInfo;
    }
    //获取单个群组信息通过群组Id
    public Group getGroupInfoById(String groupId){
        Group group=new Group();
        List<String> userIdList=new ArrayList<>();
        String hql = "select "+"model.userId "+"from " + "GrouprelationshipmappingEntity"+ " as model where model.groupId"+"=?";
        Query query=session.createQuery(hql).setParameter(0,groupId);
        userIdList=query.list();
        group.setGroupId(groupId);
        group.setGroupUserIdList(userIdList);
        return group;
    }
}

package SocketServer.util;

import SocketServer.entity.MessageEntity;
import SocketServer.entity.UserEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by Administrator on 2017/3/26.
 */
public class TestCase {
    private SessionFactory sessionFactory;
    private Session session;
    private Transaction transaction;

    @Before
    public void init() {
        Configuration configuration = new Configuration().configure();//创建配置对象
        sessionFactory = configuration.buildSessionFactory();//创建会话工厂
        session = sessionFactory.openSession();//开启会话
        transaction = session.beginTransaction();//开启事务
    }

    @After
    public void destory() {
        transaction.commit();//事务提交
        session.close();//关闭会话
        sessionFactory.close();//关闭会话工厂
    }

    @Test
    public void testGoods() {
        //生成对象
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setmSenderId("11111111");
        messageEntity.setmReceiverId("2222222");
        messageEntity.setmMsgText("djakdjakjkfjkajfkajkfja");
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        Date date =new Date();
        String sendtime=sdf.format(date);
        messageEntity.setmSendTime(sendtime);
        //保存对象进数据库
        messageEntity.setmMsgText("dfefwfwfwfnv");

        //DbHelper dbHelper=new DbHelper();
        //dbHelper.saveMessageEntity(messageEntity);

    }
}

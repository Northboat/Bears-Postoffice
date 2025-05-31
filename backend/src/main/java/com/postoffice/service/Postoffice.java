package com.postoffice.service;

import com.postoffice.mapper.MailMapper;
import com.postoffice.utils.Postman;
import com.postoffice.pojo.Mail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;

//vnufsybifrabicjj SMTP/POP3(154)
//oxftgstrzznrbddc SMTP/POP3(193)
//bgmrnmqksqabbfaa IMAP/SMTP


@Service
public class Postoffice {

    private final MailMapper mailMapper;
    private static Session session;
    private static Map<Integer, Postman> office;
    private static final String from = "northboat@qq.com";

    @Autowired
    public Postoffice(MailMapper mailMapper){
        this.mailMapper = mailMapper;

        office = new HashMap<>();

        Properties properties = new Properties();
        properties = System.getProperties();
        //设置第三方服务器
        properties.setProperty("mail.smtp.host", "smtp.qq.com");
        //开启密码验证
        properties.setProperty("mail.smtp.auth", "true");
        //设置超时时间
        properties.setProperty("mail.smtp.timeout", "4000");
        //开启debug
        properties.setProperty("mail.debug", "true");

        //开启ssl服务
        properties.setProperty("mail.smtp.ssl.enable", "true");
        //设置端口
        properties.setProperty("mail.smtp.port", "465");
        //设置ssl端口，必要的，否则连接不上
        properties.setProperty("mail.smtp.socketFactory.port", "465");
        properties.setProperty("mail.smtp.socketFactory.fallback", "false");
        properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        session = Session.getDefaultInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("northboat@qq.com", "oxftgstrzznrbddc");
            }
        });
    }


    public MimeMessage getMimeMessage(Mail mail) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(mail.getTo()));
        message.setSubject(mail.getSubject());
        message.setText(mail.getText());

        return message;
    }

    public void flush(){
        List<Mail> mails = mailMapper.queryMailList();
        for(Mail mail: mails){
            if(!office.containsKey(mail.getId())){
                try {
                    MimeMessage message = getMimeMessage(mail);
                    Postman postman = new Postman(message);
                    office.put(mail.getId(), postman);
                } catch (MessagingException e) {
                    System.out.println("初始化邮件" + mail.getId() + "失败，已跳过");
                }
            }
        }
    }

    public void beginWork(){
        for(Postman postman: office.values()){
            postman.start();
        }
    }

    public Mail send(Mail mail) {
        // 写入数据库
        mailMapper.addMail(mail);
        Postman postman;
        try {
            MimeMessage message = getMimeMessage(mail);
            postman = new Postman(message);
            postman.start();
            if(postman.isStopped()){
                return null;
            }
            office.put(mail.getId(), postman);
        } catch (MessagingException e) {
            System.out.println("初始化邮差报错，发送邮件失败");
        }
        return mail;
    }


    public String send(int id){
        if(office.containsKey(id)){
            return "邮件已经启动发送，不要重复操作";
        }
        try {
            MimeMessage message = getMimeMessage(mailMapper.getMailById(id));
            Postman postman = new Postman(message);
            postman.start();
            if(postman.isStopped()){
                return null;
            }
            office.put(id, postman);
            return "邮件定时发送成功";
        } catch (MessagingException e) {
            return "初始化邮差报错，发送邮件失败";
        }
    }

    public boolean has(int num){
        return office.getOrDefault(num, null) != null;
    }

    public void remove(int num){
        office.get(num).shutdown();
        //office.get(num).destroy();
        office.remove(num);
        mailMapper.removeMail(num);
    }

    public Collection<Postman> getPostmen(){
        return office.values();
    }

    public Postman getPostman(int num){
        return office.get(num);
    }
}
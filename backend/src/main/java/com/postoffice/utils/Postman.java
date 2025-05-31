package com.postoffice.utils;

import com.postoffice.service.Postoffice;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;



public class Postman extends Thread{

    //邮件信息
    private boolean first;
    private final MimeMessage message;
    //使线程停止
    private boolean stop;

    public Postman(MimeMessage message) throws MessagingException {
        first = true;
        this.message = message;
    }

    public boolean isStopped(){
        return stop;
    }

    @Override
    public void run() {
        do{
            try {
                //say hello
                if(first){
                    String subject = message.getSubject();
                    String text = (String) message.getContent();

                    message.setSubject("Hello");
                    message.setText("这是由" + Arrays.toString(message.getFrom()) + "为您订阅的邮件，将会每周定时为你发送");
                    Transport.send(message);
                    System.out.println("提示邮件发送成功");
                    TimeUnit.SECONDS.sleep(20);
                    first = false;

                    message.setSubject(subject);
                    message.setText(text);
                }

                Transport.send(message);
                TimeUnit.DAYS.sleep(7);
            } catch (Exception e){
                System.out.println("线程异常，已中断");
                e.printStackTrace();
                stop = true;
                // 从邮局中删除，牛逼，手动维护的单例
                // if(Postoffice.has(id)){
                    //Postoffice.remove(id);
                //}
                // 应该维护一个定时任务清除 stop 为 true 的 postman
                break;
            }
        } while(!stop);

        //say goodbye
        try {
            message.setSubject("goodbye");
            message.setText("订阅已结束，爷光荣下班");
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        System.out.println("bye, i am gone");
    }

    public void shutdown(){
        stop = true;
    }
}
---
date: 2022-1-15
permalink: /pages/63219d/
author: 
  name: Northboat
  link: https://github.com/Northboat
title: 邮件系统 - SpringBoot +Thread
---

基于 springboot、bootstrap、mybatis 实现的邮件管理系统

定时发送基于朴素的 Thread 多线程管理，并发编程的一个小玩具

## Thread 线程管理

### 邮件 VO

Mail.java

```java
@Data
@AllArgsConstructor
public class Mail {
    private int id;
    private String name;
    private String from;
    private String to;
    private String subject;
    private String text;

    public Mail(String name, String to, String subject, String text) {
        this.name = name;
        this.to = to;
        this.subject = subject;
        this.text = text;
    }
}
```

### 快递员 Postman

`Postman`继承`Thread`类

- 在`run`方法中实现发送邮件功能
- 定时实现：在死循环里直接 sleep 一周

~~~java
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
~~~

邮递员下班

~~~java
public void shutdown(){
    stop = true;
}

//用于office判断并在Map和sql中销毁邮件
public boolean isStopped(){
    return stop;
}
~~~

准备邮件，参数从上一级 PostOffice 中传入

~~~java
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
}
~~~

完整 Postman 类

~~~java
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
~~~

### 邮局 Postoffice

`Postoffice`维护一个`Map<Integer, Postman>`用于管理线程，初始化

- 设置发送邮件端口
- 开启ssl
- 设置邮件服务器
- 设置超时时间
- 验证账号密码

~~~java
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
}
~~~

封装 MimeMessage

```java
public MimeMessage getMimeMessage(Mail mail) throws MessagingException {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(mail.getTo()));
    message.setSubject(mail.getSubject());
    message.setText(mail.getText());

    return message;
}
```

从数据库中同步邮件数据，构造 Postman 线程存于 Map 中

~~~java
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
                continue;
            }
        }
    }
}
~~~

首次启动时触发，令所有线程开始工作

~~~java
public void beginWork(){
    for(Postman postman: office.values()){
        postman.start();
    }
}
~~~

新增邮件发送

~~~java
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
        System.out.println("初始化邮差报错，准备邮件失败");
    }
    return mail;
}
~~~

启动已有的邮件

```java
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
```

丢弃邮件

~~~java
public void remove(int id){
    office.get(id).shutdown();
    office.remove(id);
    mailMapper.removeMail(id);
}
~~~

获取邮递员

~~~java
public Collection<Postman> getPostmen(){
    return office.values();
}

public Postman getPostman(int num){
    return office.get(num);
}
~~~

完整代码

~~~java
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
                    continue;
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
~~~

## 数据库 Mapper

### 数据库设计

`postoffice/mail`

```sql
CREATE TABLE mail (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    `from` VARCHAR(100) NOT NULL,
    `to` VARCHAR(100) NOT NULL,
    subject VARCHAR(200),
    text TEXT
);
```

- `id`: 主键，自动递增
- `name`, `from`, `to`: 邮件名、发件人、收件人，使用 `VARCHAR(100)`
- `subject`: 邮件主题，可稍长，`VARCHAR(200)`
- `text`: 邮件正文，用`TEXT`类型更合适
- `from`和`to`是 **SQL 保留关键字**，需要使用反引号 ``` 包裹以避免语法冲突

### 改用 druid 数据源

spring-jdbc

- 默认数据源`HikariDataSource`，优势：高效
- `druid`优势：信息监控

application.yml

~~~yml
spring:
  application:
    name: PostOffice
  datasource:
    username: root
    password: "011026"
    url: jdbc:mysql://localhost:3306/postoffice?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    filters: stat,wall,log4j

server:
  port: 8083
~~~

### druid 的后台监控

通过`config`将`ServletRegistrationBean`注入`bean`

设置账号密码等基础信息，通过`/druid`可直接访问到`druid`已集成好的后台监控网站

~~~java
@Configuration
public class DruidConfig {

    @ConfigurationProperties(prefix = "spring.datasource")
    @Bean
    public DataSource druidDataSource(){
        return new DruidDataSource();
    }

    //后台监控
    //相当于web.xml
    @Bean
    public ServletRegistrationBean servlet(){
        ServletRegistrationBean bean = new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");

        Map<String, String> properties = new HashMap<>();
        properties.put("loginUsername", "NorthBoat");
        properties.put("loginPassword", "011026");

        //允许谁能访问
        properties.put("allow", "");
        //禁止谁能访问
        //properties.put("NorthBoat", "39.106.160.174");

        bean.setInitParameters(properties);
        return bean;
    }
}
~~~

### MyBatis Mapper

application.yml

在此处绑定实体类目录，使mapper.xml可以找到对应类，同时绑定mapper.xml所在目录

~~~yml
#整合mybatis
mybatis:
  type-aliases-package: com.postoffice.pojo
  mapper-locations: classpath:mapper/*.xml
~~~

MailMapper.java，放置在`com/postoffice/mapper`目录下

~~~java
//这个注解表示这是一个mybatis的mapper类
@Mapper
@Repository
public interface MailMapper {
    
    Mail getMailById(int id);

    List<Mail> queryMailList();

    void removeMail(int id);

    void addMail(Mail mail);

    void updateMailCount(Map<String, Integer> map);
}
~~~

MailMapper.xml，放在`resources/mapper`目录下

- 第一件事：绑定命名空间，即绑定对应的 Mapper 接口
- 第二件事：绑定方法，通过`id="方法名"`进行绑定

传参问题：若为单一参数，直接写参数名即可；若为多个参数，这里采用 Map 的方式传入，通过 key 进行取值

~~~xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.postoffice.mapper.MailMapper">
    
    <select id="queryMailList" resultType="Mail">
        select * from `mail`
    </select>


    <select id="getMailById" parameterType="int" resultType="Mail">
        select * from `mail` where `id` = #{id}
    </select>

    <delete id="removeMail" parameterType="int">
        delete from mail where `id` = #{id}
    </delete>

    <insert id="addMail" parameterType="Mail">
        insert into mail(`id`, `name`, `to`, `from`, `subject`, `text`) values(#{id}, #{name}, #{to}, #{from}, #{subject}, #{text})
    </insert>
</mapper>
~~~

## 配置 Config

### 设置拦截器

检查当前网页session，若无直接将msg重转发到登录页，进行打印输出

~~~java
public class LoginHandlerInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //登录成功之后session中有用户信息，据此判断
        Object loginUser = request.getSession().getAttribute("loginUser");
        if(loginUser == null){
            request.setAttribute("msg", "没有权限，请先登录！");
            request.getRequestDispatcher("/").forward(request, response);
            return false;
        } else{
            return true;
        }
    }
}
~~~

### MVC 设置

注册拦截器，放行静态资源

~~~java
@Configuration
public class MyMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        //登录页
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/index.html").setViewName("index");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginHandlerInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/index.html", "/", "/login",
                        "/css/**", "/icons/**", "/img/**",
                        "/bootstrap/**", "/sweetalert/**");
    }
}
~~~

### druid 数据源设置

DruidConfig.java

~~~java
@Configuration
public class DruidConfig {

    @ConfigurationProperties(prefix = "spring.datasource")
    @Bean
    public DataSource druidDataSource(){
        return new DruidDataSource();
    }

    //后台监控
    //相当于web.xml
    @Bean
    public ServletRegistrationBean servlet(){
        ServletRegistrationBean bean = new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");

        Map<String, String> properties = new HashMap<>();
        properties.put("loginUsername", "NorthBoat");
        properties.put("loginPassword", "011026");

        //允许谁能访问
        properties.put("allow", "");
        //禁止谁能访问
        //properties.put("NorthBoat", "39.106.160.174");

        bean.setInitParameters(properties);
        return bean;
    }

}
~~~

## 接口 Controller

### MailController

注入 Service

```java
@Controller
public class MailController {

    private boolean first = true;

    private Postoffice postoffice;
    @Autowired
    public void setMailMapper(Postoffice postoffice){
        this.postoffice = postoffice;
    }
}
```

登录登出：`/login, /logout, /main`

- 每次回到`/mail`，从 office 中拿取 mail 信息，返回前端
- 每次登录`/login`成功，调用 flush 函数同步数据库数据
- 注销账号`/logout`，删除掉 session 中`loginUser`，返回 index 页面

```java
@RequestMapping("/login")
public String login(@RequestParam("username") String username,
                    @RequestParam("password") String password,
                    Model model, HttpSession session){
    if(username.equals("") || password.equals("")){
        model.addAttribute("msg", "用户名或密码不能为空");
        return "index";
    }
    if(!password.equals("123456")){
        model.addAttribute("msg", "密码错误");
    }
    session.setAttribute("loginUser", username);
    postoffice.flush();
    if(first){
        postoffice.beginWork();
        first = false;
    }
    model.addAttribute("postmen", postoffice.getPostmen());
    return "main";
}

@RequestMapping("/logout")
public String logout(HttpSession session){
    session.removeAttribute("loginUser");
    return "index";
}

@RequestMapping("/main")
public String main(Model model){
    postoffice.flush();
    model.addAttribute("postmen", postoffice.getPostmen());
    return "main";
}
```

发送邮件：`/send, /creat`

1. 创建线程，开启线程
2. 将线程录入Office（Map）
3. 将mail信息录入数据库

~~~java
@RequestMapping("/creat")
public String creat(@RequestParam("name")String name,
                    @RequestParam("to")String to,
                    @RequestParam("subject")String subject,
                    @RequestParam("text")String text){
    Mail mail = new Mail(name, to, subject, text);
    Mail m = postoffice.send(mail);
    //System.out.println(m.getNum() + m.getFrom());
    postoffice.send(m);
    return "redirect:/main";
}

@RequestMapping("/send")
public String send(@RequestParam("id")int id){
    System.out.println(postoffice.send(id));
    return "redirect:/main";
}
~~~

删除邮件，通过路径中 num 分别在内存中删除数据和在数据库中删除邮件，并停止线程

~~~java
@RequestMapping("/drop/{num}")
public String drop(@PathVariable("num")Integer num){
    postoffice.remove(num);
    return "redirect:/main";
}
~~~

完整代码

~~~java
@Controller
public class MailController {

    private Postoffice postoffice;
    @Autowired
    public void setMailMapper(Postoffice postoffice){
        this.postoffice = postoffice;
    }

    @RequestMapping("/login")
    public String login(@RequestParam("username") String username,
                       @RequestParam("password") String password,
                       Model model, HttpSession session){
        if(username.equals("") || password.equals("")){
            model.addAttribute("msg", "用户名或密码不能为空");
            return "index";
        }
        if(!password.equals("123456")){
            model.addAttribute("msg", "密码错误");
        }
        session.setAttribute("loginUser", username);
        postoffice.flush();
        model.addAttribute("postmen", postoffice.getPostmen());
////        for(Postman p: PostOffice.getPostmen()){
////            System.out.println(p.getMail().getNum() + p.getMail().getName());
////        }
        return "main";
    }

    @RequestMapping("/logout")
    public String logout(HttpSession session){
        session.removeAttribute("loginUser");
        return "index";
    }

    @RequestMapping("/creat")
    public String creat(@RequestParam("name")String name,
                       @RequestParam("to")String to,
                       @RequestParam("subject")String subject,
                       @RequestParam("text")String text){
        Mail mail = new Mail(name, to, subject, text);
        Mail m = postoffice.send(mail);
        //System.out.println(m.getNum() + m.getFrom());
        postoffice.send(m);
        return "redirect:/main";
    }

    @RequestMapping("/send")
    public String send(@RequestParam("id")int id){
        System.out.println(postoffice.send(id));
        return "redirect:/main";
    }

    @RequestMapping("/main")
    public String main(Model model){
        postoffice.flush();
        model.addAttribute("postmen", postoffice.getPostmen());
        return "main";
    }

    @RequestMapping("/drop/{num}")
    public String drop(@PathVariable("num")Integer num){
        postoffice.remove(num);
        return "redirect:/main";
    }
}

~~~

## 前端及 Issues

### 前端

> 采用 thymeleaf 进行前后端交互
>

开发阶段设置缓存为 false，使每次重启后更改立即生效，否则缓存会引发各种bug猜想，开始折磨

~~~yml
spring:
	thymeleaf:
    	cache: false
~~~

路径传参：通过`@{/drop/}+${p.getMail().getNum()}`这种方式传入

~~~html
<div class="text-right">						    	
    <a th:href="@{/drop/}+${p.getMail().getNum()}" class="card-more" data-toggle="read" data-id="1">
        <i class="ion-ios-arrow-left"></i>
        删除
        <i class="ion-ios-arrow-right"></i>
    </a>
</div>
~~~

主页`main.html`

~~~html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
	<head>
		<meta charset="utf-8">
		<title>Post Office</title>
		<link rel="stylesheet" href="icons/css/ionicons.min.css">
		<link rel="stylesheet" href="bootstrap/css/bootstrap.min.css">
		<link rel="stylesheet" href="sweetalert/dist/sweetalert.css">
		<link rel="stylesheet" href="css/stisla.css">

		<style>
			body{
				background-color: black;
			}

			#footer p{
				color: white;
			}

			#footer h2{
				color: white;
			}
		</style>
	</head>

	<body>

		<!--导航栏-->>
		<nav class="navbar navbar-expand-lg main-navbar" style="position: fixed; top: 0; background-color: black;">
			<div class="container-fluid">
				<div class="navbar-brand">
				  <h4 style="color: white; font-family: Georgia, 'Times New Roman', Times, serif">
					  <a th:href="@{/druid}" style="color: white; text-decoration: none"><span class="bold">NorthBoat's Post Office</span></a>
				  </h4>
				</div>

				<button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
					<span class="navbar-toggler-icon">
						<i class="ion-navicon"></i>
					</span>
				</button>

				<div class="collapse navbar-collapse" id="navbarNav">
					<div class="mr-auto"></div>
					<ul class="navbar-nav">
						<li class="nav-item"><a class="nav-link smooth-link" href="#home">Home</a></li>
						<li class="nav-item"><a class="nav-link smooth-link" href="#manage">Mails Manage</a></li>
						<li class="nav-item"><a class="nav-link smooth-link" href="#send">Send Mails</a></li>
						<li class="nav-item"><a class="nav-link smooth-link" th:href="@{/logout}">Logout</a></li>
					</ul>
				</div>
			</div>
		</nav>


		<!--Home页-->>
		<section class="hero bg-overlay" id="home" style="margin-bottom: 49px; margin-top: -49px;">
			<div class="text" style="margin-top: -24px;">
				<p class="lead">Welcome To NorthBoat's Post Office</p>
				<h1><span class="bold">常 </span>联 系 勿 <span class="bold">相 忘</span></h1>
				<div class="cta">
					<a href="#send" class="btn btn-primary smooth-link">Get Started</a>
					<div class="link" style="margin-top: 4%;">
						<a href="https://northboat.github.io/" style="text-decoration: none;">
							love is the one thing that transcends time and space
						</a>
					</div>
				</div>
			</div>
		</section>


		<!--管理页面-->
		<section class="padding bg-grey" id="manage">
			<div class="container">
				<h2 class="section-title" style="margin-top: 3%">Mail Manage</h2>
				<p class="section-lead text-muted">管理已收录的邮件</p>
				<div class="section-body">
					<div class="row col-spacing">

						<div class="col-12 col-md-6 col-lg-4" th:each="p:${postmen}">
							<article class="card">
								<div class="card-body">
								  	<div class="card-subtitle mb-2 text-muted">
										To <span th:text="${p.getMail().getTo()}"></span>
										from <span th:text="${p.getMail().getName()}"></span>
									</div>
							    	<h4 class="card-title"><span th:text="${p.getMail().getSubject()}"></span></h4>
							    	<p class="card-text" th:text="${p.getMail().getText()}"></p>
							    	<div class="text-right">						    	
								    	<a th:href="@{/drop/}+${p.getMail().getNum()}" class="card-more" data-toggle="read" data-id="1">
											<i class="ion-ios-arrow-left"></i>
											删除
											<i class="ion-ios-arrow-right"></i>
										</a>
							    	</div>
						    	</div>
						  	</article>
					  	</div>
					</div>
				</div>
			</div>
		</section>



		<section class="padding bg-grey" id="send" style="margin-top: -4%; padding-bottom: 12%;">
			<div class="container">
				<br><br><h2 class="section-title text-center">Send Mail</h2>
				<p class="section-lead text-center text-muted">Send friends u greetings, which will be sent once a week by u personal postman</p>
				<div class="section-body">				
					<div class="row col-spacing">
						<div class="col-12 col-md-6">
							<h2>stick u message ♂ in their heart</h2>
							<p class="text-muted">这发邮件，多是一件美事呀</p>
						</div>

						<div class="col-12 col-md-6">
							<div class="subscribe">
								<input type="button" class="btn btn-primary" onclick="resetForm()" value="清空">
							</div>
						</div>
						
						<div class="col-12 col-md-5">
							<p class="contact-text">If something wrong happened, please contact me</p>
							<ul class="contact-icon">
								<li><i class="ion ion-ios-telephone"></i> <div>+86 18630338418</div></li>
								<li><i class="ion ion-ios-email"></i> <div>1543625674@qq.com</div></li>
							</ul>
						</div>
						<div class="col-12 col-md-7">
							<form th:action="@{/send}" method="post" class="contact row" id="contact-form">

								<div class="form-group col-6">
									<input type="text" class="form-control" placeholder="Your Name" name="name">
								</div>
								<div class="form-group col-6">
									<input type="email" class="form-control" placeholder="the Email Address u wanna send" name="to" required>
								</div>
								<div class="form-group col-12">
									<input type="text" class="form-control" placeholder="Subject" name="subject" required>
								</div>
								<div class="form-group col-12">
									<textarea class="form-control" placeholder="Text Here" name="text" required></textarea>
								</div>
								<br>
								<div class="form-group col-12 mt-2">
									<button type="submit" class="btn btn-primary">
										Send Message
									</button>
								</div>
							</form>
						</div>

					</div>
				</div>
			</div>
		</section>

		<script src="js/jquery.min.js"></script>
		<script src="bootstrap/js/bootstrap.min.js"></script>
		<script>
			function resetForm(){
				document.getElementById("contact-form").reset();
			}		
		</script>
	</body>
</html>
~~~

### Issues

400 错误，传参有问题，检查参数名

- `java.lang.IllegalStateException: Failed to load property source from 'file:/E:/JavaWeb/springboot/PostOffice/target/classes/application.yml' `检查yml文件是否有不合规字符
- `Couldn't connect to host, port: localhost, 25; timeout -1` 纯纯牛马

改用 javax.mail，同样出现默认端口 25 无法使用的情况，加上以下设置修改端口号并且开启 ssl 连接

~~~java
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
~~~

## Spring Task 定时任务

定时任务，其使用分三步

1. 依赖注入
2. 启用注解
3. 定时方法

应用场景

- 数据同步：例如把 Redis 同步到关系型数据库
- 日志清理：定期清理无用日志
- 邮件发送：定时发送邮件

相比于 Timer，其优势在于：支持 cron 表达式；使用线程池管理定时线程；不会因异常终止任务；支持异步和分布式场景

### Quick Start

在 Spring 2.x+ 中，`spring-boot-starter`已经内置了 Spring Task，我们需要在 Spring 主类上启用 Scheduling 注解

```java
@SpringBootApplication
@EnableScheduling
public class TaskApplication{
    public static void main(String[] args){
        SpringApplication.run(TaskApplication.class, args)
    }
}
```

编写并注入定时方法

```java
@Component
public class MyTask{
    // 每天 23:59:59 触发
    @Scheduled(cron = "59 59 23 * * ?")
    public void dailyReport(){
        System.out.println("该睡觉了，明天再干吧");
    }
}
```

### Cron 表达式

即注解`@Scheduled`中`cron`字段的解析，他是一个 7 位的解码器，从前往后分别是

```
秒 分 时 日 月 周 年(可选)
```

一些常见的组合方式

| 含义                     | Cron 表达式         |
| ------------------------ | ------------------- |
| 每 30 分钟执行一次       | `0 0/30 * * * ？`   |
| 工作日上午 9 点执行一次  | `0 0 9 ? * MON-FRI` |
| 每月最后一天凌晨清空缓存 | `0 0 0 L * ?`       |
| 每天三顿提醒吃饭         | `0 0 7,12,18 * * ?` |

其中

- `*`表示每时每刻
- `?`表示不指定具体值
- `L`表示最后一刻，比如月的最后一天，年的最后一月
- `W`最近工作日
- `#`第 N 个周 X

一些应用实例

每一小时同步一次数据

```java
@Scheduled(finxedRate = 3600000)
public void syncOrderStatus(){}
```

每天凌晨 3 点清理缓存

```java
@Scheduled(cron = "0 0 3 * * ?")
public void cleanLogs(){}
```

每周一早上 9 点定时发送邮件

```java
@Scheduled(cron = "0 0 9 ? * MON")
public void sendWeeklyReport(){}
```

### 运行时修改 corn 表达式

不直接使用`@Scheduled`注解，而使用`ScheduledTaskRegistrar`来手动注册任务，并配合`CronTrigger`控制执行频率

- [Spring Boot 动态定时任务，别再写死 cron 表达式了！ - 腾讯云开发者社区-腾讯云](https://cloud.tencent.com/developer/news/2387563#:~:text=通过实现Sche,，无需重启服务。)

### XXL-JOB

XXL-JOB，开源的分布式任务调度平台

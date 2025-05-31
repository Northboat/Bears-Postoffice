package com.postoffice.controller;


import com.postoffice.service.Postoffice;
import com.postoffice.pojo.Mail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

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

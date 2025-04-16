package com.nowcoder.community;

import com.nowcoder.community.utils.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Scanner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTest {
    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void sendMail(){
        mailClient.sendmail("3513825673@qq.com","O_O","OvO");
    }
    @Test
    public void sendHtmlMail(){
        Context context = new Context();
        context.setVariable("username","傻逼");

        String content = templateEngine.process("/mail/demo",context);


        mailClient.sendmail("bfsh00000@outlook.com","O_O","1");
    }

    public static void main(String[] args) {
        String s= "iqfhbnei";
        boolean hbn = s.matches("hbn");
        System.out.println(hbn);
    }


}

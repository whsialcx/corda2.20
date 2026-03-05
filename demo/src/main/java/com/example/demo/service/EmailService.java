package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.verification.admin-email}")
    private String adminEmail;  // 接收审批邮件的超级管理员邮箱

    public void sendAdminVerificationEmail(String username, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject("【系统通知】新管理员注册审批");

        // 构造激活链接（假设后端运行在 8080 端口，可配置）
        String verificationUrl = "http://localhost:8080/api/auth/admin/verify?token=" + token;

        String content = "系统管理员您好：\n\n" +
                         "用户 [" + username + "] 正在申请注册成为管理员。\n" +
                         "请点击以下链接批准并激活该管理员账号（链接有效期12小时）：\n\n" +
                         verificationUrl + "\n\n" +
                         "如果链接无法点击，请复制到浏览器地址栏访问。";

        message.setText(content);
        mailSender.send(message);
    }
    
    public void sendApplicationRejectEmail(String userEmail, String organization, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(userEmail);
        message.setSubject("【系统通知】您的节点申请未通过审核");

        String content = "您好：\n\n" +
                        "您为组织 [" + organization + "] 提交的节点申请未通过审核。\n" +
                        "未通过原因如下：\n" +
                        "----------------------\n" +
                        reason + "\n" +
                        "----------------------\n" +
                        "如有疑问，请咨询系统管理员。";

        message.setText(content);
        mailSender.send(message);
    }
}
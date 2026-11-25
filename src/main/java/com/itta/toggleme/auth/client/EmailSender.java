package com.itta.toggleme.auth.client;

/** 인증 메일 발송. 실패하면 EmailSendFailedException을 던진다. */
public interface EmailSender {

    void sendVerificationCode(String to, String code);
}

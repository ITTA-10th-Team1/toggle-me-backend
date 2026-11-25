package com.itta.toggleme.auth.dto;

public record EmailVerificationTokenResponse(String verificationToken) {

    @Override
    public String toString() {
        return "EmailVerificationTokenResponse[]";
    }
}

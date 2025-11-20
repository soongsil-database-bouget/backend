package com.dbapplication.bouget.dto;

import lombok.Data;

/**
 * 카카오에서 가져온 유저 정보를
 * 우리 서비스 내부 형식으로 정리한 DTO
 */
@Data
public class KakaoUserInfo {

    private String id;              // kakao user id (문자열로 저장)
    private String email;
    private String nickname;
    private String profileImageUrl;
}

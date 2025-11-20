package com.dbapplication.bouget.service;

import com.dbapplication.bouget.dto.UserMeResponse;

public interface UserService {

    /**
     * userId 기준으로 DB에서 내 정보 조회
     */
    UserMeResponse getMyInfo(Long userId);
}

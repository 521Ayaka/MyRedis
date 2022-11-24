package com.ganga.dto;

import lombok.Data;

@Data
public class UserDTO {

    public static final String USER_DTO_ID = "id";
    public static final String USER_DTO_NICKNAME = "nickName";
    public static final String USER_DTO_ICON = "icon";

    private Long id;
    private String nickName;
    private String icon;
}

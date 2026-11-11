package com.example.crud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserCreateRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank(message = "手机号不能为空")
    //@Pattern 本身允许 null 值通过校验，因此不能单独使用
    @Pattern(
            regexp = "^1[3-9][0-9]{9}$",
            //这个正则只检查号码格式，不代表能够确认该号码真实存在或已经分配给用户。
            message = "请输入有效的11位中国大陆手机号码，13-19开头"
    )
    private String phone;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

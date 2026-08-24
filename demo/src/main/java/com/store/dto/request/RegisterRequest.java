package com.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest implements Serializable {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
    private String email;

    @Pattern(regexp = "^$|^(0[35789])[0-9]{8}$", message = "Số điện thoại không hợp lệ (10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09)")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự")
    @Pattern(regexp = "^[\\x21-\\x7E]+$", message = "Mật khẩu chỉ được chứa chữ cái không dấu, chữ số và ký tự đặc biệt (không chứa dấu tiếng Việt hoặc khoảng trắng)")
    private String password;
}

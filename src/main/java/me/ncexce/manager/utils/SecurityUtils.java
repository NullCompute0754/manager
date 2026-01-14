package me.ncexce.manager.utils;

import me.ncexce.manager.exceptions.InvalidCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 从安全上下文中获取当前登录的用户名
     * @return 当前用户名
     * @throws RuntimeException 如果用户未认证
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 检查认证信息是否存在且不是匿名用户
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }

        throw new InvalidCredentialsException("当前操作缺少有效的身份认证信息");
    }

    public static boolean isBusinessAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        // 从我们刚才设置的 Details 中取出业务角色字符串
        Object details = auth.getDetails();
        return "Administrator".equals(details);
    }
}
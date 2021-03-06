package com.sogou.upd.passport.admin.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * 随机生成id工具类
 * User: chengang
 * Date: 14-6-25
 * Time: 下午3:37
 */
public final class UuidUtil {

    private UuidUtil() {
    }


    private static final String STR_SPLIT = "-";

    /**
     * 生成原始带分隔符的uuid
     *
     * @return
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 随机初始化密码
     *
     * @return
     */
    public static String generatePassword() {
        return StringUtils.splitByWholeSeparator(generateUuid(), STR_SPLIT)[0];
    }

    /**
     * 生成不带分隔符的uuid
     *
     * @return
     */
    public static String genreateUUidWithOutSplit() {
        return generateUuid().replaceAll("-", "");
    }

    public static void main(String args[]) {
        System.out.println(generateUuid());
        System.out.println(genreateUUidWithOutSplit() +" | "+ genreateUUidWithOutSplit().length());
    }


}

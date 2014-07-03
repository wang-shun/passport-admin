package com.sogou.upd.passport.admin.account;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sogou.upd.passport.admin.BaseTest;
import com.sogou.upd.passport.admin.manager.accountAdmin.AccountAdminManager;
import com.sogou.upd.passport.common.result.Result;
import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 用户账号信息单元测试
 * User: chengang
 * Date: 14-6-26
 * Time: 下午5:26
 */
public class AccountAdminManagerImplTest extends BaseTest {


    @Autowired
    private AccountAdminManager accountAdminManager;

    @Ignore
    @Test
    public void testResetPassword() {
        String passportId = "gang.chen050dddd5@gmail.com";

//        String newPp = UuidUtil.generatePassword();

        Result result = accountAdminManager.resetUserPassword(passportId, true);
//        Assert.assertNotNull(result);
//        Assert.assertEquals(true, result.isSuccess());
        System.out.println(" ===== result " + result.toString());
    }

    @Test
    public void testUnBindMobiles() {
        String mobiles = "13071155730\\r\\n18910872640";
        List<String> mobileList = Lists.newArrayList();
        if (!Strings.isNullOrEmpty(mobiles)) {
            String[] mobilesArray = StringUtils.split(mobiles, "\r\n");
            if (mobilesArray.length > 0) {
                for (String mobile : mobilesArray) {
                    mobileList.add(mobile);
                }
            }
        }
        Result result = accountAdminManager.unBindMobiles(mobileList);
        Assert.assertNotNull(result);
        System.out.println(" testUnBindMobiles result:" + result.toString());
    }


}

package com.sogou.upd.passport.admin.web.controller;

import com.sogou.upd.passport.admin.common.model.Page;
import com.sogou.upd.passport.admin.manager.accountAdmin.AccountAdminManager;
import com.sogou.upd.passport.admin.manager.blackList.BlackListManager;
import com.sogou.upd.passport.admin.manager.form.AccountSearchParam;
import com.sogou.upd.passport.admin.manager.model.AccountDetailInfo;
import com.sogou.upd.passport.admin.web.BaseController;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * Created with IntelliJ IDEA.
 * User: lzy_clement
 * Date: 14-11-26
 * Time: 下午3:25
 * To change this template use File | Settings | File Templates.
 */
@Controller
@RequestMapping("/admin")
public class BlackListAdminController extends BaseController {

    private Logger log = LoggerFactory.getLogger(BlackListAdminController.class);

    @Autowired
    private BlackListManager blackListManager;
    @Autowired
    private AccountAdminManager accountAdminManager;

    /**
     * 查询黑名单列表（模糊查询）
     * @param passportId
     * @param pageNo
     * @param model
     * @return
     */
    @RequestMapping(value = "/blackList/queryBlackList")
    public String queryAllBlackList(String passportId, Integer pageNo, Model model) {
        try{
            if (null == pageNo)
                pageNo = 1;
            Page page = blackListManager.getBlackList(passportId, pageNo, 10);
            model.addAttribute("passportId",passportId);
            model.addAttribute("page", page);
        }catch(Exception e){
            log.error("查询失败",e);
        }
        return "/pages/admin/blacklist/blacklist_list.jsp";
    }

    /**
     * 新增黑名单
     * @param passportId_input
     * @param expire_time
     * @return
     */
    @RequestMapping(value = "/blackList/addBlackList")
    @ResponseBody
    public String addBlackList(String passportId_input,long expire_time){
        JSONObject json = new JSONObject();
        try{
            AccountSearchParam tmp = new AccountSearchParam();
            tmp.setUserName(passportId_input);
            AccountDetailInfo accountDetail = accountAdminManager.getAccountDetailInfo(tmp);
            if(accountDetail.getPassportId() == null){
                json.put("success", false);
                json.put("message","没有此用户");
            } else{
                expire_time = System.currentTimeMillis() + expire_time * 60000 ;
                boolean flag = blackListManager.insertBlackList(passportId_input,expire_time);
                if(flag){
                    json.put("success", true);
                    json.put("message", "添加成功");
                } else {
                    json.put("success", false);
                    json.put("message", "黑名单中已有该用户！");
                }
            }
        }catch (Exception e){
            log.error("新增失败",e);
            json.put("success", false);
            json.put("message","添加失败，请联系管理员！");
        }
        return json.toString();
    }

    /**
     * 更新黑名单状态
     * @param passportId
     * @param status
     * @return
     */
    @RequestMapping("/blackList/updateBlackList")
    @ResponseBody
    public String updateBlackListStatus(String id,boolean status){
        JSONObject json = new JSONObject();
        try{
        boolean flag = blackListManager.updateBlackListStatus(id,status);
        if(flag){
            json.put("success", true);
            json.put("message", "修改成功");
        } else{
            json.put("success", false);
            json.put("message", "修改失败，请联系管理员！");
        }
        }catch(Exception e){
            log.error("修改失败",e);
            json.put("success", false);
            json.put("message","修改失败，请联系管理员！");
        }
        return json.toString();
    }

}

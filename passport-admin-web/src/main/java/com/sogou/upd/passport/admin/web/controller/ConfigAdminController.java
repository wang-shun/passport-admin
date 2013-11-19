package com.sogou.upd.passport.admin.web.controller;

import com.alibaba.dubbo.common.json.JSONObject;
import com.google.common.collect.Maps;
import com.sogou.upd.passport.admin.manager.config.ConfigManager;
import com.sogou.upd.passport.admin.web.BaseController;
import com.sogou.upd.passport.admin.web.form.ClientVo;
import com.sogou.upd.passport.admin.web.form.LevelVo;
import com.sogou.upd.passport.common.result.APIResultSupport;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.model.app.AppConfig;
import com.sogou.upd.passport.model.config.ClientIdLevelMapping;
import com.sogou.upd.passport.model.config.InterfaceLevelMapping;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.util.JSONPObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.hash.JacksonHashMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: liuling
 * Date: 13-11-11
 * Time: 上午1:28
 * To change this template use File | Settings | File Templates.
 */
@Controller
@RequestMapping("/admin")
public class ConfigAdminController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigAdminController.class);

    private final static String PRIMARY_LEVEL = "0";
    private final static String MIDDLE_LEVEL = "1";
    private final static String HIGH_LEVEL = "2";
    private final static String PRIMARY_LEVEL_COUNT = "0";
    private final static String MIDDLE_LEVEL_COUNT = "0";
    private final static String HIGH_LEVEL_COUNT = "0";
    private final static String PRIMARY_LEVEL_NAME = "初级";
    private final static String MIDDLE_LEVEL_NAME = "中级";
    private final static String HIGH_LEVEL_NAME = "高级";


    @Autowired
    private ConfigManager configManager;

    /**
     * 接口列表查询页面
     *
     * @param model
     * @return
     */
    @RequestMapping(value = "/interface/queryinterfacelist", method = RequestMethod.GET)
    public String queryInterfaceList(Model model) throws Exception {
        List<InterfaceLevelMapping> interfaceVOList = null;
        try {
             interfaceVOList = configManager.findInterfaceLevelMappingList();
        } catch (Exception e) {
            logger.error("queryInterfaceList error:", e);
        }
        model.addAttribute("interfaceVOList", interfaceVOList);
        return "/pages/admin/config/interface.jsp";
    }

    /**
     * 保存接口,新增或修改的
     *
     * @param interfaceName
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/interface/saveinterface", method = RequestMethod.POST)
    public String saveInterface(@RequestParam("interfaceName") String interfaceName, @RequestParam("interId") String interId) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            InterfaceLevelMapping ilm = new InterfaceLevelMapping();
            if (!"".equals(interId) && interId != null) {
                ilm.setId(Long.parseLong(interId));
            }
            ilm.setInterfaceName(interfaceName);
            ilm.setPrimaryLevel(PRIMARY_LEVEL);
            ilm.setPrimaryLevelCount(PRIMARY_LEVEL_COUNT);
            ilm.setMiddleLevel(MIDDLE_LEVEL);
            ilm.setMiddleLevelCount(MIDDLE_LEVEL_COUNT);
            ilm.setHighLevel(HIGH_LEVEL);
            ilm.setHighLevelCount(HIGH_LEVEL_COUNT);
            boolean isSuccess = configManager.saveOrUpdateInterfaceLevelMapping(ilm);
            if (isSuccess) {
                return "redirect:/admin/interface/queryinterfacelist";
            } else {
                result.setMessage("保存接口失败！");
            }
        } catch (Exception e) {
            logger.error("saveInterfcae error:", e);
        }
        return result.toString();
    }

    /**
     * 修改接口信息之前先读取接口信息,显示在新增页面上,新增页面与修改页面相同
     *
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/interface/getinterface", method = RequestMethod.GET)
    public String getInterfaceById(@RequestParam("id") String id, Model model) throws Exception {
        InterfaceLevelMapping ilm = null;
        try {
            ilm = configManager.findInterfaceById(id);
        } catch (Exception e) {
            logger.error("getInterfaceById Error:id is " + id, e);
        }
        model.addAttribute("interfaceVO", ilm);
        return "/pages/admin/config/addInterface.jsp";
    }

    /**
     * 删除指定接口
     *
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/interface/delInterface", method = RequestMethod.GET)
    public String delInterface(@RequestParam("id") String id) throws Exception {
        try {
            configManager.deleteInterfaceLevelById(id);
        } catch (Exception e) {
            logger.error("delInterface error:id is " + id, e);
        }
        return "forward:/admin/interface/queryinterfacelist";
    }

    /**
     * 获取应用和等级列表,初始化应用和等级的下拉列表
     *
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/interface/getclientandlevel", method = RequestMethod.GET)
    public String getClientIdAndLevelList(Model model) throws Exception {
        List<ClientVo> clientVOList = new ArrayList<>();
        List<LevelVo> levelVOList = new ArrayList<>();
        try {

            List<AppConfig> appList = configManager.getAppList();
            if (appList != null && appList.size() > 0) {
                for (AppConfig ac : appList) {
                    ClientVo cv = new ClientVo();
                    String clientId = String.valueOf(ac.getClientId());
                    String clientName = ac.getClientName();
                    cv.setClientId(clientId);
                    cv.setClientName(clientName);
                    clientVOList.add(cv);
                }
                model.addAttribute("clientVOList", clientVOList);
                levelVOList = getLevelList();
                model.addAttribute("levelVOList", levelVOList);
            }
        } catch (Exception e) {
            logger.error("getClientIdAndLevelList error:", e);
        }

        return "/pages/admin/config/clientAndLevel.jsp";
    }

    private List<LevelVo> getLevelList() throws Exception {
        List<LevelVo> levelVoList = new ArrayList<>();
        LevelVo lv1 = new LevelVo();
        lv1.setLevelId(PRIMARY_LEVEL);
        lv1.setLevelName(PRIMARY_LEVEL_NAME);
        LevelVo lv2 = new LevelVo();
        lv2.setLevelId(MIDDLE_LEVEL);
        lv2.setLevelName(MIDDLE_LEVEL_NAME);
        LevelVo lv3 = new LevelVo();
        lv3.setLevelId(HIGH_LEVEL);
        lv3.setLevelName(HIGH_LEVEL_NAME);
        levelVoList.add(lv1);
        levelVoList.add(lv2);
        levelVoList.add(lv3);
        return levelVoList;
    }

    /**
     * 根据应用查询该应用对应的等级,应用和等级做联动处理，有等级的显示，无等级显示“请选择”
     *
     * @param clientId
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/interface/getlevelbyclientid", method = RequestMethod.GET)
    @ResponseBody
    public String getLevelByClientId(@RequestParam("clientId") String clientId, Model model) throws Exception {
        ClientIdLevelMapping clm = null;
        try {
            clm = configManager.getLevelByClientId(clientId);
        } catch (Exception e) {
            logger.error("getLevelByClientId error:clientId is " + clientId, e);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        if (clm != null) {
            return objectMapper.writeValueAsString(clm);
        } else {
            ClientIdLevelMapping clmNull = new ClientIdLevelMapping();
            clmNull.setLevelInfo("");
            return objectMapper.writeValueAsString(clmNull);
        }
    }

    /**
     * 保存应用和等级信息
     *
     * @param clientId
     * @param level
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/interface/saveclientlevel", method = RequestMethod.POST)
    public String saveClientIdAndLevel(@RequestParam("clientId") String clientId, @RequestParam("level") String level) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            ClientIdLevelMapping clm = new ClientIdLevelMapping();
            clm.setClientId(clientId);
            clm.setLevelInfo(level);
            boolean isSuccess = configManager.saveOrUpdateClientAndLevel(clm);
            if (isSuccess) {
                result.setSuccess(true);
                result.setMessage("保存应用与等级成功！");
            } else {
                result.setMessage("保存应用与等级失败！");
            }
        } catch (Exception e) {
            logger.error("saveClientIdAndLevel error:clientid is " + clientId, e);
        }
        return result.toString();
    }

    /**
     * 显示接口，等级，及对应的次数列表
     *
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/interface/getinterfaceandlevellist", method = RequestMethod.GET)
    public String getInterfaceAndLevelList(Model model) throws Exception {
        Map<String, List<InterfaceLevelMapping>> maps;
        try {
            maps = configManager.getInterfaceMapByLevel();
            if (maps != null && maps.size() > 0) {
                List<InterfaceLevelMapping> interfaceVOList = maps.get("primaryList");
                if (interfaceVOList != null && interfaceVOList.size() > 0) {
                    model.addAttribute("interfaceVOList", interfaceVOList);
                    model.addAttribute("rowCount", interfaceVOList.size());
                }

            }
        } catch (Exception e) {
            logger.error("getInterfaceAndLevelList error:", e);
        }
        return "/pages/admin/config/interfaceMain.jsp";
    }

    /**
     * 修改之前，先根据id查询该接口，等级及次数的对应信息
     *
     * @param id
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/interface/getinterfacelevelbyid", method = RequestMethod.GET)
    public String getInterfaceLevelById(@RequestParam("id") String id, Model model) throws Exception {
        InterfaceLevelMapping ilm = null;
        try {
            ilm = configManager.findInterfaceById(id);
        } catch (Exception e) {
            logger.error("getInterfaceLevelById", e);
        }
        model.addAttribute("interfaceLevelVO", ilm);
        return "/pages/admin/config/interfaceMain.jsp";
    }

    /**
     * 保存接口与等级，及对应的次数
     *
     * @param interfaceName
     * @param level
     * @param levelCount
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/interface/saveinterfaceandlevel", method = RequestMethod.POST)
    public String saveInterfaceAndLevel(@RequestParam("id") String id, @RequestParam("interfaceName") String interfaceName, @RequestParam("level") String level, @RequestParam("levelCount") String levelCount) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            InterfaceLevelMapping ilm = new InterfaceLevelMapping();
            setValue(ilm, level, levelCount);
            ilm.setId(Long.parseLong(id));
            ilm.setInterfaceName(interfaceName);
            boolean isSuccess = configManager.saveOrUpdateInterfaceLevelMapping(ilm);
            if (isSuccess) {
                return "redirect:/admin/interface/getinterfaceandlevellist";
            } else {
                result.setMessage("保存接口与等级信息失败！");
            }
        } catch (Exception e) {
            logger.error("saveInterfaceAndLevel error: id is " + id, e);
        }
        return result.toString();
    }

    private InterfaceLevelMapping setValue(InterfaceLevelMapping ilm, String level, String levelCount) {
        switch (level) {
            case "0":
                ilm.setPrimaryLevel(level);
                ilm.setPrimaryLevelCount(levelCount);
                break;
            case "1":
                ilm.setMiddleLevel(level);
                ilm.setMiddleLevelCount(levelCount);
                break;
            case "2":
                ilm.setHighLevel(level);
                ilm.setHighLevelCount(levelCount);
                break;
        }
        return ilm;
    }

}

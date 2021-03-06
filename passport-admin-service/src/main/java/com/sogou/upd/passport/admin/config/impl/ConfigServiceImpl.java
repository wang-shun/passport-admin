package com.sogou.upd.passport.admin.config.impl;

import com.sogou.upd.passport.admin.config.ConfigService;
import com.sogou.upd.passport.admin.dao.problem.config.ConfigDAO;
import com.sogou.upd.passport.admin.model.config.ClientIdLevelMapping;
import com.sogou.upd.passport.admin.model.config.InterfaceLevelMapping;
import com.sogou.upd.passport.common.CacheConstant;
import com.sogou.upd.passport.common.utils.RedisUtils;
import com.sogou.upd.passport.exception.ServiceException;
import com.sogou.upd.passport.model.app.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: liuling
 * Date: 13-11-6
 * Time: 下午8:58
 * To change this template use File | Settings | File Templates.
 */
@Service
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private ConfigDAO configDAO;

    @Autowired
    private RedisUtils redisUtils;

    private static final String CACHE_PREFIX_PASSPORT_INTER_AND_LEVEL = CacheConstant.CACHE_PREFIX_CLIENTID_INTERFACE_LIMITED_INIT;

    private static final String CACHE_PREFIX_PASSPORT_CLIENT = CacheConstant.CACHE_PREFIX_CLIENTID;


    /**
     * 修改之前需要读出接口信息
     *
     * @param id
     * @return
     * @throws ServiceException
     */
    @Override
    public InterfaceLevelMapping findInterfaceById(long id) throws ServiceException {
        try {
            InterfaceLevelMapping inter = configDAO.findInterfaceById(id);
            if (inter != null) {
                return inter;
            }
        } catch (Exception e) {
            throw new ServiceException();
        }
        return null;
    }

    /**
     * 页面加载后，需要显示接口列表信息
     *
     * @return
     * @throws ServiceException
     */
    @Override
    public List<InterfaceLevelMapping> findInterfaceLevelMappingList() throws ServiceException {
        List<InterfaceLevelMapping> listInters;
        try {
            listInters = configDAO.findInterfaceLevelMappingList();
            if (listInters != null && listInters.size() > 0) {
                return listInters;
            }
        } catch (Exception e) {
            throw new ServiceException();
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * 在接口列表中，新增或修改某个接口信息,新增id为空，修改id非空，只针对接口，没有等级信息
     *
     * @param interfaceLevelMapping 新增或修改接口和等级信息
     * @return
     * @throws ServiceException
     */
    @Override
    public boolean saveOrUpdateInterfaceLevelMapping(InterfaceLevelMapping interfaceLevelMapping) throws ServiceException {
        interfaceLevelMapping.setCreateTime(new Date());
        int row;
        try {
            if (interfaceLevelMapping.getId() != 0) {
                //修改接口
                row = configDAO.updateInterfaceLevelMapping(interfaceLevelMapping);
            } else {
                //新增接口
                row = configDAO.insertInterfaceLevelMapping(interfaceLevelMapping);
            }
            if (row != 0) {
                //数据库操作成功后，缓存操作,先读数据库中所有有等级的应用
                List<ClientIdLevelMapping> appList = configDAO.findClientIdAndLevelList();
                if (appList != null && appList.size() > 0) {
                    for (ClientIdLevelMapping clm : appList) {
                        int clientId = clm.getClientId();
                        //先删除应用所有接口信息
                        String hashCacheKey = buildCacheKey(clientId);
                        redisUtils.delete(hashCacheKey);
                        //重新刷新一遍缓存,先得到应用等级信息
                        int level = clm.getLevelInfo();
                        reInitInterfaceLevel(hashCacheKey, level);
                    }
                }
                return true;
            }
        } catch (Exception e) {
            throw new ServiceException();
        }
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public InterfaceLevelMapping findInterfaceByName(String interName) throws ServiceException {
        try {
            if (!"".equals(interName) && interName != null) {
                InterfaceLevelMapping ilm = configDAO.getInterfaceByName(interName);
                if (ilm != null) {
                    return ilm;
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            throw new ServiceException();
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * 重新刷新一遍已有应用的接口，等级信息
     *
     * @param hashCacheKey
     * @param level
     */
    private void reInitInterfaceLevel(String hashCacheKey, int level) {
        //获取接口列表
        List<InterfaceLevelMapping> interfaceList = configDAO.getInterfaceListAll();
        for (InterfaceLevelMapping ilm : interfaceList) {
            //key是接口名称，value是此等级下该接口对应的频次限制
            String key = ilm.getInterfaceName();
            long value = getValue(ilm, level);
            //更新缓存,更新该应用下所有接口等级
            try {
                redisUtils.hPut(hashCacheKey, key, value);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    /**
     * 在接口列表中，删除某个接口信息
     *
     * @param id 要删除的接口id
     * @return
     * @throws ServiceException
     */
    @Override
    public boolean deleteInterfaceLevelById(long id) throws ServiceException {
        try {
            //查询接口信息
            InterfaceLevelMapping ilm = configDAO.findInterfaceById(id);
            String key = ilm.getInterfaceName();
            //删除数据库中接口
            int row = configDAO.deleteInterfaceLevelMappingById(id);
            if (row != 0) {
                //查询所有应用列表
                List<ClientIdLevelMapping> listResult = configDAO.findClientIdAndLevelList();
                if (listResult != null && listResult.size() > 0) {
                    for (ClientIdLevelMapping clm : listResult) {
                        String cacheKey = buildCacheKey(clm.getClientId());
                        //删除缓存中以该接口为key的缓存记录
                        redisUtils.hDelete(cacheKey, key);
                    }
                }
                return true;
            }
        } catch (Exception e) {
            throw new ServiceException();
        }
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * 获取接口总数目
     *
     * @return
     * @throws ServiceException
     */
    @Override
    public int getInterfaceCount() throws ServiceException {
        int count;
        try {
            count = configDAO.getInterfaceCount();
        } catch (Exception e) {
            throw new ServiceException();
        }
        return count;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * 在显示应用和等级列表后，先会读出每个应用已有等级
     *
     * @return
     * @throws ServiceException
     */
    @Override
    public ClientIdLevelMapping findLevelByClientId(int clientId) throws ServiceException {
        if (clientId != 0) {
            ClientIdLevelMapping clientIdLevelMapping = configDAO.findLevelByClientId(clientId);
            if (clientIdLevelMapping != null) {
                return clientIdLevelMapping;
            }
        }
        return null;
    }

    /**
     * 显示应用与等级的下拉列表信息
     *
     * @return
     * @throws ServiceException
     */
    @Override
    public List<ClientIdLevelMapping> findClientIdLevelMappingList() throws ServiceException {
        List<ClientIdLevelMapping> list;
        try {
            list = configDAO.findClientIdAndLevelList();
            if (list != null && list.size() > 0) {
                return list;
            }
        } catch (Exception e) {
            throw new ServiceException();
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * 保存应用与等级的映射关系,其中涉及到写缓存,会往缓存里写应用与接口的对应关系及次数,就要根据等级查出下面所有的接口列表
     *
     * @param clientIdLevelMapping
     * @return
     * @throws com.sogou.upd.passport.exception.ServiceException
     *
     */
    @Override
    public boolean saveOrUpdateClientAndLevel(ClientIdLevelMapping clientIdLevelMapping) throws ServiceException {
        try {
            int clientId = clientIdLevelMapping.getClientId();
            int row;
            //先查是否存在，确定是新增还是修改
            ClientIdLevelMapping clmForLevel = configDAO.findLevelByClientId(clientId);
            if (clmForLevel == null) {
                //新增应用与等级映射关系
                row = configDAO.insertClientIdAndLevel(clientIdLevelMapping);
            } else {
                //更新数据库中应用与等级映射关系
                row = configDAO.updateClientIdAndLevelMapping(clientIdLevelMapping);
            }
            if (row != 0) {
                //新增应用与等级关系，才写缓存
                if (clmForLevel == null) {
                    //将新添等级的产品id写入缓存
                    String clientHashCacheKey = buildClientHashKey();
                    redisUtils.sadd(clientHashCacheKey, String.valueOf(clientId));
                }
                String hashCacheKey = buildCacheKey(clientId);
                int level = clientIdLevelMapping.getLevelInfo();
                reInitInterfaceLevel(hashCacheKey, level);
                return true;
            }
        } catch (Exception e) {
            throw new ServiceException();
        }
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private String buildClientHashKey() throws ServiceException {
        return CACHE_PREFIX_PASSPORT_CLIENT;
    }

    /**
     * 在页面上显示三个等级下的所有接口及其对应的次数
     *
     * @return
     * @throws ServiceException
     */
    @Override
    public List<InterfaceLevelMapping> getInterfaceMapByLevel() throws ServiceException {
        List<InterfaceLevelMapping> interListAll = null;
        try {
            interListAll = configDAO.getInterfaceListAll();
        } catch (Exception e) {
            new ServiceException();
        }
        return interListAll;
    }

    /**
     * 根据应用id查询该应用对应的等级
     *
     * @param clientId
     * @return
     * @throws ServiceException
     */
    @Override
    public ClientIdLevelMapping getLevelByClientId(int clientId) throws ServiceException {
        ClientIdLevelMapping clm;
        try {
            clm = configDAO.getLevelByClientId(clientId);
            if (clm != null) {
                return clm;
            }
        } catch (Exception e) {
            throw new ServiceException();
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * 根据该应用id查询该应用名称
     *
     * @param clientId
     * @return
     * @throws ServiceException
     */
    @Override
    public AppConfig getAppNameByAppId(int clientId) throws ServiceException {
        AppConfig appConfig;
        try {
            appConfig = configDAO.getAppNameByAppId(clientId);
            if (appConfig != null) {
                return appConfig;
            }
        } catch (Exception e) {
            throw new ServiceException();
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * 列表形式获取所有应用
     *
     * @return
     * @throws ServiceException
     */
    @Override
    public List<AppConfig> getAppList() throws ServiceException {
        List<AppConfig> appList;
        try {
            appList = configDAO.getAppList();
            if (appList != null && appList.size() > 0) {
                return appList;
            }
        } catch (Exception e) {
            throw new ServiceException();
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    private long getValue(InterfaceLevelMapping inter, int level) {
        long value = 0;
        switch (level) {
            case 0:
                value = inter.getPrimaryLevelCount();
                break;
            case 1:
                value = inter.getMiddleLevelCount();
                break;
            case 2:
                value = inter.getHighLevelCount();
                break;
        }
        return value;
    }

    private String buildCacheKey(int clientId) {
        return CACHE_PREFIX_PASSPORT_INTER_AND_LEVEL + clientId;
    }
}

package cn.iocoder.yudao.module.agent.service.skillmeta;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo.SkillMetaPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo.SkillMetaSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.skillmeta.AiSkillMetaDO;

import java.util.List;

/**
 * 技能商店 Service 接口（QwenPaw 技能池元数据管理）
 *
 * @author 吴皓
 */
public interface AiSkillMetaService {

    /**
     * 创建技能商店项（仅 Java 侧元数据，不上传到 QwenPaw）
     */
    Long createSkillMeta(SkillMetaSaveReqVO createReqVO);

    /**
     * 更新技能商店项（仅 Java 侧元数据：icon/可见性/描述等）
     */
    void updateSkillMeta(SkillMetaSaveReqVO updateReqVO);

    /**
     * 删除技能商店项（同时从 QwenPaw 技能池删除）
     */
    void deleteSkillMeta(Long id);

    /**
     * 获取技能商店项详情
     */
    AiSkillMetaDO getSkillMeta(Long id);

    /**
     * 分页查询技能商店项
     */
    PageResult<AiSkillMetaDO> getSkillMetaPage(SkillMetaPageReqVO pageReqVO);

    /**
     * 查询当前用户可见的技能列表（公开 + 自己的个人技能）
     */
    List<AiSkillMetaDO> getVisibleSkillMetaList(Long userId);

    /**
     * 上传 zip 到 QwenPaw 技能池并创建 Java 侧元数据记录
     *
     * @param data       zip 文件字节
     * @param fileName   文件名
     * @param targetName 目标技能名（可为 null）
     * @param displayName 显示名称
     * @param description 描述
     * @param icon       图标
     * @param visibility 可见性: 0=个人, 1=公开
     * @param ownerUserId 创建者用户ID
     * @param tags       标签
     * @return 创建的技能商店项 ID
     */
    Long uploadSkillToPool(byte[] data, String fileName, String targetName,
                           String displayName, String description, String icon,
                           Integer visibility, Long ownerUserId, String tags);

    /**
     * 从 QwenPaw 技能池同步技能列表到 Java 侧（为池中尚无元数据记录的技能创建默认记录）
     *
     * @return 新增同步的记录数
     */
    int syncFromQwenPaw();

    /**
     * 从技能池安装技能到智能体（通过 QwenPaw pool/download）
     *
     * @param skillName  QwenPaw 技能池中的技能名
     * @param qwenpawAgentId 目标智能体的 QwenPaw agent ID
     */
    void installSkillFromPool(String skillName, String qwenpawAgentId);

}

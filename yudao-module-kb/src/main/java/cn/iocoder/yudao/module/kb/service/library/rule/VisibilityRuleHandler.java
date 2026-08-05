package cn.iocoder.yudao.module.kb.service.library.rule;

import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;

/**
 * 可见性规则处理器（策略接口）
 * <p>
 * 每个 visibilityRule 值对应一个实现类，封装该规则下的：
 * - 可见性判断（canSee）
 * - 管理权限判断（canManage）
 * - SQL 条件生成（toSqlCondition，用于分页查询下推到 SQL 层）
 * <p>
 * 新增规则（如 rule=7/8/9）只需新增一个实现类并注册为 Spring Bean，
 * 无需修改 LibraryServiceImpl / LibraryMapper 中的任何代码。
 *
 * @author 吴皓
 */
public interface VisibilityRuleHandler {

    /**
     * 该处理器负责的 visibilityRule 值
     */
    int rule();

    /**
     * 判断知识库是否对当前用户可见（Java 层过滤）
     *
     * @param ctx  可见性上下文
     * @param lib  知识库
     * @param cfg  层级配置
     * @return true=可见
     */
    boolean canSee(VisibilityContext ctx, LibraryDO lib, LevelConfigDO cfg);

    /**
     * 判断当前用户是否可管理该知识库（创建/编辑/删除）
     *
     * @param ctx  可见性上下文
     * @param lib  知识库
     * @param cfg  层级配置
     * @return true=可管理
     */
    boolean canManage(VisibilityContext ctx, LibraryDO lib, LevelConfigDO cfg);

    /**
     * 生成 SQL WHERE 条件片段（用于分页查询下推到 SQL 层）
     * <p>
     * 返回 null 表示该规则不参与 SQL 过滤（例如某些纯 Java 层判断的场景）。
     *
     * @param ctx  可见性上下文
     * @param cfg  层级配置
     * @return SQL 条件片段，如 "(kb_level_id = 1 AND owner_id = 123)"
     */
    default String toSqlCondition(VisibilityContext ctx, LevelConfigDO cfg) {
        return null;
    }
}

package cn.iocoder.yudao.module.kb.service.wiki;

import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiGraphRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiPageRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiPageSaveReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiRevisionRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiRevertReqVO;

import java.util.List;

/**
 * Wiki 页面 Service（对标 WeKnora wiki_page.service）
 *
 * <p>数据面在 python-vector（ES kb_wiki_all）：只读/issue/图谱转发；人工编辑 save/revert/revisions
 * 在 ES 正文之上 + MySQL kb_wiki_revision 修订薄表纯载体（(kb_id, slug, revision) 键控）。
 * 不再写 kb_wiki_page 主表，也不接收蒸馏落库（python 直落 ES）。</p>
 */
public interface WikiPageService {

    /** 列某知识库全部页面（列表/目录树） */
    List<WikiPageRespVO> listByKb(Long kbId);

    /** 页面详情（按 KB+slug 键控） */
    WikiPageRespVO getPage(Long kbId, String slug);

    /** 保存（新增/人工编辑），乐观锁防并发覆盖；正文写 ES */
    void savePage(WikiPageSaveReqVO reqVO);

    /** 回滚到某版本（revision+1 且 edit_source=revert） */
    Integer revertPage(WikiRevertReqVO reqVO);

    /** 修订历史列表（读 MySQL 薄表载体） */
    List<WikiRevisionRespVO> listRevisions(Long kbId, String slug);

    /** 图谱（overview=top-N 全局；ego=以某页为中心；types=page_type 白名单，逗号分隔） */
    WikiGraphRespVO graph(Long kbId, String mode, String centerSlug, Integer depth,
                          String types, Integer limit);

    /** 列出某知识库全部目录树（含各目录直接挂载页数，供前端组树） */
    List<cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiFolderRespVO> listFolders(Long kbId);

    /** 知识库 index 聚合（intro + 按 page_type 分组目录） */
    cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiIndexRespVO getIndex(Long kbId);

    /** 列出知识库待修复问题（可按 slug/status 过滤） */
    List<cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiIssueRespVO> listIssues(Long kbId, String slug, String status);

    /** 更新问题状态（pending/ignored/resolved）——处理或标记误报 */
    void updateIssueStatus(Long kbId, String slug, String issueType, String status);

    /** 统计某知识库待处理问题数 */
    Long countPendingIssues(Long kbId);
}
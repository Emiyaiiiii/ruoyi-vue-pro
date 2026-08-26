package cn.iocoder.yudao.module.kb.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * kb 模块错误码枚举类
 *
 * kb 系统，使用 1-010-000-000 段
 */
public interface ErrorCodeConstants {

    ErrorCode CATEGORY_NOT_EXISTS = new ErrorCode(1, "知识库分类不存在");
    ErrorCode CATEGORY_EXITS_CHILDREN = new ErrorCode(2, "存在存在子知识库分类，无法删除");
    ErrorCode CATEGORY_PARENT_NOT_EXITS = new ErrorCode(3,"父级知识库分类不存在");
    ErrorCode CATEGORY_PARENT_ERROR = new ErrorCode(4, "不能设置自己为父知识库分类");
    ErrorCode CATEGORY_NAME_DUPLICATE = new ErrorCode(5, "已经存在该分类名称的知识库分类");
    ErrorCode CATEGORY_PARENT_IS_CHILD = new ErrorCode(6, "不能设置自己的子Category为父Category");

    // ========== 知识库层级配置 1-010-001-000 ==========
    ErrorCode LEVEL_CONFIG_NOT_EXISTS = new ErrorCode(7, "知识库层级配置不存在");

    ErrorCode LIBRARY_NOT_EXISTS = new ErrorCode(8, "知识库不存在");

    ErrorCode SHARE_DEPT_NOT_EXISTS = new ErrorCode(9, "知识库共享部门关联不存在");

    ErrorCode DOCUMENT_NOT_EXISTS = new ErrorCode(10, "知识库文件不存在");

    ErrorCode LIBRARY_PERMISSION_DENIED = new ErrorCode(11, "无权限操作该知识库");

    ErrorCode DOCUMENT_PERMISSION_DENIED = new ErrorCode(12, "无权限查看该知识库文件内容");

    // ========== 文档文件夹 1-010-002-000 ==========
    ErrorCode FOLDER_NOT_EXISTS = new ErrorCode(13, "文档文件夹不存在");
    ErrorCode FOLDER_EXITS_CHILDREN = new ErrorCode(14, "文件夹下存在子文件夹，无法删除");
    ErrorCode FOLDER_NAME_DUPLICATE = new ErrorCode(15, "同层级下已存在同名文件夹");

    // ========== 向量处理任务 1-010-003-000 ==========
    ErrorCode VECTOR_TASK_NOT_EXISTS = new ErrorCode(16, "向量处理任务不存在");
    ErrorCode VECTOR_TASK_SUBMIT_FAILED = new ErrorCode(17, "向量处理任务提交失败");
    ErrorCode VECTOR_TASK_CANCEL_FAILED = new ErrorCode(18, "向量处理任务取消失败");
    ErrorCode VECTOR_TASK_ALREADY_FINISHED = new ErrorCode(19, "向量处理任务已结束，无法取消");

    // ========== 大模型配置 1-010-004-000 ==========
    ErrorCode MODEL_CONFIG_NOT_EXISTS = new ErrorCode(20, "大模型配置不存在");
    ErrorCode MODEL_CONFIG_UID_EXISTS = new ErrorCode(21, "模型UID已存在");
    ErrorCode MODEL_CONFIG_ALREADY_ACTIVE = new ErrorCode(22, "该配置已激活");
    ErrorCode MODEL_CONFIG_ALREADY_INACTIVE = new ErrorCode(23, "该配置已停用");
    ErrorCode MODEL_CONFIG_BATCH_ACTION_INVALID = new ErrorCode(24, "不支持的批量操作类型");
    ErrorCode MODEL_CONFIG_TYPE_INVALID = new ErrorCode(44, "不支持的用途分类（应为 embedding/llm/ocr）");

    // ========== 切片方法 1-010-005-000 ==========
    ErrorCode CHUNK_METHOD_NOT_EXISTS = new ErrorCode(25, "切片方法不存在");
    ErrorCode CHUNK_METHOD_CODE_EXISTS = new ErrorCode(26, "方法代码已存在");
    ErrorCode CHUNK_METHOD_DEFAULT_DELETE = new ErrorCode(27, "不能删除默认切片方法，请先设置其他方法为默认");
    ErrorCode CHUNK_METHOD_IN_USE = new ErrorCode(28, "该切片方法正在被配置使用，无法删除");

    // ========== RAG系统配置 1-010-006-000 ==========
    ErrorCode RAG_CONFIG_NOT_EXISTS = new ErrorCode(29, "RAG配置不存在");
    ErrorCode RAG_CONFIG_KEY_EXISTS = new ErrorCode(30, "该模块下配置键名已存在");
    ErrorCode RAG_CONFIG_VALUE_TYPE_ERROR = new ErrorCode(31, "配置值类型不匹配");
    ErrorCode RAG_CONFIG_MODULE_REQUIRED = new ErrorCode(32, "所属模块不能为空");
    ErrorCode RAG_CONFIG_KEY_REQUIRED = new ErrorCode(33, "配置键名不能为空");
    ErrorCode RAG_CONFIG_VALUE_REQUIRED = new ErrorCode(34, "配置值不能为空");
    ErrorCode RAG_CONFIG_VALUE_TYPE_REQUIRED = new ErrorCode(35, "值类型不能为空");

    // ========== 新闻管理 1-010-007-000 ==========
    ErrorCode NEWS_SOURCE_NOT_EXISTS = new ErrorCode(36, "新闻数据源不存在");
    ErrorCode NEWS_RECORD_NOT_EXISTS = new ErrorCode(37, "新闻记录不存在");
    ErrorCode NEWS_SYNC_LOG_NOT_EXISTS = new ErrorCode(38, "新闻同步日志不存在");
    ErrorCode NEWS_SOURCE_DB_CONNECT_FAILED = new ErrorCode(39, "外部数据库连接失败，请检查连接配置");
    ErrorCode NEWS_SOURCE_DB_TABLE_NOT_FOUND = new ErrorCode(40, "外部表不存在，请检查表名和字段映射配置");
    ErrorCode NEWS_SOURCE_FIELD_INVALID = new ErrorCode(41, "字段映射配置无效，请检查必填字段(ID/标题/内容)");
    ErrorCode NEWS_SYNC_FAILED = new ErrorCode(42, "新闻数据同步失败");
    ErrorCode NEWS_PARSE_NOT_IMPLEMENTED = new ErrorCode(43, "新闻解析功能暂未实现，敬请期待");

    // ========== 标签管理 1-010-008-000 ==========
    ErrorCode TAG_NOT_EXISTS = new ErrorCode(44, "标签不存在");
    ErrorCode TAG_NAME_DUPLICATE = new ErrorCode(45, "相同归属下已存在同名标签");
    ErrorCode TAG_PERMISSION_DENIED = new ErrorCode(46, "无权限操作该标签");
}

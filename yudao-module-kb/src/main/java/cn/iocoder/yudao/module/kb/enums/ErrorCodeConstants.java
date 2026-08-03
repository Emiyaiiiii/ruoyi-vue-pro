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

}

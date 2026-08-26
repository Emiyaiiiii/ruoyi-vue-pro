package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 前端 C 端当前用户对某个知识库的权限。
 *
 * <p>字段对齐 Python {@code /knowledge/bases/{id}/my-permissions/}。
 * 写操作（编辑/删除/上传/重建）与后台知识库大屏一致，均映射 {@code LibraryService.canManage}。
 */
@Data
public class FrontendPermissionsVO {

    @JsonProperty("can_view")
    private Boolean canView;

    @JsonProperty("can_get")
    private Boolean canGet;

    @JsonProperty("can_edit")
    private Boolean canEdit;

    @JsonProperty("can_manage")
    private Boolean canManage;

    @JsonProperty("can_upload")
    private Boolean canUpload;

    @JsonProperty("can_download")
    private Boolean canDownload;

    @JsonProperty("can_delete")
    private Boolean canDelete;

    @JsonProperty("can_share")
    private Boolean canShare;

    @JsonProperty("can_approve")
    private Boolean canApprove;
}

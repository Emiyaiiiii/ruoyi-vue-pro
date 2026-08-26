package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 前端 C 端 - 文件夹树节点（folder-tree）
 *
 * <p>前端 BatchMove 组件的 handleData 读取 id/name/children 构建级联选择器。
 *
 * @author 吴皓
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FrontendFolderTreeNodeVO {

    private Long id;

    private String name;

    private List<FrontendFolderTreeNodeVO> children;
}

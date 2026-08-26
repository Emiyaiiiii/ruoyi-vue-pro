package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 前端 C 端 - 批量删除节点请求
 *
 * @author 吴皓
 */
@Data
public class FrontendFileNodeBatchDeleteReqVO {

    @JsonProperty("node_ids")
    private List<Long> nodeIds;
}

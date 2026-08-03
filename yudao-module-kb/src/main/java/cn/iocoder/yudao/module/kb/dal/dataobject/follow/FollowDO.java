package cn.iocoder.yudao.module.kb.dal.dataobject.follow;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 知识库关注 DO
 *
 * @author 吴皓
 */
@TableName("kb_follow")
@KeySequence("kb_follow_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 知识库ID
     */
    private Long kbId;
    /**
     * 关注用户ID
     */
    private Long userId;

}

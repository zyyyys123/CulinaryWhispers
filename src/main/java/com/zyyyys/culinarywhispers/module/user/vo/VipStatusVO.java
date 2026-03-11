package com.zyyyys.culinarywhispers.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "VipStatusVO", description = "用户 VIP 状态")
public class VipStatusVO {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "VIP 等级（0-未开通）", example = "1")
    private Integer vipLevel;

    @Schema(description = "VIP 到期时间", example = "2026-03-14T00:00:00")
    private LocalDateTime vipExpireTime;
}


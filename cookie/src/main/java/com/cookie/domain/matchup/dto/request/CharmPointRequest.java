package com.cookie.domain.matchup.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CharmPointRequest {
    private long ost;
    private long direction;
    private long story;
    private long dialogue;
    private long visual;
    private long acting;
    private long specialEffect;
}

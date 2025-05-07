package com.boot.cms.entity.mapview;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_map_view")
public class MapViewEntity {
    @Id
    @Column(name = "RPTCD")
    private String rptCd;

    @Column(name = "JOBNM")
    private String jobNm;

    @Column(name = "PARAMCNT")
    private int paramCnt;
}
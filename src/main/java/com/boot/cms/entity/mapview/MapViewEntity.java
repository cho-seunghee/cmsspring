package com.boot.cms.entity.mapview;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity
@Getter
@Setter
public class MapViewEntity {

    @Column(name = "ERRCD")
    private String errCd;

    @Column(name = "ERRMSG")
    private String errMsg;

    @Id
    @Column(name = "JOBNM")
    private String jobNm;

    @Column(name = "JOBTYPE")
    private String jobType;

    @Column(name = "PARAMCNT")
    private int paramCnt;

    @Transient
    private String dynamicCall; // Transient field for dynamic call string
}
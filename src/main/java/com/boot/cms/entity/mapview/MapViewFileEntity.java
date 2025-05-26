package com.boot.cms.entity.mapview;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
@JsonIgnoreProperties({"dynamicCall", "hibernateLazyInitializer", "handler"}) // JPA 프록시 및 transient 필드 제외
public class MapViewFileEntity {

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

    @Override
    public String toString() {
        return "MapViewFileEntity{errCd='" + errCd + "', errMsg='" + errMsg + "', jobNm='" + jobNm + "', paramCnt=" + paramCnt + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapViewFileEntity)) return false;
        MapViewFileEntity that = (MapViewFileEntity) o;
        return paramCnt == that.paramCnt &&
                Objects.equals(errCd, that.errCd) &&
                Objects.equals(jobNm, that.jobNm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errCd, jobNm, paramCnt);
    }
}
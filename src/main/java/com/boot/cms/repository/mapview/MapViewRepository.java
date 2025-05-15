package com.boot.cms.repository.mapview;

import com.boot.cms.entity.mapview.MapViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MapViewRepository extends JpaRepository<MapViewEntity, String> {

    @Query(value = "CALL UP_MAPVIEW_SELECT(:pEMPNO, :pIP, :pRPTCD, :pJOBGB, :pPARAMS, :pUSERCONGB, :pUSERAGENT);", nativeQuery = true)
    MapViewEntity findMapViewInfoByRptCd(
            String pEMPNO,
            String pIP,
            String pRPTCD,
            String pJOBGB,
            String pPARAMS,
            String pUSERCONGB,
            String pUSERAGENT
    );
}
package com.roadbook.poi.repository;

import com.roadbook.poi.entity.Poi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PoiRepository extends JpaRepository<Poi, Long> {

    /**
     * Paginated query with optional category and province filters.
     */
    @Query("SELECT p FROM Poi p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:province IS NULL OR p.province = :province) AND " +
           "(:name IS NULL OR p.name LIKE %:name%)")
    Page<Poi> findByFilters(@Param("category") String category,
                            @Param("province") String province,
                            @Param("name") String name,
                            Pageable pageable);

    /**
     * Find POIs within a radius of a given coordinate using spatial distance calculation.
     * MySQL's ST_Distance_Sphere returns distance in meters.
     * Columns are returned in explicit order via aliases for index-based access:
     *   [0]=id, [1]=name, [2]=category, [3]=lng, [4]=lat, [5]=province,
     *   [6]=city, [7]=district, [8]=address, [9]=phone, [10]=cover_image,
     *   [11]=images, [12]=drive_score, [13]=parking_score, [14]=road_score,
     *   [15]=rv_friendly, [16]=camping_allowed, [17]=signal_quality,
     *   [18]=pet_friendly, [19]=source, [20]=amap_poi_id, [21]=confirmed_count,
     *   [22]=created_at, [23]=updated_at, [24]=distance_m
     *
     * @param lng          center longitude
     * @param lat          center latitude
     * @param radius       search radius in meters
     * @param categories   comma-separated category filter (null to skip)
     * @param categoryList list of categories for IN clause
     * @return array of results with documented column ordering
     */
    @Query(value = """
            SELECT p.id, p.name, p.category, p.lng, p.lat,
                   p.province, p.city, p.district, p.address, p.phone,
                   p.cover_image, p.images, p.drive_score, p.parking_score, p.road_score,
                   p.rv_friendly, p.camping_allowed, p.signal_quality,
                   p.pet_friendly, p.source, p.amap_poi_id, p.confirmed_count,
                   p.created_at, p.updated_at,
                   ST_Distance_Sphere(POINT(:lng, :lat), POINT(p.lng, p.lat)) AS distance_m
            FROM pois p
            WHERE ST_Distance_Sphere(POINT(:lng, :lat), POINT(p.lng, p.lat)) <= :radius
              AND (:categoryList IS NULL OR p.category IN (:categoryList))
            ORDER BY distance_m
            LIMIT 20
            """, nativeQuery = true)
    List<Object[]> findNearby(@Param("lng") BigDecimal lng,
                              @Param("lat") BigDecimal lat,
                              @Param("radius") int radius,
                              @Param("categoryList") List<String> categoryList);

    /**
     * Find a POI by its AMAP POI ID.
     */
    Optional<Poi> findByAmapPoiId(String amapPoiId);

    /**
     * Find POIs by category and province (for admin querying).
     */
    List<Poi> findByCategoryAndProvince(String category, String province);

    /**
     * H2-compatible nearby search using coordinate distance approximation.
     * 1 longitude degree ≈ 85,390m, 1 latitude degree ≈ 111,320m.
     */
    @Query("SELECT p FROM Poi p WHERE " +
           "(:categoryList IS NULL OR p.category IN :categoryList) AND " +
           "((p.lng - :lng) * (p.lng - :lng) * 85390 * 85390 + " +
           " (p.lat - :lat) * (p.lat - :lat) * 111320 * 111320) <= :radiusSq")
    List<Poi> findNearbySimple(@Param("lng") BigDecimal lng,
                               @Param("lat") BigDecimal lat,
                               @Param("radiusSq") double radiusSq,
                               @Param("categoryList") List<String> categoryList);
}

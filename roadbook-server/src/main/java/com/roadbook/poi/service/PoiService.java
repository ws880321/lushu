package com.roadbook.poi.service;

import com.roadbook.poi.entity.Poi;
import com.roadbook.poi.repository.PoiRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PoiService {

    private final PoiRepository poiRepo;

    public PoiService(PoiRepository poiRepo) {
        this.poiRepo = poiRepo;
    }

    /**
     * List POIs with optional filters, paginated, ordered by creation time descending.
     *
     * @param category optional category filter (null to skip)
     * @param province optional province filter (null to skip)
     * @param name     optional name search (null to skip)
     * @param page     page number (0-based)
     * @param size     page size
     * @return paginated POI list
     */
    public Page<Poi> findAll(String category, String province, String name, int page, int size) {
        return poiRepo.findByFilters(
                category,
                province,
                name,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Get a single POI by ID.
     *
     * @param id POI ID
     * @return the POI, or null if not found
     */
    public Poi findById(Long id) {
        return poiRepo.findById(id).orElse(null);
    }

    /**
     * Create a new POI.
     *
     * @param poi POI entity to persist
     * @return persisted POI with generated ID
     */
    @Transactional
    public Poi create(Poi poi) {
        return poiRepo.save(poi);
    }

    /**
     * Update an existing POI.
     *
     * @param id   POI ID
     * @param data updated POI data
     * @return updated POI, or null if not found
     */
    @Transactional
    public Poi update(Long id, Poi data) {
        Poi existing = poiRepo.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        existing.setName(data.getName());
        existing.setCategory(data.getCategory());
        existing.setLng(data.getLng());
        existing.setLat(data.getLat());
        existing.setProvince(data.getProvince());
        existing.setCity(data.getCity());
        existing.setDistrict(data.getDistrict());
        existing.setAddress(data.getAddress());
        existing.setPhone(data.getPhone());

        existing.setDriveScore(data.getDriveScore());
        existing.setParkingScore(data.getParkingScore());
        existing.setRoadScore(data.getRoadScore());
        existing.setRvFriendly(data.getRvFriendly());
        existing.setSignalQuality(data.getSignalQuality());
        existing.setPetFriendly(data.getPetFriendly());
        existing.setCampingAllowed(data.getCampingAllowed());

        if (data.getCoverImage() != null) {
            existing.setCoverImage(data.getCoverImage());
        }
        if (data.getImages() != null) {
            existing.setImages(data.getImages());
        }

        return poiRepo.save(existing);
    }

    /**
     * Delete a POI by ID.
     *
     * @param id POI ID
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean delete(Long id) {
        if (!poiRepo.existsById(id)) {
            return false;
        }
        poiRepo.deleteById(id);
        return true;
    }
}

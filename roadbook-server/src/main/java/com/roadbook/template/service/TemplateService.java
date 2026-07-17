package com.roadbook.template.service;

import com.roadbook.template.entity.RouteTemplate;
import com.roadbook.template.entity.TemplateWaypoint;
import com.roadbook.template.repository.TemplateRepository;
import com.roadbook.template.repository.TemplateWaypointRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TemplateService {

    private static final int ACTIVE_STATUS = 1;

    private final TemplateRepository templateRepository;
    private final TemplateWaypointRepository waypointRepository;

    public TemplateService(TemplateRepository templateRepository,
                           TemplateWaypointRepository waypointRepository) {
        this.templateRepository = templateRepository;
        this.waypointRepository = waypointRepository;
    }

    /**
     * Match the best route template based on region, total days, and preferred tags.
     *
     * <p>Matching strategy:
     * <ol>
     *   <li>Exact match: region + days (status=1)</li>
     *   <li>Loose match: region + days within {@code dayRange} (default ±1)</li>
     *   <li>Rank by tag intersection count with preferred tags</li>
     * </ol>
     *
     * @param region    the target region (nullable — skipped when null/blank)
     * @param totalDays number of days for the trip
     * @param preferTags preferred tags (nullable — skipped when null/empty)
     * @return the best matching template, or empty if nothing found
     */
    public Optional<RouteTemplate> match(String region, int totalDays, List<String> preferTags) {
        int dayRange = 1; // ±1 day tolerance
        String normalizedRegion = (region != null) ? region.trim() : null;

        // Step 1: exact match on region + days
        List<RouteTemplate> exactMatches = findExact(normalizedRegion, totalDays);

        if (!exactMatches.isEmpty()) {
            return pickBestByTags(exactMatches, preferTags);
        }

        // Step 2: loose match on region + days ± dayRange
        List<RouteTemplate> looseMatches = findLoose(normalizedRegion, totalDays, dayRange);

        if (!looseMatches.isEmpty()) {
            return pickBestByTags(looseMatches, preferTags);
        }

        // Step 3: try region-only match with any days
        if (normalizedRegion != null && !normalizedRegion.isEmpty()) {
            List<RouteTemplate> regionOnly = templateRepository
                    .findByRegionAndStatusAndTotalDaysBetween(normalizedRegion, ACTIVE_STATUS, 1, 365);
            if (!regionOnly.isEmpty()) {
                return pickBestByTags(regionOnly, preferTags);
            }
        }

        // Step 4: try days-only match with any region
        List<RouteTemplate> daysOnly = findLoose(null, totalDays, dayRange);
        if (!daysOnly.isEmpty()) {
            return pickBestByTags(daysOnly, preferTags);
        }

        return Optional.empty();
    }

    /**
     * Get all waypoints for a given template, ordered by day and sort order.
     */
    public List<TemplateWaypoint> getWaypoints(Long templateId) {
        return waypointRepository.findByTemplateIdOrderByDayNumberAscSortOrderAsc(templateId);
    }

    /**
     * List popular templates sorted by usage count descending.
     */
    public List<RouteTemplate> listPopular(int limit) {
        return templateRepository.findByStatusOrderByUsageCountDesc(ACTIVE_STATUS, PageRequest.of(0, Math.max(1, limit)));
    }

    /**
     * Increment usage count for a template by 1.
     *
     * @param templateId the template ID
     * @return true if the template existed and was incremented, false otherwise
     */
    @Transactional
    public boolean incrementUsage(Long templateId) {
        Optional<RouteTemplate> opt = templateRepository.findById(templateId);
        if (opt.isPresent()) {
            RouteTemplate template = opt.get();
            template.setUsageCount(template.getUsageCount() + 1);
            templateRepository.save(template);
            return true;
        }
        return false;
    }

    // ========== Private helpers ==========

    private List<RouteTemplate> findExact(String region, int days) {
        if (region != null && !region.isEmpty()) {
            return templateRepository.findByRegionAndTotalDaysAndStatus(region, days, ACTIVE_STATUS);
        }
        return Collections.emptyList();
    }

    private List<RouteTemplate> findLoose(String region, int days, int range) {
        int minDays = Math.max(1, days - range);
        int maxDays = days + range;

        if (region != null && !region.isEmpty()) {
            return templateRepository.findByRegionAndStatusAndTotalDaysBetween(region, ACTIVE_STATUS, minDays, maxDays);
        }
        // Fall back to days-only search across all regions
        if (region == null || region.isEmpty()) {
            return templateRepository.findByStatusOrderByUsageCountDesc(ACTIVE_STATUS, PageRequest.of(0, 100))
                    .stream()
                    .filter(t -> t.getTotalDays() >= minDays && t.getTotalDays() <= maxDays)
                    .toList();
        }
        return Collections.emptyList();
    }

    /**
     * Pick the template with the highest tag intersection count.
     * Among ties (or when preferTags is empty/null), picks the one with higher usageCount.
     */
    private Optional<RouteTemplate> pickBestByTags(List<RouteTemplate> candidates, List<String> preferTags) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        if (preferTags == null || preferTags.isEmpty()) {
            // No tag preference: return the most popular one
            return candidates.stream()
                    .max(Comparator.comparingInt(RouteTemplate::getUsageCount));
        }

        // Parse tags JSON and compute intersection count
        Map<RouteTemplate, Integer> scoreMap = new HashMap<>();
        for (RouteTemplate t : candidates) {
            int intersection = countTagIntersection(t.getTags(), preferTags);
            scoreMap.put(t, intersection);
        }

        int maxScore = scoreMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        if (maxScore == 0) {
            // No tag match: fall back to popular
            return candidates.stream()
                    .max(Comparator.comparingInt(RouteTemplate::getUsageCount));
        }

        // Among those with max score, return the most popular
        return scoreMap.entrySet().stream()
                .filter(e -> e.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .max(Comparator.comparingInt(RouteTemplate::getUsageCount));
    }

    /**
     * Count how many of the preferred tags appear in the template's tags JSON string.
     * Handles JSON array format like ["摄影","雪山"] — simple substring containment.
     */
    private int countTagIntersection(String tagsJson, List<String> preferTags) {
        if (tagsJson == null || tagsJson.isEmpty() || preferTags.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (String preferTag : preferTags) {
            if (preferTag != null && !preferTag.isEmpty() && tagsJson.contains(preferTag)) {
                count++;
            }
        }
        return count;
    }
}

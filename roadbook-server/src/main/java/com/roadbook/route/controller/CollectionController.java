package com.roadbook.route.controller;

import com.roadbook.common.ApiResponse;
import com.roadbook.route.entity.Route;
import com.roadbook.route.repository.RouteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class CollectionController {
    private final RouteRepository routeRepo;
    public CollectionController(RouteRepository routeRepo) { this.routeRepo = routeRepo; }

    @GetMapping("/collections")
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Map<String, Object>> cols = new ArrayList<>();
        cols.add(Map.of("id",1,"name","川西大全","cover","🏔️","desc","从成都出发，川西最美的自驾路线"));
        cols.add(Map.of("id",2,"name","此生必驾","cover","🛣️","desc","这一生值得走一次的传奇路线"));
        cols.add(Map.of("id",3,"name","亲子出游","cover","👨‍👩‍👧","desc","适合全家出行的轻松自驾路线"));
        cols.add(Map.of("id",4,"name","西北苍茫","cover","🏜️","desc","青海甘肃大环线，大西北的辽阔"));
        cols.add(Map.of("id",5,"name","云南漫游","cover","🌸","desc","大理丽江香格里拉，云南慢生活"));
        cols.add(Map.of("id",6,"name","摄影天堂","cover","📷","desc","新都桥稻城亚丁，摄影天堂"));
        return ApiResponse.success(cols);
    }

    @GetMapping("/collections/{id}/routes")
    public ApiResponse<List<Route>> routes(@PathVariable int id) {
        return ApiResponse.success(routeRepo.findByIsPublicAndStatusOrderByCreatedAtDesc(1,1,PageRequest.of(0,12)).getContent());
    }
}

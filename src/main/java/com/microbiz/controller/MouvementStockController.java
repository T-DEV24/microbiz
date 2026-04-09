package com.microbiz.controller;

import com.microbiz.service.MouvementStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/mouvements-stock")
public class MouvementStockController {

    @Autowired private MouvementStockService mouvementStockService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "createdAt") String sort,
                        @RequestParam(defaultValue = "desc") String dir,
                        Model model) {
        String sortField = switch (sort) {
            case "type", "quantite", "id" -> sort;
            default -> "createdAt";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(5, size), 100), Sort.by(direction, sortField));
        var mouvementsPage = mouvementStockService.findAll(pageable);

        model.addAttribute("mouvements", mouvementsPage.getContent());
        model.addAttribute("mouvementsPage", mouvementsPage);
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", direction.name().toLowerCase());
        model.addAttribute("size", pageable.getPageSize());
        return "mouvements-stock";
    }
}

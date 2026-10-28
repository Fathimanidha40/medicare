package com.medicare.controller;

import com.medicare.dto.MedicineRequest;
import com.medicare.dto.RefillRequest;
import com.medicare.entity.Medicine;
import com.medicare.entity.RefillHistory;
import com.medicare.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping
    public ResponseEntity<List<Medicine>> getMedicines(
            Authentication auth,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String form) {
        List<Medicine> list = medicineService.getUserMedicines(auth.getName(), activeOnly, search, form);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id, Authentication auth) {
        Medicine medicine = medicineService.getMedicineById(id, auth.getName());
        return ResponseEntity.ok(medicine);
    }

    @PostMapping
    public ResponseEntity<Medicine> createMedicine(Authentication auth, @Valid @RequestBody MedicineRequest req) {
        Medicine created = medicineService.createMedicine(auth.getName(), req);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medicine> updateMedicine(
            @PathVariable Long id,
            Authentication auth,
            @Valid @RequestBody MedicineRequest req) {
        Medicine updated = medicineService.updateMedicine(id, auth.getName(), req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMedicine(@PathVariable Long id, Authentication auth) {
        medicineService.deleteMedicine(id, auth.getName());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Medicine deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/refill")
    public ResponseEntity<Medicine> refillStock(
            @PathVariable Long id,
            Authentication auth,
            @Valid @RequestBody RefillRequest req) {
        Medicine refilled = medicineService.refillStock(id, auth.getName(), req);
        return ResponseEntity.ok(refilled);
    }

    @GetMapping("/{id}/refills")
    public ResponseEntity<List<RefillHistory>> getRefillHistory(@PathVariable Long id, Authentication auth) {
        List<RefillHistory> history = medicineService.getRefillHistory(id, auth.getName());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Medicine>> getLowStock(Authentication auth) {
        List<Medicine> lowStock = medicineService.getLowStockMedicines(auth.getName());
        return ResponseEntity.ok(lowStock);
    }
}

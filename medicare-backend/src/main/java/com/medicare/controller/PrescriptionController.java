package com.medicare.controller;

import com.medicare.entity.Prescription;
import com.medicare.service.PrescriptionService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping
    public ResponseEntity<List<Prescription>> getUserPrescriptions(Authentication auth) {
        List<Prescription> list = prescriptionService.getUserPrescriptions(auth.getName());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prescription> getPrescriptionById(@PathVariable Long id, Authentication auth) {
        Prescription prescription = prescriptionService.getPrescriptionById(id, auth.getName());
        return ResponseEntity.ok(prescription);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Prescription> uploadPrescription(
            Authentication auth,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "doctorName", required = false) String doctorName,
            @RequestParam(value = "prescriptionDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prescriptionDate,
            @RequestParam(value = "notes", required = false) String notes) {

        Prescription uploaded = prescriptionService.uploadPrescription(
                auth.getName(), title, doctorName, prescriptionDate, notes, file
        );
        return new ResponseEntity<>(uploaded, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadPrescriptionFile(@PathVariable Long id, Authentication auth) {
        Prescription prescription = prescriptionService.getPrescriptionById(id, auth.getName());
        Resource resource = prescriptionService.loadFileAsResource(id, auth.getName());

        String filename = prescription.getOriginalFileName() != null ? prescription.getOriginalFileName().toLowerCase() : "";
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        if (filename.endsWith(".pdf") || (prescription.getFileType() != null && prescription.getFileType().contains("pdf"))) {
            mediaType = MediaType.APPLICATION_PDF;
        } else if (filename.endsWith(".png") || (prescription.getFileType() != null && prescription.getFileType().contains("png"))) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || (prescription.getFileType() != null && prescription.getFileType().contains("jpeg"))) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (prescription.getFileType() != null) {
            try {
                mediaType = MediaType.parseMediaType(prescription.getFileType());
            } catch (Exception ignored) { }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + prescription.getOriginalFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePrescription(@PathVariable Long id, Authentication auth) {
        prescriptionService.deletePrescription(id, auth.getName());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Prescription deleted successfully");
        return ResponseEntity.ok(response);
    }
}

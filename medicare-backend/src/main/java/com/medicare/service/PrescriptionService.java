package com.medicare.service;

import com.medicare.entity.Prescription;
import com.medicare.entity.User;
import com.medicare.exception.BadRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.PrescriptionRepository;
import com.medicare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final Path uploadLocation;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               UserRepository userRepository,
                               @Value("${medicare.upload.dir:./uploads/prescriptions}") String uploadDir) {
        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload storage directory", e);
        }
    }

    public List<Prescription> getUserPrescriptions(String userEmail) {
        User user = getUserByEmail(userEmail);
        return prescriptionRepository.findByUserIdOrderByPrescriptionDateDesc(user.getId());
    }

    public Prescription getPrescriptionById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));

        if (!prescription.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to prescription");
        }
        return prescription;
    }

    @Transactional
    public Prescription uploadPrescription(String userEmail,
                                           String title,
                                           String doctorName,
                                           LocalDate prescriptionDate,
                                           String notes,
                                           MultipartFile file) {
        User user = getUserByEmail(userEmail);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a file to upload");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "prescription.png"
        );

        // Security: Path traversal protection
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Invalid filename format");
        }

        // Extension validation
        String ext = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            ext = originalFilename.substring(dotIndex + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BadRequestException("Invalid file format. Allowed formats: PDF, JPG, JPEG, PNG");
        }

        // Content-Type validation
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported file MIME type: " + contentType);
        }

        // Size validation (15 MB max)
        if (file.getSize() > 15 * 1024 * 1024) {
            throw new BadRequestException("File size exceeds 15 MB limit");
        }

        // Generate safe unique stored filename
        String storedFileName = UUID.randomUUID() + "." + ext;

        try {
            Path targetLocation = this.uploadLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file securely: " + e.getMessage(), e);
        }

        Prescription prescription = new Prescription(
                user,
                title != null && !title.isBlank() ? title : "Medical Prescription",
                doctorName,
                prescriptionDate != null ? prescriptionDate : LocalDate.now(),
                originalFilename,
                storedFileName,
                contentType != null ? contentType : "application/octet-stream",
                file.getSize(),
                notes
        );

        return prescriptionRepository.save(prescription);
    }

    public Resource loadFileAsResource(Long id, String userEmail) {
        Prescription prescription = getPrescriptionById(id, userEmail);

        try {
            Path filePath = this.uploadLocation.resolve(prescription.getStoredFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Prescription file not found on disk");
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Prescription file location error");
        }
    }

    @Transactional
    public void deletePrescription(Long id, String userEmail) {
        Prescription prescription = getPrescriptionById(id, userEmail);

        try {
            Path filePath = this.uploadLocation.resolve(prescription.getStoredFileName()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // Log warning but continue entity deletion
        }

        prescriptionRepository.delete(prescription);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}

package com.medicare.service;

import com.medicare.dto.MedicineRequest;
import com.medicare.dto.RefillRequest;
import com.medicare.entity.Medicine;
import com.medicare.entity.RefillHistory;
import com.medicare.entity.User;
import com.medicare.exception.BadRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.MedicineRepository;
import com.medicare.repository.RefillHistoryRepository;
import com.medicare.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final RefillHistoryRepository refillHistoryRepository;
    private final UserRepository userRepository;

    public MedicineService(MedicineRepository medicineRepository,
                           RefillHistoryRepository refillHistoryRepository,
                           UserRepository userRepository) {
        this.medicineRepository = medicineRepository;
        this.refillHistoryRepository = refillHistoryRepository;
        this.userRepository = userRepository;
    }

    public List<Medicine> getUserMedicines(String userEmail, Boolean activeOnly, String search, String form) {
        User user = getUserByEmail(userEmail);
        List<Medicine> list = activeOnly != null && activeOnly
                ? medicineRepository.findByUserIdAndActiveTrueOrderByNameAsc(user.getId())
                : medicineRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase().trim();
            list = list.stream()
                    .filter(m -> m.getName().toLowerCase().contains(lower)
                            || (m.getInstructions() != null && m.getInstructions().toLowerCase().contains(lower))
                            || (m.getPrescribingDoctor() != null && m.getPrescribingDoctor().toLowerCase().contains(lower)))
                    .collect(Collectors.toList());
        }

        if (form != null && !form.isBlank() && !form.equalsIgnoreCase("ALL")) {
            list = list.stream()
                    .filter(m -> m.getForm().equalsIgnoreCase(form))
                    .collect(Collectors.toList());
        }

        return list;
    }

    public Medicine getMedicineById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + id));

        if (!medicine.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to requested medicine");
        }
        return medicine;
    }

    @Transactional
    public Medicine createMedicine(String userEmail, MedicineRequest req) {
        User user = getUserByEmail(userEmail);

        if (req.getEndDate() != null && req.getEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("End date cannot precede the start date");
        }

        Medicine medicine = new Medicine(
                user,
                req.getName(),
                req.getDosage(),
                req.getUnit(),
                req.getForm(),
                req.getInstructions(),
                req.getFoodTiming(),
                req.getStartDate(),
                req.getEndDate(),
                req.getFrequency(),
                req.getPrescribingDoctor(),
                req.getTotalStock(),
                req.getQuantityPerDose(),
                req.getLowStockThreshold(),
                req.getColorTag(),
                req.getNotes()
        );

        return medicineRepository.save(medicine);
    }

    @Transactional
    public Medicine updateMedicine(Long id, String userEmail, MedicineRequest req) {
        Medicine medicine = getMedicineById(id, userEmail);

        if (req.getEndDate() != null && req.getEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("End date cannot precede the start date");
        }

        medicine.setName(req.getName());
        medicine.setDosage(req.getDosage());
        medicine.setUnit(req.getUnit());
        medicine.setForm(req.getForm());
        medicine.setInstructions(req.getInstructions());
        medicine.setFoodTiming(req.getFoodTiming());
        medicine.setStartDate(req.getStartDate());
        medicine.setEndDate(req.getEndDate());
        medicine.setFrequency(req.getFrequency());
        medicine.setPrescribingDoctor(req.getPrescribingDoctor());
        medicine.setTotalStock(req.getTotalStock());
        medicine.setQuantityPerDose(req.getQuantityPerDose());
        medicine.setLowStockThreshold(req.getLowStockThreshold());
        medicine.setColorTag(req.getColorTag());
        medicine.setNotes(req.getNotes());
        if (req.getActive() != null) {
            medicine.setActive(req.getActive());
        }

        return medicineRepository.save(medicine);
    }

    @Transactional
    public void deleteMedicine(Long id, String userEmail) {
        Medicine medicine = getMedicineById(id, userEmail);
        refillHistoryRepository.deleteByMedicineId(medicine.getId());
        medicineRepository.delete(medicine);
    }

    @Transactional
    public Medicine refillStock(Long id, String userEmail, RefillRequest req) {
        Medicine medicine = getMedicineById(id, userEmail);
        int oldStock = medicine.getTotalStock() != null ? medicine.getTotalStock() : 0;
        int newStock = oldStock + req.getQuantity();
        medicine.setTotalStock(newStock);

        RefillHistory history = new RefillHistory(
                medicine,
                req.getQuantity(),
                oldStock,
                newStock,
                req.getNotes()
        );
        refillHistoryRepository.save(history);

        return medicineRepository.save(medicine);
    }

    public List<RefillHistory> getRefillHistory(Long medicineId, String userEmail) {
        getMedicineById(medicineId, userEmail); // verifies ownership
        return refillHistoryRepository.findByMedicineIdOrderByRefillDateDesc(medicineId);
    }

    public List<Medicine> getLowStockMedicines(String userEmail) {
        User user = getUserByEmail(userEmail);
        return medicineRepository.findLowStockMedicines(user.getId());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}

package com.hospital.queue.service;

import com.hospital.queue.domain.entity.Patient;
import com.hospital.queue.dto.request.PatientRequest;
import com.hospital.queue.exception.DuplicateResourceException;
import com.hospital.queue.exception.ResourceNotFoundException;
import com.hospital.queue.repository.PatientRepository;
import com.hospital.queue.util.QueueNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository     patientRepository;
    private final QueueNumberGenerator  generator;

    @Transactional
    public Patient register(PatientRequest req) {
        if (patientRepository.existsByPhone(req.getPhone())) {
            throw new DuplicateResourceException(
                    "A patient with phone " + req.getPhone() + " already exists.");
        }
        Patient patient = Patient.builder()
                .patientNumber(generator.nextPatientNumber())
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .dateOfBirth(req.getDateOfBirth())
                .gender(req.getGender())
                .bloodType(req.getBloodType())
                .address(req.getAddress())
                .allergies(req.getAllergies())
                .emergencyContactName(req.getEmergencyContactName())
                .emergencyContactPhone(req.getEmergencyContactPhone())
                .build();
        return patientRepository.save(patient);
    }

    @Transactional
    public Patient update(Long id, PatientRequest req) {
        Patient patient = findById(id);
        patient.setFullName(req.getFullName());
        patient.setEmail(req.getEmail());
        patient.setPhone(req.getPhone());
        patient.setDateOfBirth(req.getDateOfBirth());
        patient.setGender(req.getGender());
        patient.setBloodType(req.getBloodType());
        patient.setAddress(req.getAddress());
        patient.setAllergies(req.getAllergies());
        patient.setEmergencyContactName(req.getEmergencyContactName());
        patient.setEmergencyContactPhone(req.getEmergencyContactPhone());
        return patientRepository.save(patient);
    }

    @Transactional(readOnly = true)
    public Patient findById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }

    @Transactional(readOnly = true)
    public Patient findByPatientNumber(String number) {
        return patientRepository.findByPatientNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "number", number));
    }

    @Transactional(readOnly = true)
    public Page<Patient> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return patientRepository.findAll(pageable);
        }
        return patientRepository.search(query.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    public long count() {
        return patientRepository.count();
    }
}

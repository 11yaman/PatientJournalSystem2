package com.example.patient.repository;

import com.example.patient.model.Note;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends CrudRepository<Note, Long> {
    List<Note> findAllByPatientId(Long patientId);
    List<Note> findByPatientIdOrderByDateTimeCreatedDesc(Long patientId);
}

package com.example.patient.controller;

import com.example.patient.dto.request.NoteRequest;
import com.example.patient.dto.response.EncounterDto;
import com.example.patient.dto.response.NoteDto;
import com.example.patient.dto.response.UserDto;
import com.example.patient.mapping.StrategyMapper;
import com.example.patient.model.*;
import com.example.patient.service.EncounterService;
import com.example.patient.service.NoteService;
import com.example.patient.service.PatientService;
import com.example.patient.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@CrossOrigin
@RestController
public class NoteController {
    private final UserService userService;
    private final StrategyMapper<User, UserDto> userMapper;
    private final NoteService noteService;
    private final StrategyMapper<Note, NoteDto> noteMapper;
    private final StrategyMapper<Encounter, EncounterDto> encounterMapper;
    private final PatientService patientService;
    private final EncounterService encounterService;


    @Autowired
    public NoteController(NoteService noteService, UserService userService,
                          StrategyMapper<Note, NoteDto> noteMapper, StrategyMapper<User, UserDto> userMapper,
                          StrategyMapper<Encounter, EncounterDto> encounterMapper,
                          PatientService patientService, EncounterService encounterService) {
        this.noteService = noteService;
        this.userService = userService;
        this.noteMapper = noteMapper;
        this.userMapper = userMapper;
        this.encounterMapper = encounterMapper;
        this.patientService = patientService;
        this.encounterService = encounterService;
    }

    @PostMapping("/api/v1/patients/notes")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<NoteDto> createNoteByEmployee(Authentication authentication,
                                                        @RequestBody NoteRequest noteRequest,
                                                        @RequestParam Long patientId) {
        try {
            String username = userService.getAuthenticatedUsername(authentication);
            Employee employee = (Employee) userService.getUserByUsername(username);
            Patient patient = patientService.getPatientById(patientId);
            Encounter encounter = encounterService.getEncounterById(noteRequest.encounterId());

            Note createdNote = noteService.createNote(
                    new Note(noteRequest.text(), employee, patient, encounter));
            return new ResponseEntity<>(noteMapper.map(createdNote), HttpStatus.CREATED);
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Note could not be created");
        }
    }

    @GetMapping("/api/v1/patients/notes/{noteId}")
    @PreAuthorize("hasRole('EMPLOYEE') or @customSecurityExpressionMethods.isNoteOwner(authentication, #noteId)")
    public ResponseEntity<NoteDto> getNote(Authentication authentication,
                                           @PathVariable Long noteId) {
        try {
            Note note = noteService.getNoteById(noteId);

            return ResponseEntity.ok(
                    new NoteDto(note.getId(), note.getText(), note.getDateTimeCreated(),
                            userMapper.map(note.getEmployee()), userMapper.map(note.getPatient()),
                            encounterMapper.map(note.getEncounter())
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/api/v1/patients/notes/list")
    @PreAuthorize("hasRole('EMPLOYEE') or @customSecurityExpressionMethods.isPatient(authentication, #patientId)")
    public ResponseEntity<List<NoteDto>> getPatientNotes(Authentication authentication,
                                                         @RequestParam(required = false) Long patientId){
        try {
            List<Note> userNotes;
            if (userService.isEmployee(authentication))
                userNotes = noteService.getAllNotesForPatient(patientId);
            else
                userNotes = noteService.getAllNotesForPatient(
                        userService.getAuthenticatedUserId(authentication));
            return ResponseEntity.ok(noteMapper.mapAll(userNotes));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
    }
}
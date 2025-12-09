package tn.fst.eventsproject.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.fst.eventsproject.entities.Event;
import tn.fst.eventsproject.entities.Logistics;
import tn.fst.eventsproject.entities.Participant;
import tn.fst.eventsproject.repositories.EventRepository;
import tn.fst.eventsproject.repositories.LogisticsRepository;
import tn.fst.eventsproject.repositories.ParticipantRepository;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServicesTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private LogisticsRepository logisticsRepository;

    @InjectMocks
    private EventServicesImpl eventServices;

    @Test
    public void testAddAffectEvenParticipant() {
        // Given
        Event event = new Event();
        event.setDescription("Test Event");
        event.setDateDebut(LocalDate.now());
        event.setDateFin(LocalDate.now().plusDays(1));
        event.setCout(1000.0f);

        Participant participant = new Participant();
        participant.setIdPart(1);
        
        Set<Event> events = new HashSet<>();
        participant.setEvents(events);

        when(participantRepository.findById(1)).thenReturn(Optional.of(participant));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // When
        Event savedEvent = eventServices.addAffectEvenParticipant(event, 1);

        // Then
        assertNotNull(savedEvent);
        assertEquals("Test Event", savedEvent.getDescription());
        verify(eventRepository, times(1)).save(event);
        verify(participantRepository, times(1)).findById(1);
    }

    @Test
    public void testAddAffectEvenParticipantWithEventOnly() {
        // Given
        Event event = new Event();
        event.setDescription("Test Event");
        
        Participant participant = new Participant();
        participant.setIdPart(1);
        
        Set<Participant> participants = new HashSet<>();
        participants.add(participant);
        event.setParticipants(participants);
        
        when(participantRepository.findById(1)).thenReturn(Optional.of(participant));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // When
        Event savedEvent = eventServices.addAffectEvenParticipant(event);

        // Then
        assertNotNull(savedEvent);
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    public void testAddParticipant() {
        // Given
        Participant participant = new Participant();
        participant.setNom("Test");
        participant.setPrenom("User");
        
        when(participantRepository.save(any(Participant.class))).thenReturn(participant);

        // When
        Participant savedParticipant = eventServices.addParticipant(participant);

        // Then
        assertNotNull(savedParticipant);
        assertEquals("Test", savedParticipant.getNom());
        verify(participantRepository, times(1)).save(participant);
    }

        @Test
    public void testAddAffectLog() {
        // Given
        Logistics logistics = new Logistics();
        logistics.setDescription("Test Logistics");
        
        Event event = new Event();
        event.setDescription("Test Event");
        
        // Simuler que findByDescription retourne un événement existant
        when(eventRepository.findByDescription("Test Event")).thenReturn(event);
        
        // La méthode addAffectLog ne retourne rien (void) dans votre code
        // OU elle retourne le logistics - vérifiez votre implémentation
        
        // When
        // Si votre méthode retourne void, faites juste l'appel
        eventServices.addAffectLog(logistics, "Test Event");
        
        // Then
        // Vérifiez que les méthodes sont appelées
        verify(eventRepository, times(1)).findByDescription("Test Event");
        verify(eventRepository, times(1)).save(event);
        
        // Si votre méthode retourne Logistics, décommentez :
        // Logistics result = eventServices.addAffectLog(logistics, "Test Event");
        // assertNotNull(result);
    }
    @Test
    public void testGetLogisticsDates() {
        // Given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);
        
        Event event = new Event();
        Logistics logistics = new Logistics();
        logistics.setReserve(true);
        
        Set<Logistics> logisticsSet = new HashSet<>();
        logisticsSet.add(logistics);
        event.setLogistics(logisticsSet);
        
        List<Event> events = Arrays.asList(event);
        
        when(eventRepository.findByDateDebutBetween(startDate, endDate)).thenReturn(events);

        // When
        List<Logistics> result = eventServices.getLogisticsDates(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findByDateDebutBetween(startDate, endDate);
    }

    @Test
    public void testCalculCout() {
        // Given
        Event event = new Event();
        event.setDescription("Test Event");
        
        Logistics logistics = new Logistics();
        logistics.setReserve(true);
        logistics.setPrixUnit(100.0f);
        logistics.setQuantite(5);
        
        Set<Logistics> logisticsSet = new HashSet<>();
        logisticsSet.add(logistics);
        event.setLogistics(logisticsSet);
        
        List<Event> events = Arrays.asList(event);
        
        // Note: Cette méthode utilise une requête spécifique
        // On mocke le repository pour retourner une liste d'événements
        when(eventRepository.findByParticipants_NomAndParticipants_PrenomAndParticipants_Tache(
            eq("Tounsi"), eq("Ahmed"), any())).thenReturn(events);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // When
        eventServices.calculCout();

        // Then
        // Vérifiez que la méthode s'exécute sans erreur
        // Le coût devrait être calculé: 100.0 * 5 = 500.0
        verify(eventRepository, atLeastOnce()).save(any(Event.class));
    }
}

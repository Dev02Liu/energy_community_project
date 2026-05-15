package com.energy_community_project.usage_service.service;

import com.energy_community_project.usage_service.entity.HourlyUsageEntity;
import com.energy_community_project.usage_service.messaging.EnergyMessage;
import com.energy_community_project.usage_service.messaging.HourlyUsageUpdatedMessage;
import com.energy_community_project.usage_service.repository.HourlyUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HourlyUsageUpdateServiceTest {

    @Mock
    private HourlyUsageRepository hourlyUsageRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private HourlyUsageUpdateService service;

    @BeforeEach
    void setUp() {
        service = new HourlyUsageUpdateService(hourlyUsageRepository, rabbitTemplate, "percentage_update_queue");
    }

    private EnergyMessage message(String type, double kwh, LocalDateTime datetime) {
        EnergyMessage msg = new EnergyMessage();
        msg.setType(type);
        msg.setAssociation("COMMUNITY");
        msg.setKwh(kwh);
        msg.setDatetime(datetime);
        return msg;
    }

    // --- Hour bucketing ---

    @Test
    void minutesAreTruncatedToHourBucket() {
        LocalDateTime at1434 = LocalDateTime.of(2025, 1, 10, 14, 34, 0);
        LocalDateTime expectedHour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        when(hourlyUsageRepository.findById(expectedHour)).thenReturn(Optional.empty());
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("PRODUCER", 5.0, at1434));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        assertThat(captor.getValue().getHour()).isEqualTo(expectedHour);
    }

    @Test
    void secondsAndNanosAreTruncatedToHourBucket() {
        LocalDateTime at143459 = LocalDateTime.of(2025, 1, 10, 14, 34, 59, 999_999_999);
        LocalDateTime expectedHour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        when(hourlyUsageRepository.findById(expectedHour)).thenReturn(Optional.empty());
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("USER", 1.0, at143459));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        assertThat(captor.getValue().getHour()).isEqualTo(expectedHour);
    }

    // --- Producer message aggregation ---

    @Test
    void producerMessageCreatesNewRowWithCommunityProduced() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.empty());
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("PRODUCER", 18.05, hour));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        HourlyUsageEntity saved = captor.getValue();
        assertThat(saved.getCommunityProduced()).isCloseTo(18.05, within(0.0001));
        assertThat(saved.getCommunityUsed()).isEqualTo(0.0);
        assertThat(saved.getGridUsed()).isEqualTo(0.0);
    }

    @Test
    void producerMessageAccumulatesIntoExistingRow() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        HourlyUsageEntity existing = new HourlyUsageEntity(hour, 10.0, 5.0, 0.5);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.of(existing));
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("PRODUCER", 8.05, hour));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        HourlyUsageEntity saved = captor.getValue();
        assertThat(saved.getCommunityProduced()).isCloseTo(18.05, within(0.0001));
        assertThat(saved.getCommunityUsed()).isEqualTo(5.0);
        assertThat(saved.getGridUsed()).isEqualTo(0.5);
    }

    // --- User message aggregation ---

    @Test
    void userMessageUsesOnlyCommunityWhenEnoughAvailable() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        HourlyUsageEntity existing = new HourlyUsageEntity(hour, 20.0, 5.0, 0.0);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.of(existing));
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("USER", 3.0, hour));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        HourlyUsageEntity saved = captor.getValue();
        assertThat(saved.getCommunityUsed()).isCloseTo(8.0, within(0.0001));
        assertThat(saved.getGridUsed()).isEqualTo(0.0);
    }

    // --- Spec example (Acceptance Criteria) ---

    @Test
    void specExample_production18_05_previousUsed18_02_grid1_056_newUser0_05() {
        LocalDateTime at1434 = LocalDateTime.of(2025, 1, 10, 14, 34, 0);
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        // Row before the new user message: produced=18.05, used=18.02, grid=1.056
        HourlyUsageEntity existing = new HourlyUsageEntity(hour, 18.05, 18.02, 1.056);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.of(existing));
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("USER", 0.05, at1434));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        HourlyUsageEntity saved = captor.getValue();
        // available = 18.05 - 18.02 = 0.03 → community portion = min(0.05, 0.03) = 0.03
        // grid portion = 0.05 - 0.03 = 0.02
        assertThat(saved.getCommunityProduced()).isCloseTo(18.05, within(0.0001));
        assertThat(saved.getCommunityUsed()).isCloseTo(18.05, within(0.0001));
        assertThat(saved.getGridUsed()).isCloseTo(1.076, within(0.0001));
    }

    // --- Grid fallback ---

    @Test
    void userMessageFallsBackEntirelyToGridWhenCommunityFullyDepleted() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        HourlyUsageEntity existing = new HourlyUsageEntity(hour, 5.0, 5.0, 0.0);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.of(existing));
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("USER", 3.0, hour));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        HourlyUsageEntity saved = captor.getValue();
        assertThat(saved.getCommunityUsed()).isCloseTo(5.0, within(0.0001));
        assertThat(saved.getGridUsed()).isCloseTo(3.0, within(0.0001));
    }

    @Test
    void userMessageSplitsBetweenCommunityAndGridWhenPartiallyAvailable() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        // 2 kWh available in community
        HourlyUsageEntity existing = new HourlyUsageEntity(hour, 10.0, 8.0, 0.0);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.of(existing));
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("USER", 5.0, hour));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        HourlyUsageEntity saved = captor.getValue();
        assertThat(saved.getCommunityUsed()).isCloseTo(10.0, within(0.0001)); // 8 + 2
        assertThat(saved.getGridUsed()).isCloseTo(3.0, within(0.0001));       // 5 - 2
    }

    // --- community_used never exceeds community_produced ---

    @Test
    void communityUsedNeverExceedsCommunityProduced() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        HourlyUsageEntity existing = new HourlyUsageEntity(hour, 5.0, 4.9, 0.0);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.of(existing));
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("USER", 10.0, hour));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        HourlyUsageEntity saved = captor.getValue();
        assertThat(saved.getCommunityUsed())
                .isLessThanOrEqualTo(saved.getCommunityProduced() + 0.0001);
        assertThat(saved.getCommunityUsed()).isCloseTo(5.0, within(0.0001));
        assertThat(saved.getGridUsed()).isCloseTo(9.9, within(0.0001));
    }

    @Test
    void communityUsedNeverExceedsCommunityProducedWhenStartingFromZero() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        HourlyUsageEntity existing = new HourlyUsageEntity(hour, 0.0, 0.0, 0.0);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.of(existing));
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("USER", 5.0, hour));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        HourlyUsageEntity saved = captor.getValue();
        assertThat(saved.getCommunityUsed()).isEqualTo(0.0);
        assertThat(saved.getGridUsed()).isCloseTo(5.0, within(0.0001));
    }

    // --- Update message is published ---

    @Test
    void updateMessageIsPublishedToQueueAfterProducerMessage() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.empty());
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("PRODUCER", 5.0, hour));

        ArgumentCaptor<HourlyUsageUpdatedMessage> msgCaptor = ArgumentCaptor.forClass(HourlyUsageUpdatedMessage.class);
        verify(rabbitTemplate).convertAndSend(eq("percentage_update_queue"), msgCaptor.capture());
        assertThat(msgCaptor.getValue().getHour()).isEqualTo(hour);
    }

    @Test
    void updateMessageIsPublishedToQueueAfterUserMessage() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        HourlyUsageEntity existing = new HourlyUsageEntity(hour, 10.0, 0.0, 0.0);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.of(existing));
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("USER", 2.0, hour));

        ArgumentCaptor<HourlyUsageUpdatedMessage> msgCaptor = ArgumentCaptor.forClass(HourlyUsageUpdatedMessage.class);
        verify(rabbitTemplate).convertAndSend(eq("percentage_update_queue"), msgCaptor.capture());
        assertThat(msgCaptor.getValue().getHour()).isEqualTo(hour);
    }

    // --- Invalid / null message handling ---

    @Test
    void nullMessageIsIgnored() {
        service.handleEnergyMessage(null);
        verifyNoInteractions(hourlyUsageRepository, rabbitTemplate);
    }

    @Test
    void messageWithNullDatetimeIsIgnored() {
        EnergyMessage msg = new EnergyMessage();
        msg.setType("USER");
        msg.setKwh(1.0);
        msg.setDatetime(null);

        service.handleEnergyMessage(msg);

        verifyNoInteractions(hourlyUsageRepository, rabbitTemplate);
    }

    @Test
    void messageWithNullTypeIsIgnored() {
        EnergyMessage msg = new EnergyMessage();
        msg.setType(null);
        msg.setKwh(1.0);
        msg.setDatetime(LocalDateTime.of(2025, 1, 10, 14, 0, 0));

        service.handleEnergyMessage(msg);

        verifyNoInteractions(hourlyUsageRepository, rabbitTemplate);
    }

    @Test
    void messageWithUnknownTypeDoesNotChangeUsageValues() {
        LocalDateTime hour = LocalDateTime.of(2025, 1, 10, 14, 0, 0);
        HourlyUsageEntity existing = new HourlyUsageEntity(hour, 10.0, 5.0, 1.0);
        when(hourlyUsageRepository.findById(hour)).thenReturn(Optional.of(existing));
        when(hourlyUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handleEnergyMessage(message("UNKNOWN", 99.0, hour));

        ArgumentCaptor<HourlyUsageEntity> captor = ArgumentCaptor.forClass(HourlyUsageEntity.class);
        verify(hourlyUsageRepository).save(captor.capture());
        HourlyUsageEntity saved = captor.getValue();
        assertThat(saved.getCommunityProduced()).isEqualTo(10.0);
        assertThat(saved.getCommunityUsed()).isEqualTo(5.0);
        assertThat(saved.getGridUsed()).isEqualTo(1.0);
    }
}

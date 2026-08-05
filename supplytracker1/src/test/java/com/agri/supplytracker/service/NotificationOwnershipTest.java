package com.agri.supplytracker.service;

import com.agri.supplytracker.model.Notification;
import com.agri.supplytracker.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationOwnershipTest {
    @Test void userCannotMutateAnotherUsersNotification() {
        NotificationRepository repository=mock(NotificationRepository.class); NotificationService service=new NotificationService();
        ReflectionTestUtils.setField(service,"notificationRepository",repository);
        when(repository.findByIdAndRecipientId("n-1","bob")).thenReturn(Optional.empty());
        assertNull(service.markAsRead("n-1","bob")); assertFalse(service.deleteNotification("n-1","bob"));
        verify(repository,never()).delete(any());
    }
}

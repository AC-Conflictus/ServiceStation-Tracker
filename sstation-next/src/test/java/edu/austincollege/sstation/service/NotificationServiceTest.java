package edu.austincollege.sstation.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

/** Unit coverage for {@link NotificationService} (TC-108a). */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
  @Mock private JavaMailSender mailSender;
  @Mock private SpringTemplateEngine templateEngine;

  private NotificationService service;

  @BeforeEach
  void setUp() {
    service = serviceWithMailHost("smtp.austincollege.edu");
  }

  private NotificationService serviceWithMailHost(String host) {
    return new NotificationService(
        mailSenderProvider, templateEngine, "no-reply@austincollege.edu", host);
  }

  @Test
  void noopForNonTerminalStatus() {
    service.notifyStatusChange(hour(), Status.PENDING);
    verify(mailSenderProvider, never()).getIfAvailable();
  }

  @Test
  void noopWhenNoMailSenderConfigured() {
    when(mailSenderProvider.getIfAvailable()).thenReturn(null);
    // Must not throw even though there's no SMTP server (the dev default).
    service.notifyStatusChange(hour(), Status.APPROVED);
    verify(mailSender, never()).send(any(MimeMessage.class));
  }

  @Test
  void sendsMailOnApproval() {
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
    when(templateEngine.process(org.mockito.ArgumentMatchers.eq("email/status-change"), any()))
        .thenReturn("<p>body</p>");

    service.notifyStatusChange(hour(), Status.APPROVED);

    verify(mailSender, times(1)).send(any(MimeMessage.class));
  }

  private static ServiceHour hour() {
    Student s = new Student();
    s.setFirstname("Sam");
    s.setLastname("Student");
    s.setAcEmail("sam@austincollege.edu");
    ServiceHour h = new ServiceHour();
    h.setStudent(s);
    h.setDescription("Tutoring");
    h.setDuration(3.0);
    h.setStartTime(LocalDateTime.now());
    h.setStatus(Status.PENDING);
    return h;
  }

  @Test
  void noopWhenTheConfiguredMailHostIsEmpty() {
    // TC-115. Under prod, spring.mail.host binds to ${SSTATION_MAIL_HOST:} and is *present but
    // empty* when AC IT has not supplied a relay — which @ConditionalOnProperty counts as set, so
    // a JavaMailSender bean does exist. Checking only for the bean meant every notification opened
    // a doomed SMTP connection instead of logging.
    NotificationService noRelay = serviceWithMailHost("");

    noRelay.notifyStatusChange(hour(), Status.APPROVED);

    verify(mailSenderProvider, never()).getIfAvailable();
    verify(mailSender, never()).send(any(MimeMessage.class));
  }

  @Test
  void passwordResetIsAlsoANoopWhenTheMailHostIsEmpty() {
    NotificationService noRelay = serviceWithMailHost("   ");

    noRelay.sendPasswordReset("student@austincollege.edu", "https://app/reset-password?token=x");

    verify(mailSenderProvider, never()).getIfAvailable();
    verify(mailSender, never()).send(any(MimeMessage.class));
  }
}

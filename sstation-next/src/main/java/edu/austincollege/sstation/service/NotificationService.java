package edu.austincollege.sstation.service;

import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Sends a notification email to a student when their service hours are APPROVED or REJECTED
 * (TC-021, built in the new stack as TC-108a — replaces the unused Grails {@code mail} plugin
 * wiring).
 *
 * <p>Email is best-effort:
 *
 * <ul>
 *   <li>When no SMTP sender is configured (the dev default — no {@code SSTATION_MAIL_*} env vars,
 *       so no {@link JavaMailSender} bean) the call is a no-op with a {@code log.info} line.
 *   <li>A send failure is logged but never propagates — a flaky mail server must not fail the
 *       status change that triggered it.
 * </ul>
 */
@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final ObjectProvider<JavaMailSender> mailSender;
  private final SpringTemplateEngine templateEngine;
  private final String fromAddress;

  public NotificationService(
      ObjectProvider<JavaMailSender> mailSender,
      SpringTemplateEngine templateEngine,
      @Value("${sstation.mail.from:no-reply@austincollege.edu}") String fromAddress) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.fromAddress = fromAddress;
  }

  /**
   * Notifies the hour's student of a transition to APPROVED or REJECTED; any other target status is
   * ignored. The hour's {@code student} must already be loaded (open-in-view is off) — callers use
   * {@code ServiceHourRepository.findByIdWithStudent}.
   */
  public void notifyStatusChange(ServiceHour hour, Status to) {
    if (to != Status.APPROVED && to != Status.REJECTED) {
      return;
    }
    String recipient = hour.getStudent().getAcEmail();
    JavaMailSender sender = mailSender.getIfAvailable();
    if (sender == null) {
      log.info(
          "[mail disabled] would notify {} that service hour #{} is now {}",
          recipient,
          hour.getId(),
          to);
      return;
    }
    try {
      Context ctx = new Context();
      ctx.setVariable("studentName", hour.getStudent().getFirstname());
      ctx.setVariable("status", to.name());
      ctx.setVariable("hour", hour);
      String body = templateEngine.process("email/status-change", ctx);

      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(recipient);
      helper.setSubject("Your service hours have been " + to.name().toLowerCase());
      helper.setText(body, true);
      sender.send(message);
      log.info("Sent {} notification to {} for service hour #{}", to, recipient, hour.getId());
    } catch (MessagingException | RuntimeException e) {
      log.error(
          "Failed to send {} notification to {} for service hour #{}: {}",
          to,
          recipient,
          hour.getId(),
          e.getMessage());
    }
  }

  /**
   * Sends a password-reset link (TC-028 / TC-108g). When no SMTP sender is configured (dev) the
   * reset URL is logged so the flow is still testable locally.
   */
  public void sendPasswordReset(String toEmail, String resetUrl) {
    JavaMailSender sender = mailSender.getIfAvailable();
    if (sender == null) {
      log.info("[mail disabled] password reset link for {}: {}", toEmail, resetUrl);
      return;
    }
    try {
      Context ctx = new Context();
      ctx.setVariable("resetUrl", resetUrl);
      String body = templateEngine.process("email/password-reset", ctx);

      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(toEmail);
      helper.setSubject("Reset your Service Station password");
      helper.setText(body, true);
      sender.send(message);
      log.info("Sent password reset email to {}", toEmail);
    } catch (MessagingException | RuntimeException e) {
      log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
    }
  }
}

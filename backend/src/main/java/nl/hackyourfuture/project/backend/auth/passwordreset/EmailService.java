package nl.hackyourfuture.project.backend.auth.passwordreset;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.auth.passwordreset.exceptions.EmailSendingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

  private static final String RESET_PASSWORD_SUBJECT = "Reset your password";
  private final JavaMailSender mailSender;
  @Value("${frontend.base-url}")
  private String frontendBaseUrl;
  @Value("${spring.mail.username}")
  private String fromAddress;

  public void sendPasswordResetEmail(String toEmail, String rawToken) {
    String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;

    String htmlContent = """
        <!DOCTYPE html>
                    <html>
                    <body style="margin: 0; padding: 0; background-color: #f5f5f5; font-family: -apple-system, Segoe UI, \
                    Roboto, sans-serif;">
                        <table width="100%%" cellpadding="0" cellspacing="0" style="padding: 40px 0;">
                            <tr>
                                <td align="center">
                                    <table width="480" cellpadding="0" cellspacing="0" style="background-color: #ffffff; \
                                    border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
                                        <tr>
                                            <td style="padding: 32px 40px 0 40px;" align="center">
                                               <table cellpadding="0" cellspacing="0" style="margin: 0 auto;">
                                                  <tr>
                                                      <td style="width: 32px; height: 32px; background-color: #EA580C; \
                                                      border-radius: 8px; text-align: center; vertical-align: middle; \
                                                      font-size: 16px;">
                                                         📍
                                                      </td>
                                                       <td style="padding-left: 8px; font-size: 18px; font-weight: 700; \
                                                       color: #1a1a1a; vertical-align: middle;">
                                                         Loc
                                                      </td>
                                                  </tr>
                                               </table>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 16px 40px 24px 40px;">
                                                <h1 style="margin: 0 0 16px 0; font-size: 20px; color: #1a1a1a;">Reset your password</h1>
                                                <p style="margin: 0 0 24px 0; font-size: 15px; line-height: 1.5; color: #4a4a4a;">
                                                    We received a request to reset your password. Click the button below to choose a \
                                                    new one. This link expires in 15 minutes.
                                                </p>
                                                <table cellpadding="0" cellspacing="0">
                                                    <tr>
                                                        <td style="border-radius: 8px; background-color: #EA580C;">
                                                            <a href="%s" style="display: inline-block; padding: 12px 28px; \
                                                            font-size: 15px; font-weight: 600; color: #ffffff; text-decoration: none;">
                                                                Reset password
                                                            </a>
                                                        </td>
                                                    </tr>
                                                </table>
                                                <p style="margin: 24px 0 0 0; font-size: 13px; line-height: 1.5; color: #999999;">
                                                    If you didn't request this, you can safely ignore this email — your password won't be changed.
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
        """.formatted(resetLink);

    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(toEmail);
      helper.setSubject(RESET_PASSWORD_SUBJECT);
      helper.setText(htmlContent, true);
      mailSender.send(mimeMessage);
    } catch (MessagingException e) {
      throw new EmailSendingException("Failed to send password reset email");
    }
  }
}

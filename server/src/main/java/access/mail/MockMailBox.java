package access.mail;

import access.cron.IdPMetaDataResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;

public class MockMailBox extends MailBox {

    private final Environment environment;

    public MockMailBox(ObjectMapper objectMapper, IdPMetaDataResolver idPMetaDataResolver, JavaMailSender mailSender, String emailFrom, String contactEmail, String baseUrl, String welcomeUrl, Environment environment) throws IOException {
        super(objectMapper, idPMetaDataResolver, mailSender, emailFrom, contactEmail, baseUrl, welcomeUrl, "local");
        this.environment = environment;
    }

    @Override
    protected void doSendMail(MimeMessage message) {
        //nope
    }

    @Override
    protected void setText(String plainText, String htmlText, MimeMessageHelper helper) throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac os x") && !environment.matchesProfiles("test")) {
            openInBrowser(htmlText);
        }
    }

    private void openInBrowser(String html) throws IOException {
        File tempFile = File.createTempFile("javamail", ".html");
        FileCopyUtils.copy(html.getBytes(), tempFile);
        String absolutePath = tempFile.getAbsolutePath();
        new ProcessBuilder("open", absolutePath).start();
    }
}

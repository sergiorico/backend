package se.lth.cs.connect.modules;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ro.pippo.core.PippoSettings;

/**
* Send email.
*/
public class Mailman extends MailClient {

    private static Properties smtp;
    private static String USER, EMAIL, PASSW;

    private static void loadSetting(PippoSettings props, String field, String def) {
        smtp.put(field, props.getString(field, def));
    }

    @Override
	public void configure(PippoSettings props){
        smtp = new Properties();
        loadSetting(props, "mail.smtp.host", "smtp.gmail.com");
        loadSetting(props, "mail.smtp.auth", "true");
        loadSetting(props, "mail.smtp.starttls.enable", "true");
        loadSetting(props, "mail.smtp.socketFactory.port", "465");
        loadSetting(props, "mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        loadSetting(props, "mail.smtp.port", "465");

        USER = props.getString("mail.user", "superdupermail");
        EMAIL = props.getString("mail.email", "superdupermail");
        PASSW = props.getString("mail.password", "superduperpass");
    }

    /**
    * Send the email via SMTP using StartTLS and SSL
    */
    @Override
	public boolean sendEmail(String to, String subject, String html) {
        // Create the session
        Session session = Session.getDefaultInstance(smtp, new javax.mail.Authenticator() {
            @Override
			protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USER, PASSW);
            }
        });

        // Create and send the message
        try {
            // Create the message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(html);
            Transport.send(message);
            return true;
        } catch (Exception e) {
            System.err.println(e.getMessage() + "::" + to);
            return false;
        }
    }
}
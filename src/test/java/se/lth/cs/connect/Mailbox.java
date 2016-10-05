package se.lth.cs.connect;

import ro.pippo.core.PippoSettings;
import se.lth.cs.connect.modules.MailClient;

public class Mailbox extends MailClient {

	@Override
	public boolean sendEmail(String to, String subject, String html) {
		return false;
	}

	@Override
	public void configure(PippoSettings conf) {
		
	}

}

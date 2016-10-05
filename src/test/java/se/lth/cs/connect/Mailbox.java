package se.lth.cs.connect;

import java.util.ArrayList;
import java.util.List;

import ro.pippo.core.PippoSettings;
import se.lth.cs.connect.modules.MailClient;

/**
 * Utility class for collecting emails rather than actually sending them.
 */
public class Mailbox extends MailClient {

	private List<Mail> inbox;

	public Mailbox() {
		inbox = new ArrayList<Mail>();
	}

	public static class Mail {
		public String recipient, subject, content;

		public Mail(String to, String s, String c) {
			recipient = to;
			subject = s;
			content = c;
		}
	}

	@Override
	public boolean sendEmail(String to, String subject, String html) {
		return inbox.add(new Mail(to, subject, html));
	}

	/**
	 * 
	 * @return
	 */
	public List<Mail> getInbox() { return inbox; }
	
	
	public Mail top() { return inbox.get(inbox.size() - 1); }
	
	
	@Override
	public void configure(PippoSettings conf) {

	}

}

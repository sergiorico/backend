package se.lth.cs.connect.modules;

import ro.pippo.core.PippoSettings;

public abstract class MailClient {
	public abstract void configure(PippoSettings conf);
	public abstract boolean sendEmail(String to, String subject, String html);
}

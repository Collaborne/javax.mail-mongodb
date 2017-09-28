/**
 * Copyright (C) 2015 Collaborne B.V. (opensource@collaborne.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.collaborne.email.javaxmail.mongodb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class MongoDBMailTransport extends Transport {
	private final MongoDBServiceHelper helper;

	public MongoDBMailTransport(Session session, URLName urlname) {
		super(session, urlname);
		this.helper = new MongoDBServiceHelper(session, "mongodb-transport");
	}

	@Override
	protected boolean protocolConnect(String host, int port, String user, String password) throws MessagingException {
		return helper.protocolConnect(host, port, user, password);
	}
	
	// @VisibleForTesting
	protected BasicDBList convertAddresses(Address... addresses) {
		if (addresses == null) {
			return null;
		}
		BasicDBList result = new BasicDBList();
		for (Address address : addresses) {
			result.add(address.toString());
		}
		
		return result;
	}
	
	@Override
	public void sendMessage(Message message, Address[] addresses) throws MessagingException {
		if (!(message instanceof MimeMessage)) {
			throw new MessagingException("Can only send RFC822 messages");
		}
		
		MimeMessage mimeMessage = (MimeMessage) message;

		// Serialize the actual email into a single field
		String content;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			mimeMessage.writeTo(out);
			content = out.toString(StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			throw new MessagingException("Cannot process content", e);
		}
		
		BasicDBObject item = new BasicDBObject();
		item.put("messageId", mimeMessage.getMessageID());
		item.put("from", convertAddresses(mimeMessage.getFrom()));
		item.put("to", convertAddresses(mimeMessage.getRecipients(RecipientType.TO)));
		item.put("cc", convertAddresses(mimeMessage.getRecipients(RecipientType.CC)));
		item.put("bcc", convertAddresses(mimeMessage.getRecipients(RecipientType.BCC)));
		item.put("content", content);
		
		boolean crossOver = helper.isCrossOver();
		DBCollection outbox = helper.getFolderCollection(crossOver ? "inbox" : "outbox");
		outbox.save(item);
	}
}

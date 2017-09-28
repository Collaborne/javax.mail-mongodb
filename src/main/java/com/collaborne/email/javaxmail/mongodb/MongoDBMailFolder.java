/**
 * Copyright © 2015 Collaborne B.V. (opensource@collaborne.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.FolderNotFoundException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

public class MongoDBMailFolder extends AbstractMongoDBFolder {
	// Modified messages
	private final List<MongoDBMimeMessage> messages = new ArrayList<>();
	
	private final Session session;
	private final MongoDBMailFolder parent;
	private final String name;
	private final Set<String> ownerFields;
	private final String ownerEmail;
	private DBCollection collection;
	
	// READ_WRITE, READ_ONLY, or -1 for closed.
	private int open = -1;
	private boolean subscribed = false;
	
	public MongoDBMailFolder(MongoDBMailStore store, Session session, MongoDBMailFolder parent, String name, Set<String> ownerFields, String ownerEmail, DBCollection collection) {
		super(store);
		this.session = session;
		this.parent = parent;
		this.name = name;
		this.ownerFields = ownerFields;
		this.ownerEmail = ownerEmail;
		this.collection = collection;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFullName() {
		if (parent == null) {
			return getName();
		} else {
			return parent.getName() + getSeparator() + getName();
		}
	}

	@Override
	public Folder getParent() {
		return parent;
	}

	@Override
	public boolean exists() {
		return collection != null;
	}

	@Override
	public Folder[] list(String pattern) {
		// No nesting, really.
		return new Folder[0];
	}

	@Override
	public int getType() throws MessagingException {
		return Folder.HOLDS_MESSAGES;
	}

	@Override
	public boolean create(int type) throws MessagingException {
		if (type == Folder.HOLDS_FOLDERS) {
			// No nesting allowed
			throw new MessagingException("Cannot create folders that hold other folders");
		}
		this.collection = ((MongoDBMailStore) getStore()).getFolderCollection(getName());
		return exists();
	}

	@Override
	public boolean hasNewMessages() throws MessagingException {
		return getNewMessageCount() > 0;
	}

	@Override
	public Folder getFolder(String name) throws MessagingException {
		return getStore().getFolder(getFullName() + getSeparator() + name);
	}

	@Override
	public boolean delete(boolean recurse) throws MessagingException {
		throw new MessagingException("Not implemented", new UnsupportedOperationException("MongoDBMailFolder#delete() is not implemented"));
	}

	@Override
	public boolean renameTo(Folder f) throws MessagingException {
		throw new MessagingException("Not implemented", new UnsupportedOperationException("MongoDBMailFolder#renameTo() is not implemented"));
	}

	@Override
	public void open(int mode) throws MessagingException {
		if (isOpen()) {
			throw new MessagingException("Already opened");
		}
		
		if (!exists()) {
			throw new FolderNotFoundException();
		}
		
		// TODO: Verify that we can open it in the requested mode, possibly use the ownerFields for decision
		open = mode;
	}

	@Override
	public void close(boolean expunge) throws MessagingException {
		if (!isOpen()) {
			throw new MessagingException("Already closed");
		}
		if (expunge) {
			expunge();
		}
		open = -1;
	}

	@Override
	public boolean isOpen() {
		return open != -1;
	}

	@Override
	public Flags getPermanentFlags() {
		// See receive/send providers: we support all standard flags
		Flags result = new Flags();
		result.add(Flag.ANSWERED);
		result.add(Flag.DELETED);
		result.add(Flag.DRAFT);
		result.add(Flag.FLAGGED);
		result.add(Flag.RECENT);
		result.add(Flag.SEEN);
		return result;
	}

	// @VisibleForTesting
	protected BasicDBObject createQuery() {
		if (ownerEmail == null) {
			return new BasicDBObject();
		}
		
		// Specific owner, so filter for that one
		BasicDBList ownerQueries = new BasicDBList();
		for (String ownerField : ownerFields) {
			// Either exact match, ...
			BasicDBObject ownerQuery = new BasicDBObject(ownerField, ownerEmail);
			ownerQueries.add(ownerQuery);
			// ... or something with <email> in it.
			BasicDBObject ownerFuzzyQuery = new BasicDBObject(ownerField, new BasicDBObject("$regex", "<" + ownerEmail + ">"));
			ownerQueries.add(ownerFuzzyQuery);
		}
		return new BasicDBObject("$or", ownerQueries);
	}
	
	@Override
	public int getMessageCount() throws MessagingException {
		// Find all messages for the current user.
		BasicDBObject query = createQuery();
		return (int) collection.count(query);
	}

	@Override
	public Message getMessage(int msgnum) throws MessagingException {
		BasicDBObject query = createQuery();
		// NB: Sort, so that we get a stable order.
		try (DBCursor cursor = collection.find(query).sort(new BasicDBObject("_id", 1)).limit(1).skip(msgnum - 1)) {
			if (cursor.hasNext()) {
				MongoDBMimeMessage message = new MongoDBMimeMessage(this, msgnum, cursor.next());
				messages.add(message);
				return message;
			}
		}
		throw new MessagingException("No such message");
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
	public void appendMessages(Message[] msgs) throws MessagingException {
		for (Message msg : msgs) {
			MimeMessage mimeMessage;
			if (msg instanceof MimeMessage) {
				mimeMessage = (MimeMessage) msg;
			} else {
				// TODO: must convert that one
				throw new MessagingException("Cannot handle message " + msg);
			}

			// Serialize the actual email into a single field
			String content;
			try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				mimeMessage.writeTo(out);
				content = out.toString(StandardCharsets.UTF_8.name());
			} catch (IOException e) {
				throw new MessagingException("Invalid message", e);
			}
			
			BasicDBObject item = new BasicDBObject();
			item.put("messageId", mimeMessage.getMessageID());
			item.put("from", convertAddresses(mimeMessage.getFrom()));
			item.put("to", convertAddresses(mimeMessage.getRecipients(RecipientType.TO)));
			item.put("cc", convertAddresses(mimeMessage.getRecipients(RecipientType.CC)));
			item.put("bcc", convertAddresses(mimeMessage.getRecipients(RecipientType.BCC)));
			item.put("content", content);
			
			messages.add(new MongoDBMimeMessage(session, item));
		}
	}
	
	@Override
	public Message[] expunge() throws MessagingException {
		// Process all messages: 
		// update flags back into mongo, delete the ones with a DELETED flag.
		List<Message> expungedMessage = new ArrayList<>();
		for (MongoDBMimeMessage message : messages) {
			if (message.isSet(Flag.DELETED)) {
				message.setExpunged();
				collection.remove(message.getDbObject());
				expungedMessage.add(message);
			} else {
				collection.save(message.getDbObject());
			}
		}
		return expungedMessage.toArray(new Message[expungedMessage.size()]);
	}
	
	@Override
	public void setSubscribed(boolean subscribe) throws MessagingException {
		// FIXME: GatorMail should handle MethodNotSupportedException
		this.subscribed = subscribe;
	}
	
	@Override
	public boolean isSubscribed() {
		return subscribed;
	}
}

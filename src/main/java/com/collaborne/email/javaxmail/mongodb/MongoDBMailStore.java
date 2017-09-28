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

import java.util.HashSet;
import java.util.Set;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import com.mongodb.DBCollection;

public class MongoDBMailStore extends Store {
	private final MongoDBServiceHelper helper;
	
	public MongoDBMailStore(Session session, URLName urlname) {
		super(session, urlname);
		this.helper = new MongoDBServiceHelper(session, "mongodb-store");
	}

	@Override
	protected boolean protocolConnect(String host, int port, String login, String password) throws MessagingException {
		return helper.protocolConnect(host, port, login, password);
	}
	
	@Override
	public Folder getDefaultFolder() throws MessagingException {
		return new MongoDBDefaultFolder(this);
	}

	@Override
	public Folder getFolder(String name) throws MessagingException {
		return getFolder(name, true);
	}
	
	protected Folder getFolder(String name, boolean open) throws MessagingException {
		DBCollection collection;
		Set<String> ownerFields = new HashSet<>();

		boolean crossOver = helper.isCrossOver();
		if ("INBOX".equals(name)) {
			collection = getFolderCollection(crossOver ? "outbox" : "inbox");
			ownerFields.add("to");
			ownerFields.add("cc");
			ownerFields.add("bcc");
		} else if ("OUTBOX".equals(name)) {
			collection = getFolderCollection(crossOver ? "inbox" : "outbox");
			ownerFields.add("from");
		} else {
			// Lazy-create
			collection = null;
		}
		Folder result = new MongoDBMailFolder(this, session, null, name, ownerFields, helper.getMailFrom(), collection);
		if (open && result.exists()) {
			result.open(Folder.READ_WRITE);
		}
		return result;
	}

	@Override
	public Folder getFolder(URLName url) throws MessagingException {
		return getFolder(url.getFile(), false);
	}

	public DBCollection getFolderCollection(String name) {
		return helper.getFolderCollection(name);
	}
}

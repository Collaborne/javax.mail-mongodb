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

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.MethodNotSupportedException;
import javax.mail.Store;

// Pretty much identical to POP3: Only "INBOX" is available.
class MongoDBDefaultFolder extends AbstractMongoDBFolder {
	protected MongoDBDefaultFolder(Store store) {
		super(store);
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public String getFullName() {
		return "";
	}

	@Override
	public Folder getParent() {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public Folder[] list(String pattern) throws MessagingException {
		return new Folder[] { getInbox() };
	}

	@Override
	public int getType() {
		return HOLDS_FOLDERS;
	}

	@Override
	public boolean create(int type) throws MessagingException {
		return false;
	}

	@Override
	public boolean hasNewMessages() throws MessagingException {
		return false;
	}

	protected Folder getInbox() throws MessagingException {
		return getStore().getFolder("INBOX");
	}

	@Override
	public Folder getFolder(String name) throws MessagingException {
		if ("INBOX".equals(name)) {
			return getInbox();
		} else {
			throw new MessagingException("only INBOX supported");
		}
	}

	@Override
	public boolean delete(boolean recurse) throws MessagingException {
		throw new MethodNotSupportedException("delete");
	}

	@Override
	public boolean renameTo(Folder f) throws MessagingException {
		throw new MethodNotSupportedException("renameTo");
	}

	@Override
	public void open(int mode) throws MessagingException {
		throw new MethodNotSupportedException("open");
	}

	@Override
	public void close(boolean expunge) throws MessagingException {
		throw new MethodNotSupportedException("close");
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public Flags getPermanentFlags() {
		return new Flags();
	}

	@Override
	public int getMessageCount() throws MessagingException {
		return 0;
	}

	@Override
	public Message getMessage(int msgno) throws MessagingException {
		throw new MethodNotSupportedException("getMessage");
	}

	@Override
	public void appendMessages(Message[] msgs) throws MessagingException {
		throw new MethodNotSupportedException("appendMessages");
	}

	@Override
	public Message[] expunge() throws MessagingException {
		throw new MethodNotSupportedException("expunge");
	}
}
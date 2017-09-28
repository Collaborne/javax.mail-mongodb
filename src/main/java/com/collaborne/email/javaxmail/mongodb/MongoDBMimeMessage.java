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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;

public class MongoDBMimeMessage extends MimeMessage {
	private final Logger logger = LoggerFactory.getLogger(MongoDBMimeMessage.class);
	private final DBObject dbObject;
	
	public MongoDBMimeMessage(Session session, DBObject dbObject) throws MessagingException {
		super(session);
		this.dbObject = dbObject;
		
		parse();
	}
	
	public MongoDBMimeMessage(Folder folder, int msgnum, DBObject dbObject) throws MessagingException {
		super(folder, msgnum);
		this.dbObject = dbObject;
		
		parse();
	}
	
	private final void parse() throws MessagingException {			
		String content = (String) dbObject.get("content");
		InputStream contentStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		parse(contentStream);
		
		Flags flags = new Flags();
		BasicDBList flagList = (BasicDBList) dbObject.get("flags");
		if (flagList != null) {
			for (Object flagValue : flagList) {
				if ("answered".equals(flagValue)) {
					flags.add(Flag.ANSWERED);
				} else if ("deleted".equals(flagValue)) {
					flags.add(Flag.DELETED);
				} else if ("draft".equals(flagValue)) {
					flags.add(Flag.DRAFT);
				} else if ("flagged".equals(flagValue)) {
					flags.add(Flag.FLAGGED);
				} else if ("recent".equals(flagValue)) {
					flags.add(Flag.RECENT);
				} else if ("seen".equals(flagValue)) {
					flags.add(Flag.SEEN);
				} else {
					logger.warn("Unknown flag {} when parsing message {}", flagValue, getMessageID());
				}
			}
		}
		setFlags(flags, true);
	}
	
	public DBObject getDbObject() {
		return dbObject;
	}
	
	@Override
	public void saveChanges() throws MessagingException {
		super.saveChanges();
		
		BasicDBList flagList = new BasicDBList();
		if (isSet(Flag.ANSWERED)) {
			flagList.add("answered");
		}
		if (isSet(Flag.DELETED)) {
			flagList.add("deleted");
		}
		if (isSet(Flag.DRAFT)) {
			flagList.add("draft");
		}
		if (isSet(Flag.FLAGGED)) {
			flagList.add("flagged");
		}
		if (isSet(Flag.RECENT)) {
			flagList.add("recent");
		}
		if (isSet(Flag.SEEN)) {
			flagList.add("seen");
		}
		dbObject.put("flags", flagList);
	}
	
	public void setExpunged() {
		this.expunged = true;
	}
	
	@Override
	public Date getReceivedDate() throws MessagingException {
		ObjectId id = (ObjectId) dbObject.get("_id");
		return id.getDate();
	}
}

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

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;

import com.mongodb.DB;
import com.mongodb.DBCollection;

/**
 * Helper for implementing the Store/Transport services.
 * 
 * @author andreas
 */
public class MongoDBServiceHelper {
	private final Session session;
	private final String name;
	private final DB db;
	
	public MongoDBServiceHelper(Session session, String name) {
		this.session = session;
		this.name = name;
		this.db = (DB) session.getProperties().get("mail." + name + ".db");
	}
	
	public String getTenant() {
		return session.getProperty("mail." + name + ".tenant");
	}
	
	public String getMailFrom() {
		return session.getProperty("mail.from");
	}
	
	/**
	 * @param host
	 * @param port
	 * @param login
	 * @param password  
	 */
	public boolean protocolConnect(String host, int port, String login, String password) throws MessagingException {
		String tenant = getTenant(); 
		if (tenant == null) {
			throw new AuthenticationFailedException("Tenant not configured");
		}
		return true;
	}
	
	public DBCollection getFolderCollection(String name) {
		return db.getCollection(getTenant() + ".email." + name);
	}

	/**
	 * Whether this session is in cross-over mode.
	 * 
	 * "Cross-over mode" means that the session behaves like a remote partner,
	 * i.e. its outbox is the other's inbox.
	 * 
	 * @return
	 */
	public boolean isCrossOver() {
		return Boolean.parseBoolean(session.getProperty("mail." + name + ".crossover"));
	}
}

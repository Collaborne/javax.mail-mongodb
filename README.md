# javax.mail-mongodb [![Build Status](https://travis-ci.org/Collaborne/javax.mail-mongodb.svg?branch=master)](https://travis-ci.org/Collaborne/javax.mail-mongodb)

javax.mail provider that uses MongoDB as storage. This is mostly intended for testing applications that use the javax.mail API.

## Installation

The library is published to Maven Central, so the following dependency will add it to your project.
```xml
<dependency>
    <groupId>com.collaborne.mail</groupId>
    <artifactId>mongodb</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
</dependency>
```

## Usage

```java
// Initialize MongoDB
com.mongodb.DB mongoDb = ...

// Set connection details
// See https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html
// for the various properties
Properties props = new Properties();
props.setProperty("mail.transport.protocol", "mongodb-transport");
// Force RFC822 addresses to get mapped to this protocol (in this session)
props.setProperty("mail.transport.protocol.rfc822", "mongodb-transport");

// Configure the DB to use by the transport
props.put("mail.mongodb-transport.db", mongoDb);

// Set the 'tenant' for handling multiple mail stores
props.setProperty("mail.mongodb-transport.tenant", "default");

// Create a session
javax.mail.Session session = Session.getInstance(props);

```

## License

    This software is licensed under the Apache 2 license, quoted below.

    Copyright 2015-2017 Collaborne B.V. <http://github.com/Collaborne/>

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.

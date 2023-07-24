/*
 * SmartTestAutoFramework
 * Copyright 2023 and beyond [Madhav Krishna]
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
 * 
 */
package org.uitnet.testing.smartfwk.database;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.testng.Assert;
import org.uitnet.testing.smartfwk.common.MethodArg;
import org.uitnet.testing.smartfwk.common.ReturnType;
import org.uitnet.testing.smartfwk.ui.core.config.DatabaseProfile;
import org.uitnet.testing.smartfwk.ui.core.utils.ObjectUtil;
import org.uitnet.testing.smartfwk.ui.core.utils.StringUtil;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;

/**
 * Mongo database action handler also called the MongoDatabaseActionHandler.
 * Used to perform CRUD(Create, Read, Update and Delete) operations on MongoDB database
 * entities / collections.
 * 
 * @author Madhav Krishna
 *
 */
public class MongoDatabaseActionHandler extends AbstractDatabaseActionHandler {
	protected String databaseName = null;

	public MongoDatabaseActionHandler(String appName, int sessionExpiryDurationInSeconds,
			DatabaseProfile databaseProfile) {
		super(appName, sessionExpiryDurationInSeconds, databaseProfile);
	}

	@Override
	public DatabaseConnection connect(DatabaseProfile dbProfile) {
		Map<String, Object> dbAdditionalProps = dbProfile.getAdditionalProps();
		databaseName = (String) dbAdditionalProps.get("databaseName");

		MongoCredential auth = prepareMongoCredentials(dbAdditionalProps);

		ServerAddress serverAddress = new ServerAddress((String) dbAdditionalProps.get("hostNameOrIpAddress"),
				(Integer) dbAdditionalProps.get("port"));

		MongoClientOptions options = prepareOptions(dbProfile, dbAdditionalProps);
		MongoClient client = null;
		if(auth == null) {
			client = new MongoClient(serverAddress, options);
		} else {
			client = new MongoClient(serverAddress, auth, options);
		}

		DatabaseConnection connection = new DatabaseConnection(client);
		return connection;
	}
	
	protected MongoCredential prepareMongoCredentials(Map<String, Object> dbAdditionalProps) {
		String authMechanism = "" + dbAdditionalProps.get("auth.mechanism");
		MongoCredential auth = null;
		switch (authMechanism) {
		case "1": { // Basic(SCRAM-SHA-256)
			auth = MongoCredential.createScramSha256Credential((String) dbAdditionalProps.get("auth.mechanism.1.userName"),
					(String) dbAdditionalProps.get("auth.mechanism.1.databaseName"),
					((String) dbAdditionalProps.get("auth.mechanism.1.password")).toCharArray());
			break;
		}
		case "2": { //Legacy(SCRAM-SHA-1)
			auth = MongoCredential.createScramSha1Credential((String) dbAdditionalProps.get("auth.mechanism.2.userName"),
					(String) dbAdditionalProps.get("auth.mechanism.2.databaseName"),
					((String) dbAdditionalProps.get("auth.mechanism.2.password")).toCharArray());
			break;
		}
		case "3": { //X.509
			String userName = (String) dbAdditionalProps.get("auth.mechanism.3.userName");
			if(StringUtil.isEmptyAfterTrim(userName)) {
				auth = MongoCredential.createMongoX509Credential();
			} else {
				auth = MongoCredential.createMongoX509Credential(userName);
			}
			break;
		}
		case "4": { //Kerberos(GSSAPI)
			String userName = (String) dbAdditionalProps.get("auth.mechanism.4.userName");
			if(StringUtil.isEmptyAfterTrim(userName)) {
				auth = MongoCredential.createGSSAPICredential("");
			} else {
				auth = MongoCredential.createGSSAPICredential(userName);
			}
			break;
		}
		case "5": { //LDAP (PLAIN)
			auth = MongoCredential.createPlainCredential((String) dbAdditionalProps.get("auth.mechanism.5.userName"),
					(String) dbAdditionalProps.get("auth.mechanism.5.databaseName"),
					((String) dbAdditionalProps.get("auth.mechanism.5.password")).toCharArray());
			break;
		}
		default: {
			auth = null;
			break;
		}
		}
			

		return auth;
	}
	
	@SuppressWarnings("unchecked")
	protected MongoClientOptions prepareOptions(DatabaseProfile dbProfile, Map<String, Object> dbAdditionalProps) {
		Map<String, Object> options = (Map<String, Object>)dbAdditionalProps.get("options");
		Builder builder = MongoClientOptions.builder();
		if(options != null) {
			Method m = null;
			
			try {
				for(Map.Entry<String, Object> entry : options.entrySet()) {
					m = ObjectUtil.findClassMethod(Builder.class, entry.getKey(), 1);
					m.invoke(builder, entry.getValue());
				}
			} catch(Exception ex) {
				Assert.fail("Failed to set '' property with '' value into mongo client driver.", ex);
			}
		}
		
		MongoClientOptions moptions = builder.build();

		return moptions;
	}

	@Override
	protected void disconnect(DatabaseConnection connection) {
		MongoClient client = (MongoClient) connection.getConnection();
		if (client != null) {
			client.close();
		}

	}

	protected String runCommand(DatabaseConnection connection, String jsonCommand, boolean shouldCommit) {
		MongoClient client = (MongoClient) connection.getConnection();
		ClientSession session = null;
		String returnJson = new Document().toJson();

		try {
			MongoDatabase db = client.getDatabase(databaseName);

			Document command = Document.parse(jsonCommand);

			session = client.startSession();
			
			if(shouldCommit) {
				session.startTransaction();
			}

			Document results = db.runCommand(session, command);

			if (shouldCommit) {
				session.commitTransaction();
			}

			if (results != null) {
				returnJson = results.toJson();
			}
		} catch (Exception ex) {
			if (shouldCommit && session != null) {
				session.abortTransaction();
			}
			Assert.fail(ex.getMessage(), ex);
		} finally {
			if (session != null) {
				session.close();
			}
		}

		return returnJson;
	}

	@Override
	protected String getDataAsJsonString(DatabaseConnection connection, String entityName, String searchStatement) {
		return runCommand(connection, searchStatement, false);
	}

	@Override
	protected void updateData(DatabaseConnection connection, String entityName, String updateStatement) {
		runCommand(connection, updateStatement, true);
	}

	@Override
	protected void deleteData(DatabaseConnection connection, String entityName, String deleteStatement) {
		runCommand(connection, deleteStatement, true);
	}

	@Override
	protected void insertData(DatabaseConnection connection, String entityName, String insertStatement) {
		runCommand(connection, insertStatement, true);
	}

	@Override
	protected void insertDataInBatch(DatabaseConnection connection, String entityName, List<String> insertStatements) {
		for (String stmt : insertStatements) {
			runCommand(connection, stmt, true);
		}
	}

	@Override
	protected String executeFunctionReturnAsJson(String functionName, ReturnType returnType, Object... args) {
		Assert.fail("Functions are not supported in MongoDB. So not supported.");
		return null;
	}

	@Override
	protected String executeProcedureReturnAsJson(String procedureName, MethodArg<?>... args) {
		Assert.fail("Procedures are not supported in MongoDB. So not supported.");
		return null;
	}

	@Override
	protected void create(DatabaseConnection connection, String entityName, String createStatement) {
		runCommand(connection, createStatement, true);
	}

	@Override
	protected void drop(DatabaseConnection connection, String entityName, String dropStatement) {
		runCommand(connection, dropStatement, true);
	}

}

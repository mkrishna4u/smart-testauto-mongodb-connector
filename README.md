# MongoDB Connector for STAS (Smart Test Automation Studio)
STAS Project Link: https://github.com/mkrishna4u/smart-testauto-framework

This is a MongoDB connector for performing CRUD (Create, Read, Update, Delete) operations on MongoDB database. Include the following maven dependency into STAS project **pom.xml** file.


	<dependency>
	    <groupId>org.uitnet.testing.smartfwk</groupId>
	    <artifactId>smart-testauto-mongodb-connector</artifactId>
	    <version>${smart-testauto-mongodb-connector.version}</version>
	</dependency>
	
**Note:** Update **${smart-testauto-mongodb-connector.version}** string with the specific version like **6.0.8**. You can find more details about it on Maven Central or Github Releases under the artifact **smart-testauto-mongodb-connector**.
	
## How to use MongoDB connector into STAS project?
Once you will add maven dependency into your STAS project. Follow the following steps to use it:
	
1. Take the **sample-db.yaml** database configuration file from the link given below and copy into your project directory **"test-config/apps-config/&lt;app-name&gt;/database-profiles/"**

[https://github.com/mkrishna4u/smart-testauto-framework/tree/main/src/main/resources](https://github.com/mkrishna4u/smart-testauto-framework/tree/main/src/main/resources "STAS Tool - MongoDB Sample Database Profile") 

2. You can change the file name as per your database profile name like **admin-db.yaml** and also within the file contents update the **profileName:** property to specify database profile name like **admin-db**.

3. Update **dbProfileNames:** property in **"test-config/apps-config/&lt;app-name&gt;/AppConfig.yaml"** file of your project to include this **admin-db** into it like given below:

	dbProfileNames: [admin-db]
	
This database profile name can be used in feature file or in the custom code to perform crud operations on MongoDB.

4. NOTE: Don't forgot to update your custom **admin-db.yaml** file as per your MongoDB connection settings.

You are all set with MongoDB connector for your STAS project.

## How to use MongoDB in feature file to perform CRUD operation using STAS project?
Here is the sample code (Sample Usecases) to perform CRUD operations on MongoDB using **smart-testauto-mongodb-connector**. Please refer the following MongoDB manual link to know more on MongoDB commands:

[https://www.mongodb.com/docs/manual/reference/command/](https://www.mongodb.com/docs/manual/reference/command/ "MongoDB Commands Reference") 

**A. Read operation on MongoDb entity / collection:**

	Scenario: Verify find query on MongoDB to retrieve the data and verify retrieved data.
      When get "Customer" entity data as JSON document using query below and store into "JSON_RESP_VAR" variable. Target DB Info [AppName="MyApp", DatabaseProfileName="sample-db-mongo"]:
      """
      {
        find: "customer",
        filter: {"age": {"$gt": 45}},
        batchSize: 999999
      }
      """
      Then print JSON object [JSONObjRefVariable="JSON_RESP_VAR"].
      Then verify the following parameters of JSON object matches with the expected information as per the tabular info given below [JSONObjRefVariable="JSON_RESP_VAR"]:
      | Parameter/JSON Path                                             | Operator | Expected Information           |
      | {path: "$.cursor.firstBatch[*].age", valueType: "integer-list"} | >        | {ev: 45, valueType: "integer"} |
	
**B. Update operation on MongoDb entity / collection:**

	Scenario: Perform update operation on MongoDB entity/collection.
      When update "customer" entity data using query below. Target DB Info [AppName="MyApp", DatabaseProfileName="sample-db-mongo"]:
      """
      {
        update: "customer",
        updates: [
          {
            q: {"age": {"$eq": 80}},
            u: { $set: { age: 84, lname: "David" } },
          }
        ]
      }
      """

**C. Delete operation from MongoDB entity / collection:**

	Scenario: Perform delete operation on MongoDB entity/collection.
      When delete "customer" entity data using query below. Target DB Info [AppName="MyApp", DatabaseProfileName="sample-db-mongo"]:
      """
      {
        delete: "customer",
        deletes: [
          {
            q: {"age": {"$eq": 65}},
            limit: 0
          }
        ]
      }
      """

**D. Insert/Create operation in MongoDb entity / collection:**

	Scenario: Perform insert operation on MongoDB entity/collection.
      When insert new data into "customer" entity using query below. Target DB Info [AppName="MyApp", DatabaseProfileName="sample-db-mongo"]:
      """
      {
        insert: "customer",
        documents: [
          {
            "custId": "3",
            "fname": "Vipul",
            "lname": "Krishna",
            "age": 77,
            "email": "vipul.krishna2@gmail.com",
            "phone": "222-333-9999",
            "salary": 90000,
            "state": "VA",
            "country": "US"
          }
        ]
      }
      """


# License
Apache License, 2.0

projectforge-continuous-db
==========================

Use this package for continuous delivery of your software.

Any initial setup of the data-base as well as update-scripts are very easy to manage programmatically. The JPA annotations are supported,
therefore you may set-up a complete data-base schema with only a few lines of code.

## Example: Initial setup of a data-base

```java
Class< ? >[] doClasses = new Class< ? >[] { //
        UserDO.class, //
        TaskDO.class, GroupDO.class, TaskDO.class, GroupTaskAccessDO.class, //
        AccessEntryDO.class, //
    };

if (databaseUpdateDao.doesEntitiesExist(doClasses) == false) {
  SchemaGenerator schemaGenerator = configuration.createSchemaGenerator().add(doClasses);
  schemaGenerator.createSchema();
  databaseUpdateDao.createMissingIndices(); // Create missing indices.
}
```

Please note: foreign-keys, one-to-many, many-to-many relations are supported as well as different column types. You may extend
this module very easy for further support of JPA annotations.

## Example: Update script

```java
if (databaseUpdateDao.doesEntitiesExist(Address1DO.class) == false) {
  // Initial creation of t_address because data-base table doesn't yet exist:
  configuration.createSchemaGenerator().add(Address1DO.class).createSchema();
}
if (databaseUpdateDao.doesTableAttributesExist(Address2DO.class, "birthday", "address") == false) {
  // One or both attributes don't yet exist, alter table to add the missing columns now:
  databaseUpdateDao.addTableAttributes(Address2DO.class, "birthday", "address"); // Works also, if one of both attributes does already
  // exist.
}
```

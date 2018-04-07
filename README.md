# Bug-Free-Spork
A query builder and ORM inspired by [Eloquent](https://laravel.com/docs/master/eloquent)

## Setup
Add the following to your `pom.xml`
````
<repository>
  <id>mrkirby153</id>
  <url>https://repo.mrkirby153.com/repository/maven-public/</url>
</repository>
...
<dependency>
  <groupId>com.mrkirby153</groupId>
  <artifactId>bug-free-spork</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
````

## Usage
Before attempting to use the `QueryBuilder` or any model methods, set the `ConnectionFactory` that
will be used to make connections to the database by setting `QueryBuilder.connectionFactory` to your
connection factory.
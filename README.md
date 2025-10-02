# Tomcat AWS Application

## Overview
A web application deployed on AWS using Apache Tomcat and MySQL database. The application features multiple interconnected pages accessible through hyperlinks, filtering, searching, and a payment cart system.

## Technologies Used
- Apache Tomcat
- AWS Cloud Platform
- MySQL Database
- Java Web Technologies
- Docker containerization
- Kubernetes orchestration

## Features
- Multi-page web application
- Database integration
- Cloud deployment
- Navigation system through hyperlinks
- Payment System
- Filtering and Search
- Employee Dashboard to add and insert movies
- XML Parser

## Connection Pooling
  - #### Include the filename/path of all code/configuration files in GitHub of using JDBC Connection Pooling(Filenames found in src)
    - AddStarServlet.java
    - AddStarServlet.java
    - AutocompleteServlet.java
    - CartServlet.java
    - EmployeeLoginServlet.java
    - GenresServlet.java
    - LoginServlet.java
    - MoviesServlet.java
    - MetadataServlet.java
    - PlaceOrderServlet.java
    - SearchStarServlet.java
    - SingleMovieServlet.java
    - SingleStarServlet.java
    - WebContent/META-INF/context.xml
    - WebContent/WEB-INF/web.xml
  - #### Explain how Connection Pooling is utilized in the Fabflix code.
    - context.xml + web.xml defines connection pools of size 100 at the specified resource name. This sets up connection pooling in the tomcat layer. Src files that require mysql queries are inititated as classes, with their init function modified to perform a connection look up - rather than creating their own connection - within the connection pool. This allows every src file to borrow and reuse connections from the connection pool rather than needing to create their own connection, significiantly reducing peformance costs during the application lifetime. Logic existed within these blocks - try (Connection conn = dataSource.getConnection()) - so that connections were returned back to the pool once done, ensuring max reusability of resources.
  - #### Explain how Connection Pooling works with two backend SQL.
    - Connection pools can come from any source so long as the network conditions allowed it, meaning the to use connection pools with 2 backend SQL is the following. You would need to treat the two backend SQL's as 2 different instances, with each connection pool connecting to each backend SQL. This means 2 separate connection pools for 2 separate mysql instances. context.xml + web.xml defines two different SQL resources, one for each backend SQL. This in effect set up 2 different connection pools for each of the backend SQL for me to distribute accross the different src files that used connection pooling. Each src file chose between 1 of the 2 data pools to source their sql connections from, predetermined in their init function. Essentially a combination of the two pools were able to be used as the driving force towards all database connection within this project, allowing 2 different backend SQL to power up my application. Having 2 SQL's instead of one allowed for a wider capacity of database operations, potentially allowing me to serve more clients. 

## Master/Slave
  - #### Include the filename/path of all code/configuration files in GitHub of routing queries to Master/Slave SQL.
      - AddStarServlet.java
      - AddStarServlet.java
      - AutocompleteServlet.java
      - CartServlet.java
      - EmployeeLoginServlet.java
      - GenresServlet.java
      - LoginServlet.java
      - MoviesServlet.java
      - MetadataServlet.java
      - PlaceOrderServlet.java
      - SearchStarServlet.java
      - SingleMovieServlet.java
      - SingleStarServlet.java
      - WebContent/META-INF/context.xml
      - WebContent/WEB-INF/web.xml
  - #### How read/write requests were routed to Master/Slave SQL?
     - Essentially this is borrowing from the aforementioned implementation of connection pools. I treated the Master and the slave SQL as 2 different backend SQL machines. 2 connection pools were defined in my context.xml, one being for the master and one being for the slave instance. Since only write operations could apply to the slave instance, as only writes there can properly allow the master to propogate any changes to its slaves, the datasource pool associated with the master ended up being called "MySQLReadWrite" to encompass its usage in read + write sql operations. I utilized datasource "MySQLReadWrite" in src files that performed insertion based queries in mysql. This composed of AddMovieServlet.java, AddStarServlet.java, and PlaceOrderServlet.java. All of these endpoints aimed to modified the database, whence why routing their operations into the master SQL was necessary. Since every other mysql service/file in my project consisted of read only operations, I assigned the datasource pool to "MySQLReadOnly" - the slave SQL. To summarize, any usage of writing into the database got assigned the master SQL, and everything else the slave SQL.  


## Demo
[Video Demonstration](https://youtu.be/AtRqVmQtBBk?si=jPRFi0w9YbUJv7Yk)


## Project Structure - Microservice setup
- #### jasontran-app
  - `GET /api/autocomplete` - Returns auto complete content
  - `GET /api/cart` - Returns cart content
  - `POST /api/cart` - changes cart content
  - `GET /api/check-employee` - Checks if current user is an employee
  - `GET /api/genres` - Returns genres in database
  - `GET /api/dashboard/metadata` - Returns database metadata
  - `GET /api/movies` - Returns database metadata 
  - `GET /api/save-page` - Returns saved page
  - `GET /api/search-star` - Returns searched star
  - `GET /api/single-movie` - Returns single movie info
  - `GET /api/single-star` - Returns single star info
  - `POST /api/save-page` - Saves current page into server
  - `POST /api/order` - Processes an order
  - `POST /api/dashboard/add-movie` - Adds movie through employee dashboard
  - `POST /api/dashboard/add-star` - Adds star through employee dashboard
- #### jasontran-login
    - `POST /api/dashboard-login` - Logs employee in
    - `POST /api/login` - Logs user in
- #### yaml files
  - fabflix2.yaml - microservice setup yaml
  - ingress2.taml - microservice ingress setup yaml

## Contributors
**Jason Tran**
- Solo developer
- Responsible for complete project implementation including:
  - Application development
  - Database design and integration
  - AWS deployment
  - Testing and documentation

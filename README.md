# Tomcat AWS Application

## Overview
A web application deployed on AWS using Apache Tomcat and MySQL database. The application features multiple interconnected pages accessible through hyperlinks, filtering, searching, and a payment cart system.

## Technologies Used
- Apache Tomcat
- AWS Cloud Platform
- MySQL Database
- Java Web Technologies

## Features
- Multi-page web application
- Database integration
- Cloud deployment
- Navigation system through hyperlinks
- Payment System
- Filtering and Search
- Employee Dashboard to add and insert movies
- XML Parser

## Substring-Matching
- Implemented substring matching in the search queries in the form '%search%', where lookup queries will execute m.title LIKE '%search%'. This was used extensively in src/MoviesServlet.java to handle the search parameters in title, directors, and star name. This effectively turns searching in any of these fields as a search for a substring in general, and not just a prefix or suffix.

## Prepared Statement Usage(Files found in src)
- AddStarServlet.java
- CartServlet.java
- EmployeeLoginServlet.java
- GenresServlet.java
- LoginServlet.java
- MoviesServlet.java
- PlaceOrderServlet.java
- SearchStarServlet.java
- SingleMovieServlet.java
- SingleStarServlet.java

## Parsing Time Optimizations
- Batch inserts of batch sizes ranging from 100-200 depending on file parsed, rather than individual inserts
- Intensive use of hash mapping to optimize look up times for existing data entries + FID & Star name hashmaps to parse cast.xml faster, considering they used fid's and star name to link data entries. 

## Inconsistent Data Reports
This can be found in the file "parser_log.txt" that contains statistics and logging of 
specific exceptions that had occured during the parsing of the xml files. Search for keywords "Parsing completed" to get back 3 result reports of the 3 different xml files: actor.xml, main.xml, and cast.xml in that order. This is just text copied from that file using that method for convinience. The rest of the logging content can be found in that file


For actor.xml: 

- Total actors inserted: 6005
- Total errors encountered (missing vital information): 1
- Number of duplicates (Already existing in database or during this session): 857

For main.xml:
- Total movies inserted: 12004
- Total errors encountered (Invalid information): 45
- Number of duplicates (Already existing in database or during this session): 66
- Number of genres inserted: 108
- Number of movie genres inserted: 9767

For cast.xml: 

- Note: missing stars were handled by simply adding them into the database with their star name and a null dob
- Stars inserted before cast.xml: 6005
- Missing stars inserted during cast.xml: 10471
- Total stars inserted after cast.xml: 16476

______________________________________________________________________________________________________
- Total star-movie relations inserted: 46913
- Total errors encountered (Invalid Information): 787
- Number of duplicates (Already existing in database or during this session): 0

## Demo
[Video Demonstration](https://youtu.be/UfVzlpAo1nE?si=zdfJ6yPJrL5PvPcS)


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
## Project Structure
- Web pages interconnected via hyperlinks
- MySQL database backend
- Deployed on AWS infrastructure
- Tomcat server configuration

## Contributors
**Jason Tran**
- Solo developer
- Responsible for complete project implementation including:
  - Application development
  - Database design and integration
  - AWS deployment
  - Testing and documentation

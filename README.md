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
specific exceptions that had occured during the parsing of the xml files. Search for keywords "Parsing completed" to get back 3 result reports of the 3 different xml files: actor.xml, main.xml, and cast.xml in that order

## Demo
[Video Demonstration](https://youtu.be/PUN42ua-Jmk?si=FI801WWxQBb19QCS)

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

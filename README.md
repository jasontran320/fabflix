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

## Substring-Matching
- Implemented substring matching in the search queries in the form '%search%', where lookup queries will execute m.title LIKE '%search%'. This was used extensively in src/MoviesServlet.java to handle the search parameters in title, directors, and star name. This effectively turns searching in any of these fields as a search for a substring in general, and not just a prefix or suffix.

## Demo
[Video Demonstration](https://youtu.be/vrkEskx_dQk)

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

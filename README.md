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

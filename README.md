# Austin College Service Station Hours Registration Web Application
### Backgrounds
[Austin College Service Station](https://www.austincollege.edu/campus-life/service-station/) is a student-run office which organizes volunteer and community service events. Every year, Service Station organizes huge volunteer events focusing on diverse purposes, such as *Great Day of Service*, *GreenServe*, and *JanServe* and corporates with student service organizations and local non-profit agencys to hold these events. 

Many students participte in these events and use paper forms to record the number of hours participated in volunteer activities. These service hour records are helpful for students' future application to non-profit organizations or social work related positions. Nonetheless, keeping and organizing a large stack of paper forms is not easy. We aim to design a platform in order to help students keep track of their service hours and replace the tedious and error-prone paperwork. The platform has the following specific goals:

For the Service Station office: 
- post service events and recruit students to participate
- approve students' pariticpation and service hour records
- get useful statistics about student participation in service events to improve future events

For students: 
- keep a digital record of all pariticipated service events and service hours and generate a summary report
- get useful statistics about their participation in service events and plan for future involvement in service activities

### Structure of the Web App
#### Tech Stack
We utilized the Groovy-on-Grails framework which adopts the Spring MVC structure.

- Front-End: jQuery, Javascript, Bootstrap, AJAX, HTML, CSS
- Back-End Framework: Groovy-on-Grails
- Test: Groovy-base unit tests, Selenium

#### Users 
There are three types of users:
- Super Users: Service Station supervisor who could create and update service events and non-profit organizations and approve students service hours records.
- Admin Users: Service Station student workers. Admin users could approve or reject service hour records submitted by students.
- Students: Students could create, modify or delete service hour records for themselves and view report about their service hours.

#### Service Hour Related Classes
- Service Hour Records: An *ServiceHour* class documents a student's ID and name, the number of hours participated in a particular service event and some additional information about this event. After a service event, student users will create their hour records and admin users will review and approve these records. 

- Service Events: A *Event* class includes related information of a service event, such as event name, pariticpated non-profit organizations, and event date. Super users will create and update these events.

- Campus Organizations: A *CampusOrg* class includes 

- Non-profit Agencys: An *CommAg* class includes related information about a local non-profit agency, such as agency name, agency mission, and agency contact. Super users will create and update these organizations.

### Workflow



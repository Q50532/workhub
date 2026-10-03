Create database IF NOT EXISTS workhub;
Use workhub;
Create Table users(
	id Int Primary Key Auto_Increment,
	username Varchar(50) Not Null,
    email Varchar(100) Not Null);
    Describe users;
Create Table tickets(
	id Int Primary key Auto_Increment,
    title Varchar(150) Not Null,
    description Text Not Null,
    category Varchar(50) Not NUll,
    
    created_by Int Not NUll,
    assigned_to Int,
    
    created_at Datetime Not Null,
    status Varchar(30) Not Null Default 'OPEN',
    resolution Text);
    DESCRIBE tickets;
    
Alter Table tickets
Add Constraint fk_ticket_creator
Foreign Key (created_by) References users(id);

Alter Table tickets
Add Constraint fk_ticket_assignee
Foreign Key (assigned_to) References users(id);

Alter Table tickets
Modify created_at Datetime Not Null Default Current_Timestamp;

Alter Table tickets
Modify status Varchar(30) Not Null Default 'Open';

Insert Into users(username,email)
Values('Tom','tom@test.com');
select * from users;

select* from tickets;

Create Table comments(
	id Int Primary Key Auto_increment,
    ticket_id Int Not Null,
    author_id Int Not Null,
    content Text Not Null,
    created_at Datetime Not Null Default Current_Timestamp);
Alter Table comments
Add constraint fk_comment_ticket_id
Foreign Key (ticket_id) References tickets(id);
Alter Table comments
Add constraint fk_comment_author_id
Foreign Key (author_id) References users(id);

Describe comments;

CREATE TABLE ticket_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ticket_id INT NOT NULL,
    old_status VARCHAR(30) NOT NULL,
    new_status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ticket_log_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
);
describe ticket_logs;

    

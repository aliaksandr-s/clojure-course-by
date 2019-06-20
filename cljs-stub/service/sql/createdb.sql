create database testdb;
grant all on testdb.* to testdb@localhost identified by 'testdb';
create table testdb.article(id int auto_increment primary key, title varchar(1024), body text);

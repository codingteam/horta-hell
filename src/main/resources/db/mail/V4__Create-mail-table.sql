create table mail (
  id integer auto_increment primary key,
  room varchar(255),
  sender varchar(255),
  receiver varchar(255),
  message text
)
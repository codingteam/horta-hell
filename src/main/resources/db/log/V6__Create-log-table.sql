create table mail (
  id integer auto_increment primary key,
  time timestamp,
  room varchar(255),
  sender varchar(255),
  type varchar(32)
    check (type in ('enter', 'leave', 'message')),
  message text
)
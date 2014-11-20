create table KarmaChanges (
  id integer auto_increment primary key,
  room varchar(255),
  member varchar(255),
  changetime timestamp
);
create index KarmaChanges_room_member on KarmaChanges(room, member);
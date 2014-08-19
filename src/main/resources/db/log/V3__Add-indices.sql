create index log_time on log(time);
create index log_room on log(room);
create index log_room_sender on log(room, sender);
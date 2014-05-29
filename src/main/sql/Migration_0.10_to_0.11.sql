-- This script will help you to migrate existing 0.10 horta installation to 0.11.
-- 1. Update the horta code.
-- 2. Start horta.
-- 3. Make sure it initialized the databases for every plugin (you can do it by executing the wtf command).
-- 4. Stop horta.
-- 5. Execute the following script:
update "pet_version" set "success" = true;
update "mail_version" set "success" = true;
update "wtf_version" set "success" = true;
update "log_version" set "success" = true;

drop table "schema_version";
cd $PSScriptRoot
rm .\.vagrant\machines\default\virtualbox\synced_folders # To fix the bug in Vagrant 1.7.2
vagrant up --provision

# -*- mode: ruby -*-

VAGRANTFILE_API_VERSION = "2"

$script = <<SCRIPT
apt-get update
apt-get install -y openjdk-7-jdk scala curl git
wget -O /tmp/sbt.deb http://repo.scala-sbt.org/scalasbt/sbt-native-packages/org/scala-sbt/sbt/0.13.0/sbt.deb --no-verbose
dpkg -i /tmp/sbt.deb
update-alternatives --set java /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java
SCRIPT

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "precise64"
  config.vm.box_url = "http://files.vagrantup.com/precise64.box"

  # Configure the horta log directory so it will read MUC logs from
  # there:
  config.vm.synced_folder ENV['HORTA_LOGS'], "/horta_logs"

  config.vm.provider "virtualbox" do |vb|
    vb.customize ["modifyvm", :id, "--memory", "1024"]
  end

  config.vm.provision :shell, :inline => $script
end

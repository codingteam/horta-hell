# -*- mode: ruby -*-

Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.network :forwarded_port, guest: 9000, host: 8059 # Change 8059 to the port you need

  # Configure the horta log directory so it will read MUC logs from
  # there:
  config.vm.synced_folder ENV['HORTA_LOGS'] || "", "/horta_logs"

  config.vm.provider "virtualbox" do |vb|
    vb.customize ["modifyvm", :id, "--memory", "1024"]
  end

  # Disable the default ssh forwarding and enable another one
  config.vm.network :forwarded_port, guest: 22, id: "ssh", disabled: true
  config.vm.network :forwarded_port, guest: 22, host: 2202

  config.vm.provision "shell", inline: "command -v chef-solo >/dev/null 2>&1 || { curl -L https://www.chef.io/chef/install.sh | bash ; }"
  config.vm.provision "chef_solo" do |chef|
    chef.add_recipe "horta-hell"
  end
end

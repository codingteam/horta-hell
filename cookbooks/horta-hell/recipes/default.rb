#
# Cookbook Name:: horta-hell
# Recipe:: default
#
# Copyright (c) 2015 Friedrich von Never, All Rights Reserved.

package 'openjdk-7-jdk'
template '/etc/init/horta-hell.conf' do
  source 'horta-hell.conf.erb'
end

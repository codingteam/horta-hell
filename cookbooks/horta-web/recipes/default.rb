#
# Cookbook Name:: horta-web
# Recipe:: default
#
# Copyright (c) 2015 Friedrich von Never, All Rights Reserved.

package 'openjdk-7-jdk'
template '/etc/init/horta-web.conf' do
  source 'horta-web.conf.erb'
end

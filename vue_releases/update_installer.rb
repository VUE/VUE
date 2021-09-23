#!/usr/bin/env /usr/bin/ruby
require "/usr/local/atsys/jenkins/vue_releases/installer_modify.rb"
require "date"

props = InstallerModify.new(ARGV[0], ARGV[1])
props.save

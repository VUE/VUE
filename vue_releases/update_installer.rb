#!/usr/bin/env /usr/bin/ruby
require "./installer_modify.rb"
require "date"

props = InstallerModify.new(ARGV[0], ARGV[1])
props.save

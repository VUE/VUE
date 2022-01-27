#!/usr/bin/env /usr/bin/ruby
require File.expand_path("./installer_modify.rb", __FILE__)
require "date"

props = InstallerModify.new(ARGV[0], ARGV[1])
props.save

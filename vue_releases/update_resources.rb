#!/usr/bin/env /usr/bin/ruby
require "/usr/local/atsys/jenkins/vue_releases/java_props.rb"
require "date"

props = JavaProps.new(ARGV[0])
props.write_property "vue.version", ARGV[1]
date_mark = "2003-" + Date.today.strftime("%Y")
props.write_property "vue.build.date",date_mark
props.save

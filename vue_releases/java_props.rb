class JavaProps
  attr :file, :properties

#Takes a file and loads the properties in that file
def initialize file
 @file = file
 @properties = {}
# IO.foreach(file) do |line|
#   @properties[$1.strip] = $2 if line =~ /([^=]*)=(.*)\/\/(.*)/ || line =~ /([^=]*)=(.*)/
#   @properties[$1.strip] = $2 if line =~ /([^=]*)=(.*)\/\/(.*)/ || line =~ /([^=]*)=(.*)/
# end
File.open(file, 'r') do |properties_file|
      properties_file.read.each_line do |line|
        line.strip!
        if (line[0] != ?# and line[0] != ?=)
          i = line.index('=')
          if (i)
            @properties[line[0..i - 1].strip] = line[i + 1..-1].strip
          else
            @properties[line] = ''
          end
        end
      end
    end
end
  
#Helpfull to string
def to_s
 output = "File Name #{@file} \n"
 @properties.each {|key,value| output += "#{key}=#{value}\n" }
  output
end

#Write a property
def write_property (key,value)
  @properties[key] = value
end

#Save the properties back to file
def save
  file = File.new(@file,"w+")
  @properties.each {|key,value| file.puts "#{key}=#{value}\n" }
end

end

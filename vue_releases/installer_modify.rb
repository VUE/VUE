class InstallerModify
  attr :file, :version

@properties = {}
@max_lines = 0

#Takes a file and loads the properties in that file
def initialize(file, version)
 @file = file
 @version = version
 @properties = {}
 index = 0
 @max_lines = 0
 IO.foreach(file) do |line|
   @properties[index] = line
   index +=1
 end
 @max_lines = index
 prop_define = '!define PRODUCT_VERSION "' + @version + '"'
 write_property(5,prop_define)
end

#Write a property
def write_property (key,value)
  @properties[key] = value
end

#Save the properties back to file
def save
  file = File.new(@file,"w+")
  index =0
  while index < @max_lines do
    file.puts @properties[index]
    index +=1
  end
end

end

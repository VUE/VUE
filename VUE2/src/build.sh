echo No longer used -- build with Apache Ant.
exit
set -x
javac -d /VUE/classes tufts/google/*.java
javac -d /VUE/classes tufts/oki/shared/*.java
javac -d /VUE/classes tufts/oki/authentication/*.java
javac -d /VUE/classes tufts/oki/authorization/*.java
javac -d /VUE/classes tufts/oki/hierarchy/*.java
javac -d /VUE/classes tufts/oki/localFiling/*.java
javac -d /VUE/classes tufts/oki/removeFiling/*.java
javac -d /VUE/classes tufts/oki/dr/fedora/*.java
javac -d /VUE/classes tufts/vue/action/*.java tufts/vue/gui/*.java tufts/vue/filter/*.java tufts/vue/beans/*.java tufts/vue/shape/*.java tufts/vue/*.java

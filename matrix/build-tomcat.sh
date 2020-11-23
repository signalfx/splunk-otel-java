rm -r build
mkdir -p build
for tomcat in "7" "8" "9" "10"; do
  for jdk in "8" "11" "15"; do
    sed -e "s/<JDK>/${jdk}/" -e "s/<TOMCAT>/$tomcat/" tomcat-dockerfile.template > build/tomcat-$tomcat-$jdk.dockerfile
    docker build -t splunk-tomcat:$tomcat-jdk$jdk -f build/tomcat-$tomcat-$jdk.dockerfile .
  done
done

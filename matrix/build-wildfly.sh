rm -r build
mkdir -p build
for wildfly in "13.0.0.Final" "17.0.1.Final" "21.0.0.Final"; do
  for jdk in "8" "11" "15"; do
    sed -e "s/<JDK>/${jdk}/" -e "s/<WILDFLY>/$wildfly/" wildfly-dockerfile.template > build/wildfly-$wildfly-$jdk.dockerfile
    docker build -t splunk-wildfly:$wildfly-jdk$jdk -f build/wildfly-$wildfly-$jdk.dockerfile .
  done
done

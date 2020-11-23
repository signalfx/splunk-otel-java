rm -r build
mkdir -p build
for jetty in "9.4" "10.0.0.beta3" "11.0.0.beta3"; do
  for jdk in "8" "11" "15"; do
    sed -e "s/<JDK>/${jdk}/" -e "s/<JETTY>/$jetty/" jetty-dockerfile.template > build/jetty-$jetty-$jdk.dockerfile
    docker build -t splunk-jetty:$jetty-jdk$jdk -f build/jetty-$jetty-$jdk.dockerfile .
  done
done

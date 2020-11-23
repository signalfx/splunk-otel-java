rm -r build
mkdir -p build
for payara in "5.2020.6" "5.2020.6-jdk11"; do
  sed -e "s/<PAYARA>/$payara/g" payara-dockerfile.template >build/payara-$payara.dockerfile
  docker build -t splunk-payara:$payara -f build/payara-$payara.dockerfile .
done

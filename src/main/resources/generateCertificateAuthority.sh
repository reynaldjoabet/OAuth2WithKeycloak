VALIDITY_DAYS=3650
CA_PRIVATE_KEY_FILE="ca-privateKey"
CA_CERT_FILE="ca-cert" 


openssl req -new -x509 -keyout $CA_PRIVATE_KEY_FILE -out $CA_CERT_FILE -days $VALIDITY_DAYS

#### Example Values ####
# Passphrase: password
# Country Name: Cameroon
# State or Province: Northwest
# City: Bamenda
# Organization Name: Emi Consulting
# Organizational Unit Name: Software Consulting
# Common Name: Root CA
# Email: learner@scala.com
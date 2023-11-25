VALIDITY_DAYS=3650
CA_PRIVATE_KEY_FILE="ca-privateKey"
CA_CERT_FILE="ca-cert" 
#example.com
COMMON_NAME=$1 
ORGANIZATIONAL_UNIT="Software Consulting"
ORGANIZATION="Emi Consulting"
CITY="Bamenda"
STATE="Northwest"
COUNTRY="Cameroon"
CA_ALIAS="ca-root"


# Generate Keystore with Private Key
keytool -keystore $COMMON_NAME.keystore.p12 -alias $COMMON_NAME -validity $VALIDITY_DAYS -genkey -keyalg RSA -dname "CN=$COMMON_NAME, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE, C=$COUNTRY"

# Generate Certificate Signing Request (CSR) using the newly created KeyStore
keytool -keystore $COMMON_NAME.keystore.p12 -alias $COMMON_NAME -certreq -file $COMMON_NAME.csr 

# Sign the CSR using the custom CA
openssl x509 -req -CA ca-cert -CAkey ca-privateKey -in $COMMON_NAME.csr -out $COMMON_NAME.signed -days $VALIDITY_DAYS -CAcreateserial
#A certificate chain has the following components:
#A root CA certificate
#One or more intermediate certificates
#Client/server certificate signed by the intermediate CA certificate
# Import ROOT CA certificate into Keystore
# the client needs the CA certificate to validate the client 
keytool -keystore $COMMON_NAME.keystore.p12 -alias $CA_ALIAS -importcert -file $CA_CERT_FILE

# Import newly signed certificate into Keystore
# needs its own certificate to verify its own identity
keytool -keystore $COMMON_NAME.keystore.p12 -alias $COMMON_NAME -importcert -file $COMMON_NAME.signed

# Clean-up 
rm $COMMON_NAME.csr
rm $COMMON_NAME.signed
rm ca-cert.srl
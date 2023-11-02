# OAuth2WithKeycloak
implementing OpenID Connect for SPAs in Javascript (React, Angular, Vue...)is no longer recommended for the following reasons:
1. Using access tokens in the browser has more security risks than using secure cookies.
2. A SPA is a public client and cannot keep a secret, as such a secret would be part of the JavaScript and could be accessible to anyone inspecting the source code.
3. Recent browser changes to prevent tracking may result in 'third-party cookies' being dropped.
4. It is not possible to store something securely in the browser for a long period of time, as it can be stolen by various attacks.

// This are different origins since subdomain are different
1. https://developer.mozilla.org
2. https://mozilla.org

// This are also different origins since port number is different
1. https://localhost:5001
2. https://localhost:7001

A domain name typically has two parts: The top-level domain (TLD) is the extension, such as .com or .org, and the second-level domain (SLD) is the unique part of the domain name, often a business or brand name. In the hubspot.com example, com is the TLD and hubspot is the SLD.The subdomain is what goes before the SLD. The most common subdomain is www.

The backend for frontend pattern for authentication helps mitigate a risk associated with negotiating and handling access tokens from public clients.This pattern makes use of OpenIDConnect to request and receive identity information about authenticated users.BFFs are often used in banking and healthcare scenarios

This means that the spa frontend application must be deployed on the same domain(on the domain) and the backend api(on a subdomain). For instance  `www.example.com` for the SPA, and `api.example.com` for the backend.This enables cookies issued to be first party and prevents them being dropped by browsers. The cookies should also use the SameSite=strict parameter, to maintain a high level of security.This makes the cookie secure and also not shared across different domains, hence why the frontend and the backend need to be on the same top-level domain; request can still be same-site even if it's issued cross-origin
This hides the complexity of authorization flows from the SPA. A simple API can be exposed to the SPA, and the SPA does not need to be aware of the security details. 

![Getting Started](./1_Lcb7ku4Qx1sfSaPlr45wJg.webp)

## with BFF we can:
1. statefull - stores tokens in memory and uses a session to manage them(redis).
2. stateless - stores the tokens in encrypted HTTP-only, same-page cookies.
### There are two main ways to map the token to the cookie:
1. Keep a map of cookie -> token relations( redis)
  In this case, the cookie can be an opaque string.
2. Encrypting the token and returning it as a cookie

![Getting Started](./b001f5aa-image5-1024x442.jpg)

![Getting Started](./BFF-Sequence.png)


## Back Channel logout
In back-channel logout, logout tokens sent directly to your application contain claims to determine which session to end. To use back-channel logout, your application must be able to store and track the session information, including the session ID (sid) claim 
obtained during user authentication so that its local session can be terminated later during back-channel logout flow via a logout token.

Back-channel communications between applications and identity providers (IdP) use the publish–subscribe pattern.Applications register HTTP(s) webhooks to receive logout tokens when the identity provider requests session logout.Back-channel communications do not involve the end-user’s browser, but use a backend to receive and synchronize the application’s session status.
  
Applications cannot rely on session cookies to determine which session to terminate when communications are performed via the back-channel. Rather, the service depends on a shared session identifier (sid) claim on ID and logout tokens.

When end-users successfully authenticate with Auth0 during login, the authorization server assigns an access, ID, and logout token. The ID and logout token contain the claims you need in the back-channel logout workflow.

![Getting Started](./backchannellogout.png)
             
## The "__Host-" Prefix
If a cookie's name begins with a case-sensitive match for the string __Host-, then the cookie will have been set with a Secure attribute, a Path attribute with a value of /, and no Domain attribute.
The __Host- prefix expects cookies to fulfill the following conditions:

1. The cookie must be set with the Secure attribute.
2. The cookie must be set from a URI considered secure by the user agent.
3. Sent only to the host who set the cookie and MUST NOT include any Domain attribute.
4. The cookie must be set with the Pathattribute with a value of / so it would be sent to every request to the host.

This combination yields a cookie that hews as closely as a cookie can to treating the origin as a security boundary. The lack of a Domain attribute ensures that the cookie's host-only-flag is true, locking the cookie to a particular host, rather than allowing it to span subdomains. Setting the Path to / means that the cookie is effective for the entire host, and won't be overridden for specific paths. The Secure attribute ensures that the cookie is unaltered by non-secure origins, and won't span protocols.
 If cookie has __Host- prefix e.g. Set-Cookie: __Host-token=RANDOM; path=/; Secure then the cookie:

1. Cannot be (over)written from another subdomain.
2. Must have the path of /.
3. Must be marked as Secure (i.e, cannot be sent over unencrypted HTTP).

Ports are the only piece of the origin model that __Host- cookies continue to ignore.

For example, the following cookies would always be rejected:

Set-Cookie: __Host-SID=12345
Set-Cookie: __Host-SID=12345; Secure
Set-Cookie: __Host-SID=12345; Domain=site.example
Set-Cookie: __Host-SID=12345; Domain=site.example; Path=/
Set-Cookie: __Host-SID=12345; Secure; Domain=site.example; Path=/
While the following would be accepted if set from a secure origin (e.g. "https://site.example/"), and rejected otherwise:

Set-Cookie: __Host-SID=12345; Secure; Path=/

CNAME records map a domain name to another (canonical) domain name

1. [Auth0](https://auth0.com/docs/authenticate/login/logout/back-channel-logout)
2. [cloudentity](https://cloudentity.com/developers/blog/adding-oauth-proxy-bff-component-to-spa/)
3. [SameSite Restrictions](https://portswigger.net/web-security/csrf/bypassing-samesite-restrictions)


4. [DNS CName Record](https://www.cloudflare.com/learning/dns/dns-records/dns-cname-record/)

5. [DNS A Record](https://www.cloudflare.com/learning/dns/dns-records/dns-a-record/)
 
6. [DNS Server](https://www.cloudflare.com/learning/dns/dns-server-types/#authoritative-nameserver)

7. [Keystore and Trustsore](https://www.baeldung.com/java-keystore-truststore-difference)

8. [OAuth](https://www.shuttle.rs/blog/2023/08/30/using-oauth-with-axum)

## Certificate
``keytool -genkeypair -alias senderKeyPair -keyalg RSA -keysize 2048 
  -dname "CN=Baeldung" -validity 365 -storetype JKS 
  -keystore sender_keystore.jks -storepass changeit``

This creates a private key and its corresponding public key for us. The public key is wrapped into an X.509 self-signed certificate which is wrapped in turn into a single-element certificate chain. We store the certificate chain and the private key in the Keystore file sender_keystore.jks, which we can process using the KeyStore API.

  ``keytool -genkey -alias serverkey -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -keystore serverkeystore.p12 -storepass password -ext san=ip:127.0.0.1,dns:localhost -validity 3650 -keypass password``

A SAN or subject alternative name is a structured way to indicate all of the domain names and IP addresses that are secured by the certificate.Originally, SSL certificates only allowed the designation of a single host name in the certificate subject called Common Name.The common name represents the host name that’s covered by the SSL certificate. The most common example is a single certificate covering both the root domain and the www subdomain. In fact, it’s common to reuse the same SSL certificate for example.com and www.example.com.
We use the keytool -ext option to set the Subject Alternative Names (SAN) to define the local hostname/IP address that identifies the server
Next, we export the certificate to the file server-certificate.pem:

``keytool -exportcert -keystore serverkeystore.p12 -alias serverkey -storepass password -rfc -file server-certificate.pem``
When using a self-signed certificate, we need only to export it from the Keystore file. We can do this with the exportcert command:

``keytool -exportcert -alias senderKeyPair -storetype JKS 
  -keystore sender_keystore.jks -file 
  sender_certificate.cer -rfc -storepass changeit``

  Public certificate can have the following common file extensions:

1. “.crt” extension: Certificate may be encoded as binary DER Or ASCII PEM.
2. “.der” extension: Certificate encoded with binary DER.
3. “.pem” extension: Certificate encoded with ASCII (Base64).

## Back-Channel logout
A back-channel logout takes place between Keycloak and its clients. Keycloak detects a user's logout and sends a request containing a logout token to all clients where the user is logged in.

Back-channel logout operates as follows:

1. The user presses the logout button in Application 01.
2. Application 01 clears out the user's session while informing Keycloak that the user is logging out.
3. Keycloak invokes Application 02's logout endpoint, asking it to remove the user's session.





  



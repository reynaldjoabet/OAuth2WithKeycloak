# OAuth2WithKeycloak
The backend for frontend pattern for authentication helps mitigate a risk associated with negotiating and handling access tokens from public clients
This pattern makes use of OpenIDConnect to request and receive identity information about authenticated users

This means that the spa frontend application must be deployed one the same domain(onn the domain) as the backend api(on a subdomain). For instance  www.example.com for the SPA, and api.example.com for the backend.This enables cookies issued to be first party and prevents them being dropped by browsers. The cookies should also use the SameSite=strict parameter, to maintain a high level of security.
This hides the complexity of authorization flows from the SPA. A simple API can be exposed to the SPA, and the SPA does not need to be aware of the security details
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

[cloudentity](https://cloudentity.com/developers/blog/adding-oauth-proxy-bff-component-to-spa/)


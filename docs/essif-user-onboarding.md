# ESSIF User Onboarding

Specification:
https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/User+Onboarding+API

Onboarding proceeds as follows:

    User visits the EBSI Onboarding Service web page
    The user needs to prove he/she is not a robot. This step is required to prevent malicious onboarding to EBSI. In EBSI V2.0 we support two approaches:
        EU Login (with email or eID)
        reCAPTCHA challenge (a simple "I'm not a robot" proof)
    The user provides login credentials or solves the challenge.
    EU Login/reCAPTCHA service manage the process
    The onboarding service issues a session token
    The user is redirected to the DID Authentication page
        The user needs to have the session token
    The user can perform DID Authentication using
        Web Wallet
        Wallet Library
        Mobile Wallet
    The user receives the authentication request
    The user creates and sends the authentication response to the onboarding service
    The onboarding service verifies the Authentication Response and issues a Verifiable Authorisation
    The user receives the Verifiable Authorisation


GET
https://ecas.ec.europa.eu/cas/redirecting-to/EBSI?loginRequestId=ECAS_LR-1079622-iKnKkRdSC0zXlzbAgam31nVZNpV0fflDMKqh2vXVQHT8gsnj0dOhPIGQnBnX1fSwYQwvB6x7hLORHOtxYmqX1p-jpJZscgsw0KzKiZJN5vgPN-R5zuWnkIui5F0zwCzuzWuzHjTWCkYXHqhS4vsl7mb5eAEmcBM3zzXJ9V0fzmi0OwWi0F1VV3lN0qQRmaMHe6FVRE

Location: https://app.ebsi.xyz/wallet/credentials?ticket=ST-851759-O9hQiNjpGATy91knG5EyAIIGOV99SHVe9Z0kyaJKcszcCB6HMEvCzJDr3WIUE37H0FhiKOYEmJawbAkkSPefhx-jpJZscgsw0KIEKaRPg58F4-HEZbCHeUUqfEXXGWtWjurtidPzM2ioGXhGgZWMWvThexo39VApvcaPPF4e8eQDNUdCpczkTzlUF0V2YofX2nM0G

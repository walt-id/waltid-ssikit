Execute mitm (https://mitmproxy.org/) like this to trace EBSI WCT calls and inject conformance headers:

./mitmdump -s mitm-save.py -H "/Conformance/286dc8c9-15ce-4f4b-a32b-8ce5a5b7c4f5" -m reverse:https://api.conformance.intebsi.xyz -p 8080

Get Bearer Token from https://app.preprod.ebsi.eu/users-onboarding/ and save to: wct_bearer_token

Run WCTTest against mitm reverse proxy, by setting EBSI_WCT_ENV = "http://localhost:8080"

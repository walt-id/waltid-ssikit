import http from 'k6/http';
import { check } from 'k6';

export const options = {
    thresholds: {
        http_req_failed: ['rate==0'], // not a single http request may fail
        http_req_duration: ['p(95)<10'], // 95% of requests have to be below 10ms
        checks: ['rate==1'], // all checks must pass
    },
    vus: 24,
    duration: '60s',
};

export default function () {
    const payload = `{"templateId":"VerifiableId","config":{"issuerDid":"did:key:z6Mkr6T1hhzQRt3ySK4SxMQAAgxNUprhx8VYZwk6nyHeN1kQ","subjectDid":"did:key:z6Mkr6T1hhzQRt3ySK4SxMQAAgxNUprhx8VYZwk6nyHeN1kQ"}}`

    const res = http.post('http://127.0.0.1:7001/v1/credentials/issue', payload, {
        headers: {
            'Content-Type': 'application/json',
        },
    });

    check(res, {
        'is status 200': (r) => r.status === 200,
        'verify JWT': (r) => r.body.startsWith("ey")
    })
}

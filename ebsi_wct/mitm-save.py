from mitmproxy.net.http.http1.assemble import assemble_request, assemble_response

f = open('output.txt', 'w')

def response(flow):
    f.write('\n')
    f.write(assemble_request(flow.request).decode('utf-8'))
    f.write('\n')
    f.write('\n')
    f.write(assemble_response(flow.response).decode('utf-8', 'replace'))
    f.write('\n')
    f.flush()

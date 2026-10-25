// Synthetic OpenAI-compatible response for the existing AstropodsClient contract.
// No model is run here; this verifies HTTP wiring and confirmed-history serialization.
const http = require('node:http');
http.createServer((req, res) => {
  if (req.method !== 'POST' || req.url !== '/v1/chat/completions') {
    res.writeHead(501).end('Endpoint not modelled');
    return;
  }
  let body = '';
  req.on('data', chunk => { body += chunk; });
  req.on('end', () => {
    try {
      const request = JSON.parse(body);
      const input = JSON.parse(request.messages[1].content);
      if (input.transaction.amount !== '1850.00'
          || input.confirmedHistory[0].confirmedType !== 'SALARY') {
        res.writeHead(400).end('Expected synthetic transaction and confirmed salary history');
        return;
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ choices: [{ message: { content: JSON.stringify({
        type: 'SALARY', explanation: 'A prior salary from this sender was confirmed by the user.',
        reviewRequired: true, question: 'Is this your main salary for the next period?'
      }) } }] }));
    } catch {
      res.writeHead(400).end('Invalid request');
    }
  });
}).listen(process.env.PORT);

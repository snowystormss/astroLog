const https = require('https');
https.get('https://search.maven.org/solrsearch/select?q=swisseph&rows=10&wt=json', (res) => {
  let data = '';
  res.on('data', chunk => data += chunk);
  res.on('end', () => {
    let j = JSON.parse(data);
    j.response.docs.forEach(d => console.log(d.id + ' : ' + d.latestVersion));
  });
}).on('error', console.error);

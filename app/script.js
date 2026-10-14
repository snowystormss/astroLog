const https = require('https');
https.get('https://api.github.com/repos/th-mack/swisseph-java/contents/build.gradle', {
    headers: { 'User-Agent': 'node.js', 'Accept': 'application/vnd.github.v3.raw' }
}, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => console.log(data));
}).on('error', console.error);

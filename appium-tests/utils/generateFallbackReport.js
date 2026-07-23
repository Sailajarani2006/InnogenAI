const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const resultsFile = path.join(__dirname, '..', '.wdio-results.jsonl');

if (!fs.existsSync(resultsFile)) {
    console.log('No results file found, generating fallback entry...');
    fs.writeFileSync(resultsFile, JSON.stringify({
        title: 'Fatal Execution Crash',
        parent: 'CI Pipeline',
        passed: false,
        duration: 0,
        error: 'The WebdriverIO process exited prematurely or crashed before tests could run.'
    }) + '\n');
}

console.log('Running standard HTML/Excel reporter over fallback data...');
try {
    execSync('node utils/generateHtmlReport.js', { stdio: 'inherit', cwd: path.join(__dirname, '..') });
} catch (e) {
    console.error('Fallback report generation failed:', e);
}

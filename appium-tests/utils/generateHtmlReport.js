const fs = require('fs');
const path = require('path');
const { generateExcelReport } = require('./xlsxReporter');

const resultsFile = path.join(__dirname, '..', '.wdio-results.jsonl');
const htmlOutputFile = path.join(__dirname, '..', 'execution-report.html');
const excelOutputFile = path.join(__dirname, '..', 'InnoGenAI_Appium_Test_Report.xlsx');

async function main() {
    console.log('[HTML Reporter] Reading WDIO results...');
    if (!fs.existsSync(resultsFile)) {
        console.error('No results file found at', resultsFile);
        return;
    }

    const lines = fs.readFileSync(resultsFile, 'utf-8').split('\n').filter(Boolean);
    const results = lines.map(line => JSON.parse(line));

    // Generate Excel File
    await generateExcelReport(resultsFile, excelOutputFile);

    // Calculate Summary
    const totalTests = results.length;
    const passedTests = results.filter(r => r.passed).length;
    const failedTests = totalTests - passedTests;
    const passRate = totalTests > 0 ? ((passedTests / totalTests) * 100).toFixed(2) : 0;

    // Generate HTML
    let rowsHtml = '';
    results.forEach(r => {
        rowsHtml += `
            <tr class="${r.passed ? 'pass' : 'fail'}">
                <td>${r.parent}</td>
                <td>${r.title}</td>
                <td class="${r.passed ? 'text-success' : 'text-danger'}">${r.passed ? 'PASS' : 'FAIL'}</td>
                <td>${r.duration}</td>
                <td>${r.error || ''}</td>
            </tr>
        `;
    });

    const htmlContent = `
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Appium Test Report - InnoGenAI</title>
        <style>
            body { background-color: #121212; color: #e0e0e0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; }
            h1 { color: #bb86fc; }
            .summary-box { background: #1e1e1e; padding: 20px; border-radius: 8px; margin-bottom: 20px; border: 1px solid #333; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            th, td { padding: 12px; text-align: left; border-bottom: 1px solid #333; }
            th { background-color: #1e1e1e; color: #bb86fc; }
            .pass { border-left: 4px solid #03dac6; }
            .fail { border-left: 4px solid #cf6679; background-color: rgba(207, 102, 121, 0.1); }
            .text-success { color: #03dac6; font-weight: bold; }
            .text-danger { color: #cf6679; font-weight: bold; }
        </style>
    </head>
    <body>
        <h1>InnoGenAI Appium E2E Test Report</h1>
        <div class="summary-box">
            <p><strong>Total Tests:</strong> ${totalTests}</p>
            <p><strong>Passed:</strong> <span class="text-success">${passedTests}</span></p>
            <p><strong>Failed:</strong> <span class="text-danger">${failedTests}</span></p>
            <p><strong>Pass Rate:</strong> ${passRate}%</p>
        </div>
        <table>
            <thead>
                <tr>
                    <th>Category</th>
                    <th>Test Title</th>
                    <th>Status</th>
                    <th>Duration (ms)</th>
                    <th>Error</th>
                </tr>
            </thead>
            <tbody>
                ${rowsHtml}
            </tbody>
        </table>
    </body>
    </html>
    `;

    fs.writeFileSync(htmlOutputFile, htmlContent);
    console.log(`HTML report generated at: ${htmlOutputFile}`);

    // Generate GitHub Actions Summary
    if (process.env.GITHUB_STEP_SUMMARY) {
        const summaryContent = `
# Appium Test Results 📱
- **Total Tests:** ${totalTests}
- **Passed:** ${passedTests} ✅
- **Failed:** ${failedTests} ❌
- **Pass Rate:** ${passRate}%

See the artifacts for the full Excel and HTML reports!
        `;
        fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, summaryContent + '\n');
    }
}

main().catch(console.error);

const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');

async function generateExcelReport(resultsFilePath, outputPath) {
    if (!fs.existsSync(resultsFilePath)) {
        console.error('No results file found at', resultsFilePath);
        return;
    }

    const lines = fs.readFileSync(resultsFilePath, 'utf-8').split('\n').filter(Boolean);
    const results = lines.map(line => JSON.parse(line));

    const workbook = new ExcelJS.Workbook();
    
    // Sheet 1: Summary
    const summarySheet = workbook.addWorksheet('Summary');
    const totalTests = results.length;
    const passedTests = results.filter(r => r.passed).length;
    const failedTests = totalTests - passedTests;
    const passRate = totalTests > 0 ? ((passedTests / totalTests) * 100).toFixed(2) + '%' : '0%';

    summarySheet.columns = [
        { header: 'Metric', key: 'metric', width: 25 },
        { header: 'Value', key: 'value', width: 20 }
    ];
    summarySheet.addRows([
        { metric: 'Total Tests', value: totalTests },
        { metric: 'Passed Tests', value: passedTests },
        { metric: 'Failed Tests', value: failedTests },
        { metric: 'Pass Rate', value: passRate },
        { metric: 'Date Executed', value: new Date().toLocaleString() }
    ]);

    // Sheet 2: By Category
    const categorySheet = workbook.addWorksheet('By Category');
    categorySheet.columns = [
        { header: 'Category (Parent)', key: 'parent', width: 40 },
        { header: 'Total', key: 'total', width: 10 },
        { header: 'Passed', key: 'passed', width: 10 },
        { header: 'Failed', key: 'failed', width: 10 }
    ];

    const categoryMap = {};
    results.forEach(r => {
        if (!categoryMap[r.parent]) {
            categoryMap[r.parent] = { total: 0, passed: 0, failed: 0 };
        }
        categoryMap[r.parent].total++;
        if (r.passed) categoryMap[r.parent].passed++;
        else categoryMap[r.parent].failed++;
    });

    for (const [parent, stats] of Object.entries(categoryMap)) {
        categorySheet.addRow({ parent, ...stats });
    }

    // Sheet 3: Test Cases
    const testCasesSheet = workbook.addWorksheet('Test Cases');
    testCasesSheet.columns = [
        { header: 'Category', key: 'parent', width: 30 },
        { header: 'Test Title', key: 'title', width: 60 },
        { header: 'Status', key: 'status', width: 15 },
        { header: 'Duration (ms)', key: 'duration', width: 15 },
        { header: 'Error', key: 'error', width: 60 }
    ];

    results.forEach(r => {
        testCasesSheet.addRow({
            parent: r.parent,
            title: r.title,
            status: r.passed ? 'PASS' : 'FAIL',
            duration: r.duration,
            error: r.error || ''
        });
    });

    await workbook.xlsx.writeFile(outputPath);
    console.log(`Excel report generated at: ${outputPath}`);
}

module.exports = { generateExcelReport };

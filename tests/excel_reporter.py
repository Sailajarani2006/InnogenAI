import pytest
import pandas as pd
from datetime import datetime
import os

# Global list to store test results
test_results = []

@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    # execute all other hooks to obtain the report object
    outcome = yield
    rep = outcome.get_result()
    
    # We only want to log the actual test execution (not setup/teardown) unless setup failed
    if rep.when == "call" or (rep.when == "setup" and rep.failed):
        test_id = item.name
        
        # Determine module from the filename or marker
        module = item.fspath.basename
        
        # Determine type based on naming conventions or markers in the test
        test_type = "E2E"
        if "vuln" in test_id.lower() or "security" in test_id.lower() or "xss" in test_id.lower():
            test_type = "Vulnerability"
        elif "unit" in test_id.lower() or "component" in test_id.lower():
            test_type = "Unit"
            
        description = item.function.__doc__ or "No description provided"
        description = description.strip()
        
        status = "Pass" if rep.passed else "Fail" if rep.failed else "Skip"
        
        duration = round(rep.duration, 3)
        
        error_msg = ""
        if rep.failed:
            # Extract the core error message (last line of exception)
            error_msg = str(rep.longrepr).split('\n')[-1][:200]
            
        test_results.append({
            "Test ID": test_id,
            "Module": module,
            "Type": test_type,
            "Description": description,
            "Status": status,
            "Execution Time (s)": duration,
            "Error Message": error_msg
        })

def pytest_sessionfinish(session, exitstatus):
    """
    Hook executed after all tests finish. Generates the Excel report.
    """
    if not test_results:
        print("No test results to write.")
        return
        
    df = pd.DataFrame(test_results)
    
    # Generate timestamped filename
    timestamp = datetime.now().strftime("%Y-%m-%dT%H-%M-%S")
    report_name = f"E2E_Test_Report_PancreaScan_{timestamp}.xlsx"
    report_path = os.path.join(os.getcwd(), report_name)
    
    # Save to Excel
    try:
        # Requires pandas and openpyxl
        df.to_excel(report_path, index=False, engine='openpyxl')
        print(f"\n✅ Excel Test Report generated successfully: {report_path}")
    except Exception as e:
        print(f"\n❌ Failed to generate Excel report: {e}")
